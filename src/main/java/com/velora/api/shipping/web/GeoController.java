package com.velora.api.shipping.web;

import com.velora.api.catalog.web.LocaleResolver;
import com.velora.api.shipping.dto.GovernorateResponse;
import com.velora.api.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Geography", description = "Egyptian governorates")
@RestController
@RequestMapping("/api/v1/geo")
public class GeoController {

    private final ShippingService shippingService;

    public GeoController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @Operation(summary = "List the 27 governorates with their shipping terms",
            description = """
                    Drives the address form and the shipping estimator.

                    Governorates with no configured rate come back with `served: false`
                    rather than being hidden — a customer should see their governorate
                    and be told we do not deliver there, not fail to find it.

                    City is free text: courier city lists differ per company, so it is
                    better captured as text until a courier is chosen.
                    """,
            security = {})
    @GetMapping("/governorates")
    public List<GovernorateResponse> governorates(HttpServletRequest request) {
        return shippingService.listGovernorates(LocaleResolver.resolve(request));
    }
}
