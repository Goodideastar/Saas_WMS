package com.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wms.common.Result;
import com.wms.dto.ProductDto;
import com.wms.dto.ProductQueryDto;
import com.wms.dto.StockAdjustDto;
import com.wms.service.ProductService;
import com.wms.vo.ProductVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:list')")
    public Result<IPage<ProductVo>> pageQuery(@Validated ProductQueryDto queryDto) {
        IPage<ProductVo> page = productService.pageQuery(queryDto);
        return Result.success(page);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    public Result<Void> addProduct(@Valid @RequestBody ProductDto dto) {
        productService.addProduct(dto);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('product:edit')")
    public Result<Void> updateProduct(@Valid @RequestBody ProductDto dto) {
        productService.updateProduct(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    @PostMapping("/adjustStock")
    @PreAuthorize("hasAuthority('product:adjust')")
    public Result<Void> adjustStock(@Valid @RequestBody StockAdjustDto dto) {
        productService.adjustStock(dto);
        return Result.success();
    }
}