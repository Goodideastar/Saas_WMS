package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class InboundOrderItem extends BaseEntity {
    private Long orderId;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
