package com.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wms.dto.ProductDto;
import com.wms.dto.ProductQueryDto;
import com.wms.dto.StockAdjustDto;
import com.wms.entity.Product;
import com.wms.vo.ProductVo;

public interface ProductService extends IService<Product> {
    IPage<ProductVo> pageQuery(ProductQueryDto queryDto);

    void addProduct(ProductDto dto);

    void updateProduct(ProductDto dto);

    void deleteProduct(Long id);

    void adjustStock(StockAdjustDto dto);

    boolean checkProductCodeExists(String productCode, Long excludeId);
}
