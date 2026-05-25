package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wms.dto.InboundOrderDto;
import com.wms.dto.InboundOrderItemDto;
import com.wms.dto.InboundOrderQueryDto;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Product;
import com.wms.entity.StockLog;
import com.wms.exception.BusinessException;
import com.wms.mapper.InboundOrderItemMapper;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockLogMapper;
import com.wms.service.InboundOrderService;
import com.wms.service.StockAlertService;
import com.wms.vo.InboundOrderVo;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InboundOrderServiceImpl extends ServiceImpl<InboundOrderMapper, InboundOrder>
        implements InboundOrderService {

    private static final Logger logger = LoggerFactory.getLogger(InboundOrderServiceImpl.class);

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final ProductMapper productMapper;
    private final StockLogMapper stockLogMapper;
    private final StockAlertService stockAlertService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInboundOrder(InboundOrderDto dto) {
        InboundOrder order = new InboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setWarehouseId(dto.getWarehouseId());
        order.setSupplier(dto.getSupplier());
        order.setInboundType(dto.getInboundType());
        order.setRelatedOrderNo(dto.getRelatedOrderNo());
        order.setStatus("PENDING");
        order.setRemark(dto.getRemark());
        inboundOrderMapper.insert(order);

        for (InboundOrderItemDto itemDto : dto.getItems()) {
            InboundOrderItem item = new InboundOrderItem();
            item.setInboundOrderId(order.getId());
            item.setProductId(itemDto.getProductId());
            item.setExpectedQuantity(itemDto.getExpectedQuantity());
            item.setActualQuantity(
                    itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : itemDto.getExpectedQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            if (itemDto.getUnitPrice() != null) {
                item.setSubtotal(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(item.getActualQuantity())));
            }
            item.setBatchNo(itemDto.getBatchNo());
            item.setProductionDate(itemDto.getProductionDate());
            item.setExpiryDate(itemDto.getExpiryDate());
            inboundOrderItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInboundOrder(Long id) {
        logger.info("========== START: Audit Inbound Order ==========");
        logger.info("[InboundAudit] Order ID: {}", id);

        InboundOrder order = inboundOrderMapper.selectForUpdate(id);
        if (order == null) {
            logger.error("[InboundAudit] Order not found, ID: {}", id);
            throw new BusinessException(404, "Inbound order not found");
        }
        logger.info(
                "[InboundAudit] Order details - OrderNo: {}, WarehouseId: {}, Supplier: {}, InboundType: {}, Status: {}",
                order.getOrderNo(), order.getWarehouseId(), order.getSupplier(),
                order.getInboundType(), order.getStatus());

        if (!"PENDING".equals(order.getStatus())) {
            logger.error("[InboundAudit] Order status not allowed for audit - OrderNo: {}, CurrentStatus: {}",
                    order.getOrderNo(), order.getStatus());
            throw new BusinessException(400, "Order status not allowed for audit");
        }

        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getInboundOrderId, id);
        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(itemWrapper);
        logger.info("[InboundAudit] Number of order items: {}", items.size());

        for (InboundOrderItem item : items) {
            logger.info(
                    "[InboundAudit] Processing item - ItemId: {}, ProductId: {}, ExpectedQty: {}, ActualQty: {}, UnitPrice: {}",
                    item.getId(), item.getProductId(), item.getExpectedQuantity(),
                    item.getActualQuantity(), item.getUnitPrice());

            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                logger.error("[InboundAudit] Product not found, ProductId: {}", item.getProductId());
                throw new BusinessException(404, "Product not found: " + item.getProductId());
            }
            logger.info("[InboundAudit] Product info - Code: {}, Name: {}, CurrentStock: {}, Category: {}, Status: {}",
                    product.getProductCode(), product.getProductName(),
                    product.getCurrentStock(), product.getCategory(), product.getStatus());

            int quantityBefore = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
            int quantityChange = item.getActualQuantity() != null ? item.getActualQuantity()
                    : item.getExpectedQuantity();
            int quantityAfter = quantityBefore + quantityChange;

            logger.info(
                    "[InboundAudit] Stock change calculation - Product: {} ({}), Before: {}, Change: +{}, After: {}",
                    product.getProductName(), product.getProductCode(),
                    quantityBefore, quantityChange, quantityAfter);

            LambdaQueryWrapper<Product> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(Product::getId, product.getId())
                    .eq(Product::getVersion, product.getVersion());
            product.setCurrentStock(quantityAfter);
            product.setVersion(product.getVersion() + 1);
            int updateResult = productMapper.update(product, updateWrapper);
            logger.info("[InboundAudit] Optimistic lock update result - AffectedRows: {}", updateResult);

            if (updateResult == 0) {
                logger.error("[InboundAudit] Optimistic lock update failed! ProductId: {}, Another transaction may have modified this record",
                        product.getId());
                throw new BusinessException(400, "Stock update failed, please retry");
            }

            StockLog stockLog = new StockLog();
            stockLog.setProductId(product.getId());
            stockLog.setOperationType("INBOUND");
            stockLog.setQuantityBefore(quantityBefore);
            stockLog.setQuantityChange(quantityChange);
            stockLog.setQuantityAfter(quantityAfter);
            stockLog.setRelatedOrderNo(order.getOrderNo());
            stockLogMapper.insert(stockLog);
            logger.info("[InboundAudit] Stock log created - LogId: {}", stockLog.getId());

            logger.info("[InboundAudit] Triggering stock alert check - ProductId: {}", product.getId());
            stockAlertService.checkAndCreateAlerts(product.getId());
        }

        order.setStatus("COMPLETED");
        order.setInboundTime(LocalDateTime.now());
        inboundOrderMapper.updateById(order);
        logger.info("[InboundAudit] Order status updated to 'completed', InboundTime: {}", order.getInboundTime());
        logger.info("========== END: Audit Inbound Order - OrderNo: {} ==========", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInboundOrder(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "Inbound order not found");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "Order status not allowed for cancellation");
        }

        order.setStatus("CANCELLED");
        inboundOrderMapper.updateById(order);
    }

    @Override
    public IPage<InboundOrderVo> pageQuery(InboundOrderQueryDto queryDto) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDto.getOrderNo()), InboundOrder::getOrderNo, queryDto.getOrderNo())
                .eq(StringUtils.hasText(queryDto.getStatus()), InboundOrder::getStatus, queryDto.getStatus())
                .ge(queryDto.getStartTime() != null, InboundOrder::getCreateTime, queryDto.getStartTime())
                .le(queryDto.getEndTime() != null, InboundOrder::getCreateTime, queryDto.getEndTime())
                .orderByDesc(InboundOrder::getCreateTime);

        Page<InboundOrder> page = new Page<>(queryDto.getPageNum() != null ? queryDto.getPageNum() : 1,
                queryDto.getPageSize() != null ? queryDto.getPageSize() : 10);
        IPage<InboundOrder> orderPage = inboundOrderMapper.selectPage(page, wrapper);

        IPage<InboundOrderVo> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream().map(this::convertToVo).collect(Collectors.toList()));
        return voPage;
    }

    private InboundOrderVo convertToVo(InboundOrder order) {
        InboundOrderVo vo = new InboundOrderVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setSupplier(order.getSupplier());
        vo.setInboundType(order.getInboundType());
        vo.setRelatedOrderNo(order.getRelatedOrderNo());
        vo.setOperatorId(order.getOperatorId());
        vo.setStatus(order.getStatus());
        vo.setInboundTime(order.getInboundTime());
        vo.setRemark(order.getRemark());
        vo.setCreateBy(order.getCreateBy());
        vo.setCreateTime(order.getCreateTime());

        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getInboundOrderId, order.getId());
        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(itemWrapper);
        vo.setItems(items.stream().map(item -> {
            InboundOrderVo.InboundOrderItemVo itemVo = new InboundOrderVo.InboundOrderItemVo();
            itemVo.setId(item.getId());
            itemVo.setInboundOrderId(item.getInboundOrderId());
            itemVo.setProductId(item.getProductId());
            itemVo.setExpectedQuantity(item.getExpectedQuantity());
            itemVo.setActualQuantity(item.getActualQuantity());
            itemVo.setUnitPrice(item.getUnitPrice());
            itemVo.setSubtotal(item.getSubtotal());
            itemVo.setBatchNo(item.getBatchNo());
            itemVo.setProductionDate(item.getProductionDate());
            itemVo.setExpiryDate(item.getExpiryDate());

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
        RLock lock = redissonClient.getLock("order-no-lock:inbound");
        try {
            lock.lockInterruptibly(10, TimeUnit.SECONDS);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.likeRight(InboundOrder::getOrderNo, "IN" + dateStr)
                    .orderByDesc(InboundOrder::getOrderNo)
                    .last("LIMIT 1");
            InboundOrder lastOrder = inboundOrderMapper.selectOne(wrapper);

            int sequence = 1;
            if (lastOrder != null && lastOrder.getOrderNo().startsWith("IN" + dateStr)) {
                String lastSeq = lastOrder.getOrderNo().substring(("IN" + dateStr).length());
                sequence = Integer.parseInt(lastSeq) + 1;
            }
            return "IN" + dateStr + String.format("%04d", sequence);
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
