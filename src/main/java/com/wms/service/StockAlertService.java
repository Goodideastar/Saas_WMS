package com.wms.service;

import com.wms.dto.PageResult;
import com.wms.dto.StockAlertHandleDto;
import com.wms.dto.StockAlertQueryDto;
import com.wms.dto.StockThresholdDto;
import com.wms.vo.StockAlertVo;

import java.util.List;
import java.util.Map;

public interface StockAlertService {
    void checkAndCreateAlerts(Long productId);
    PageResult<StockAlertVo> pageQuery(StockAlertQueryDto queryDto);
    void handleAlert(StockAlertHandleDto dto);
    void batchUpdateThresholds(List<StockThresholdDto> thresholds);
    Map<String, Object> getStats();
}
