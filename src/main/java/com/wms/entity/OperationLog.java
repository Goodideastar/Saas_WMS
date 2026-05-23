package com.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_operation_log")
public class OperationLog extends BaseEntity {
    private String operator;
    private String ip;
    private String requestUrl;
    private String methodName;
    private String params;
    private Long duration;
    private Integer status;
    private String result;
}
