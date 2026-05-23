package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

    @TableField("permission_code")
    private String permissionCode;

    @TableField("permission_name")
    private String permissionName;

    @TableField("resource_type")
    private String resourceType;

    @TableField("parent_id")
    private Long parentId;

    @TableField("path")
    private String path;

    @TableField("status")
    private Integer status;
}
