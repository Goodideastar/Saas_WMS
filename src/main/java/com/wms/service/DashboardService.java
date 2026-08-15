package com.wms.service;

import com.wms.vo.TodaySummaryVo;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    TodaySummaryVo getTodaySummary();

    List<Map<String, Object>> getLast7DaysTrend();

    Map<String, Long> getAlertStats();

    List<Map<String, Object>> getTopProducts();

    List<Map<String, Object>> getWarehouseDistribution();
}
