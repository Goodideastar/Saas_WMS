package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseDto {
    private Long id;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 50, message = "仓库编码不能超过50个字符")
    private String warehouseCode;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称不能超过100个字符")
    private String warehouseName;

    @Size(max = 255, message = "仓库地址不能超过255个字符")
    private String location;

    @Size(max = 50, message = "联系人不能超过50个字符")
    private String contactPerson;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    private Integer status;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
