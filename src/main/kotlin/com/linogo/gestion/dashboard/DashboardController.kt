package com.linogo.gestion.dashboard

import com.linogo.gestion.security.config.Authenticated
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/dashboard")
@Authenticated
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping("/sales")
    fun getSalesSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?
    ): ResponseEntity<SalesSummaryResponse> {
        return ResponseEntity.ok(dashboardService.getSalesSummary(from, to))
    }
}
