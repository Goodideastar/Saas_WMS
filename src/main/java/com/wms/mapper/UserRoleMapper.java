package com.wms.mapper;

import com.wms.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper {
    List<UserRole> selectByUserId(@Param("userId") Long userId);
}
