package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wms.dto.OutboundOrderDto;
import com.wms.dto.OutboundOrderItemDto;
import com.wms.dto.OutboundOrderQueryDto;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.StockLog;
import com.wms.exception.BusinessException;
import com.wms.mapper.OutboundOrderItemMapper;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockLogMapper;
import com.wms.service.OutboundOrderService;
import com.wms.service.StockAlertService;
import com.wms.vo.OutboundOrderVo;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboundOrderServiceImpl extends ServiceImpl<OutboundOrderMapper, OutboundOrder>
        implements OutboundOrderService {

    private static final Logger logger = LoggerFactory.getLogger(OutboundOrderServiceImpl.class);

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final ProductMapper productMapper;
    private final StockLogMapper stockLogMapper;
    private final StockAlertService stockAlertService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOutboundOrder(OutboundOrderDto dto) {
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setWarehouseId(dto.getWarehouseId());
        order.setCustomer(dto.getCustomer());
        order.setOutboundType(dto.getOutboundType());
        order.setRelatedOrderNo(dto.getRelatedOrderNo());
        order.setStatus("PENDING");
        order.setRemark(dto.getRemark());
        outboundOrderMapper.insert(order);

        for (OutboundOrderItemDto itemDto : dto.getItems()) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setOutboundOrderId(order.getId());
            item.setProductId(itemDto.getProductId());
            item.setExpectedQuantity(itemDto.getExpectedQuantity());
            item.setActualQuantity(
                    itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : itemDto.getExpectedQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            if (itemDto.getUnitPrice() != null) {
                item.setSubtotal(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(item.getActualQuantity())));
            }
            outboundOrderItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dashboard", allEntries = true)
    public void auditOutboundOrder(Long id) {
        logger.info("========== START: Audit Outbound Order ==========");
        logger.info("[OutboundAudit] Order ID: {}", id);

        OutboundOrder order = outboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            logger.error("[OutboundAudit] Order not found, ID: {}", id);
            throw new BusinessException(404, "Outbound order not found");
        }
        logger.info(
                "[OutboundAudit] Order details - OrderNo: {}, WarehouseId: {}, Customer: {}, OutboundType: {}, Status: {}",
                order.getOrderNo(), order.getWarehouseId(), order.getCustomer(),
                order.getOutboundType(), order.getStatus());

        if (!"PENDING".equals(order.getStatus())) {
            logger.error("[OutboundAudit] Order status not allowed for audit - OrderNo: {}, CurrentStatus: {}",
                    order.getOrderNo(), order.getStatus());
            throw new BusinessException(400, "Order status not allowed for audit");
        }

        LambdaQueryWrapper<OutboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OutboundOrderItem::getOutboundOrderId, id);
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(itemWrapper);
        logger.info("[OutboundAudit] Number of order items: {}", items.size());

        for (OutboundOrderItem item : items) {
            logger.info(
                    "[OutboundAudit] Processing item - ItemId: {}, ProductId: {}, ExpectedQty: {}, ActualQty: {}, UnitPrice: {}",
                    item.getId(), item.getProductId(), item.getExpectedQuantity(),
                    item.getActualQuantity(), item.getUnitPrice());

            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                logger.error("[OutboundAudit] Product not found, ProductId: {}", item.getProductId());
                throw new BusinessException(404, "Product not found: " + item.getProductId());
            }
            logger.info(
                    "[OutboundAudit] Product info - Code: {}, Name: {}, CurrentStock: {}, Version: {}, Category: {}, Status: {}",
                    product.getProductCode(), product.getProductName(),
                    product.getCurrentStock(), product.getVersion(), product.getCategory(), product.getStatus());

            int quantityBefore = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            int quantityChange = item.getActualQuantity() != null ? item.getActualQuantity()
                    : item.getExpectedQuantity();

            logger.info("[OutboundAudit] Stock pre-check - Product: {} ({}), CurrentStock: {}, NeedToDeduct: {}",
                    product.getProductName(), product.getProductCode(),
                    quantityBefore, quantityChange);

            if (quantityBefore < quantityChange) {
                logger.error(
                        "[OutboundAudit] Insufficient stock! Product: {} ({}), CurrentStock: {}, NeedToDeduct: {}, Shortage: {}",
                        product.getProductName(), product.getProductCode(),
                        quantityBefore, quantityChange, quantityChange - quantityBefore);
                throw new BusinessException(400, "Insufficient stock for product: " + product.getProductName()
                        + ", current stock: " + quantityBefore);
            }

            int quantityAfter = quantityBefore - quantityChange;
            logger.info(
                    "[OutboundAudit] Stock change calculation - Product: {} ({}), Before: {}, Change: -{}, After: {}",
                    product.getProductName(), product.getProductCode(),
                    quantityBefore, quantityChange, quantityAfter);

            logger.info("[OutboundAudit] Optimistic lock update - ProductId: {}, CurrentVersion: {}",
                    product.getId(), product.getVersion());

            LambdaQueryWrapper<Product> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(Product::getId, product.getId())
                    .eq(Product::getVersion, product.getVersion());
            product.setCurrentStock(quantityAfter);
            product.setVersion(product.getVersion() + 1);
            int updated = productMapper.update(product, updateWrapper);

            if (updated == 0) {
                logger.error(
                        "[OutboundAudit] Optimistic lock update failed! ProductId: {}, ExpectedVersion: {}, Another transaction may have modified this record",
                        product.getId(), product.getVersion() - 1);
                throw new BusinessException(400, "Stock update failed, please retry");
            }
            logger.info("[OutboundAudit] Optimistic lock update success - AffectedRows: {}, NewVersion: {}", updated,
                    product.getVersion());

            StockLog stockLog = new StockLog();
            stockLog.setProductId(product.getId());
            stockLog.setOperationType("OUTBOUND");
            stockLog.setQuantityBefore(quantityBefore);
            stockLog.setQuantityChange(-quantityChange);
            stockLog.setQuantityAfter(quantityAfter);
            stockLog.setRelatedOrderNo(order.getOrderNo());
            stockLogMapper.insert(stockLog);
            logger.info("[OutboundAudit] Stock log created - LogId: {}, Change: {}", stockLog.getId(), -quantityChange);

            logger.info("[OutboundAudit] Triggering stock alert check - ProductId: {}", product.getId());
            stockAlertService.checkAndCreateAlerts(product.getId());
        }

        order.setStatus("COMPLETED");
        order.setOutboundTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);
        logger.info("[OutboundAudit] Order status updated to 'completed', OutboundTime: {}", order.getOutboundTime());
        logger.info("========== END: Audit Outbound Order - OrderNo: {} ==========", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOutboundOrder(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "Outbound order not found");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "Order status not allowed for cancellation");
        }

        order.setStatus("CANCELLED");
        outboundOrderMapper.updateById(order);
    }

    @Override
    public IPage<OutboundOrderVo> pageQuery(OutboundOrderQueryDto queryDto) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDto.getOrderNo()), OutboundOrder::getOrderNo, queryDto.getOrderNo())
                .eq(StringUtils.hasText(queryDto.getStatus()), OutboundOrder::getStatus, queryDto.getStatus())
                .ge(queryDto.getStartTime() != null, OutboundOrder::getCreateTime, queryDto.getStartTime())
                .le(queryDto.getEndTime() != null, OutboundOrder::getCreateTime, queryDto.getEndTime())
                .orderByDesc(OutboundOrder::getCreateTime);

        Page<OutboundOrder> page = new Page<>(queryDto.getPageNum() != null ? queryDto.getPageNum() : 1,
                queryDto.getPageSize() != null ? queryDto.getPageSize() : 10);
        IPage<OutboundOrder> orderPage = outboundOrderMapper.selectPage(page, wrapper);

        IPage<OutboundOrderVo> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream().map(this::convertToVo).collect(Collectors.toList()));
        return voPage;
    }

    private OutboundOrderVo convertToVo(OutboundOrder order) {
        OutboundOrderVo vo = new OutboundOrderVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setCustomer(order.getCustomer());
        vo.setOutboundType(order.getOutboundType());
        vo.setRelatedOrderNo(order.getRelatedOrderNo());
        vo.setOperatorId(order.getOperatorId());
        vo.setStatus(order.getStatus());
        vo.setOutboundTime(order.getOutboundTime());
        vo.setRemark(order.getRemark());
        vo.setCreateBy(order.getCreateBy());
        vo.setCreateTime(order.getCreateTime());

        LambdaQueryWrapper<OutboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OutboundOrderItem::getOutboundOrderId, order.getId());
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(itemWrapper);
        vo.setItems(items.stream().map(item -> {
            OutboundOrderVo.OutboundOrderItemVo itemVo = new OutboundOrderVo.OutboundOrderItemVo();
            itemVo.setId(item.getId());
            itemVo.setOutboundOrderId(item.getOutboundOrderId());
            itemVo.setProductId(item.getProductId());
            itemVo.setExpectedQuantity(item.getExpectedQuantity());
            itemVo.setActualQuantity(item.getActualQuantity());
            itemVo.setUnitPrice(item.getUnitPrice());
            itemVo.setSubtotal(item.getSubtotal());

            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                itemVo.setProductName(product.getProductName());
                itemVo.setProductCode(product.getProductCode());
            }
            return itemVo;
        }).collect(Collectors.toList()));

        return vo;
    }

    private String generateOrderNo() {
        RLock lock = redissonClient.getLock("order-no-lock:outbound");
        try {
            lock.lockInterruptibly(10, TimeUnit.SECONDS);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.likeRight(OutboundOrder::getOrderNo, "OUT" + dateStr)
                    .orderByDesc(OutboundOrder::getOrderNo)
                    .last("LIMIT 1");
            OutboundOrder lastOrder = outboundOrderMapper.selectOne(wrapper);

            int sequence = 1;
            if (lastOrder != null && lastOrder.getOrderNo().startsWith("OUT" + dateStr)) {
                String lastSeq = lastOrder.getOrderNo().substring(("OUT" + dateStr).length());
                sequence = Integer.parseInt(lastSeq) + 1;
            }
            return "OUT" + dateStr + String.format("%04d", sequence);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "Failed to generate order number");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
