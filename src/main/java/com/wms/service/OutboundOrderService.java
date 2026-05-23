package com.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wms.dto.OutboundOrderDto;
import com.wms.dto.OutboundOrderQueryDto;
import com.wms.entity.OutboundOrder;
import com.wms.vo.OutboundOrderVo;

public interface OutboundOrderService extends IService<OutboundOrder> {

    void createOutboundOrder(OutboundOrderDto dto);

    void auditOutboundOrder(Long id);

    void cancelOutboundOrder(Long id);

    IPage<OutboundOrderVo> pageQuery(OutboundOrderQueryDto queryDto);
}