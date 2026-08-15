package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class Warehouse extends BaseEntity {
    private String warehouseCode;
    private String warehouseName;
    private String location;
    private Integer status;
}
