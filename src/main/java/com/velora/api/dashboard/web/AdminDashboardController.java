package com.velora.api.dashboard.web;

import com.velora.api.dashboard.dto.DashboardResponse;
import com.velora.api.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Dashboard", description = "Store overview. Requires ROLE_ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Everything the owner needs on opening the admin",
            description = """
                    One call, because a dashboard that needs eight requests renders in
                    stages and looks broken on a slow connection.

                    Start with `alerts` — it is ordered by severity and every entry names
                    an action rather than a fact. The rest is context.

                    Note `sales` is what was SOLD, and `codPosition` is what the courier
                    still owes you. With cash on delivery those two differ by a lot, and
                    treating revenue as cash is how a profitable month runs out of money.
                    """)
    @GetMapping
    public DashboardResponse overview() {
        return dashboardService.build();
    }
}
