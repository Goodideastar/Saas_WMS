package com.wms.service.impl;

import com.wms.mapper.DashboardMapper;
import com.wms.service.DashboardService;
import com.wms.vo.TodaySummaryVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    public TodaySummaryVo getTodaySummary() {
        TodaySummaryVo vo = new TodaySummaryVo();
        Map<String, Object> inbound = dashboardMapper.todayInboundStats();
        Map<String, Object> outbound = dashboardMapper.todayOutboundStats();
        Map<String, Object> alertStats = dashboardMapper.alertStats();
        Map<String, Object> pendingIn = dashboardMapper.todayPendingInboundStats();
        Map<String, Object> pendingOut = dashboardMapper.todayPendingOutboundStats();

        vo.setInboundCount(toLong(inbound.get("orderCount")));
        vo.setOutboundCount(toLong(outbound.get("orderCount")));
        vo.setInboundAmount(toDecimal(inbound.get("totalAmount")));
        vo.setOutboundAmount(toDecimal(outbound.get("totalAmount")));
        Long totalStockLong = dashboardMapper.totalStock();
        vo.setTotalStock(totalStockLong != null ? totalStockLong.intValue() : 0);
        vo.setAlertCount(toLong(alertStats.get("unhandled")));
        vo.setTotalProducts(dashboardMapper.totalProductCount());
        vo.setPendingInbound(toInt(pendingIn.get("pendingCount")));
        vo.setPendingOutbound(toInt(pendingOut.get("pendingCount")));
        return vo;
    }

    @Override
    public List<Map<String, Object>> getLast7DaysTrend() {
        List<Map<String, Object>> rows = dashboardMapper.last7DaysTrend();
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDate.put(String.valueOf(row.get("statDate")), row);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(fmt);
            Map<String, Object> row = byDate.get(date);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("inboundQuantity", row != null ? toLong(row.get("inboundQuantity")) : 0L);
            item.put("outboundQuantity", row != null ? toLong(row.get("outboundQuantity")) : 0L);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Long> getAlertStats() {
        Map<String, Object> stats = dashboardMapper.alertStats();
        Map<String, Long> result = new HashMap<>();
        result.put("unhandled", toLong(stats.get("unhandled")));
        result.put("belowMin", toLong(stats.get("belowMin")));
        result.put("aboveMax", toLong(stats.get("aboveMax")));
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopProducts() {
        return normalizeKeys(dashboardMapper.topOutboundProducts(10));
    }

    @Override
    public List<Map<String, Object>> getWarehouseDistribution() {
        return normalizeKeys(dashboardMapper.warehouseDistribution());
    }

    /** MySQL SUM 返回 BigDecimal，前端图表需要数字类型 */
    private List<Map<String, Object>> normalizeKeys(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getValue() instanceof BigDecimal bd) {
                    item.put(e.getKey(), bd.longValue());
                } else {
                    item.put(e.getKey(), e.getValue());
                }
            }
            result.add(item);
        }
        return result;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal bd) return bd.longValue();
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
