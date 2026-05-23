package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_outbound_order_item")
public class OutboundOrderItem extends BaseEntity {
    private Long outboundOrderId;
    private Long productId;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
