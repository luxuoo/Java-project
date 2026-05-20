package com.gym.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.entity.Coach;
import com.gym.entity.Member;
import com.gym.entity.SysUser;
import com.gym.mapper.CoachMapper;
import com.gym.mapper.MemberMapper;
import com.gym.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserMapper userMapper;
    private final MemberMapper memberMapper;
    private final CoachMapper coachMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 是否在每次启动时把演示账号密码重置为默认值。
     * 默认 false：避免用户在前台改了密码后，重启就丢。
     * 演示环境/首次部署时可以在 application-dev.yml 设为 true。
     */
    @Value("${app.demo.reset-passwords:false}")
    private boolean resetPasswords;

    public DataInitializer(SysUserMapper userMapper, MemberMapper memberMapper,
                           CoachMapper coachMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.memberMapper = memberMapper;
        this.coachMapper = coachMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!resetPasswords) {
            log.info("Demo password reset is disabled (app.demo.reset-passwords=false). " +
                    "Skipping default account password initialization.");
            // 即使不重置密码，仍尝试修复 user_id 与 member/coach 档案的绑定关系
            try {
                rebindProfiles();
            } catch (Exception e) {
                log.warn("Profile re-bind skipped: {}", e.getMessage());
            }
            return;
        }
        try {
            updatePassword("admin", "admin123");
            for (String u : new String[]{"coach01", "coach02", "coach03"}) updatePassword(u, "coach123");
            for (String u : new String[]{"member01", "member02", "member03", "member04", "member05"}) {
                updatePassword(u, "member123");
            }
            log.info("Default account passwords initialized");
        } catch (Exception e) {
            log.warn("Password initialization skipped (DB not ready): {}", e.getMessage());
        }
    }

    private void updatePassword(String username, String rawPassword) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) return;
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        rebindOne(user);
    }

    private void rebindProfiles() {
        for (SysUser u : userMapper.selectList(null)) {
            rebindOne(u);
        }
    }

    /** 兜底确保 sys_user 与 member/coach 档案双向绑定（处理历史数据 user_id 为空的情况）。 */
    private void rebindOne(SysUser user) {
        if ("MEMBER".equals(user.getRole())) {
            Member m = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>().eq(Member::getUserId, user.getId()));
            if (m == null && user.getPhone() != null) {
                m = memberMapper.selectOne(
                        new LambdaQueryWrapper<Member>().eq(Member::getPhone, user.getPhone()));
                if (m != null && m.getUserId() == null) {
                    m.setUserId(user.getId());
                    memberMapper.updateById(m);
                }
            }
        } else if ("COACH".equals(user.getRole())) {
            Coach c = coachMapper.selectOne(
                    new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, user.getId()));
            if (c == null && user.getPhone() != null) {
                c = coachMapper.selectOne(
                        new LambdaQueryWrapper<Coach>().eq(Coach::getPhone, user.getPhone()));
                if (c != null && c.getUserId() == null) {
                    c.setUserId(user.getId());
                    coachMapper.updateById(c);
                }
            }
        }
    }
}
