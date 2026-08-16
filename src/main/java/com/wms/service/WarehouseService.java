package com.wms.service;

import com.wms.dto.PageResult;
import com.wms.dto.WarehouseDto;
import com.wms.vo.WarehouseVo;

import java.util.List;

public interface WarehouseService {

    PageResult<WarehouseVo> pageQuery(Integer pageNum, Integer pageSize, String keyword, Integer status);

    List<WarehouseVo> listEnabled();

    WarehouseVo getById(Long id);

    void addWarehouse(WarehouseDto dto);

    void updateWarehouse(WarehouseDto dto);

    void deleteWarehouse(Long id);
}
