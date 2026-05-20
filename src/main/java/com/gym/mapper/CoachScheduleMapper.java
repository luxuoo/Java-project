package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.CoachSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CoachScheduleMapper extends BaseMapper<CoachSchedule> {

    /**
     * 原子地"占用一个排班名额"，避免并发预约时超约。
     * @return 1 占座成功，0 已满或排班不存在
     */
    @Update("UPDATE coach_schedule SET current_appointment = ISNULL(current_appointment, 0) + 1 " +
            "WHERE id = #{scheduleId} AND status = 1 " +
            "AND ISNULL(current_appointment, 0) < ISNULL(max_appointment, 0)")
    int incrementCurrentAppointment(@Param("scheduleId") Long scheduleId);
}
