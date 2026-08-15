package com.wms.service.impl;

import com.wms.dto.InboundOrderDto;
import com.wms.dto.InboundOrderItemDto;
import com.wms.dto.InboundOrderQueryDto;
import com.wms.dto.PageResult;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.exception.BusinessException;
import com.wms.mapper.InboundOrderMapper;
import com.wms.mapper.ProductMapper;
import com.wms.service.InboundOrderService;
import com.wms.vo.InboundOrderVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InboundOrderServiceImpl implements InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;
    private final ProductMapper productMapper;
    private final com.wms.mapper.StockLogMapper stockLogMapper;

    public InboundOrderServiceImpl(InboundOrderMapper inboundOrderMapper, ProductMapper productMapper,
                                   com.wms.mapper.StockLogMapper stockLogMapper) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.productMapper = productMapper;
        this.stockLogMapper = stockLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInboundOrder(InboundOrderDto dto) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = inboundOrderMapper.selectSeq();
        String orderNo = "IN" + dateStr + String.format("%04d", seq + 1);

        InboundOrder order = new InboundOrder();
        order.setOrderNo(orderNo);
        order.setWarehouseId(dto.getWarehouseId());
        order.setInboundType(dto.getInboundType());
        order.setSupplier(dto.getSupplier());
        order.setRelatedOrderNo(dto.getRelatedOrderNo());
        order.setStatus("PENDING");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        inboundOrderMapper.insert(order);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (InboundOrderItemDto itemDto : dto.getItems()) {
                InboundOrderItem item = new InboundOrderItem();
                item.setOrderId(order.getId());
                item.setProductId(itemDto.getProductId());
                item.setExpectedQuantity(itemDto.getExpectedQuantity());
                item.setActualQuantity(itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : 0);
                item.setUnitPrice(itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : BigDecimal.ZERO);
                BigDecimal subtotal = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getActualQuantity() != null ? item.getActualQuantity() : 0));
                item.setSubtotal(subtotal);
                total = total.add(subtotal);
                inboundOrderMapper.insertItem(item);
            }
            order.setTotalAmount(total);
            inboundOrderMapper.update(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInboundOrder(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "入库单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "该入库单不允许审核");
        }

        List<InboundOrderItem> items = inboundOrderMapper.selectItemsByOrderId(id);
        BigDecimal total = BigDecimal.ZERO;
        for (InboundOrderItem item : items) {
            if (item.getActualQuantity() == null || item.getActualQuantity() <= 0) {
                throw new BusinessException(400, "请完成实入数量录入: " + item.getProductName());
            }
            int beforeStock = item.getProductId() != null ? getProductStock(item.getProductId()) : 0;
            int afterStock = beforeStock + item.getActualQuantity();
            if (item.getProductId() != null) {
                productMapper.updateVersionById(item.getProductId(), 0, afterStock);
            }
            total = total.add(item.getUnitPrice() != null
                    ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getActualQuantity()))
                    : BigDecimal.ZERO);
            createStockLog(item.getProductId(), item.getProductCode(), item.getProductName(), "IN",
                    item.getActualQuantity(), beforeStock, afterStock, id);
        }

        order.setStatus("COMPLETED");
        order.setInboundTime(LocalDateTime.now());
        order.setTotalAmount(total);
        inboundOrderMapper.update(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInboundOrder(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "入库单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "只有待审核状态的入库单可以取消");
        }
        order.setStatus("CANCELLED");
        inboundOrderMapper.update(order);
    }

    @Override
    public PageResult<InboundOrderVo> pageQuery(InboundOrderQueryDto queryDto) {
        int pageNum = queryDto.getPageNum() != null ? queryDto.getPageNum() : 1;
        int pageSize = queryDto.getPageSize() != null ? queryDto.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        List<InboundOrder> records = inboundOrderMapper.selectPage(offset, pageSize,
                queryDto.getOrderNo(), queryDto.getStatus(),
                queryDto.getStartTime(), queryDto.getEndTime());
        int total = inboundOrderMapper.selectCount(
                queryDto.getOrderNo(), queryDto.getStatus(),
                queryDto.getStartTime(), queryDto.getEndTime());

        List<InboundOrderVo> voList = records.stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        return new PageResult<>(voList, total, pageNum, pageSize);
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

    private InboundOrderVo convertToVo(InboundOrder order) {
        InboundOrderVo vo = new InboundOrderVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setInboundType(order.getInboundType());
        vo.setSupplier(order.getSupplier());
        vo.setRelatedOrderNo(order.getRelatedOrderNo());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        List<InboundOrderItem> items = inboundOrderMapper.selectItemsByOrderId(order.getId());
        List<InboundOrderVo.InboundOrderItemVo> itemVos = items.stream().map(item -> {
            InboundOrderVo.InboundOrderItemVo iv = new InboundOrderVo.InboundOrderItemVo();
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
