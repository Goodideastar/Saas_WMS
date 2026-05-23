package com.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustDto {
    @NotNull(message = "货品ID不能为空")
    private Long productId;

    @NotNull(message = "调整数量不能为空")
    private Integer quantity;

    @NotNull(message = "调整类型不能为空")
    private String adjustType;

    private String remark;
}
