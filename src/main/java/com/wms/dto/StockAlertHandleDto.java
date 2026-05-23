package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAlertHandleDto {

    @NotNull(message = "预警ID不能为空")
    private Long id;

    @NotBlank(message = "处理备注不能为空")
    private String handleRemark;
}