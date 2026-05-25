package com.wms.vo;

import lombok.Data;

@Data
public class TodaySummaryVo {
    private Long inboundCount;
    private Long outboundCount;
    private Long alertCount;
    private Integer totalProducts;
    private Integer totalStock;
    private java.math.BigDecimal inboundAmount;
    private java.math.BigDecimal outboundAmount;
}