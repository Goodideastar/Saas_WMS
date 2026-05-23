package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock_log")
public class StockLog extends BaseEntity {
    private Long productId;
    private Long warehouseId;
    private String operationType;
    private Integer quantityBefore;
    private Integer quantityChange;
    private Integer quantityAfter;
    private String relatedOrderNo;
    private String remark;
}
