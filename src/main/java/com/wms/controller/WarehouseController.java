package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.PageResult;
import com.wms.dto.WarehouseDto;
import com.wms.service.WarehouseService;
import com.wms.vo.WarehouseVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@Validated
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('warehouse:list')")
    public Result<PageResult<WarehouseVo>> pageQuery(@RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(warehouseService.pageQuery(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('warehouse:list')")
    public Result<List<WarehouseVo>> listEnabled() {
        return Result.success(warehouseService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:list')")
    public Result<WarehouseVo> getById(@PathVariable Long id) {
        return Result.success(warehouseService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:add')")
    public Result<Void> addWarehouse(@Valid @RequestBody WarehouseDto dto) {
        warehouseService.addWarehouse(dto);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('warehouse:edit')")
    public Result<Void> updateWarehouse(@Valid @RequestBody WarehouseDto dto) {
        warehouseService.updateWarehouse(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:delete')")
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.success();
    }
}
