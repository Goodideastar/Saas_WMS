package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_outbound_order")
public class OutboundOrder extends BaseEntity {
    private String orderNo;
    private Long warehouseId;
    private String customer;
    private String outboundType;
    private String relatedOrderNo;
    private Long operatorId;
    private String status;
    private LocalDateTime outboundTime;
    private String remark;
}
