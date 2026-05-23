package com.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAlertVo {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private Long warehouseId;
    private String alertType;
    private Integer alertValue;
    private Integer actualStock;
    private String status;
    private String handleRemark;
    private String createBy;
    private LocalDateTime createTime;
}