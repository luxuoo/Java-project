package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.Notification;
import com.gym.entity.SysUser;
import com.gym.security.SecurityUtils;
import com.gym.service.NotificationService;
import com.gym.service.SysUserService;
import com.gym.util.PageResult;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知管理")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final SysUserService userService;

    public NotificationController(NotificationService notificationService, SysUserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Operation(summary = "通知列表（管理员）")
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer isRead) {
        Page<Notification> result = notificationService.pageList(page, size, userId, isRead);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "我的通知")
    @GetMapping("/my")
    public Result<PageResult<Notification>> myNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录");
        Page<Notification> result = notificationService.pageList(page, size, userId, null);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = currentUserId();
        if (userId == null) return Result.ok(0L);
        return Result.ok(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录");
        Notification n = notificationService.getById(id);
        // 数据隔离：不能标别人的通知为已读
        if (n == null || (!SecurityUtils.isAdmin() && !userId.equals(n.getUserId()))) {
            return Result.fail(403, "无权操作");
        }
        notificationService.markAsRead(id);
        return Result.ok(null);
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<?> markAllRead() {
        Long userId = currentUserId();
        if (userId == null) return Result.fail(401, "未登录");
        notificationService.markAllAsRead(userId);
        return Result.ok(null);
    }

    @Operation(summary = "发送通知（管理员）")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@RequestBody Notification notification) {
        if (notification.getIsRead() == null) notification.setIsRead(0);
        notificationService.save(notification);
        return Result.ok(null, "发送成功");
    }

    @Operation(summary = "广播通知给指定角色")
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> broadcast(@RequestBody BroadcastRequest req) {
        if (req.getRole() == null || req.getRole().isEmpty()) {
            return Result.fail(400, "请指定目标角色");
        }
        if (req.getTitle() == null || req.getTitle().isEmpty()) {
            return Result.fail(400, "请填写标题");
        }
        if (req.getContent() == null || req.getContent().isEmpty()) {
            return Result.fail(400, "请填写内容");
        }
        String type = req.getType() == null || req.getType().isEmpty() ? "SYSTEM" : req.getType();
        int count = notificationService.broadcast(req.getRole(), req.getTitle(), req.getContent(), type);
        return Result.ok(count, "已发送给 " + count + " 位用户");
    }

    /** 群发通知请求体；用 JSON Body 而不是 query string 接收，避免长文本/中文转义问题。 */
    public static class BroadcastRequest {
        private String role;
        private String title;
        private String content;
        private String type;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    private Long currentUserId() {
        String username = SecurityUtils.currentUsername();
        if (username == null) return null;
        SysUser u = userService.lambdaQuery().eq(SysUser::getUsername, username).one();
        return u == null ? null : u.getId();
    }
}
