package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.CoachSchedule;
import com.gym.mapper.CoachScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CoachScheduleService extends ServiceImpl<CoachScheduleMapper, CoachSchedule> {

    public Page<CoachSchedule> pageList(int page, int size, Long coachId, LocalDate date) {
        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<>();
        if (coachId != null) {
            wrapper.eq(CoachSchedule::getCoachId, coachId);
        }
        if (date != null) {
            wrapper.eq(CoachSchedule::getScheduleDate, date);
        }
        wrapper.orderByAsc(CoachSchedule::getScheduleDate);
        return page(new Page<>(page, size), wrapper);
    }

    public List<CoachSchedule> getByCoachAndDate(Long coachId, LocalDate date) {
        return lambdaQuery().eq(CoachSchedule::getCoachId, coachId)
                .eq(CoachSchedule::getScheduleDate, date)
                .orderByAsc(CoachSchedule::getTimeSlot).list();
    }

    @Transactional
    public void bookSchedule(Long scheduleId) {
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null) throw new RuntimeException("排班不存在");

        // 原子占座：affected rows == 1 才算抢到，避免并发超约
        int affected = baseMapper.incrementCurrentAppointment(scheduleId);
        if (affected == 0) {
            throw new RuntimeException("该时段已约满");
        }

        // 占座成功后，回查当前状态，决定是否要把状态置为"满"
        CoachSchedule fresh = getById(scheduleId);
        if (fresh != null && fresh.getMaxAppointment() != null
                && fresh.getCurrentAppointment() != null
                && fresh.getCurrentAppointment() >= fresh.getMaxAppointment()
                && fresh.getStatus() != null && fresh.getStatus() == 1) {
            fresh.setStatus(0);
            updateById(fresh);
        }
    }
}
