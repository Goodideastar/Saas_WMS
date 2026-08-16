package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.OutboundOrderDto;
import com.wms.dto.OutboundOrderQueryDto;
import com.wms.dto.PageResult;
import com.wms.service.OutboundOrderService;
import com.wms.vo.OutboundOrderVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outbound")
@RequiredArgsConstructor
@Validated
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    @PostMapping
    @PreAuthorize("hasAuthority('outbound:create')")
    public Result<Void> createOutboundOrder(@Valid @RequestBody OutboundOrderDto dto) {
        outboundOrderService.createOutboundOrder(dto);
        return Result.success();
    }

    @PutMapping("/audit/{id}")
    @PreAuthorize("hasAuthority('outbound:audit')")
    public Result<Void> auditOutboundOrder(@PathVariable Long id) {
        outboundOrderService.auditOutboundOrder(id);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasAuthority('outbound:cancel')")
    public Result<Void> cancelOutboundOrder(@PathVariable Long id) {
        outboundOrderService.cancelOutboundOrder(id);
        return Result.success();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('outbound:query')")
    public Result<PageResult<OutboundOrderVo>> pageQuery(@Validated OutboundOrderQueryDto queryDto) {
        PageResult<OutboundOrderVo> page = outboundOrderService.pageQuery(queryDto);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('outbound:query')")
    public Result<OutboundOrderVo> getById(@PathVariable Long id) {
        return Result.success(outboundOrderService.getById(id));
    }
}