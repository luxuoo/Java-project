package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.*;
import com.gym.security.SecurityUtils;
import com.gym.service.*;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前登录用户（含学员/教练/管理员）的"我的"接口集合。
 * 所有数据均基于当前登录账号自动过滤，前端不需要也不应该传 memberId/coachId。
 */
@Tag(name = "我的中心")
@RestController
@RequestMapping("/api/me")
public class MePortalController {

    private final SysUserService userService;
    private final MemberService memberService;
    private final CoachService coachService;
    private final MembershipCardService cardService;
    private final CourseReservationService reservationService;
    private final CourseService courseService;
    private final CheckInService checkInService;
    private final PaymentService paymentService;

    public MePortalController(SysUserService userService, MemberService memberService,
                              CoachService coachService, MembershipCardService cardService,
                              CourseReservationService reservationService, CourseService courseService,
                              CheckInService checkInService, PaymentService paymentService) {
        this.userService = userService;
        this.memberService = memberService;
        this.coachService = coachService;
        this.cardService = cardService;
        this.reservationService = reservationService;
        this.courseService = courseService;
        this.checkInService = checkInService;
        this.paymentService = paymentService;
    }

    // ============= 共用：个人信息 =============

    @Operation(summary = "我的资料概览")
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        SysUser user = currentUser();
        if (user == null) return Result.fail(401, "未登录");
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        if (SecurityUtils.isMember()) {
            data.put("memberProfile", memberService.getByUserId(user.getId()));
        } else if (SecurityUtils.isCoach()) {
            data.put("coachProfile", coachService.getByUserId(user.getId()));
        }
        return Result.ok(data);
    }

    @Operation(summary = "更新会员档案（学员本人）")
    @PutMapping("/member-profile")
    public Result<?> updateMemberProfile(@RequestBody Member input) {
        if (!SecurityUtils.isMember()) return Result.fail(403, "仅学员可调用");
        Member self = currentMember();
        if (self == null) return Result.fail(404, "未找到会员档案");
        // 只允许修改少量信息字段，敏感字段（memberNo/userId/status）忽略
        self.setName(input.getName());
        self.setGender(input.getGender());
        self.setPhone(input.getPhone());
        self.setBirthday(input.getBirthday());
        self.setEmergencyContact(input.getEmergencyContact());
        self.setEmergencyPhone(input.getEmergencyPhone());
        self.setHealthNote(input.getHealthNote());
        self.setPhoto(input.getPhoto());
        memberService.updateById(self);
        return Result.ok(null, "保存成功");
    }

    @Operation(summary = "更新教练档案（教练本人）")
    @PutMapping("/coach-profile")
    public Result<?> updateCoachProfile(@RequestBody Coach input) {
        if (!SecurityUtils.isCoach()) return Result.fail(403, "仅教练可调用");
        Coach self = currentCoach();
        if (self == null) return Result.fail(404, "未找到教练档案");
        self.setName(input.getName());
        self.setGender(input.getGender());
        self.setPhone(input.getPhone());
        self.setSpecialty(input.getSpecialty());
        self.setCertification(input.getCertification());
        self.setBio(input.getBio());
        self.setPhoto(input.getPhoto());
        // 不允许学员/教练自己改 hourlyRate/level/status
        coachService.updateById(self);
        return Result.ok(null, "保存成功");
    }

    // ============= 学员：会员卡 / 预约 / 签到 / 消费 =============

    @Operation(summary = "我的会员卡（学员）")
    @GetMapping("/cards")
    public Result<List<MembershipCard>> myCards() {
        Member m = currentMember();
        if (m == null) return Result.ok(List.of());
        return Result.ok(cardService.getByMemberId(m.getId()));
    }

    @Operation(summary = "我的预约（学员）")
    @GetMapping("/reservations")
    public Result<PageResult<CourseReservation>> myReservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Member m = currentMember();
        if (m == null) return Result.ok(new PageResult<>(List.of(), 0, page, size));
        Page<CourseReservation> p = reservationService.pageList(page, size, null, m.getId(), status);
        return Result.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "预约课程（学员本人）")
    @PostMapping("/reservations")
    public Result<?> reserve(@RequestParam Long courseId,
                             @RequestParam(required = false) String remark) {
        Member m = currentMember();
        if (m == null) return Result.fail(403, "未找到会员档案");
        reservationService.reserve(courseId, m.getId(), remark);
        return Result.ok(null, "预约成功");
    }

    @Operation(summary = "取消我的预约（学员本人）")
    @PostMapping("/reservations/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        Member m = currentMember();
        if (m == null) return Result.fail(403, "未找到会员档案");
        reservationService.cancelByMember(id, m.getId());
        return Result.ok(null, "已取消");
    }

    @Operation(summary = "我的签到记录（学员，分页）")
    @GetMapping("/checkins")
    public Result<PageResult<CheckIn>> myCheckins(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        Member m = currentMember();
        if (m == null) return Result.ok(new PageResult<>(List.of(), 0, page, size));
        Page<CheckIn> p = checkInService.pageList(page, size, m.getId(), null);
        return Result.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "我的消费记录（学员）")
    @GetMapping("/payments")
    public Result<List<Payment>> myPayments() {
        Member m = currentMember();
        if (m == null) return Result.ok(List.of());
        return Result.ok(paymentService.lambdaQuery().eq(Payment::getMemberId, m.getId())
                .orderByDesc(Payment::getCreatedAt).list());
    }

    // ============= 教练：我的课程 / 我的预约名单 =============

    @Operation(summary = "我的课程（教练）")
    @GetMapping("/courses")
    public Result<List<Course>> myCourses() {
        Coach c = currentCoach();
        if (c == null) return Result.ok(List.of());
        return Result.ok(courseService.lambdaQuery().eq(Course::getCoachId, c.getId())
                .orderByDesc(Course::getStartTime).list());
    }

    @Operation(summary = "我的课程预约名单（教练）")
    @GetMapping("/courses/reservations")
    public Result<List<CourseReservation>> myCourseReservations(@RequestParam(required = false) Long courseId) {
        Coach c = currentCoach();
        if (c == null) return Result.ok(List.of());
        // 找到该教练的全部课程 ID
        List<Long> courseIds = courseService.lambdaQuery().eq(Course::getCoachId, c.getId())
                .list().stream().map(Course::getId).toList();
        if (courseIds.isEmpty()) return Result.ok(List.of());
        return Result.ok(reservationService.lambdaQuery()
                .in(CourseReservation::getCourseId, courseIds)
                .eq(courseId != null, CourseReservation::getCourseId, courseId)
                .orderByDesc(CourseReservation::getReserveTime)
                .list());
    }

    // ============= 工具方法 =============

    private SysUser currentUser() {
        String username = SecurityUtils.currentUsername();
        if (username == null) return null;
        return userService.lambdaQuery().eq(SysUser::getUsername, username).one();
    }

    private Member currentMember() {
        SysUser u = currentUser();
        return u == null ? null : memberService.getByUserId(u.getId());
    }

    private Coach currentCoach() {
        SysUser u = currentUser();
        return u == null ? null : coachService.getByUserId(u.getId());
    }
}
