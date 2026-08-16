package com.wms.service;

import com.wms.dto.InboundOrderDto;
import com.wms.dto.InboundOrderQueryDto;
import com.wms.dto.PageResult;
import com.wms.vo.InboundOrderVo;

public interface InboundOrderService {
    void createInboundOrder(InboundOrderDto dto);
    void auditInboundOrder(Long id);
    void cancelInboundOrder(Long id);
    PageResult<InboundOrderVo> pageQuery(InboundOrderQueryDto queryDto);
    InboundOrderVo getById(Long id);
}
