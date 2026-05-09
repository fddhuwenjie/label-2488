package com.library.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户注册请求 DTO
 * 包含完整的参数合法性校验规则，对应接口规范要求的请求参数校验
 */
@Data
public class RegisterRequest {

    /** 用户名：3-20位，仅限字母、数字、下划线 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在 3-20 个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 密码：6-20位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 个字符之间")
    private String password;

    /** 邮箱地址（可选，格式校验） */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号（可选，11位大陆手机号格式） */
    @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确")
    private String phone;
}
