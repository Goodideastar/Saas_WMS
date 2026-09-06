package com.wms.mapper;

import com.wms.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseMapper {
    Warehouse selectById(Long id);

    List<Warehouse> selectPage(@Param("offset") int offset,
                               @Param("pageSize") int pageSize,
                               @Param("keyword") String keyword,
                               @Param("status") Integer status);

    int selectCount(@Param("keyword") String keyword,
                    @Param("status") Integer status);

    List<Warehouse> selectAllEnabled();

    int countByCode(@Param("warehouseCode") String warehouseCode,
                    @Param("excludeId") Long excludeId);

    int insert(Warehouse warehouse);

    int updateById(Warehouse warehouse, @Param("version") int version);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);

    int countOrdersByWarehouseId(@Param("warehouseId") Long warehouseId);
}
