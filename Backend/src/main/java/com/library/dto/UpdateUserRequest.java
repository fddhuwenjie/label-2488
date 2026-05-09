package com.library.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户信息更新请求 DTO
 * 所有字段均为可选，只更新非空字段
 */
@Data
public class UpdateUserRequest {

    /** 新邮箱地址（可选，格式校验） */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 新手机号（可选，11位大陆手机号） */
    @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确")
    private String phone;

    /**
     * 新密码（可选）。
     * 不填则不修改密码；填写后将使用 BCrypt 重新加密存储
     */
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 个字符之间")
    private String newPassword;
}
