package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
}
