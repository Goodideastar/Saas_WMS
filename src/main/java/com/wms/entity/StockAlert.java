package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockAlert extends BaseEntity {
    private Long productId;
    private String productCode;
    private String productName;
    private Long warehouseId;
    private String alertType;
    private Integer alertValue;
    private Integer actualStock;
    private String status;
    private String handleRemark;
    private LocalDateTime alertTime;
    private LocalDateTime handleTime;
    private List<StockAlertHandle> handles;
}
