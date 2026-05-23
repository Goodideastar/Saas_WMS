package com.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM wms_product WHERE id = #{id} FOR UPDATE")
    Product selectForUpdate(@Param("id") Long id);
}