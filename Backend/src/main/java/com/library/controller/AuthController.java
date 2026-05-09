package com.library.controller;

import com.library.common.Result;
import com.library.dto.LoginRequest;
import com.library.dto.LoginResponse;
import com.library.dto.RegisterRequest;
import com.library.entity.User;
import com.library.security.JwtTokenProvider;
import com.library.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>提供用户登录和注册接口，这两个接口在 SecurityConfig 中配置为公开访问（无需 JWT）。
 *
 * <p>登录流程：
 * <pre>
 * 1. 接收用户名和密码（@Valid 触发参数校验）
 * 2. AuthenticationManager 委托 DaoAuthenticationProvider 认证
 * 3. DaoAuthenticationProvider 调用 UserDetailsServiceImpl 加载用户，BCrypt 比对密码
 * 4. 认证成功后，JwtTokenProvider 生成 JWT 令牌
 * 5. 返回令牌和用户信息给客户端
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 用户登录接口（POST /api/auth/login）
     *
     * <p>请求体示例：
     * <pre>
     * {"username": "admin", "password": "admin123"}
     * </pre>
     *
     * @param loginRequest 登录请求（username 和 password 不能为空）
     * @return JWT 令牌及用户角色信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 通过 Spring Security 的 AuthenticationManager 执行认证（包含密码比对）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 认证成功，将认证信息存入当前请求的 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 JWT 令牌（有效期由 jwt.expiration 配置决定）
        String token = tokenProvider.generateToken(authentication);

        // 查询用户信息（含角色），组装响应
        User user = userService.getCurrentUser(loginRequest.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setExpiresIn(jwtExpiration / 1000L); // 转换为秒

        log.info("用户登录成功: {}, 角色: {}", user.getUsername(), user.getRole());
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册接口（POST /api/auth/register）
     *
     * <p>注册后默认角色为 ROLE_USER（普通用户），需要管理员手动升级为 ROLE_ADMIN。
     *
     * @param registerRequest 注册请求（含完整的参数格式校验）
     * @return 注册成功的用户信息（密码字段为 null）
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = userService.register(registerRequest);
        return Result.success("注册成功，欢迎加入校园图书馆", user);
    }
}
