package com.velora.api.audit.web;

import com.velora.api.audit.dto.AuditLogResponse;
import com.velora.api.audit.service.AuditService;
import com.velora.api.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Admin — Audit log", description = "Who changed what. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Recorded changes, newest first",
            description = """
                    Filter by action, by who did it, or leave empty for everything.

                    Order status changes are NOT here — they live in the order's own
                    timeline, which is richer and already scoped to the order.
                    """)
    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {

        return auditService.list(action, actorId, null, null, pageable);
    }

    @Operation(summary = "Everything that happened to one thing",
            description = "The usual investigation: `PRODUCT_VARIANT` + the variant id "
                    + "shows every price change that variant ever had.")
    @GetMapping("/{entityType}/{entityId}")
    public PageResponse<AuditLogResponse> forEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {

        return auditService.list(null, null, entityType.toUpperCase(), entityId, pageable);
    }
}
