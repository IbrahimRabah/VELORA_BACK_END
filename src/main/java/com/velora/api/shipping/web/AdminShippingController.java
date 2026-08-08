package com.velora.api.shipping.web;

import com.velora.api.shipping.dto.ShippingRateRequest;
import com.velora.api.shipping.dto.ShippingZoneResponse;
import com.velora.api.shipping.service.ShippingAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Shipping", description = "Zones and rates. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/shipping")
public class AdminShippingController {

    private final ShippingAdminService shippingAdminService;

    public AdminShippingController(ShippingAdminService shippingAdminService) {
        this.shippingAdminService = shippingAdminService;
    }

    @Operation(summary = "List zones with their rates and covered governorates")
    @GetMapping("/zones")
    public List<ShippingZoneResponse> zones() {
        return shippingAdminService.listZones();
    }

    @Operation(summary = "Set a zone's rate",
            description = """
                    Replaces the zone's existing rate rather than adding a second one, so a
                    governorate can never match two competing prices.

                    Leave `freeShippingOver` null to never offer free delivery.
                    Leave `maxWeightGrams` null to ignore weight entirely.
                    """)
    @PutMapping("/rates")
    public Map<String, Long> saveRate(@Valid @RequestBody ShippingRateRequest request) {
        return Map.of("id", shippingAdminService.saveRate(request));
    }
}
