package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_role_permission")
public class RolePermission implements Serializable {

    @TableField("role_id")
    private Long roleId;

    @TableField("permission_id")
    private Long permissionId;
}
