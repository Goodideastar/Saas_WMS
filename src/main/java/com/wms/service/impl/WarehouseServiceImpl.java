package com.wms.service.impl;

import com.wms.dto.PageResult;
import com.wms.dto.WarehouseDto;
import com.wms.entity.Warehouse;
import com.wms.exception.BusinessException;
import com.wms.mapper.WarehouseMapper;
import com.wms.service.WarehouseService;
import com.wms.vo.WarehouseVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public WarehouseServiceImpl(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public PageResult<WarehouseVo> pageQuery(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 10;
        int offset = (page - 1) * size;

        List<Warehouse> records = warehouseMapper.selectPage(offset, size, keyword, status);
        int total = warehouseMapper.selectCount(keyword, status);

        List<WarehouseVo> voList = records.stream().map(this::convertToVo).collect(Collectors.toList());
        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public List<WarehouseVo> listEnabled() {
        return warehouseMapper.selectAllEnabled().stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseVo getById(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        return convertToVo(warehouse);
    }

    @Override
    public void addWarehouse(WarehouseDto dto) {
        if (warehouseMapper.countByCode(dto.getWarehouseCode(), null) > 0) {
            throw new BusinessException(400, "仓库编码已存在");
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode(dto.getWarehouseCode());
        warehouse.setWarehouseName(dto.getWarehouseName());
        warehouse.setLocation(dto.getLocation());
        warehouse.setContactPerson(dto.getContactPerson());
        warehouse.setContactPhone(dto.getContactPhone());
        warehouse.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        warehouse.setRemark(dto.getRemark());
        warehouseMapper.insert(warehouse);
    }

    @Override
    public void updateWarehouse(WarehouseDto dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "仓库ID不能为空");
        }
        Warehouse existing = warehouseMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        if (!existing.getWarehouseCode().equals(dto.getWarehouseCode())
                && warehouseMapper.countByCode(dto.getWarehouseCode(), dto.getId()) > 0) {
            throw new BusinessException(400, "仓库编码已存在");
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setId(dto.getId());
        warehouse.setWarehouseName(dto.getWarehouseName());
        warehouse.setLocation(dto.getLocation());
        warehouse.setContactPerson(dto.getContactPerson());
        warehouse.setContactPhone(dto.getContactPhone());
        warehouse.setStatus(dto.getStatus() != null ? dto.getStatus() : existing.getStatus());
        warehouse.setRemark(dto.getRemark());
        warehouseMapper.updateById(warehouse);
    }

    @Override
    public void deleteWarehouse(Long id) {
        Warehouse existing = warehouseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        int orderCount = warehouseMapper.countOrdersByWarehouseId(id);
        if (orderCount > 0) {
            throw new BusinessException(400, "该仓库已关联出入库单（" + orderCount + " 条），无法删除");
        }
        warehouseMapper.deleteById(id, getCurrentOperator());
    }

    private WarehouseVo convertToVo(Warehouse warehouse) {
        WarehouseVo vo = new WarehouseVo();
        vo.setId(warehouse.getId());
        vo.setWarehouseCode(warehouse.getWarehouseCode());
        vo.setWarehouseName(warehouse.getWarehouseName());
        vo.setLocation(warehouse.getLocation());
        vo.setContactPerson(warehouse.getContactPerson());
        vo.setContactPhone(warehouse.getContactPhone());
        vo.setStatus(warehouse.getStatus());
        vo.setRemark(warehouse.getRemark());
        vo.setCreateTime(warehouse.getCreateTime());
        vo.setCreateBy(warehouse.getCreateBy());
        return vo;
    }

    private String getCurrentOperator() {
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                Object principal = auth.getPrincipal();
                if (principal instanceof com.wms.security.UserDetailsImpl) {
                    return ((com.wms.security.UserDetailsImpl) principal).getUsername();
                }
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "system";
    }
}
