package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Permission extends BaseEntity {
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private Long parentId;
    private String path;
    private Integer status;
}
