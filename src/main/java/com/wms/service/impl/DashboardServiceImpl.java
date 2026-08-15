package com.wms.service.impl;

import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.service.DashboardService;
import com.wms.vo.TodaySummaryVo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductMapper productMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    public DashboardServiceImpl(ProductMapper productMapper,
                                 InboundOrderMapper inboundOrderMapper,
                                 OutboundOrderMapper outboundOrderMapper) {
        this.productMapper = productMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
    }

    @Override
    public TodaySummaryVo getTodaySummary() {
        TodaySummaryVo vo = new TodaySummaryVo();
        vo.setTotalProducts(productMapper.selectCount(null, null, null, null, null));
        vo.setPendingInbound(inboundOrderMapper.countByStatus("PENDING"));
        vo.setPendingOutbound(outboundOrderMapper.countByStatus("PENDING"));
        return vo;
    }

    @Override
    public List<Map<String, Object>> getLast7DaysTrend() {
        return List.of();
    }

    @Override
    public Map<String, Long> getAlertStats() {
        return Map.of();
    }

    @Override
    public List<Map<String, Object>> getTopProducts() {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getWarehouseDistribution() {
        return List.of();
    }
}
