package com.wms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchStockAdjustDto {
    @NotEmpty(message = "请选择要调整的货品")
    private List<BatchAdjustItem> items;

    @NotNull(message = "调整类型不能为空")
    private String adjustType;

    private String remark;

    @Data
    public static class BatchAdjustItem {
        @NotNull(message = "货品ID不能为空")
        private Long productId;

        @NotNull(message = "调整数量不能为空")
        private Integer quantity;
    }
}
