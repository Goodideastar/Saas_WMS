package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.StockAlertHandleDto;
import com.wms.dto.StockAlertQueryDto;
import com.wms.dto.PageResult;
import com.wms.service.StockAlertService;
import com.wms.vo.StockAlertVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
@Validated
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = stockAlertService.getStats();
        return Result.success(stats);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('alert:query')")
    public Result<PageResult<StockAlertVo>> pageQuery(@Validated StockAlertQueryDto queryDto) {
        PageResult<StockAlertVo> page = stockAlertService.pageQuery(queryDto);
        return Result.success(page);
    }

    @PutMapping("/handle")
    @PreAuthorize("hasAuthority('alert:handle')")
    public Result<Void> handleAlert(@Valid @RequestBody StockAlertHandleDto dto) {
        stockAlertService.handleAlert(dto);
        return Result.success();
    }
}