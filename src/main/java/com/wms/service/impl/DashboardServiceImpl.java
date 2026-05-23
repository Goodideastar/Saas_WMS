package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.entity.InboundOrder;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Product;
import com.wms.entity.StockAlert;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockAlertMapper;
import com.wms.service.DashboardService;
import com.wms.vo.TodaySummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final StockAlertMapper stockAlertMapper;
    private final ProductMapper productMapper;

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public TodaySummaryVo getTodaySummary() {
        TodaySummaryVo summary = new TodaySummaryVo();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<InboundOrder> inboundWrapper = new LambdaQueryWrapper<>();
        inboundWrapper.ge(InboundOrder::getCreateTime, startOfDay)
                .lt(InboundOrder::getCreateTime, endOfDay);
        summary.setInboundCount(inboundOrderMapper.selectCount(inboundWrapper));

        LambdaQueryWrapper<OutboundOrder> outboundWrapper = new LambdaQueryWrapper<>();
        outboundWrapper.ge(OutboundOrder::getCreateTime, startOfDay)
                .lt(OutboundOrder::getCreateTime, endOfDay);
        summary.setOutboundCount(outboundOrderMapper.selectCount(outboundWrapper));

        LambdaQueryWrapper<StockAlert> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(StockAlert::getStatus, "pending");
        summary.setAlertCount(stockAlertMapper.selectCount(alertWrapper));

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getStatus, 1);
        List<Product> products = productMapper.selectList(productWrapper);
        summary.setTotalProducts(products.size());
        summary.setTotalStock(products.stream()
                .mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)
                .sum());

        return summary;
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public List<Map<String, Object>> getLast7DaysTrend() {
        List<Map<String, Object>> trendList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            Map<String, Object> data = new HashMap<>();
            data.put("date", date.format(formatter));

            LambdaQueryWrapper<InboundOrder> inboundWrapper = new LambdaQueryWrapper<>();
            inboundWrapper.ge(InboundOrder::getCreateTime, startOfDay)
                    .lt(InboundOrder::getCreateTime, endOfDay);
            data.put("inboundCount", inboundOrderMapper.selectCount(inboundWrapper));

            LambdaQueryWrapper<OutboundOrder> outboundWrapper = new LambdaQueryWrapper<>();
            outboundWrapper.ge(OutboundOrder::getCreateTime, startOfDay)
                    .lt(OutboundOrder::getCreateTime, endOfDay);
            data.put("outboundCount", outboundOrderMapper.selectCount(outboundWrapper));

            trendList.add(data);
        }

        return trendList;
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public Map<String, Long> getAlertStats() {
        Map<String, Long> stats = new HashMap<>();

        LambdaQueryWrapper<StockAlert> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(StockAlert::getStatus, "pending");
        stats.put("pending", stockAlertMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<StockAlert> lowStockWrapper = new LambdaQueryWrapper<>();
        lowStockWrapper.eq(StockAlert::getAlertType, "LOW_STOCK")
                .eq(StockAlert::getStatus, "pending");
        stats.put("lowStock", stockAlertMapper.selectCount(lowStockWrapper));

        LambdaQueryWrapper<StockAlert> overStockWrapper = new LambdaQueryWrapper<>();
        overStockWrapper.eq(StockAlert::getAlertType, "OVER_STOCK")
                .eq(StockAlert::getStatus, "pending");
        stats.put("overStock", stockAlertMapper.selectCount(overStockWrapper));

        return stats;
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public List<Map<String, Object>> getTopProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getCurrentStock)
                .last("LIMIT 10");
        List<Product> products = productMapper.selectList(wrapper);

        return products.stream().map(product -> {
            Map<String, Object> data = new HashMap<>();
            data.put("productName", product.getProductName());
            data.put("productCode", product.getProductCode());
            data.put("currentStock", product.getCurrentStock());
            return data;
        }).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public List<Map<String, Object>> getWarehouseDistribution() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        List<Product> products = productMapper.selectList(wrapper);

        Map<String, Integer> categoryStockMap = products.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        Collectors.summingInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)));

        return categoryStockMap.entrySet().stream().map(entry -> {
            Map<String, Object> data = new HashMap<>();
            data.put("category", entry.getKey());
            data.put("totalStock", entry.getValue());
            return data;
        }).collect(Collectors.toList());
    }
}