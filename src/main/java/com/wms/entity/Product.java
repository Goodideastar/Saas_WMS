package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_product")
public class Product extends BaseEntity {
    private String productCode;
    private String productName;
    private String specification;
    private String unit;
    private String category;
    private String imageUrl;
    private BigDecimal referenceCost;
    private BigDecimal referencePrice;
    private Integer currentStock;
    private Integer alertMin;
    private Integer alertMax;
    private Integer status;
    private String remark;
}
