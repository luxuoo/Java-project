package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.SysUser;
import com.gym.security.SecurityUtils;
import com.gym.service.SysUserService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/user")
@PreAuthorize("hasRole('ADMIN')")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        Page<SysUser> result = userService.pageList(page, size, keyword, role);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<?> create(@RequestBody SysUser user) {
        userService.createUser(user);
        return Result.ok(null, "用户创建成功");
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.ok(null, "更新成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id, "123456");
        return Result.ok(null, "密码已重置为 123456");
    }

    @Operation(summary = "启用/禁用")
    @PostMapping("/{id}/toggle-status")
    public Result<?> toggleStatus(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        // 不允许把自己禁用，避免锁死后台
        String me = SecurityUtils.currentUsername();
        if (me != null && me.equals(user.getUsername()) && user.getStatus() == 1) {
            return Result.fail("不能禁用自己");
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userService.updateById(user);
        return Result.ok(null, user.getStatus() == 1 ? "已启用" : "已禁用");
    }
}
