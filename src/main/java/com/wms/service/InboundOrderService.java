package com.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wms.dto.InboundOrderDto;
import com.wms.dto.InboundOrderQueryDto;
import com.wms.entity.InboundOrder;
import com.wms.vo.InboundOrderVo;

public interface InboundOrderService extends IService<InboundOrder> {

    void createInboundOrder(InboundOrderDto dto);

    void auditInboundOrder(Long id);

    void cancelInboundOrder(Long id);

    IPage<InboundOrderVo> pageQuery(InboundOrderQueryDto queryDto);
}