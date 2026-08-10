package com.velora.api.customer.web;

import com.velora.api.common.dto.PageResponse;
import com.velora.api.customer.dto.CustomerDetailResponse;
import com.velora.api.customer.dto.CustomerSummaryResponse;
import com.velora.api.customer.service.CustomerAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Customers", description = "Customer list and history. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {

    private final CustomerAdminService customerAdminService;

    public AdminCustomerController(CustomerAdminService customerAdminService) {
        this.customerAdminService = customerAdminService;
    }

    @Operation(summary = "Customers with their order counts and lifetime value",
            description = """
                    Search matches a name, phone or email. A phone number is normalized
                    first, so `01012345678` finds the customer stored as `+201012345678` —
                    which is what staff will type when someone calls.

                    Cancelled orders are excluded from both the count and the total: a
                    customer who ordered six times and cancelled five has not spent
                    anything.
                    """)
    @GetMapping
    public PageResponse<CustomerSummaryResponse> list(
            @Parameter(description = "Name, phone or email")
            @RequestParam(required = false) String search,
            @Parameter(description = "spent_desc | orders_desc | recent_order | name")
            @RequestParam(required = false) String sort,
            @ParameterObject @PageableDefault(size = 25) Pageable pageable) {

        return customerAdminService.list(search, sort, pageable);
    }

    @Operation(summary = "One customer in full",
            description = """
                    Purchase history, saved addresses and recent orders.

                    `purchases.failedOrders` counts refusals and failed deliveries. With
                    cash on delivery that number decides how a customer is handled — a
                    repeat refuser is worth a phone call before dispatch, not after.
                    """)
    @GetMapping("/{customerId}")
    public CustomerDetailResponse get(@PathVariable Long customerId) {
        return customerAdminService.get(customerId);
    }
}
