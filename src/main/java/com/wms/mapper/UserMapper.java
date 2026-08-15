package com.wms.mapper;

import com.wms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User selectById(Long id);
    int insert(User user);
    int update(User user);
    int deleteById(Long id);

    User selectByUsername(@Param("username") String username);
    List<User> selectList(@Param("offset") int offset, @Param("limit") int limit,
                          @Param("status") Integer status, @Param("keyword") String keyword);
    int selectCount(@Param("status") Integer status, @Param("keyword") String keyword);
}
