package com.wms.mapper;

import com.wms.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolePermissionMapper {
    List<RolePermission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}
