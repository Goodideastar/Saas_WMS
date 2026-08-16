package com.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseVo {
    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private String location;
    private String contactPerson;
    private String contactPhone;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private String createBy;
}
