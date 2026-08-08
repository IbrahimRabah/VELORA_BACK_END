package com.velora.api.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A single public endpoint so Swagger has something to render and you can confirm
 * the whole chain works end to end. Delete once real controllers exist.
 */
@Tag(name = "System", description = "Health and diagnostics")
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @Operation(summary = "Liveness check", description = "Returns the server time in UTC.")
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "service", "velora-api",
                "time", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }
}
