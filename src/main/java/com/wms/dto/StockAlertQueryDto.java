package com.wms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAlertQueryDto {

    private String status;
    private Long productId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageNum;
    private Integer pageSize;
}