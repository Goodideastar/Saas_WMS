package com.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    @Select("SELECT * FROM wms_inbound_order WHERE id = #{id} FOR UPDATE")
    InboundOrder selectForUpdate(@Param("id") Long id);
}