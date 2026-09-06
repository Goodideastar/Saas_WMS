package com.wms.service.impl;

import com.wms.dto.BatchStockAdjustDto;
import com.wms.dto.PageResult;
import com.wms.dto.ProductDto;
import com.wms.dto.ProductQueryDto;
import com.wms.dto.StockAdjustDto;
import com.wms.entity.Product;
import com.wms.exception.BusinessException;
import com.wms.mapper.ProductMapper;
import com.wms.mapper.StockLogMapper;
import com.wms.service.ProductService;
import com.wms.vo.ProductVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final StockLogMapper stockLogMapper;

    public ProductServiceImpl(ProductMapper productMapper, StockLogMapper stockLogMapper) {
        this.productMapper = productMapper;
        this.stockLogMapper = stockLogMapper;
    }

    @Override
    public PageResult<ProductVo> pageQuery(ProductQueryDto queryDto) {
        int pageNum = queryDto.getPageNum() != null ? queryDto.getPageNum() : 1;
        int pageSize = queryDto.getPageSize() != null ? queryDto.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        List<Product> records = productMapper.selectPage(offset, pageSize,
                queryDto.getCategory(), queryDto.getKeyword(),
                queryDto.getMinStock(), queryDto.getMaxStock(), queryDto.getStatus());
        int total = productMapper.selectCount(
                queryDto.getCategory(), queryDto.getKeyword(),
                queryDto.getMinStock(), queryDto.getMaxStock(), queryDto.getStatus());

        List<ProductVo> voList = records.stream().map(this::convertToVo).collect(Collectors.toList());
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    public void addProduct(ProductDto dto) {
        if (checkProductCodeExists(dto.getProductCode(), null)) {
            throw new BusinessException(400, "货品编码已存在");
        }
        Product product = new Product();
        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setSpecification(dto.getSpecification());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setImageUrl(dto.getImageUrl());
        product.setRemark(dto.getRemark());
        product.setCurrentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : 0);
        product.setReferenceCost(dto.getReferenceCost());
        product.setReferencePrice(dto.getReferencePrice());
        product.setAlertMin(dto.getAlertMin());
        product.setAlertMax(dto.getAlertMax());
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        productMapper.insert(product);
    }

    @Override
    public void updateProduct(ProductDto dto) {
        Product product = productMapper.selectById(dto.getId());
        if (product == null) {
            throw new BusinessException(404, "Product not found");
        }
        if (!product.getProductCode().equals(dto.getProductCode()) && checkProductCodeExists(dto.getProductCode(), null)) {
            throw new BusinessException(400, "货品编码已存在");
        }
        product.setProductName(dto.getProductName());
        product.setSpecification(dto.getSpecification());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setImageUrl(dto.getImageUrl());
        product.setRemark(dto.getRemark());
        product.setReferenceCost(dto.getReferenceCost());
        product.setReferencePrice(dto.getReferencePrice());
        product.setAlertMin(dto.getAlertMin());
        product.setAlertMax(dto.getAlertMax());
        productMapper.update(product);
    }

    @Override
    public void deleteProduct(Long id) {
        productMapper.softDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(StockAdjustDto dto) {
        Product product = productMapper.selectForUpdate(dto.getProductId());
        if (product == null) {
            throw new BusinessException(404, "Product not found");
        }
        int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int quantity = dto.getQuantity() != null ? dto.getQuantity() : 1;
        String adjustType = dto.getAdjustType();

        if ("IN".equals(adjustType)) {
            productMapper.adjustStockById(dto.getProductId(), quantity);
        } else if ("OUT".equals(adjustType)) {
            if (currentStock < quantity) {
                throw new BusinessException(400, "库存不足");
            }
            productMapper.adjustStockById(dto.getProductId(), -quantity);
        } else {
            throw new BusinessException(400, "Invalid adjustType");
        }

        int newStock = "IN".equals(adjustType) ? currentStock + quantity : currentStock - quantity;
        productMapper.updateVersionById(dto.getProductId(), product.getVersion(), newStock);

        // stock log
        com.wms.entity.StockLog log = new com.wms.entity.StockLog();
        log.setProductId(dto.getProductId());
        log.setProductCode(product.getProductCode());
        log.setProductName(product.getProductName());
        log.setStockType(adjustType);
        log.setQuantity(quantity);
        log.setBeforeStock(currentStock);
        log.setAfterStock(newStock);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
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
        List<Product> products = productMapper.selectList(productCode);
        if (excludeId != null) {
            products.removeIf(p -> excludeId.equals(p.getId()));
        }
        return !products.isEmpty();
    }

    private ProductVo convertToVo(Product p) {
        ProductVo vo = new ProductVo();
        vo.setId(p.getId());
        vo.setProductCode(p.getProductCode());
        vo.setProductName(p.getProductName());
        vo.setSpecification(p.getSpecification());
        vo.setUnit(p.getUnit());
        vo.setCategory(p.getCategory());
        vo.setImageUrl(p.getImageUrl());
        vo.setRemark(p.getRemark());
        vo.setCurrentStock(p.getCurrentStock());
        vo.setReferenceCost(p.getReferenceCost());
        vo.setReferencePrice(p.getReferencePrice());
        vo.setAlertMin(p.getAlertMin());
        vo.setAlertMax(p.getAlertMax());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }
}
