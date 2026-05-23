package com.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundOrderVo {
    private Long id;
    private String orderNo;
    private Long warehouseId;
    private String warehouseName;
    private String supplier;
    private String inboundType;
    private String relatedOrderNo;
    private Long operatorId;
    private String status;
    private LocalDateTime inboundTime;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private List<InboundOrderItemVo> items;

    @Data
    public static class InboundOrderItemVo {
        private Long id;
        private Long inboundOrderId;
        private Long productId;
        private String productName;
        private String productCode;
        private Integer expectedQuantity;
        private Integer actualQuantity;
        private java.math.BigDecimal unitPrice;
        private java.math.BigDecimal subtotal;
        private String batchNo;
        private java.time.LocalDate productionDate;
        private java.time.LocalDate expiryDate;
    }
}