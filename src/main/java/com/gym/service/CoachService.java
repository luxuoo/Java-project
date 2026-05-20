package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Coach;
import com.gym.mapper.CoachMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class CoachService extends ServiceImpl<CoachMapper, Coach> {

    public Page<Coach> pageList(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Coach::getName, keyword)
                    .or().like(Coach::getPhone, keyword)
                    .or().like(Coach::getSpecialty, keyword));
        }
        if (status != null) {
            wrapper.eq(Coach::getStatus, status);
        }
        wrapper.orderByDesc(Coach::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    public void createCoach(Coach coach) {
        if (coach.getCoachNo() == null || coach.getCoachNo().isEmpty()) {
            coach.setCoachNo(generateCoachNo());
        }
        if (coach.getStatus() == null) coach.setStatus(1);
        save(coach);
    }

    private String generateCoachNo() {
        String prefix = "C" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = lambdaQuery().likeRight(Coach::getCoachNo, prefix).count();
        return prefix + String.format("%04d", count + 1);
    }

    public long countActive() {
        return lambdaQuery().eq(Coach::getStatus, 1).count();
    }

    /** 根据登录账号 ID 查教练档案。 */
    public Coach getByUserId(Long userId) {
        if (userId == null) return null;
        return getOne(new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, userId));
    }
}
