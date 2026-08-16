package com.wms.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    Map<String, Object> todayInboundStats();

    Map<String, Object> todayOutboundStats();

    Long totalStock();

    List<Map<String, Object>> last7DaysTrend();

    Map<String, Object> alertStats();

    List<Map<String, Object>> topOutboundProducts(@org.apache.ibatis.annotations.Param("limit") int limit);

    List<Map<String, Object>> warehouseDistribution();
}
