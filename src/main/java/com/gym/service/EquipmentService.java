package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Equipment;
import com.gym.mapper.EquipmentMapper;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService extends ServiceImpl<EquipmentMapper, Equipment> {

    public Page<Equipment> pageList(int page, int size, String keyword, String category, Integer status) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Equipment::getName, keyword);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Equipment::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Equipment::getStatus, status);
        }
        wrapper.orderByDesc(Equipment::getId);
        return page(new Page<>(page, size), wrapper);
    }
}
