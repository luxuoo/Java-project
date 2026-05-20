package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.SysLog;
import com.gym.mapper.SysLogMapper;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/admin/log")
@PreAuthorize("hasRole('ADMIN')")
public class SysLogController {

    private final SysLogMapper logMapper;

    public SysLogController(SysLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Operation(summary = "日志列表")
    @GetMapping("/list")
    public Result<PageResult<SysLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysLog::getUserId, userId);
        }
        wrapper.orderByDesc(SysLog::getCreatedAt);
        Page<SysLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }
}
