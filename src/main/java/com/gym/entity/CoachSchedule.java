package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;

@TableName("coach_schedule")
public class CoachSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coachId;
    private LocalDate scheduleDate;
    private String timeSlot;
    private Integer maxAppointment;
    private Integer currentAppointment;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public Integer getMaxAppointment() { return maxAppointment; }
    public void setMaxAppointment(Integer maxAppointment) { this.maxAppointment = maxAppointment; }
    public Integer getCurrentAppointment() { return currentAppointment; }
    public void setCurrentAppointment(Integer currentAppointment) { this.currentAppointment = currentAppointment; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
