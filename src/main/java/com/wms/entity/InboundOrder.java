package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class InboundOrder extends BaseEntity {
    private String orderNo;
    private Long warehouseId;
    private String inboundType;
    private String supplier;
    private String relatedOrderNo;
    private String status;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime inboundTime;
    private List<InboundOrderItem> items;
}
