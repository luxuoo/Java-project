package com.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.dto.LoginRequest;
import com.gym.dto.LoginResponse;
import com.gym.entity.Coach;
import com.gym.entity.Member;
import com.gym.entity.SysUser;
import com.gym.mapper.CoachMapper;
import com.gym.mapper.MemberMapper;
import com.gym.mapper.SysUserMapper;
import com.gym.security.JwtUtil;
import com.gym.security.SecurityUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final MemberMapper memberMapper;
    private final CoachMapper coachMapper;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                       SysUserMapper sysUserMapper, MemberMapper memberMapper, CoachMapper coachMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.sysUserMapper = sysUserMapper;
        this.memberMapper = memberMapper;
        this.coachMapper = coachMapper;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));

        Long memberId = null;
        Long coachId = null;
        if ("MEMBER".equals(user.getRole())) {
            Member m = memberMapper.selectOne(
                    new LambdaQueryWrapper<Member>().eq(Member::getUserId, sysUser.getId()));
            if (m != null) memberId = m.getId();
        } else if ("COACH".equals(user.getRole())) {
            Coach c = coachMapper.selectOne(
                    new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, sysUser.getId()));
            if (c != null) coachId = c.getId();
        }

        return new LoginResponse(token, sysUser.getId(), user.getUsername(),
                sysUser.getRealName(), user.getRole(), sysUser.getAvatar(), memberId, coachId);
    }
}
