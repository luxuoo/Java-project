package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 原子地为课程"占用一个名额"。
     * <p>使用数据库的 <code>UPDATE ... WHERE current_count &lt; max_capacity</code> 保证并发安全：
     * 多个用户同时抢同一节课时，只有 affected rows == 1 的请求算抢到，剩下的看到 0 行影响 = 已满员。
     * @return 受影响行数；1 表示成功占座，0 表示已满或课程不存在/状态不对
     */
    @Update("UPDATE course SET current_count = ISNULL(current_count, 0) + 1 " +
            "WHERE id = #{courseId} AND status = 1 " +
            "AND ISNULL(current_count, 0) < ISNULL(max_capacity, 0)")
    int incrementCurrentCount(@Param("courseId") Long courseId);

    /**
     * 取消预约时释放一个名额，避免多次取消让 current_count 变成负数。
     * @return 受影响行数；1 表示成功，0 表示数据不一致（current_count 已经是 0）
     */
    @Update("UPDATE course SET current_count = current_count - 1 " +
            "WHERE id = #{courseId} AND ISNULL(current_count, 0) > 0")
    int decrementCurrentCount(@Param("courseId") Long courseId);
}
