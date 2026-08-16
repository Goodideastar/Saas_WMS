package com.wms.service;

import com.wms.dto.OutboundOrderDto;
import com.wms.dto.OutboundOrderQueryDto;
import com.wms.dto.PageResult;
import com.wms.vo.OutboundOrderVo;

public interface OutboundOrderService {
    void createOutboundOrder(OutboundOrderDto dto);
    void auditOutboundOrder(Long id);
    void cancelOutboundOrder(Long id);
    PageResult<OutboundOrderVo> pageQuery(OutboundOrderQueryDto queryDto);
    OutboundOrderVo getById(Long id);
}
