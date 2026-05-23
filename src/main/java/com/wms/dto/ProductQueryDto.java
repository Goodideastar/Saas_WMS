package com.wms.dto;

import lombok.Data;

@Data
public class ProductQueryDto {
    private String productCode;
    private String productName;
    private String category;
    private Integer status;
    private Integer pageNum;
    private Integer pageSize;
}
