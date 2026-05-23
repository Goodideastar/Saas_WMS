package com.wms.dto;

import lombok.Data;

@Data
public class StockAlertQueryDto {

    private String status;
    private Long productId;
    private Integer pageNum;
    private Integer pageSize;
}