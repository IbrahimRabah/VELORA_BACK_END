package com.velora.api.remittance.web;

import com.velora.api.common.dto.PageResponse;
import com.velora.api.identity.security.UserPrincipal;
import com.velora.api.remittance.dto.CreateRemittanceRequest;
import com.velora.api.remittance.dto.RemittanceResponse;
import com.velora.api.remittance.dto.UnsettledOrderResponse;
import com.velora.api.remittance.service.RemittanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — COD settlement",
        description = "Reconciling courier cash. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/remittances")
public class AdminRemittanceController {

    private final RemittanceService remittanceService;

    public AdminRemittanceController(RemittanceService remittanceService) {
        this.remittanceService = remittanceService;
    }

    @Operation(summary = "Money the courier still owes",
            description = """
                    Delivered cash-on-delivery orders that have not been settled, oldest
                    first. This is the list you hand over as "here is what you owe me".

                    `daysWaiting` is the number to watch. A two-day-old balance is
                    normal; the same balance at three weeks is a conversation.
                    """)
    @GetMapping("/outstanding")
    public Map<String, Object> outstanding() {
        List<UnsettledOrderResponse> orders = remittanceService.unsettled();
        return Map.of(
                "orderCount", orders.size(),
                "totalAmount", remittanceService.outstandingTotal(),
                "orders", orders);
    }

    @Operation(summary = "Record a settlement",
            description = """
                    Marks the covered orders paid and records any difference.

                    Send what ACTUALLY arrived in `receivedAmount`, not what was expected
                    — the gap between them is the reason this endpoint exists. A mismatch
                    requires a `note`, because an unexplained shortfall never gets
                    reconciled by anyone.

                    Orders are marked paid even when the batch is short. The shortfall
                    belongs to the batch, not to individual orders; leaving them pending
                    would put the same orders on next week's list and count them twice.
                    """)
    @PostMapping
    public ResponseEntity<RemittanceResponse> record(
            @Valid @RequestBody CreateRemittanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(remittanceService.record(request, principal.id()));
    }

    @Operation(summary = "Past settlements, newest first")
    @GetMapping
    public PageResponse<RemittanceResponse> list(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return remittanceService.list(pageable);
    }

    @Operation(summary = "One settlement with its orders")
    @GetMapping("/{remittanceId}")
    public RemittanceResponse get(@PathVariable Long remittanceId) {
        return remittanceService.get(remittanceId);
    }

    @Operation(summary = "Cancel a settlement",
            description = "Returns its orders to unpaid. The record stays — a cancelled "
                    + "settlement is still part of the reconciliation history.")
    @PostMapping("/{remittanceId}/cancel")
    public RemittanceResponse cancel(@PathVariable Long remittanceId,
                                     @RequestParam String reason,
                                     @AuthenticationPrincipal UserPrincipal principal) {

        return remittanceService.cancel(remittanceId, reason, principal.id());
    }
}
