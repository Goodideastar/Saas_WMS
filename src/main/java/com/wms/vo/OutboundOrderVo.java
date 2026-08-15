package com.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundOrderVo {
    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String warehouseName;
    private String customer;
    private String outboundType;
    private String relatedOrderNo;
    private Long operatorId;
    private String status;
    private java.math.BigDecimal totalAmount;
    private LocalDateTime outboundTime;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private List<OutboundOrderItemVo> items;

    @Data
    public static class OutboundOrderItemVo {
        private Long id;
        private Long outboundOrderId;
        private Long productId;
        private String productName;
        private String productCode;
        private Integer expectedQuantity;
        private Integer actualQuantity;
        private java.math.BigDecimal unitPrice;
        private java.math.BigDecimal subtotal;
    }
}