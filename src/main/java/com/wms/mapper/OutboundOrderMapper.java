package com.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    @Select("SELECT * FROM wms_outbound_order WHERE id = #{id} FOR UPDATE")
    OutboundOrder selectForUpdate(@Param("id") Long id);
}