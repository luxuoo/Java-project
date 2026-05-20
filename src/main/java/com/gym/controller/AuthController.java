package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.dto.LoginRequest;
import com.gym.dto.LoginResponse;
import com.gym.dto.PasswordChangeRequest;
import com.gym.entity.Coach;
import com.gym.entity.Member;
import com.gym.entity.SysUser;
import com.gym.security.SecurityUtils;
import com.gym.service.AuthService;
import com.gym.service.CoachService;
import com.gym.service.MemberService;
import com.gym.service.SysUserService;
import com.gym.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SysUserService userService;
    private final MemberService memberService;
    private final CoachService coachService;

    public AuthController(AuthService authService, SysUserService userService,
                          MemberService memberService, CoachService coachService) {
        this.authService = authService;
        this.userService = userService;
        this.memberService = memberService;
        this.coachService = coachService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        String username = SecurityUtils.currentUsername();
        if (username == null) return Result.fail(401, "未登录");
        SysUser user = userService.lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) return Result.fail(401, "用户不存在");
        user.setPassword(null);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("role", user.getRole());

        if ("MEMBER".equals(user.getRole())) {
            Member m = memberService.getOne(new LambdaQueryWrapper<Member>().eq(Member::getUserId, user.getId()));
            data.put("memberProfile", m);
        } else if ("COACH".equals(user.getRole())) {
            Coach c = coachService.getOne(new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, user.getId()));
            data.put("coachProfile", c);
        }
        return Result.ok(data);
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody PasswordChangeRequest req) {
        String username = SecurityUtils.currentUsername();
        if (username == null) return Result.fail(401, "未登录");
        SysUser user = userService.lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) return Result.fail(404, "用户不存在");
        userService.changePassword(user.getId(), req.getOldPassword(), req.getNewPassword());
        return Result.ok(null, "密码修改成功");
    }
}
