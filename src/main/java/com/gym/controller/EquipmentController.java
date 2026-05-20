package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.Equipment;
import com.gym.service.EquipmentService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "器材管理")
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Operation(summary = "器材列表（所有角色可读）")
    @GetMapping("/list")
    public Result<PageResult<Equipment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        Page<Equipment> result = equipmentService.pageList(page, size, keyword, category, status);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "器材详情")
    @GetMapping("/{id}")
    public Result<Equipment> detail(@PathVariable Long id) {
        return Result.ok(equipmentService.getById(id));
    }

    @Operation(summary = "新增器材")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@RequestBody Equipment equipment) {
        equipmentService.save(equipment);
        return Result.ok(null, "器材添加成功");
    }

    @Operation(summary = "更新器材")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody Equipment equipment) {
        equipment.setId(id);
        equipmentService.updateById(equipment);
        return Result.ok(null, "更新成功");
    }

    @Operation(summary = "删除器材")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        equipmentService.removeById(id);
        return Result.ok(null, "删除成功");
    }
}
