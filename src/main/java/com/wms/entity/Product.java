package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {
    private String productCode;
    private String productName;
    private String specification;
    private String unit;
    private String category;
    private String imageUrl;
    private String remark;
    private Integer currentStock;
    private BigDecimal referenceCost;
    private BigDecimal referencePrice;
    private Integer alertMin;
    private Integer alertMax;
    private Integer status;
}
