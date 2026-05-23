package com.wms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDto {
    private Long id;

    @NotBlank(message = "货品编码不能为空")
    private String productCode;

    @NotBlank(message = "货品名称不能为空")
    private String productName;

    private String specification;

    @NotBlank(message = "单位不能为空")
    private String unit;

    private String category;

    private String imageUrl;

    private BigDecimal referenceCost;

    private BigDecimal referencePrice;

    @NotNull(message = "当前库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer currentStock;

    private Integer alertMin;

    private Integer alertMax;

    private Integer status;

    private String remark;
}
