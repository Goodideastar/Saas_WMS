package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.StockAlert;
import com.wms.mapper.InboundOrderItemMapper;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.OutboundOrderItemMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockAlertMapper;
import com.wms.service.DashboardService;
import com.wms.vo.TodaySummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
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
        alertWrapper.eq(StockAlert::getStatus, "UNHANDLED");
        summary.setAlertCount(stockAlertMapper.selectCount(alertWrapper));

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getStatus, 1);
        List<Product> products = productMapper.selectList(productWrapper);
        summary.setTotalProducts(products.size());
        summary.setTotalStock(products.stream()
                .mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)
                .sum());

        // Calculate today's inbound amount
        List<InboundOrder> todayInbound = inboundOrderMapper.selectList(inboundWrapper);
        if (!todayInbound.isEmpty()) {
            List<Long> inboundIds = todayInbound.stream().map(InboundOrder::getId).collect(Collectors.toList());
            List<InboundOrderItem> inboundItems = inboundOrderItemMapper.selectList(
                    new LambdaQueryWrapper<InboundOrderItem>().in(InboundOrderItem::getInboundOrderId, inboundIds));
            summary.setInboundAmount(inboundItems.stream()
                    .map(i -> i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        // Calculate today's outbound amount
        List<OutboundOrder> todayOutbound = outboundOrderMapper.selectList(outboundWrapper);
        if (!todayOutbound.isEmpty()) {
            List<Long> outboundIds = todayOutbound.stream().map(OutboundOrder::getId).collect(Collectors.toList());
            List<OutboundOrderItem> outboundItems = outboundOrderItemMapper.selectList(
                    new LambdaQueryWrapper<OutboundOrderItem>().in(OutboundOrderItem::getOutboundOrderId, outboundIds));
            summary.setOutboundAmount(outboundItems.stream()
                    .map(i -> i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

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
            data.put("inboundQuantity", inboundOrderMapper.selectCount(inboundWrapper));

            LambdaQueryWrapper<OutboundOrder> outboundWrapper = new LambdaQueryWrapper<>();
            outboundWrapper.ge(OutboundOrder::getCreateTime, startOfDay)
                    .lt(OutboundOrder::getCreateTime, endOfDay);
            data.put("outboundQuantity", outboundOrderMapper.selectCount(outboundWrapper));

            trendList.add(data);
        }

        return trendList;
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public Map<String, Long> getAlertStats() {
        Map<String, Long> stats = new HashMap<>();

        LambdaQueryWrapper<StockAlert> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(StockAlert::getStatus, "UNHANDLED");
        stats.put("unhandled", stockAlertMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<StockAlert> lowStockWrapper = new LambdaQueryWrapper<>();
        lowStockWrapper.eq(StockAlert::getAlertType, "BELOW_MIN")
                .eq(StockAlert::getStatus, "UNHANDLED");
        stats.put("belowMin", stockAlertMapper.selectCount(lowStockWrapper));

        LambdaQueryWrapper<StockAlert> overStockWrapper = new LambdaQueryWrapper<>();
        overStockWrapper.eq(StockAlert::getAlertType, "OVER_STOCK")
                .eq(StockAlert::getStatus, "UNHANDLED");
        stats.put("aboveMax", stockAlertMapper.selectCount(overStockWrapper));

        return stats;
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public List<Map<String, Object>> getTopProducts() {
        // Aggregate outbound quantity by product from outbound items
        List<OutboundOrder> completedOrders = outboundOrderMapper.selectList(
                new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "COMPLETED"));
        List<Map<String, Object>> result = new ArrayList<>();

        if (!completedOrders.isEmpty()) {
            List<Long> orderIds = completedOrders.stream().map(OutboundOrder::getId).collect(Collectors.toList());
            List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(
                    new LambdaQueryWrapper<OutboundOrderItem>().in(OutboundOrderItem::getOutboundOrderId, orderIds));

            Map<Long, Integer> productQtyMap = new HashMap<>();
            Map<Long, String> productNameMap = new HashMap<>();
            Map<Long, String> productCodeMap = new HashMap<>();

            for (OutboundOrderItem item : items) {
                Long pid = item.getProductId();
                productQtyMap.merge(pid, item.getActualQuantity() != null ? item.getActualQuantity() : 0, Integer::sum);
            }

            // Fill product names
            if (!productQtyMap.isEmpty()) {
                List<Product> products = productMapper.selectBatchIds(productQtyMap.keySet());
                for (Product p : products) {
                    productNameMap.put(p.getId(), p.getProductName());
                    productCodeMap.put(p.getId(), p.getProductCode());
                }
            }

            result = productQtyMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("productName", productNameMap.getOrDefault(e.getKey(), "Unknown"));
                        data.put("productCode", productCodeMap.getOrDefault(e.getKey(), ""));
                        data.put("outboundQuantity", e.getValue());
                        return data;
                    }).collect(Collectors.toList());
        }

        // Fallback: return top products by stock if no outbound data
        if (result.isEmpty()) {
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, 1)
                    .orderByDesc(Product::getCurrentStock)
                    .last("LIMIT 10");
            List<Product> products = productMapper.selectList(wrapper);
            result = products.stream().map(product -> {
                Map<String, Object> data = new HashMap<>();
                data.put("productName", product.getProductName());
                data.put("productCode", product.getProductCode());
                data.put("outboundQuantity", 0);
                return data;
            }).collect(Collectors.toList());
        }

        return result;
    }

    @Override
    @Cacheable(value = "dashboard", key = "#root.methodName")
    public List<Map<String, Object>> getWarehouseDistribution() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        List<Product> products = productMapper.selectList(wrapper);

        Map<String, Integer> categoryStockMap = products.stream()
                .filter(p -> p.getCategory() != null && !p.getCategory().isEmpty())
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        Collectors.summingInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)));

        return categoryStockMap.entrySet().stream().map(entry -> {
            Map<String, Object> data = new HashMap<>();
            data.put("warehouseName", entry.getKey());
            data.put("stockQuantity", entry.getValue());
            return data;
        }).collect(Collectors.toList());
    }
}