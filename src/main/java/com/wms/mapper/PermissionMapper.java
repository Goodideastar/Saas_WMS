package com.wms.mapper;

import com.wms.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {
    Permission selectById(Long id);
    List<Permission> selectByIds(@Param("ids") List<Long> ids);
}
