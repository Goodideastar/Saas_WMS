package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockLog extends BaseEntity {
    private Long productId;
    private String productCode;
    private String productName;
    private String stockType;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private Long orderId;
}
