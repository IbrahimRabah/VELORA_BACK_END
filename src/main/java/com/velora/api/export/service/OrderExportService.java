package com.velora.api.export.service;

import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.common.util.MoneyUtils;
import com.velora.api.common.util.PhoneNormalizer;
import com.velora.api.export.dto.OrderExportFilter;
import com.velora.api.export.dto.PickingListView;
import com.velora.api.export.spec.OrderExportSpecifications;
import com.velora.api.order.domain.CustomerOrder;
import com.velora.api.order.domain.OrderItem;
import com.velora.api.order.domain.PaymentMethod;
import com.velora.api.order.domain.PaymentStatus;
import com.velora.api.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the two order exports.
 *
 * <p>They exist for different readers and therefore carry different columns:
 * <ul>
 *   <li><b>Accounting archive</b> — one row per order, totals, real numbers. For the
 *       owner and the accountant.</li>
 *   <li><b>Picking list</b> — one row per product line, grouped by order, with the
 *       address and the amount to collect. For whoever packs the boxes.</li>
 * </ul>
 *
 * <p>Putting both on one sheet produces a document that serves neither.
 */
@Service
public class OrderExportService {

    private static final Logger log = LoggerFactory.getLogger(OrderExportService.class);

    /** Beyond this the document stops being useful to a human. */
    private static final int MAX_ROWS = 5000;
    private static final int MAX_PICKING_ORDERS = 300;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;
    private final AccountingExcelWriter excelWriter;
    private final TemplateRenderer templateRenderer;
    private final PdfRenderer pdfRenderer;

    public OrderExportService(OrderRepository orderRepository,
                              AccountingExcelWriter excelWriter,
                              TemplateRenderer templateRenderer,
                              PdfRenderer pdfRenderer) {
        this.orderRepository = orderRepository;
        this.excelWriter = excelWriter;
        this.templateRenderer = templateRenderer;
        this.pdfRenderer = pdfRenderer;
    }

    // ------------------------------------------------------- accounting archive

    @Transactional(readOnly = true)
    public byte[] exportAccountingExcel(OrderExportFilter filter) {
        List<CustomerOrder> orders = findOrders(filter, MAX_ROWS);

        if (orders.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "No orders match these filters");
        }

        log.info("Exporting {} order(s) to Excel", orders.size());
        return excelWriter.write(orders, buildTitle("أرشيف الطلبات", filter));
    }

    // ------------------------------------------------------------ picking list

    /**
     * The picking list defaults to CONFIRMED orders — the ones ready to pack. Passing
     * no status would print delivered and cancelled orders too, which is worse than
     * useless in a warehouse.
     */
    @Transactional(readOnly = true)
    public byte[] exportPickingListPdf(OrderExportFilter filter) {
        OrderExportFilter effective = filter;
        if (filter.fulfillmentStatus() == null || filter.fulfillmentStatus().isBlank()) {
            effective = new OrderExportFilter(
                    filter.dateFrom(), filter.dateTo(), "CONFIRMED",
                    filter.paymentStatus(), filter.governorateId(), true);
        }

        List<CustomerOrder> orders = findOrders(effective, MAX_PICKING_ORDERS);

        PickingListView view = buildPickingView(orders, effective);
        String html = templateRenderer.render("picking-list", "view", view);

        log.info("Exporting picking list for {} order(s)", orders.size());
        return pdfRenderer.render(html);
    }

    // ------------------------------------------------------------------ internal

    private List<CustomerOrder> findOrders(OrderExportFilter filter, int limit) {
        Specification<CustomerOrder> spec = OrderExportSpecifications
                .placedFrom(filter.dateFrom())
                .and(OrderExportSpecifications.placedUntil(filter.dateTo()))
                .and(OrderExportSpecifications.hasFulfillmentStatus(filter.fulfillmentStatus()))
                .and(OrderExportSpecifications.hasPaymentStatus(filter.paymentStatus()))
                .and(OrderExportSpecifications.inGovernorate(filter.governorateId()))
                .and(OrderExportSpecifications.excludeCancelled(filter.shouldExcludeCancelled()));

        List<CustomerOrder> orders = orderRepository.findAll(
                spec, Sort.by(Sort.Direction.ASC, "placedAt"));

        if (orders.size() > limit) {
            log.warn("Export matched {} orders, capped at {}. Narrow the date range.",
                    orders.size(), limit);
            return orders.subList(0, limit);
        }

        // Items are lazy; touching them inside the read-only transaction loads them
        // before the template or the writer needs them.
        orders.forEach(order -> order.getItems().size());
        return orders;
    }

    private PickingListView buildPickingView(List<CustomerOrder> orders,
                                             OrderExportFilter filter) {
        List<PickingListView.OrderBlock> blocks = new ArrayList<>();
        int totalItems = 0;
        BigDecimal totalCod = MoneyUtils.ZERO;

        for (CustomerOrder order : orders) {
            List<PickingListView.LineItem> lines = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                lines.add(new PickingListView.LineItem(
                        item.getSku(),
                        item.getProductNameAr(),
                        item.getVariantSummary(),
                        item.getQuantity(),
                        item.getUnitPriceGross(),
                        item.getLineTotalGross()));
                totalItems += item.getQuantity();
            }

            // Only COD orders that have not been settled need collecting. Printing an
            // amount on a prepaid order is how a courier double-charges someone.
            BigDecimal codAmount = MoneyUtils.ZERO;
            if (order.getPaymentMethod() == PaymentMethod.COD
                    && order.getPaymentStatus() == PaymentStatus.PENDING) {
                codAmount = order.getGrandTotal();
                totalCod = totalCod.add(codAmount);
            }

            blocks.add(new PickingListView.OrderBlock(
                    order.getOrderNumber(),
                    order.getPlacedAt().format(DATE_TIME),
                    order.getContactName(),
                    PhoneNormalizer.toLocalFormat(order.getContactPhone()),
                    PhoneNormalizer.toLocalFormat(order.getContactAltPhone()),
                    order.getShipGovernorateName(),
                    order.formattedAddress(),
                    order.getShipLandmark(),
                    order.getCustomerNote(),
                    order.getPaymentMethod().name(),
                    codAmount,
                    order.totalQuantity(),
                    lines));
        }

        return new PickingListView(
                buildTitle("قائمة التحضير والشحن", filter),
                OffsetDateTime.now(ZoneOffset.UTC).format(DATE_TIME),
                blocks.size(),
                totalItems,
                MoneyUtils.round(totalCod),
                blocks);
    }

    /** A title that says what was actually exported, so a printed sheet is unambiguous. */
    private String buildTitle(String base, OrderExportFilter filter) {
        StringBuilder sb = new StringBuilder(base);

        LocalDate from = filter.dateFrom();
        LocalDate to = filter.dateTo();
        if (from != null && to != null) {
            sb.append(" — من ").append(from.format(DATE)).append(" إلى ").append(to.format(DATE));
        } else if (from != null) {
            sb.append(" — من ").append(from.format(DATE));
        } else if (to != null) {
            sb.append(" — حتى ").append(to.format(DATE));
        }
        return sb.toString();
    }

    /** Filename-safe suffix for the download. */
    public String fileSuffix(OrderExportFilter filter) {
        LocalDate from = filter.dateFrom();
        LocalDate to = filter.dateTo();
        if (from != null && to != null) {
            return from.format(DATE) + "_" + to.format(DATE);
        }
        return LocalDate.now().format(DATE);
    }
}
