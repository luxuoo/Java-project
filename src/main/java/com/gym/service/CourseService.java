package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Course;
import com.gym.mapper.CourseMapper;
import org.springframework.stereotype.Service;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    public Page<Course> pageList(int page, int size, String keyword, String type, String category, Integer status) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Course::getName, keyword);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Course::getType, type);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Course::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }
        wrapper.orderByDesc(Course::getStartTime);
        return page(new Page<>(page, size), wrapper);
    }

    public long countUpcoming() {
        return lambdaQuery().ge(Course::getStatus, 1).le(Course::getStatus, 2).count();
    }
}
