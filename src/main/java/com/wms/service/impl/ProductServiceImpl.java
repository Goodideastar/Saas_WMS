package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wms.dto.BatchStockAdjustDto;
import com.wms.dto.ProductDto;
import com.wms.dto.ProductQueryDto;
import com.wms.dto.StockAdjustDto;
import com.wms.entity.Product;
import com.wms.entity.StockLog;
import com.wms.exception.BusinessException;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockLogMapper;
import com.wms.service.ProductService;
import com.wms.vo.ProductVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;
    private final StockLogMapper stockLogMapper;

    @Override
    public IPage<ProductVo> pageQuery(ProductQueryDto queryDto) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDto.getProductCode()), Product::getProductCode, queryDto.getProductCode())
                .like(StringUtils.hasText(queryDto.getProductName()), Product::getProductName, queryDto.getProductName())
                .eq(StringUtils.hasText(queryDto.getCategory()), Product::getCategory, queryDto.getCategory())
                .eq(queryDto.getStatus() != null, Product::getStatus, queryDto.getStatus())
                .orderByDesc(Product::getCreateTime);

        Page<Product> page = new Page<>(queryDto.getPageNum() != null ? queryDto.getPageNum() : 1,
                queryDto.getPageSize() != null ? queryDto.getPageSize() : 10);
        IPage<Product> productPage = productMapper.selectPage(page, wrapper);

        IPage<ProductVo> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream().map(this::convertToVo).toList());
        return voPage;
    }

    @Override
    @CacheEvict(value = "product", allEntries = true)
    public void addProduct(ProductDto dto) {
        if (checkProductCodeExists(dto.getProductCode(), null)) {
            throw new BusinessException(400, "Product code already exists");
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        productMapper.insert(product);
    }

    @Override
    @CacheEvict(value = "product", allEntries = true)
    public void updateProduct(ProductDto dto) {
        if (checkProductCodeExists(dto.getProductCode(), dto.getId())) {
            throw new BusinessException(400, "Product code already exists");
        }

        Product product = productMapper.selectById(dto.getId());
        if (product == null) {
            throw new BusinessException(404, "Product not found");
        }

        BeanUtils.copyProperties(dto, product);
        productMapper.updateById(product);
    }

    @Override
    @CacheEvict(value = "product", allEntries = true)
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(StockAdjustDto dto) {
        Product product = productMapper.selectForUpdate(dto.getProductId());
        if (product == null) {
            throw new BusinessException(404, "Product not found");
        }

        int quantityBefore = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int quantityChange = dto.getQuantity();
        int quantityAfter;

        if ("IN".equals(dto.getAdjustType())) {
            quantityAfter = quantityBefore + quantityChange;
        } else if ("OUT".equals(dto.getAdjustType())) {
            if (quantityBefore < quantityChange) {
                throw new BusinessException(400, "Insufficient stock");
            }
            quantityAfter = quantityBefore - quantityChange;
        } else {
            throw new BusinessException(400, "Invalid adjust type");
        }

        LambdaQueryWrapper<Product> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Product::getId, product.getId())
                .eq(Product::getVersion, product.getVersion());
        product.setCurrentStock(quantityAfter);
        product.setVersion(product.getVersion() + 1);
        int updated = productMapper.update(product, updateWrapper);
        if (updated == 0) {
            throw new BusinessException(400, "Stock adjustment failed, please retry");
        }

        StockLog stockLog = new StockLog();
        stockLog.setProductId(product.getId());
        stockLog.setOperationType(dto.getAdjustType());
        stockLog.setQuantityBefore(quantityBefore);
        stockLog.setQuantityChange(quantityChange);
        stockLog.setQuantityAfter(quantityAfter);
        stockLog.setRemark(dto.getRemark());
        stockLogMapper.insert(stockLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAdjustStock(BatchStockAdjustDto dto) {
        for (BatchStockAdjustDto.BatchAdjustItem item : dto.getItems()) {
            StockAdjustDto singleDto = new StockAdjustDto();
            singleDto.setProductId(item.getProductId());
            singleDto.setQuantity(item.getQuantity());
            singleDto.setAdjustType(dto.getAdjustType());
            singleDto.setRemark(dto.getRemark());
            adjustStock(singleDto);
        }
    }

    @Override
    public boolean checkProductCodeExists(String productCode, Long excludeId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getProductCode, productCode);
        if (excludeId != null) {
            wrapper.ne(Product::getId, excludeId);
        }
        return productMapper.selectCount(wrapper) > 0;
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    private ProductVo convertToVo(Product product) {
        ProductVo vo = new ProductVo();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
