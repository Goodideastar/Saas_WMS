package com.wms.mapper;

import com.wms.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockLogMapper {
    int insert(StockLog log);
    List<StockLog> selectByProductId(Long productId);
}
