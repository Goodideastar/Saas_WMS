package com.wms.service.impl;

import com.wms.dto.OutboundOrderDto;
import com.wms.dto.OutboundOrderItemDto;
import com.wms.dto.OutboundOrderQueryDto;
import com.wms.dto.PageResult;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.exception.BusinessException;
import com.wms.mapper.OutboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.WarehouseMapper;
import com.wms.service.OutboundOrderService;
import com.wms.vo.OutboundOrderVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OutboundOrderServiceImpl implements OutboundOrderService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final com.wms.mapper.StockLogMapper stockLogMapper;

    public OutboundOrderServiceImpl(OutboundOrderMapper outboundOrderMapper, ProductMapper productMapper,
                                    WarehouseMapper warehouseMapper,
                                    com.wms.mapper.StockLogMapper stockLogMapper) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockLogMapper = stockLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOutboundOrder(OutboundOrderDto dto) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = outboundOrderMapper.selectSeq();
        String orderNo = "OUT" + dateStr + String.format("%04d", seq + 1);

        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(orderNo);
        order.setWarehouseId(dto.getWarehouseId());
        order.setOutboundType(dto.getOutboundType());
        order.setCustomer(dto.getCustomer());
        order.setRelatedOrderNo(dto.getRelatedOrderNo());
        order.setStatus("PENDING");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        outboundOrderMapper.insert(order);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (OutboundOrderItemDto itemDto : dto.getItems()) {
                OutboundOrderItem item = new OutboundOrderItem();
                item.setOrderId(order.getId());
                item.setProductId(itemDto.getProductId());
                if (itemDto.getProductId() != null) {
                    var product = productMapper.selectById(itemDto.getProductId());
                    if (product != null) {
                        item.setProductCode(product.getProductCode());
                        item.setProductName(product.getProductName());
                    }
                }
                item.setExpectedQuantity(itemDto.getExpectedQuantity());
                item.setActualQuantity(itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : 0);
                item.setUnitPrice(itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : BigDecimal.ZERO);
                BigDecimal subtotal = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getActualQuantity() != null ? item.getActualQuantity() : 0));
                item.setSubtotal(subtotal);
                total = total.add(subtotal);
                outboundOrderMapper.insertItem(item);
            }
            order.setTotalAmount(total);
            outboundOrderMapper.update(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOutboundOrder(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "出库单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "该出库单不允许审核");
        }

        List<OutboundOrderItem> items = outboundOrderMapper.selectItemsByOrderId(id);
        BigDecimal total = BigDecimal.ZERO;
        for (OutboundOrderItem item : items) {
            if (item.getActualQuantity() == null || item.getActualQuantity() <= 0) {
                throw new BusinessException(400, "请完成实出数量录入: " + item.getProductName());
            }
            int beforeStock = item.getProductId() != null ? getProductStock(item.getProductId()) : 0;
            if (beforeStock < item.getActualQuantity()) {
                throw new BusinessException(400, "库存不足，无法出库: " + item.getProductName());
            }
            int afterStock = beforeStock - item.getActualQuantity();
            if (item.getProductId() != null) {
                productMapper.updateVersionById(item.getProductId(), 0, afterStock);
            }
            total = total.add(item.getUnitPrice() != null
                    ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getActualQuantity()))
                    : BigDecimal.ZERO);
            createStockLog(item.getProductId(), item.getProductCode(), item.getProductName(), "OUT",
                    item.getActualQuantity(), beforeStock, afterStock, id);
        }

        order.setStatus("COMPLETED");
        order.setOutboundTime(LocalDateTime.now());
        order.setTotalAmount(total);
        outboundOrderMapper.update(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOutboundOrder(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "出库单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "只有待审核状态的出库单可以取消");
        }
        order.setStatus("CANCELLED");
        outboundOrderMapper.update(order);
    }

    @Override
    public PageResult<OutboundOrderVo> pageQuery(OutboundOrderQueryDto queryDto) {
        int pageNum = queryDto.getPageNum() != null ? queryDto.getPageNum() : 1;
        int pageSize = queryDto.getPageSize() != null ? queryDto.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        List<OutboundOrder> records = outboundOrderMapper.selectPage(offset, pageSize,
                queryDto.getOrderNo(), queryDto.getStatus(),
                queryDto.getStartTime(), queryDto.getEndTime());
        int total = outboundOrderMapper.selectCount(
                queryDto.getOrderNo(), queryDto.getStatus(),
                queryDto.getStartTime(), queryDto.getEndTime());

        List<OutboundOrderVo> voList = records.stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public OutboundOrderVo getById(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "出库单不存在");
        }
        return convertToVo(order);
    }

    private int getProductStock(Long productId) {
        if (productId == null) return 0;
        var product = productMapper.selectById(productId);
        return product != null ? (product.getCurrentStock() != null ? product.getCurrentStock() : 0) : 0;
    }

    private void createStockLog(Long productId, String productCode, String productName,
                                String stockType, int quantity, int before, int after, Long orderId) {
        com.wms.entity.StockLog log = new com.wms.entity.StockLog();
        log.setProductId(productId);
        log.setProductCode(productCode);
        log.setProductName(productName);
        log.setStockType(stockType);
        log.setQuantity(quantity);
        log.setBeforeStock(before);
        log.setAfterStock(after);
        log.setOrderId(orderId);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    private OutboundOrderVo convertToVo(OutboundOrder order) {
        OutboundOrderVo vo = new OutboundOrderVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        if (order.getWarehouseId() != null) {
            var warehouse = warehouseMapper.selectById(order.getWarehouseId());
            vo.setWarehouseName(warehouse != null ? warehouse.getWarehouseName() : null);
        }
        vo.setOutboundType(order.getOutboundType());
        vo.setCustomer(order.getCustomer());
        vo.setRelatedOrderNo(order.getRelatedOrderNo());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setCreateBy(order.getCreateBy());
        vo.setOutboundTime(order.getOutboundTime());
        List<OutboundOrderItem> items = outboundOrderMapper.selectItemsByOrderId(order.getId());
        List<OutboundOrderVo.OutboundOrderItemVo> itemVos = items.stream().map(item -> {
            OutboundOrderVo.OutboundOrderItemVo iv = new OutboundOrderVo.OutboundOrderItemVo();
            iv.setId(item.getId());
            iv.setProductId(item.getProductId());
            iv.setProductCode(item.getProductCode());
            iv.setProductName(item.getProductName());
            iv.setExpectedQuantity(item.getExpectedQuantity());
            iv.setActualQuantity(item.getActualQuantity());
            iv.setUnitPrice(item.getUnitPrice());
            iv.setSubtotal(item.getSubtotal());
            return iv;
        }).collect(Collectors.toList());
        vo.setItems(itemVos);
        return vo;
    }
}
