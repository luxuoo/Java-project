package com.gym.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String avatar;
    /** 当前账号关联的会员档案 ID（仅 MEMBER 角色，可能为 null） */
    private Long memberId;
    /** 当前账号关联的教练档案 ID（仅 COACH 角色，可能为 null） */
    private Long coachId;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId, String username, String realName,
                         String role, String avatar, Long memberId, Long coachId) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.avatar = avatar;
        this.memberId = memberId;
        this.coachId = coachId;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
}
