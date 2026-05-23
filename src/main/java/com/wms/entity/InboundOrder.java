package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inbound_order")
public class InboundOrder extends BaseEntity {
    private String orderNo;
    private Long warehouseId;
    private String supplier;
    private String inboundType;
    private String relatedOrderNo;
    private Long operatorId;
    private String status;
    private LocalDateTime inboundTime;
    private String remark;
}
