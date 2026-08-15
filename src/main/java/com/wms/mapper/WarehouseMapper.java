package com.wms.mapper;

import com.wms.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WarehouseMapper {
    Warehouse selectById(Long id);
}
