package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockAlertHandle extends BaseEntity {
    private Long alertId;
    private LocalDateTime handleTime;
    private String handleResult;
}
