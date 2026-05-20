package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("course_reservation")
public class CourseReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long memberId;
    private LocalDateTime reserveTime;
    private Integer status;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public LocalDateTime getReserveTime() { return reserveTime; }
    public void setReserveTime(LocalDateTime reserveTime) { this.reserveTime = reserveTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
