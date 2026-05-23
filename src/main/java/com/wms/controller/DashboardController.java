package com.wms.controller;

import com.wms.common.Result;
import com.wms.service.DashboardService;
import com.wms.vo.TodaySummaryVo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/today-summary")
    @PreAuthorize("hasAuthority('dashboard:query')")
    public Result<TodaySummaryVo> getTodaySummary() {
        TodaySummaryVo summary = dashboardService.getTodaySummary();
        return Result.success(summary);
    }

    @GetMapping("/last-7-days-trend")
    @PreAuthorize("hasAuthority('dashboard:query')")
    public Result<List<Map<String, Object>>> getLast7DaysTrend() {
        List<Map<String, Object>> trend = dashboardService.getLast7DaysTrend();
        return Result.success(trend);
    }

    @GetMapping("/alert-stats")
    @PreAuthorize("hasAuthority('dashboard:query')")
    public Result<Map<String, Long>> getAlertStats() {
        Map<String, Long> stats = dashboardService.getAlertStats();
        return Result.success(stats);
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAuthority('dashboard:query')")
    public Result<List<Map<String, Object>>> getTopProducts() {
        List<Map<String, Object>> topProducts = dashboardService.getTopProducts();
        return Result.success(topProducts);
    }

    @GetMapping("/warehouse-distribution")
    @PreAuthorize("hasAuthority('dashboard:query')")
    public Result<List<Map<String, Object>>> getWarehouseDistribution() {
        List<Map<String, Object>> distribution = dashboardService.getWarehouseDistribution();
        return Result.success(distribution);
    }
}
