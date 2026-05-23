package com.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVo {
    private Long id;
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
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
