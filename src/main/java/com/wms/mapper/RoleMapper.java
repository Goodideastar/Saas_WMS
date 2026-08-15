package com.wms.mapper;

import com.wms.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    Role selectById(Long id);
    List<Role> selectByIds(@Param("ids") List<Long> ids);
}
