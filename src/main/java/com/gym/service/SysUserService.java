package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.entity.Coach;
import com.gym.entity.Member;
import com.gym.entity.SysUser;
import com.gym.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final PasswordEncoder passwordEncoder;
    private final MemberService memberService;
    private final CoachService coachService;

    public SysUserService(PasswordEncoder passwordEncoder,
                          @Lazy MemberService memberService,
                          @Lazy CoachService coachService) {
        this.passwordEncoder = passwordEncoder;
        this.memberService = memberService;
        this.coachService = coachService;
    }

    public Page<SysUser> pageList(int page, int size, String keyword, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void createUser(SysUser user) {
        SysUser existing = lambdaQuery().eq(SysUser::getUsername, user.getUsername()).one();
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException("请设置初始密码");
        }
        if (user.getStatus() == null) user.setStatus(1);
        if (user.getRole() == null || user.getRole().isEmpty()) user.setRole("MEMBER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);

        // 同步创建对应的业务档案，避免出现"有登录账号但找不到档案"
        syncProfileFromUser(user);
    }

    @Transactional
    public void updateUser(SysUser user) {
        SysUser existing = getById(user.getId());
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        // 用户名不允许修改（前端已禁用，这里再兜底）
        user.setUsername(existing.getUsername());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // null 字段 MyBatis-Plus 不会更新
        }
        updateById(user);
    }

    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    public void changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser user = getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (oldPwd == null || newPwd == null || newPwd.length() < 6) {
            throw new RuntimeException("新密码至少 6 位");
        }
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        updateById(user);
    }

    /** 删除前确认：有关联的 member/coach 档案时只允许"禁用"，避免外键孤儿数据。 */
    @Transactional
    public void deleteUser(Long id) {
        SysUser user = getById(id);
        if (user == null) return;
        if ("ADMIN".equals(user.getRole())) {
            // 至少保留一个管理员
            long adminCount = lambdaQuery().eq(SysUser::getRole, "ADMIN").eq(SysUser::getStatus, 1).count();
            if (adminCount <= 1) throw new RuntimeException("必须至少保留一个启用的管理员账号");
        }
        // 同步删除档案
        memberService.lambdaUpdate().eq(Member::getUserId, id).remove();
        coachService.lambdaUpdate().eq(Coach::getUserId, id).remove();
        removeById(id);
    }

    private void syncProfileFromUser(SysUser user) {
        if ("MEMBER".equals(user.getRole())) {
            Member exists = memberService.getOne(new LambdaQueryWrapper<Member>().eq(Member::getUserId, user.getId()));
            if (exists != null) return;
            Member m = new Member();
            m.setUserId(user.getId());
            m.setName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            m.setGender(user.getGender() != null ? user.getGender() : 0);
            m.setPhone(user.getPhone());
            m.setStatus(1);
            memberService.createMember(m);
        } else if ("COACH".equals(user.getRole())) {
            Coach exists = coachService.getOne(new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, user.getId()));
            if (exists != null) return;
            Coach c = new Coach();
            c.setUserId(user.getId());
            c.setName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            c.setGender(user.getGender() != null ? user.getGender() : 0);
            c.setPhone(user.getPhone());
            c.setLevel("中级");
            c.setHourlyRate(BigDecimal.valueOf(200));
            c.setStatus(1);
            coachService.createCoach(c);
        }
    }
}
