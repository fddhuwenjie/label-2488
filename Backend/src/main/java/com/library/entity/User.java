package com.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库 users 表
 *
 * <p>包含用户基本信息与权限角色，支持管理员和普通用户两种角色。
 * 密码字段通过 {@link JsonIgnore} 注解防止序列化泄露。
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户唯一标识（自增主键） */
    private Long id;

    /** 用户名（登录账号，全局唯一，仅支持字母/数字/下划线） */
    private String username;

    /**
     * BCrypt 加密后的密码。
     * 序列化时自动忽略，防止密码泄露到接口响应
     */
    @JsonIgnore
    private String password;

    /** 邮箱地址（可选） */
    private String email;

    /** 手机号码（可选） */
    private String phone;

    /**
     * 用户角色，与 Spring Security 角色体系对应：
     * <ul>
     *   <li>{@code ROLE_ADMIN}：管理员，拥有所有权限</li>
     *   <li>{@code ROLE_USER}：普通用户，可借还图书</li>
     * </ul>
     */
    private String role;

    /** 账号状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 账号创建时间（由数据库自动填充） */
    private LocalDateTime createdAt;

    /** 账号最后更新时间（由数据库自动更新） */
    private LocalDateTime updatedAt;
}
