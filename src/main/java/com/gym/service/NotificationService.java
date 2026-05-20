package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Notification;
import com.gym.entity.SysUser;
import com.gym.mapper.NotificationMapper;
import com.gym.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    private final SysUserMapper sysUserMapper;

    public NotificationService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public Page<Notification> pageList(int page, int size, Long userId, Integer isRead) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Notification::getUserId, userId);
        }
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    public void markAsRead(Long id) {
        Notification notification = getById(id);
        if (notification != null && (notification.getIsRead() == null || notification.getIsRead() == 0)) {
            notification.setIsRead(1);
            updateById(notification);
        }
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = lambdaQuery().eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0).list();
        if (unread.isEmpty()) return;
        unread.forEach(n -> n.setIsRead(1));
        updateBatchById(unread);
    }

    public long countUnread(Long userId) {
        return lambdaQuery().eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0).count();
    }

    /** 给某个角色的所有用户群发一条通知，返回发送人数。 */
    public int broadcast(String role, String title, String content, String type) {
        if (role == null || title == null || content == null) return 0;
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, role)
                        .eq(SysUser::getStatus, 1));
        if (users.isEmpty()) return 0;

        // 注意：这里没有用 saveBatch，因为 MyBatis-Plus 的 saveBatch 在 SQL Server +
        // 自增主键 + 主键回写组合下会触发 mssql-jdbc 驱动的 bug：
        //   "必须执行该语句才能获得结果"
        // 这是 baomidou/mybatis-plus#2179 已确认的 driver 限制。
        // 改为单条循环 save，单条 insert 在 SQL Server 上是稳定的，量级（最多几百用户）也可接受。
        int count = 0;
        String safeType = type == null ? "SYSTEM" : type;
        for (SysUser u : users) {
            Notification n = new Notification();
            n.setUserId(u.getId());
            n.setTitle(title);
            n.setContent(content);
            n.setType(safeType);
            n.setIsRead(0);
            if (save(n)) count++;
        }
        return count;
    }
}
