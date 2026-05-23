package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InboundOrderDto {

    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    private String supplier;

    @NotBlank(message = "入库类型不能为空")
    private String inboundType;

    private String relatedOrderNo;

    private String remark;

    @Valid
    @NotNull(message = "入库明细不能为空")
    private List<InboundOrderItemDto> items;
}