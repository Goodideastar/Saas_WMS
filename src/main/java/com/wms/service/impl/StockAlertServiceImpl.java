package com.wms.service.impl;

import com.wms.dto.PageResult;
import com.wms.dto.StockAlertHandleDto;
import com.wms.dto.StockAlertQueryDto;
import com.wms.dto.StockThresholdDto;
import com.wms.entity.StockAlert;
import com.wms.entity.StockAlertHandle;
import com.wms.exception.BusinessException;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockAlertMapper;
import com.wms.service.StockAlertService;
import com.wms.vo.StockAlertVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertMapper stockAlertMapper;
    private final ProductMapper productMapper;

    public StockAlertServiceImpl(StockAlertMapper stockAlertMapper, ProductMapper productMapper) {
        this.stockAlertMapper = stockAlertMapper;
        this.productMapper = productMapper;
    }

    @Override
    public void checkAndCreateAlerts(Long productId, Long warehouseId) {
        var product = productMapper.selectById(productId);
        if (product == null) return;

        Integer currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        Integer alertMin = product.getAlertMin();
        Integer alertMax = product.getAlertMax();
        if (alertMin == null && alertMax == null) return;

        int existingCount = stockAlertMapper.countByProduct(productId);
        if (existingCount > 0) return;

        List<String> alerts = new ArrayList<>();
        if (alertMin != null && currentStock < alertMin) {
            alerts.add("低于最低库存阈值");
        }
        if (alertMax != null && currentStock > alertMax) {
            alerts.add("高于最高库存阈值");
        }
        if (!alerts.isEmpty()) {
            StockAlert alert = new StockAlert();
            alert.setProductId(productId);
            alert.setProductCode(product.getProductCode());
            alert.setProductName(product.getProductName());
            alert.setWarehouseId(warehouseId);
            alert.setAlertType(String.join(",", alerts));
            alert.setAlertValue(alertMin != null ? alertMin : 0);
            alert.setActualStock(currentStock);
            alert.setStatus("PENDING");
            alert.setAlertTime(LocalDateTime.now());
            alert.setCreateTime(LocalDateTime.now());
            stockAlertMapper.insert(alert);
        }
    }

    @Override
    public PageResult<StockAlertVo> pageQuery(StockAlertQueryDto queryDto) {
        int pageNum = queryDto.getPageNum() != null ? queryDto.getPageNum() : 1;
        int pageSize = queryDto.getPageSize() != null ? queryDto.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        List<StockAlert> records = stockAlertMapper.selectPage(offset, pageSize,
                queryDto.getStatus(), queryDto.getStartTime(), queryDto.getEndTime());
        int total = stockAlertMapper.selectCount(queryDto.getStatus(), queryDto.getStartTime(), queryDto.getEndTime());

        List<StockAlertVo> voList = records.stream().map(this::convertToVo).collect(Collectors.toList());
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public void handleAlert(StockAlertHandleDto dto) {
        StockAlert alert = stockAlertMapper.selectById(dto.getId());
        if (alert == null) throw new BusinessException(404, "库存预警不存在");

        StockAlertHandle handle = new StockAlertHandle();
        handle.setAlertId(dto.getId());
        handle.setHandleTime(LocalDateTime.now());
        handle.setHandleResult(dto.getHandleRemark());

        alert.setStatus("HANDLED");
        alert.setHandleTime(LocalDateTime.now());
        stockAlertMapper.update(alert);
    }

    @Override
    public void batchUpdateThresholds(List<StockThresholdDto> thresholds) {
        for (StockThresholdDto dto : thresholds) {
            var product = productMapper.selectById(dto.getProductId());
            if (product != null) {
                product.setAlertMin(dto.getAlertMin());
                product.setAlertMax(dto.getAlertMax());
                productMapper.update(product);
            }
        }
    }

    @Override
    public Map<String, Object> getStats() {
        int total = stockAlertMapper.selectCount(null, null, null);
        int pending = stockAlertMapper.countByStatus("PENDING");
        int today = stockAlertMapper.countToday();
        return Map.of("total", total, "pending", pending, "today", today);
    }

    private StockAlertVo convertToVo(StockAlert a) {
        StockAlertVo vo = new StockAlertVo();
        vo.setId(a.getId());
        vo.setProductId(a.getProductId());
        vo.setProductCode(a.getProductCode());
        vo.setProductName(a.getProductName());
        vo.setWarehouseId(a.getWarehouseId());
        vo.setAlertType(a.getAlertType());
        vo.setAlertValue(a.getAlertValue());
        vo.setActualStock(a.getActualStock());
        vo.setStatus(a.getStatus());
        vo.setHandleRemark(a.getHandleRemark());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
