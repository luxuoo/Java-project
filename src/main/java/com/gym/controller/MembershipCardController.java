package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.MembershipCard;
import com.gym.entity.Payment;
import com.gym.service.MembershipCardService;
import com.gym.service.PaymentService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "会员卡管理")
@RestController
@RequestMapping("/api/card")
@PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
public class MembershipCardController {

    private final MembershipCardService cardService;
    private final PaymentService paymentService;

    public MembershipCardController(MembershipCardService cardService, PaymentService paymentService) {
        this.cardService = cardService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "会员卡列表")
    @GetMapping("/list")
    public Result<PageResult<MembershipCard>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Integer status) {
        Page<MembershipCard> result = cardService.pageList(page, size, memberId, status);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "新增会员卡")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@RequestBody MembershipCard card) {
        if (card.getStatus() == null) card.setStatus(1);
        cardService.save(card);
        // 自动生成购卡支付记录
        Payment payment = new Payment();
        payment.setMemberId(card.getMemberId());
        payment.setType("购卡");
        payment.setAmount(card.getPrice());
        payment.setPayMethod("现金");
        payment.setRelatedId(card.getId());
        payment.setRemark("购买" + card.getCardName());
        paymentService.save(payment);
        return Result.ok(null, "会员卡创建成功");
    }

    @Operation(summary = "激活会员卡")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> activate(@PathVariable Long id) {
        cardService.activateCard(id);
        return Result.ok(null, "激活成功");
    }

    @Operation(summary = "冻结会员卡")
    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> freeze(@PathVariable Long id) {
        cardService.freezeCard(id);
        return Result.ok(null, "冻结成功");
    }

    @Operation(summary = "解冻会员卡")
    @PostMapping("/{id}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> unfreeze(@PathVariable Long id) {
        cardService.unfreezeCard(id);
        return Result.ok(null, "解冻成功");
    }
}
