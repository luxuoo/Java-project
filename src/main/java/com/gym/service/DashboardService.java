package com.gym.service;

import com.gym.dto.DashboardStats;
import com.gym.entity.Course;
import com.gym.entity.Member;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final MemberService memberService;
    private final CoachService coachService;
    private final CourseService courseService;
    private final CheckInService checkInService;
    private final PaymentService paymentService;

    public DashboardService(MemberService memberService, CoachService coachService,
                            CourseService courseService, CheckInService checkInService,
                            PaymentService paymentService) {
        this.memberService = memberService;
        this.coachService = coachService;
        this.courseService = courseService;
        this.checkInService = checkInService;
        this.paymentService = paymentService;
    }

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalMembers(memberService.count());
        stats.setActiveMembers(memberService.countActive());
        stats.setTotalCoaches(coachService.count());
        stats.setTodayCheckIns(checkInService.countToday());
        stats.setTotalCourses(courseService.count());
        stats.setUpcomingCourses(courseService.countUpcoming());
        stats.setMonthRevenue(paymentService.getMonthRevenue());
        stats.setTotalRevenue(paymentService.getTotalRevenue());

        stats.setRevenueByMonth(paymentService.getRevenueByMonth(6));

        List<Member> allMembers = memberService.list();
        Map<Integer, Long> genderMap = allMembers.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getGender() == null ? 0 : m.getGender(),
                        Collectors.counting()));
        List<Map<String, Object>> genderList = new ArrayList<>();
        Map<String, Object> male = new HashMap<>();
        male.put("name", "男");
        male.put("value", genderMap.getOrDefault(1, 0L));
        genderList.add(male);
        Map<String, Object> female = new HashMap<>();
        female.put("name", "女");
        female.put("value", genderMap.getOrDefault(2, 0L));
        genderList.add(female);
        long unknown = genderMap.getOrDefault(0, 0L);
        if (unknown > 0) {
            Map<String, Object> other = new HashMap<>();
            other.put("name", "未知");
            other.put("value", unknown);
            genderList.add(other);
        }
        stats.setMemberByGender(genderList);

        Map<String, Long> categoryMap = courseService.list().stream()
                .collect(Collectors.groupingBy(c -> c.getCategory() != null ? c.getCategory() : "其他",
                        Collectors.counting()));
        List<Map<String, Object>> categoryList = new ArrayList<>();
        categoryMap.forEach((k, v) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", k);
            item.put("value", v);
            categoryList.add(item);
        });
        stats.setCourseByCategory(categoryList);

        Map<String, Long> dayMap = checkInService.getRecentDays(7).stream()
                .filter(c -> c.getCheckInTime() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getCheckInTime().toLocalDate().toString(),
                        Collectors.counting()));
        List<Map<String, Object>> dayList = new ArrayList<>();
        dayMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", e.getKey());
            item.put("count", e.getValue());
            dayList.add(item);
        });
        stats.setCheckInByDay(dayList);

        List<Map<String, Object>> topCourses = courseService.list().stream()
                .sorted(Comparator.comparingInt((Course c) ->
                        c.getCurrentCount() == null ? 0 : c.getCurrentCount()).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", c.getName());
                    m.put("count", c.getCurrentCount() == null ? 0 : c.getCurrentCount());
                    return m;
                }).collect(Collectors.toList());
        stats.setTopCourses(topCourses);

        return stats;
    }
}
