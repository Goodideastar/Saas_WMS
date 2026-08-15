package com.wms.service;

import com.wms.dto.PageResult;
import com.wms.dto.ProductDto;
import com.wms.dto.ProductQueryDto;
import com.wms.dto.StockAdjustDto;
import com.wms.vo.ProductVo;

public interface ProductService {
    PageResult<ProductVo> pageQuery(ProductQueryDto queryDto);
    void addProduct(ProductDto dto);
    void updateProduct(ProductDto dto);
    void deleteProduct(Long id);
    void adjustStock(StockAdjustDto dto);
    void batchAdjustStock(com.wms.dto.BatchStockAdjustDto dto);
    boolean checkProductCodeExists(String productCode, Long excludeId);
}
