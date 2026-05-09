package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO
 * 使用 Bean Validation 注解对请求参数进行合法性校验
 */
@Data
public class LoginRequest {

    /** 用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
