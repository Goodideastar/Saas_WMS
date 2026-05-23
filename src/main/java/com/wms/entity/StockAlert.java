package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock_alert")
public class StockAlert extends BaseEntity {
    private Long productId;
    private Long warehouseId;
    private String alertType;
    private Integer alertValue;
    private Integer actualStock;
    private String status;
    private String handleRemark;
}
