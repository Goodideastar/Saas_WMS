package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inbound_order_item")
public class InboundOrderItem extends BaseEntity {
    private Long inboundOrderId;
    private Long productId;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
}
