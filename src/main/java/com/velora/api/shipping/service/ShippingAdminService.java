package com.velora.api.shipping.service;

import com.velora.api.common.exception.BusinessException;
import com.velora.api.common.exception.ErrorCode;
import com.velora.api.shipping.domain.ShippingRate;
import com.velora.api.shipping.domain.ShippingZone;
import com.velora.api.shipping.dto.ShippingRateRequest;
import com.velora.api.shipping.dto.ShippingZoneResponse;
import com.velora.api.shipping.repository.GovernorateRepository;
import com.velora.api.shipping.repository.ShippingRateRepository;
import com.velora.api.shipping.repository.ShippingZoneRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Editing shipping prices without touching SQL.
 *
 * <p>Courier prices change. Doing it through the API keeps the change audited and
 * stops a typo in a hand-written UPDATE from silently making delivery free.
 */
@Service
public class ShippingAdminService {

    private static final Logger log = LoggerFactory.getLogger(ShippingAdminService.class);

    private final ShippingZoneRepository zoneRepository;
    private final ShippingRateRepository rateRepository;
    private final GovernorateRepository governorateRepository;

    public ShippingAdminService(ShippingZoneRepository zoneRepository,
                                ShippingRateRepository rateRepository,
                                GovernorateRepository governorateRepository) {
        this.zoneRepository = zoneRepository;
        this.rateRepository = rateRepository;
        this.governorateRepository = governorateRepository;
    }

    @Transactional(readOnly = true)
    public List<ShippingZoneResponse> listZones() {
        List<ShippingZoneResponse> result = new ArrayList<>();

        for (ShippingZone zone : zoneRepository.findByActiveTrueOrderByIdAsc()) {
            ShippingRate rate = rateRepository.findByZoneIdAndActiveTrue(zone.getId())
                    .orElse(null);

            List<String> governorates = governorateRepository
                    .findByActiveTrueOrderByDisplayOrderAsc().stream()
                    .filter(g -> rateRepository.findForGovernorate(g.getId())
                            .map(r -> r.getZone().getId().equals(zone.getId()))
                            .orElse(false))
                    .map(g -> g.getNameAr())
                    .toList();

            result.add(new ShippingZoneResponse(
                    zone.getId(),
                    zone.getCode(),
                    zone.getNameAr(),
                    zone.getNameEn(),
                    rate == null ? null : rate.getBaseCost(),
                    rate == null ? null : rate.getFreeShippingOver(),
                    rate == null ? null : rate.getCodFee(),
                    rate == null ? 0 : rate.getDeliveryDaysMin(),
                    rate == null ? 0 : rate.getDeliveryDaysMax(),
                    zone.isActive(),
                    governorates));
        }
        return result;
    }

    /**
     * Sets the rate for a zone. Replaces the existing one rather than adding a
     * second, so a governorate can never match two competing prices.
     */
    @Transactional
    public Long saveRate(ShippingRateRequest request) {
        ShippingZone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Shipping zone not found"));

        if (request.deliveryDaysMin() != null && request.deliveryDaysMax() != null
                && request.deliveryDaysMin() > request.deliveryDaysMax()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "The minimum delivery time cannot be greater than the maximum");
        }

        ShippingRate rate = rateRepository.findByZoneIdAndActiveTrue(zone.getId())
                .orElseGet(() -> {
                    ShippingRate created = new ShippingRate();
                    created.setZone(zone);
                    return created;
                });

        BigDecimal previous = rate.getBaseCost();

        rate.setBaseCost(request.baseCost());
        rate.setMaxWeightGrams(request.maxWeightGrams());
        rate.setCostPerExtraKg(request.costPerExtraKg() == null
                ? BigDecimal.ZERO : request.costPerExtraKg());
        rate.setFreeShippingOver(request.freeShippingOver());
        rate.setCodFee(request.codFee() == null ? BigDecimal.ZERO : request.codFee());
        if (request.deliveryDaysMin() != null) {
            rate.setDeliveryDaysMin(request.deliveryDaysMin().shortValue());
        }
        if (request.deliveryDaysMax() != null) {
            rate.setDeliveryDaysMax(request.deliveryDaysMax().shortValue());
        }
        rate.setActive(true);

        ShippingRate saved = rateRepository.save(rate);

        // Price changes affect what every future customer pays. Worth a log line.
        log.info("Shipping rate for zone {} changed from {} to {}",
                zone.getCode(), previous, saved.getBaseCost());

        return saved.getId();
    }
}
