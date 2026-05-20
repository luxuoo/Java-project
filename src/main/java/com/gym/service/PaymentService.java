package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Payment;
import com.gym.mapper.PaymentMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService extends ServiceImpl<PaymentMapper, Payment> {

    public Page<Payment> pageList(int page, int size, Long memberId, String type, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(Payment::getMemberId, memberId);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Payment::getType, type);
        }
        if (startDate != null) {
            wrapper.ge(Payment::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(Payment::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Payment::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    public BigDecimal getMonthRevenue() {
        LocalDate now = LocalDate.now();
        List<Payment> payments = lambdaQuery()
                .ge(Payment::getCreatedAt, now.withDayOfMonth(1).atStartOfDay())
                .lt(Payment::getCreatedAt, now.plusMonths(1).withDayOfMonth(1).atStartOfDay())
                .gt(Payment::getAmount, 0).list();
        return payments.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalRevenue() {
        List<Payment> payments = lambdaQuery().gt(Payment::getAmount, 0).list();
        return payments.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Map<String, Object>> getRevenueByMonth(int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDateTime start = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            List<Payment> payments = lambdaQuery()
                    .ge(Payment::getCreatedAt, start)
                    .lt(Payment::getCreatedAt, end)
                    .gt(Payment::getAmount, 0).list();
            BigDecimal total = payments.stream()
                    .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new HashMap<>();
            item.put("month", month.getMonthValue() + "月");
            item.put("revenue", total);
            result.add(item);
        }
        return result;
    }
}
