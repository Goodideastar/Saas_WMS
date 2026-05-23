package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.dto.StockAlertHandleDto;
import com.wms.dto.StockAlertQueryDto;
import com.wms.dto.StockThresholdDto;
import com.wms.entity.Product;
import com.wms.entity.StockAlert;
import com.wms.exception.BusinessException;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockAlertMapper;
import com.wms.service.StockAlertService;
import com.wms.vo.StockAlertVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockAlertServiceImpl implements StockAlertService {

    private static final Logger logger = LoggerFactory.getLogger(StockAlertServiceImpl.class);

    private final StockAlertMapper stockAlertMapper;
    private final ProductMapper productMapper;

    public StockAlertServiceImpl(StockAlertMapper stockAlertMapper, ProductMapper productMapper) {
        this.stockAlertMapper = stockAlertMapper;
        this.productMapper = productMapper;
    }

    @Override
    public void checkAndCreateAlerts(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return;
        }

        if (product.getAlertMin() == null && product.getAlertMax() == null) {
            return;
        }

        int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;

        if (product.getAlertMin() != null && currentStock < product.getAlertMin()) {
            LambdaQueryWrapper<StockAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockAlert::getProductId, productId)
                    .eq(StockAlert::getAlertType, "LOW_STOCK")
                    .eq(StockAlert::getStatus, "pending");
            Long count = stockAlertMapper.selectCount(wrapper);
            if (count == 0) {
                StockAlert alert = new StockAlert();
                alert.setProductId(productId);
                alert.setAlertType("LOW_STOCK");
                alert.setAlertValue(product.getAlertMin());
                alert.setActualStock(currentStock);
                alert.setStatus("pending");
                stockAlertMapper.insert(alert);
                logger.warn("Stock alert triggered: Product [{}] current stock {} is below minimum {}",
                        product.getProductName(), currentStock, product.getAlertMin());
            }
        }

        if (product.getAlertMax() != null && currentStock > product.getAlertMax()) {
            LambdaQueryWrapper<StockAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StockAlert::getProductId, productId)
                    .eq(StockAlert::getAlertType, "OVER_STOCK")
                    .eq(StockAlert::getStatus, "pending");
            Long count = stockAlertMapper.selectCount(wrapper);
            if (count == 0) {
                StockAlert alert = new StockAlert();
                alert.setProductId(productId);
                alert.setAlertType("OVER_STOCK");
                alert.setAlertValue(product.getAlertMax());
                alert.setActualStock(currentStock);
                alert.setStatus("pending");
                stockAlertMapper.insert(alert);
                logger.warn("Stock alert triggered: Product [{}] current stock {} is above maximum {}",
                        product.getProductName(), currentStock, product.getAlertMax());
            }
        }
    }

    @Override
    public IPage<StockAlertVo> pageQuery(StockAlertQueryDto queryDto) {
        LambdaQueryWrapper<StockAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryDto.getStatus() != null, StockAlert::getStatus, queryDto.getStatus())
                .eq(queryDto.getProductId() != null, StockAlert::getProductId, queryDto.getProductId())
                .orderByDesc(StockAlert::getCreateTime);

        int pageNum = queryDto.getPageNum() != null ? queryDto.getPageNum() : 1;
        int pageSize = queryDto.getPageSize() != null ? queryDto.getPageSize() : 10;
        Page<StockAlert> page = new Page<>(pageNum, pageSize);
        IPage<StockAlert> alertPage = stockAlertMapper.selectPage(page, wrapper);

        IPage<StockAlertVo> voPage = new Page<>(alertPage.getCurrent(), alertPage.getSize(), alertPage.getTotal());
        voPage.setRecords(alertPage.getRecords().stream().map(this::convertToVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void handleAlert(StockAlertHandleDto dto) {
        StockAlert alert = stockAlertMapper.selectById(dto.getId());
        if (alert == null) {
            throw new BusinessException(404, "Alert record not found");
        }
        if (!"pending".equals(alert.getStatus())) {
            throw new BusinessException(400, "Alert has been handled already");
        }

        alert.setStatus("handled");
        alert.setHandleRemark(dto.getHandleRemark());
        stockAlertMapper.updateById(alert);
    }

    @Override
    public void batchUpdateThresholds(List<StockThresholdDto> thresholds) {
        for (StockThresholdDto dto : thresholds) {
            Product product = productMapper.selectById(dto.getProductId());
            if (product != null) {
                if (dto.getAlertMin() != null) {
                    product.setAlertMin(dto.getAlertMin());
                }
                if (dto.getAlertMax() != null) {
                    product.setAlertMax(dto.getAlertMax());
                }
                productMapper.updateById(product);
            }
        }
    }

    @Override
    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        LambdaQueryWrapper<StockAlert> totalWrapper = new LambdaQueryWrapper<>();
        Long total = stockAlertMapper.selectCount(totalWrapper);
        stats.put("total", total);

        LambdaQueryWrapper<StockAlert> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(StockAlert::getStatus, "pending");
        Long pending = stockAlertMapper.selectCount(pendingWrapper);
        stats.put("pending", pending);

        LambdaQueryWrapper<StockAlert> handledWrapper = new LambdaQueryWrapper<>();
        handledWrapper.eq(StockAlert::getStatus, "handled");
        Long handled = stockAlertMapper.selectCount(handledWrapper);
        stats.put("handled", handled);

        return stats;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCheckAlerts() {
        logger.info("Starting scheduled stock alert check");
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        List<Product> products = productMapper.selectList(wrapper);

        for (Product product : products) {
            try {
                checkAndCreateAlerts(product.getId());
            } catch (Exception e) {
                logger.error("Stock alert check exception, ProductId: {}", product.getId(), e);
            }
        }
        logger.info("Scheduled stock alert check completed, checked {} products", products.size());
    }

    private StockAlertVo convertToVo(StockAlert alert) {
        StockAlertVo vo = new StockAlertVo();
        vo.setId(alert.getId());
        vo.setProductId(alert.getProductId());
        vo.setWarehouseId(alert.getWarehouseId());
        vo.setAlertType(alert.getAlertType());
        vo.setAlertValue(alert.getAlertValue());
        vo.setActualStock(alert.getActualStock());
        vo.setStatus(alert.getStatus());
        vo.setHandleRemark(alert.getHandleRemark());
        vo.setCreateBy(alert.getCreateBy());
        vo.setCreateTime(alert.getCreateTime());

        Product product = productMapper.selectById(alert.getProductId());
        if (product != null) {
            vo.setProductCode(product.getProductCode());
            vo.setProductName(product.getProductName());
        }

        return vo;
    }
}
