package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OutboundOrderDto {

    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    private String customer;

    @NotBlank(message = "出库类型不能为空")
    private String outboundType;

    private String relatedOrderNo;

    private String remark;

    @Valid
    @NotNull(message = "出库明细不能为空")
    private List<OutboundOrderItemDto> items;
}