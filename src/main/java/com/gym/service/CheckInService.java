package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.CheckIn;
import com.gym.mapper.CheckInMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CheckInService extends ServiceImpl<CheckInMapper, CheckIn> {

    public Page<CheckIn> pageList(int page, int size, Long memberId, LocalDate date) {
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(CheckIn::getMemberId, memberId);
        }
        if (date != null) {
            wrapper.ge(CheckIn::getCheckInTime, date.atStartOfDay())
                    .lt(CheckIn::getCheckInTime, date.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(CheckIn::getCheckInTime);
        return page(new Page<>(page, size), wrapper);
    }

    public CheckIn checkIn(Long memberId, String gate) {
        CheckIn existing = lambdaQuery().eq(CheckIn::getMemberId, memberId)
                .isNull(CheckIn::getCheckOutTime).one();
        if (existing != null) {
            throw new RuntimeException("该会员已在场内，请先签退");
        }
        CheckIn record = new CheckIn();
        record.setMemberId(memberId);
        record.setCheckInTime(LocalDateTime.now());
        record.setGate(gate);
        save(record);
        return record;
    }

    public CheckIn checkOut(Long memberId) {
        CheckIn record = lambdaQuery().eq(CheckIn::getMemberId, memberId)
                .isNull(CheckIn::getCheckOutTime).one();
        if (record == null) {
            throw new RuntimeException("未找到签到记录");
        }
        record.setCheckOutTime(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(record.getCheckInTime(), record.getCheckOutTime());
        record.setDurationMin((int) minutes);
        updateById(record);
        return record;
    }

    public long countToday() {
        LocalDate today = LocalDate.now();
        return lambdaQuery().ge(CheckIn::getCheckInTime, today.atStartOfDay())
                .lt(CheckIn::getCheckInTime, today.plusDays(1).atStartOfDay()).count();
    }

    public long countCurrentInGym() {
        return lambdaQuery().isNull(CheckIn::getCheckOutTime).count();
    }

    public List<CheckIn> getRecentDays(int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        return lambdaQuery().ge(CheckIn::getCheckInTime, start.atStartOfDay())
                .orderByAsc(CheckIn::getCheckInTime).list();
    }
}
