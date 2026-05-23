package com.wms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockThresholdDto {

    @NotNull(message = "货品ID不能为空")
    private Long productId;

    private Integer alertMin;
    private Integer alertMax;
}