package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_warehouse")
public class Warehouse extends BaseEntity {
    private String warehouseCode;
    private String warehouseName;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private Integer status;
    private String remark;
}
