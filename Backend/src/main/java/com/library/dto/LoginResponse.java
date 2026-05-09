package com.library.dto;

import lombok.Data;

/**
 * 用户登录响应 DTO
 * 包含 JWT 令牌及用户基本身份信息，供客户端存储并在后续请求中携带
 */
@Data
public class LoginResponse {

    /**
     * JWT 访问令牌。
     * 客户端需在后续请求的 Authorization 请求头中携带：{@code Bearer <token>}
     */
    private String token;

    /** 当前登录用户的数据库 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /**
     * 用户角色（ROLE_ADMIN / ROLE_USER），
     * 客户端可根据此字段进行前端路由权限控制
     */
    private String role;

    /** 令牌有效期（单位：秒），默认 86400 秒（24 小时） */
    private long expiresIn;
}
