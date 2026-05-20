package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.Member;
import com.gym.entity.MembershipCard;
import com.gym.entity.CourseReservation;
import com.gym.entity.CheckIn;
import com.gym.entity.Payment;
import com.gym.service.*;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会员管理")
@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class MemberController {

    private final MemberService memberService;
    private final MembershipCardService cardService;
    private final CourseReservationService reservationService;
    private final CheckInService checkInService;
    private final PaymentService paymentService;

    public MemberController(MemberService memberService, MembershipCardService cardService,
                            CourseReservationService reservationService, CheckInService checkInService,
                            PaymentService paymentService) {
        this.memberService = memberService;
        this.cardService = cardService;
        this.reservationService = reservationService;
        this.checkInService = checkInService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "会员列表")
    @GetMapping("/list")
    public Result<PageResult<Member>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<Member> result = memberService.pageList(page, size, keyword, status);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "会员详情")
    @GetMapping("/{id}")
    public Result<Member> detail(@PathVariable Long id) {
        return Result.ok(memberService.getById(id));
    }

    @Operation(summary = "新增会员")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@RequestBody Member member) {
        memberService.createMember(member);
        return Result.ok(null, "会员创建成功");
    }

    @Operation(summary = "更新会员")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        memberService.updateById(member);
        return Result.ok(null, "更新成功");
    }

    @Operation(summary = "删除会员")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        // 删除前先检查是否有关联数据，避免产生孤儿记录
        long activeReservations = reservationService.lambdaQuery()
                .eq(CourseReservation::getMemberId, id)
                .eq(CourseReservation::getStatus, 1).count();
        if (activeReservations > 0) {
            return Result.fail(400, "该会员存在 " + activeReservations + " 个有效预约，请先取消后再删除");
        }
        long inGym = checkInService.lambdaQuery()
                .eq(CheckIn::getMemberId, id)
                .isNull(CheckIn::getCheckOutTime).count();
        if (inGym > 0) {
            return Result.fail(400, "该会员当前在场内未签退，无法删除");
        }
        long activeCards = cardService.lambdaQuery()
                .eq(MembershipCard::getMemberId, id)
                .in(MembershipCard::getStatus, 1, 0).count(); // 使用中或未激活
        if (activeCards > 0) {
            return Result.fail(400, "该会员尚有 " + activeCards + " 张有效会员卡，请先处理");
        }
        memberService.removeById(id);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "获取会员卡列表")
    @GetMapping("/{id}/cards")
    public Result<List<MembershipCard>> cards(@PathVariable Long id) {
        return Result.ok(cardService.getByMemberId(id));
    }

    @Operation(summary = "获取会员预约")
    @GetMapping("/{id}/reservations")
    public Result<List<CourseReservation>> reservations(@PathVariable Long id) {
        return Result.ok(reservationService.getByMemberId(id));
    }

    @Operation(summary = "获取会员签到记录")
    @GetMapping("/{id}/checkins")
    public Result<List<CheckIn>> checkins(@PathVariable Long id) {
        return Result.ok(checkInService.lambdaQuery().eq(CheckIn::getMemberId, id)
                .orderByDesc(CheckIn::getCheckInTime).list());
    }

    @Operation(summary = "获取会员消费记录")
    @GetMapping("/{id}/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Payment>> payments(@PathVariable Long id) {
        return Result.ok(paymentService.lambdaQuery().eq(Payment::getMemberId, id)
                .orderByDesc(Payment::getCreatedAt).list());
    }
}
