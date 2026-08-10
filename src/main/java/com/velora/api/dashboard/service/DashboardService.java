package com.velora.api.dashboard.service;

import com.velora.api.common.util.MoneyUtils;
import com.velora.api.dashboard.dto.DashboardResponse;
import com.velora.api.dashboard.repository.DashboardQueries;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the admin dashboard.
 *
 * <p>The alerts are the part that matters. Numbers on a screen become wallpaper
 * within a week; a short list of things that need doing, in severity order, stays
 * useful. Every alert names an action rather than a fact.
 */
@Service
public class DashboardService {

    /** An order untouched this long is not "in progress", it is forgotten. */
    private static final int STALE_ORDER_HOURS = 24;

    /** A hold this old means an order is stuck and its stock is frozen with it. */
    private static final int STUCK_RESERVATION_HOURS = 48;

    /** Courier remittance beyond this is worth chasing. */
    private static final int COD_SETTLEMENT_DAYS = 7;

    /** Below this, deliveries are failing often enough to have a cause. */
    private static final BigDecimal MIN_DELIVERY_SUCCESS = BigDecimal.valueOf(80);

    private final DashboardQueries queries;

    public DashboardService(DashboardQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public DashboardResponse build() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.withDayOfMonth(1);

        DashboardResponse.Sales sales = buildSales(today, weekStart, monthStart);
        DashboardResponse.CodPosition cod = queries.unsettledCod();
        List<DashboardResponse.StatusCount> queues = queries.actionQueues();
        List<DashboardResponse.StaleOrder> stale = queries.staleOrders(STALE_ORDER_HOURS);
        List<DashboardResponse.LowStockItem> lowStock = queries.lowStock();
        int stuck = queries.stuckReservations(STUCK_RESERVATION_HOURS);
        List<DashboardResponse.TopProduct> top = queries.topProducts(monthStart, 10);
        DashboardResponse.DeliveryHealth delivery = queries.deliveryHealth(monthStart);
        DashboardResponse.CustomerStats customers = queries.customerStats(monthStart);
        DashboardResponse.CatalogStats catalog = queries.catalogStats();

        List<DashboardResponse.Alert> alerts =
                buildAlerts(cod, queues, stale, lowStock, stuck, delivery, catalog);

        return new DashboardResponse(
                sales, cod, queues, stale, lowStock, stuck, top,
                delivery, customers, catalog, alerts,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    // ------------------------------------------------------------------ sales

    private DashboardResponse.Sales buildSales(LocalDate today, LocalDate weekStart,
                                               LocalDate monthStart) {
        BigDecimal revenueMonth = queries.revenueSince(monthStart);
        int ordersMonth = queries.orderCountSince(monthStart);

        BigDecimal average = ordersMonth == 0
                ? MoneyUtils.ZERO
                : revenueMonth.divide(BigDecimal.valueOf(ordersMonth), 2, RoundingMode.HALF_UP);

        return new DashboardResponse.Sales(
                queries.revenueSince(today),
                queries.orderCountSince(today),
                queries.revenueSince(weekStart),
                queries.orderCountSince(weekStart),
                revenueMonth,
                ordersMonth,
                average);
    }

    // ----------------------------------------------------------------- alerts

    /**
     * Turns the numbers into a list of things to do.
     *
     * <p>Ordered by severity, and every message says what action to take. A dashboard
     * that reports "3 items low on stock" is describing; one that says "reorder 3
     * items" is useful.
     */
    private List<DashboardResponse.Alert> buildAlerts(
            DashboardResponse.CodPosition cod,
            List<DashboardResponse.StatusCount> queues,
            List<DashboardResponse.StaleOrder> stale,
            List<DashboardResponse.LowStockItem> lowStock,
            int stuckReservations,
            DashboardResponse.DeliveryHealth delivery,
            DashboardResponse.CatalogStats catalog) {

        List<DashboardResponse.Alert> alerts = new ArrayList<>();

        // ---- HIGH: money and stock that is actually wrong ----

        long outOfStock = lowStock.stream().filter(item -> item.available() <= 0).count();
        if (outOfStock > 0) {
            alerts.add(new DashboardResponse.Alert("HIGH", "STOCK",
                    "%d منتج نفد من المخزون ولا يظهر للعملاء — راجع التوريد"
                            .formatted(outOfStock),
                    "/admin/inventory/low-stock"));
        }

        if (cod.oldestDays() > COD_SETTLEMENT_DAYS) {
            alerts.add(new DashboardResponse.Alert("HIGH", "PAYMENT",
                    ("مبلغ %s ج.م لم يورّده الكوريير، وأقدم طلب مضى عليه %d يوم — "
                            + "طالب بالتسوية")
                            .formatted(cod.amount(), cod.oldestDays()),
                    "/admin/orders?paymentStatus=PENDING"));
        }

        if (stuckReservations > 0) {
            alerts.add(new DashboardResponse.Alert("HIGH", "STOCK",
                    ("%d حجز مخزون معلّق لطلبات لم تُشحن منذ يومين — "
                            + "البضاعة محجوزة ولا تُباع")
                            .formatted(stuckReservations),
                    "/admin/orders?status=CONFIRMED"));
        }

        // ---- MEDIUM: work piling up ----

        if (!stale.isEmpty()) {
            alerts.add(new DashboardResponse.Alert("MEDIUM", "ORDERS",
                    "%d طلب لم يتحرك منذ أكثر من 24 ساعة — راجعها".formatted(stale.size()),
                    "/admin/orders"));
        }

        queues.stream()
                .filter(queue -> "PENDING".equals(queue.status()) && queue.count() > 0)
                .findFirst()
                .ifPresent(queue -> alerts.add(new DashboardResponse.Alert("MEDIUM", "ORDERS",
                        "%d طلب بانتظار التأكيد الهاتفي".formatted(queue.count()),
                        "/admin/orders?status=PENDING")));

        queues.stream()
                .filter(queue -> "CONFIRMED".equals(queue.status()) && queue.count() > 0)
                .findFirst()
                .ifPresent(queue -> alerts.add(new DashboardResponse.Alert("MEDIUM", "ORDERS",
                        "%d طلب جاهز للتجهيز والشحن".formatted(queue.count()),
                        "/admin/orders?status=CONFIRMED")));

        if (delivery.successRatePercent().compareTo(MIN_DELIVERY_SUCCESS) < 0
                && delivery.delivered() + delivery.refused() >= 5) {
            // Only worth flagging once there is enough volume for the rate to mean
            // anything. Two refusals out of three is noise, not a trend.
            alerts.add(new DashboardResponse.Alert("MEDIUM", "DELIVERY",
                    ("نسبة نجاح التوصيل %s%% هذا الشهر — راجع أسباب الرفض")
                            .formatted(delivery.successRatePercent()),
                    "/admin/orders?status=REFUSED_ON_DELIVERY"));
        }

        // ---- LOW: worth knowing ----

        long runningLow = lowStock.stream().filter(item -> item.available() > 0).count();
        if (runningLow > 0) {
            alerts.add(new DashboardResponse.Alert("LOW", "STOCK",
                    "%d منتج قارب على النفاد".formatted(runningLow),
                    "/admin/inventory/low-stock"));
        }

        if (catalog.draftProducts() > 0) {
            alerts.add(new DashboardResponse.Alert("LOW", "CATALOG",
                    "%d منتج ما زال مسودة ولا يظهر في المتجر"
                            .formatted(catalog.draftProducts()),
                    "/admin/products?status=DRAFT"));
        }

        return alerts;
    }

    /** Days between two dates, for the front end's convenience. */
    public long daysSince(OffsetDateTime from) {
        return ChronoUnit.DAYS.between(from, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
