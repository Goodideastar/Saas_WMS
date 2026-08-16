package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.InboundOrderDto;
import com.wms.dto.InboundOrderQueryDto;
import com.wms.dto.PageResult;
import com.wms.service.InboundOrderService;
import com.wms.vo.InboundOrderVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
@Validated
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @PostMapping
    @PreAuthorize("hasAuthority('inbound:create')")
    public Result<Void> createInboundOrder(@Valid @RequestBody InboundOrderDto dto) {
        inboundOrderService.createInboundOrder(dto);
        return Result.success();
    }

    @PutMapping("/audit/{id}")
    @PreAuthorize("hasAuthority('inbound:audit')")
    public Result<Void> auditInboundOrder(@PathVariable Long id) {
        inboundOrderService.auditInboundOrder(id);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasAuthority('inbound:cancel')")
    public Result<Void> cancelInboundOrder(@PathVariable Long id) {
        inboundOrderService.cancelInboundOrder(id);
        return Result.success();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('inbound:query')")
    public Result<PageResult<InboundOrderVo>> pageQuery(@Validated InboundOrderQueryDto queryDto) {
        PageResult<InboundOrderVo> page = inboundOrderService.pageQuery(queryDto);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inbound:query')")
    public Result<InboundOrderVo> getById(@PathVariable Long id) {
        return Result.success(inboundOrderService.getById(id));
    }
}