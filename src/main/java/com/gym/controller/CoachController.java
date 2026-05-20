package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.Coach;
import com.gym.entity.CoachSchedule;
import com.gym.entity.Course;
import com.gym.service.CoachService;
import com.gym.service.CoachScheduleService;
import com.gym.service.CourseService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "教练管理")
@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final CoachService coachService;
    private final CoachScheduleService scheduleService;
    private final CourseService courseService;

    public CoachController(CoachService coachService, CoachScheduleService scheduleService,
                           CourseService courseService) {
        this.coachService = coachService;
        this.scheduleService = scheduleService;
        this.courseService = courseService;
    }

    @Operation(summary = "教练列表")
    @GetMapping("/list")
    public Result<PageResult<Coach>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<Coach> result = coachService.pageList(page, size, keyword, status);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "教练详情")
    @GetMapping("/{id}")
    public Result<Coach> detail(@PathVariable Long id) {
        return Result.ok(coachService.getById(id));
    }

    @Operation(summary = "新增教练")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@RequestBody Coach coach) {
        coachService.createCoach(coach);
        return Result.ok(null, "教练创建成功");
    }

    @Operation(summary = "更新教练")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody Coach coach) {
        coach.setId(id);
        coachService.updateById(coach);
        return Result.ok(null, "更新成功");
    }

    @Operation(summary = "删除教练")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        // 检查教练是否还在带课，避免课程出现"无主"教练
        long onGoing = courseService.lambdaQuery()
                .eq(Course::getCoachId, id)
                .in(Course::getStatus, 1, 2)
                .count();
        if (onGoing > 0) {
            return Result.fail(400, "该教练尚有 " + onGoing + " 节进行中或报名中的课程，请先调整后再删除");
        }
        coachService.removeById(id);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "教练排班列表")
    @GetMapping("/{id}/schedules")
    public Result<List<CoachSchedule>> schedules(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date != null) {
            return Result.ok(scheduleService.getByCoachAndDate(id, date));
        }
        return Result.ok(scheduleService.lambdaQuery().eq(CoachSchedule::getCoachId, id)
                .orderByAsc(CoachSchedule::getScheduleDate).list());
    }

    @Operation(summary = "新增排班")
    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public Result<?> createSchedule(@RequestBody CoachSchedule schedule) {
        scheduleService.save(schedule);
        return Result.ok(null, "排班创建成功");
    }

    @Operation(summary = "预约排班")
    @PostMapping("/schedule/{id}/book")
    public Result<?> bookSchedule(@PathVariable Long id) {
        scheduleService.bookSchedule(id);
        return Result.ok(null, "预约成功");
    }
}
