package com.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InboundOrderItemDto {

    @NotNull(message = "货品ID不能为空")
    private Long productId;

    @NotNull(message = "预期数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer expectedQuantity;

    private Integer actualQuantity;

    private BigDecimal unitPrice;

    private String batchNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
}