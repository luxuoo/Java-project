package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.CheckIn;
import com.gym.service.CheckInService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "签到管理")
@RestController
@RequestMapping("/api/checkin")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @Operation(summary = "签到列表")
    @GetMapping("/list")
    public Result<PageResult<CheckIn>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Page<CheckIn> result = checkInService.pageList(page, size, memberId, date);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "前台签到")
    @PostMapping("/in")
    public Result<CheckIn> checkIn(@RequestParam Long memberId,
                                   @RequestParam(defaultValue = "GATE-01") String gate) {
        return Result.ok(checkInService.checkIn(memberId, gate));
    }

    @Operation(summary = "前台签退")
    @PostMapping("/out")
    public Result<CheckIn> checkOut(@RequestParam Long memberId) {
        return Result.ok(checkInService.checkOut(memberId));
    }

    @Operation(summary = "当前在场人数")
    @GetMapping("/current")
    public Result<Long> currentInGym() {
        return Result.ok(checkInService.countCurrentInGym());
    }
}
