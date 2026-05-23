package com.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wms.dto.StockAlertHandleDto;
import com.wms.dto.StockAlertQueryDto;
import com.wms.vo.StockAlertVo;

public interface StockAlertService {

    void checkAndCreateAlerts(Long productId);

    IPage<StockAlertVo> pageQuery(StockAlertQueryDto queryDto);

    void handleAlert(StockAlertHandleDto dto);

    void batchUpdateThresholds(java.util.List<com.wms.dto.StockThresholdDto> thresholds);
}