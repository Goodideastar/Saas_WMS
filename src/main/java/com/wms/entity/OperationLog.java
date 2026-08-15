package com.wms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLog extends BaseEntity {
    private String operator;
    private String ip;
    private String requestUrl;
    private String methodName;
    private Long duration;
    private Integer status;
    private String result;
}
