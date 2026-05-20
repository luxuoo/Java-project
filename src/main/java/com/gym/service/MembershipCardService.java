package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.MembershipCard;
import com.gym.mapper.MembershipCardMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MembershipCardService extends ServiceImpl<MembershipCardMapper, MembershipCard> {

    public Page<MembershipCard> pageList(int page, int size, Long memberId, Integer status) {
        LambdaQueryWrapper<MembershipCard> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(MembershipCard::getMemberId, memberId);
        }
        if (status != null) {
            wrapper.eq(MembershipCard::getStatus, status);
        }
        wrapper.orderByDesc(MembershipCard::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    public List<MembershipCard> getByMemberId(Long memberId) {
        return lambdaQuery().eq(MembershipCard::getMemberId, memberId)
                .orderByDesc(MembershipCard::getCreatedAt).list();
    }

    public void activateCard(Long cardId) {
        MembershipCard card = getById(cardId);
        if (card == null) throw new RuntimeException("会员卡不存在");
        card.setStatus(1);
        updateById(card);
    }

    public void freezeCard(Long cardId) {
        MembershipCard card = getById(cardId);
        if (card == null) throw new RuntimeException("会员卡不存在");
        if (card.getStatus() != 1) throw new RuntimeException("只有使用中的卡才能冻结");
        card.setStatus(3);
        updateById(card);
    }

    public void unfreezeCard(Long cardId) {
        MembershipCard card = getById(cardId);
        if (card == null) throw new RuntimeException("会员卡不存在");
        if (card.getStatus() != 3) throw new RuntimeException("只有冻结的卡才能解冻");
        card.setStatus(1);
        updateById(card);
    }
}
