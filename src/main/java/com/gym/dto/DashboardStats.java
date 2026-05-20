package com.gym.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStats {
    private long totalMembers;
    private long activeMembers;
    private long totalCoaches;
    private long todayCheckIns;
    private long totalCourses;
    private long upcomingCourses;
    private BigDecimal monthRevenue;
    private BigDecimal totalRevenue;
    private List<Map<String, Object>> revenueByMonth;
    private List<Map<String, Object>> memberByGender;
    private List<Map<String, Object>> courseByCategory;
    private List<Map<String, Object>> checkInByDay;
    private List<Map<String, Object>> topCourses;

    public long getTotalMembers() { return totalMembers; }
    public void setTotalMembers(long totalMembers) { this.totalMembers = totalMembers; }
    public long getActiveMembers() { return activeMembers; }
    public void setActiveMembers(long activeMembers) { this.activeMembers = activeMembers; }
    public long getTotalCoaches() { return totalCoaches; }
    public void setTotalCoaches(long totalCoaches) { this.totalCoaches = totalCoaches; }
    public long getTodayCheckIns() { return todayCheckIns; }
    public void setTodayCheckIns(long todayCheckIns) { this.todayCheckIns = todayCheckIns; }
    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }
    public long getUpcomingCourses() { return upcomingCourses; }
    public void setUpcomingCourses(long upcomingCourses) { this.upcomingCourses = upcomingCourses; }
    public BigDecimal getMonthRevenue() { return monthRevenue; }
    public void setMonthRevenue(BigDecimal monthRevenue) { this.monthRevenue = monthRevenue; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public List<Map<String, Object>> getRevenueByMonth() { return revenueByMonth; }
    public void setRevenueByMonth(List<Map<String, Object>> revenueByMonth) { this.revenueByMonth = revenueByMonth; }
    public List<Map<String, Object>> getMemberByGender() { return memberByGender; }
    public void setMemberByGender(List<Map<String, Object>> memberByGender) { this.memberByGender = memberByGender; }
    public List<Map<String, Object>> getCourseByCategory() { return courseByCategory; }
    public void setCourseByCategory(List<Map<String, Object>> courseByCategory) { this.courseByCategory = courseByCategory; }
    public List<Map<String, Object>> getCheckInByDay() { return checkInByDay; }
    public void setCheckInByDay(List<Map<String, Object>> checkInByDay) { this.checkInByDay = checkInByDay; }
    public List<Map<String, Object>> getTopCourses() { return topCourses; }
    public void setTopCourses(List<Map<String, Object>> topCourses) { this.topCourses = topCourses; }
}
