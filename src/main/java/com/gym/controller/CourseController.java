package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.Course;
import com.gym.entity.CourseReservation;
import com.gym.service.CourseReservationService;
import com.gym.service.CourseService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "课程管理")
@RestController
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;
    private final CourseReservationService reservationService;

    public CourseController(CourseService courseService, CourseReservationService reservationService) {
        this.courseService = courseService;
        this.reservationService = reservationService;
    }

    @Operation(summary = "课程列表（所有登录用户可见）")
    @GetMapping("/list")
    public Result<PageResult<Course>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        Page<Course> result = courseService.pageList(page, size, keyword, type, category, status);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        return Result.ok(courseService.getById(id));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public Result<?> create(@RequestBody Course course) {
        course.setCurrentCount(0);
        courseService.save(course);
        return Result.ok(null, "课程创建成功");
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public Result<?> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.updateById(course);
        return Result.ok(null, "更新成功");
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        courseService.removeById(id);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "管理员代会员预约课程")
    @PostMapping("/{courseId}/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> reserveForMember(@PathVariable Long courseId,
                                      @RequestParam Long memberId,
                                      @RequestParam(required = false) String remark) {
        reservationService.reserve(courseId, memberId, remark);
        return Result.ok(null, "预约成功");
    }

    @Operation(summary = "取消预约（管理员）")
    @PostMapping("/reservation/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> cancelReservation(@PathVariable Long id) {
        reservationService.cancel(id);
        return Result.ok(null, "取消成功");
    }

    @Operation(summary = "课程签到")
    @PostMapping("/reservation/{id}/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public Result<?> checkIn(@PathVariable Long id) {
        reservationService.checkIn(id);
        return Result.ok(null, "签到成功");
    }

    @Operation(summary = "预约列表（管理员/教练）")
    @GetMapping("/reservation/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public Result<PageResult<CourseReservation>> reservationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Integer status) {
        Page<CourseReservation> result = reservationService.pageList(page, size, courseId, memberId, status);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }
}
