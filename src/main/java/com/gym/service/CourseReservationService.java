package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Course;
import com.gym.entity.CourseReservation;
import com.gym.mapper.CourseReservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseReservationService extends ServiceImpl<CourseReservationMapper, CourseReservation> {

    private final CourseService courseService;

    public CourseReservationService(CourseService courseService) {
        this.courseService = courseService;
    }

    public Page<CourseReservation> pageList(int page, int size, Long courseId, Long memberId, Integer status) {
        LambdaQueryWrapper<CourseReservation> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(CourseReservation::getCourseId, courseId);
        }
        if (memberId != null) {
            wrapper.eq(CourseReservation::getMemberId, memberId);
        }
        if (status != null) {
            wrapper.eq(CourseReservation::getStatus, status);
        }
        wrapper.orderByDesc(CourseReservation::getReserveTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void reserve(Long courseId, Long memberId, String remark) {
        Course course = courseService.getById(courseId);
        if (course == null) throw new RuntimeException("课程不存在");
        if (course.getStatus() == null || course.getStatus() != 1) {
            throw new RuntimeException("课程不在报名中");
        }
        if (course.getStartTime() != null && course.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("课程已开始，无法预约");
        }
        long existing = lambdaQuery().eq(CourseReservation::getCourseId, courseId)
                .eq(CourseReservation::getMemberId, memberId)
                .eq(CourseReservation::getStatus, 1).count();
        if (existing > 0) throw new RuntimeException("已预约该课程，无需重复预约");

        // 原子地占用一个名额：affected rows == 1 才算抢到。
        // 这是关键的并发安全点——之前用 "读 current_count → +1 → 写回" 的方式，
        // 多人同时抢同一节课会出现超卖。
        int affected = courseService.getBaseMapper().incrementCurrentCount(courseId);
        if (affected == 0) {
            throw new RuntimeException("课程已满员");
        }

        try {
            CourseReservation reservation = new CourseReservation();
            reservation.setCourseId(courseId);
            reservation.setMemberId(memberId);
            reservation.setReserveTime(LocalDateTime.now());
            reservation.setStatus(1);
            reservation.setRemark(remark);
            save(reservation);
        } catch (RuntimeException e) {
            // 回滚名额，避免占了座却没生成预约记录
            courseService.getBaseMapper().decrementCurrentCount(courseId);
            throw e;
        }
    }

    @Transactional
    public void cancel(Long reservationId) {
        CourseReservation reservation = getById(reservationId);
        if (reservation == null) throw new RuntimeException("预约不存在");
        if (reservation.getStatus() != 1) throw new RuntimeException("只能取消已预约的记录");

        reservation.setStatus(0);
        updateById(reservation);

        // 原子地释放名额，内置兜底防止 current_count 变成负数
        courseService.getBaseMapper().decrementCurrentCount(reservation.getCourseId());
    }

    /** 学员取消自己的预约时，先校验归属。 */
    @Transactional
    public void cancelByMember(Long reservationId, Long memberId) {
        CourseReservation reservation = getById(reservationId);
        if (reservation == null) throw new RuntimeException("预约不存在");
        if (!reservation.getMemberId().equals(memberId)) throw new RuntimeException("无权取消他人预约");
        cancel(reservationId);
    }

    @Transactional
    public void checkIn(Long reservationId) {
        CourseReservation reservation = getById(reservationId);
        if (reservation == null) throw new RuntimeException("预约不存在");
        if (reservation.getStatus() != 1) throw new RuntimeException("状态不正确");
        reservation.setStatus(2);
        updateById(reservation);
    }

    public List<CourseReservation> getByMemberId(Long memberId) {
        return lambdaQuery().eq(CourseReservation::getMemberId, memberId)
                .orderByDesc(CourseReservation::getReserveTime).list();
    }
}
