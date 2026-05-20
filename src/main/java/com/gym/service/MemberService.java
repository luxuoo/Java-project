package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Member;
import com.gym.mapper.MemberMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MemberService extends ServiceImpl<MemberMapper, Member> {

    public Page<Member> pageList(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Member::getName, keyword)
                    .or().like(Member::getPhone, keyword)
                    .or().like(Member::getMemberNo, keyword));
        }
        if (status != null) {
            wrapper.eq(Member::getStatus, status);
        }
        wrapper.orderByDesc(Member::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    public void createMember(Member member) {
        if (member.getMemberNo() == null || member.getMemberNo().isEmpty()) {
            member.setMemberNo(generateMemberNo());
        }
        if (member.getStatus() == null) member.setStatus(1);
        if (member.getPhone() != null && !member.getPhone().isEmpty()
                && lambdaQuery().eq(Member::getPhone, member.getPhone()).count() > 0) {
            throw new RuntimeException("该手机号已注册");
        }
        save(member);
    }

    private String generateMemberNo() {
        String prefix = "GM" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = lambdaQuery().likeRight(Member::getMemberNo, prefix).count();
        return prefix + String.format("%04d", count + 1);
    }

    public long countActive() {
        return lambdaQuery().eq(Member::getStatus, 1).count();
    }

    /** 根据登录账号 ID 查会员档案；找不到返回 null。 */
    public Member getByUserId(Long userId) {
        if (userId == null) return null;
        return getOne(new LambdaQueryWrapper<Member>().eq(Member::getUserId, userId));
    }
}
