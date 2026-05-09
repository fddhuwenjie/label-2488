package com.library.config;

import com.library.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 核心安全配置类
 *
 * <p>配置要点：
 * <ul>
 *   <li><b>无状态认证</b>：使用 JWT，禁用 Session（STATELESS），不存储认证状态于服务端</li>
 *   <li><b>CORS 跨域</b>：允许前端跨域访问 API，生产环境建议限定具体来源域名</li>
 *   <li><b>CSRF 防护</b>：JWT 无状态认证不依赖 Cookie，禁用 CSRF 防护</li>
 *   <li><b>接口权限</b>：分层授权，公开接口、用户接口、管理员专属接口分别配置</li>
 *   <li><b>方法级安全</b>：通过 {@code @EnableMethodSecurity} 支持 {@code @PreAuthorize} 注解</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 允许跨域访问的前端来源白名单，从配置属性读取（支持逗号分隔多个域名）。
     * 本地开发由 application.yml 中 cors.allowed-origins 提供默认值；
     * 生产/Docker 环境通过 CORS_ALLOWED_ORIGINS 环境变量注入实际前端域名。
     */
    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    /**
     * 密码编码器 Bean：使用 BCrypt 算法（自带随机盐，10 轮哈希）
     * BCrypt 是目前公认的密码存储最佳实践，抗彩虹表和暴力破解
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证提供者：将 UserDetailsService 和 PasswordEncoder 关联，
     * 认证时自动从数据库加载用户并比对 BCrypt 密码
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器 Bean：在 AuthController 中手动触发认证（登录接口使用）
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * HTTP 安全过滤链配置（Spring Security 6.x Lambda DSL 风格）
     *
     * <p>接口权限规则：
     * <ul>
     *   <li>/api/auth/** ：开放（登录/注册）</li>
     *   <li>/h2-console/** ：开放（本地开发调试）</li>
     *   <li>/actuator/health ：开放（Docker 健康检查）</li>
     *   <li>GET /api/books/** ：已登录用户可访问</li>
     *   <li>POST/PUT/DELETE /api/books/** ：仅管理员</li>
     *   <li>GET /api/users ：仅管理员（列表查询）</li>
     *   <li>DELETE /api/users/** ：仅管理员</li>
     *   <li>其他所有请求：需要认证</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 禁用 CSRF（JWT 无状态认证无需 CSRF 保护）
                .csrf(csrf -> csrf.disable())
                // 启用 CORS 跨域（使用自定义配置）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态会话策略：不创建、不使用 HttpSession
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 接口权限规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 图书查询对所有已认证用户开放
                        .requestMatchers(HttpMethod.GET, "/api/books/**").authenticated()
                        // 图书增删改仅限管理员
                        .requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("ADMIN")
                        // 用户列表查询仅限管理员
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        // 用户删除仅限管理员
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        // 其余请求均需要登录认证
                        .anyRequest().authenticated()
                )
                // 未认证时返回 401（默认 Spring Security 返回 403，与测试预期不符）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint()))
                // H2 控制台需要在 iframe 中显示，禁用 X-Frame-Options
                .headers(headers ->
                        headers.frameOptions(frameOptions -> frameOptions.disable()))
                // 注册认证提供者
                .authenticationProvider(authenticationProvider())
                // 将 JWT 过滤器插入到用户名/密码认证过滤器之前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 未认证请求处理器：返回 HTTP 401 + JSON 错误信息。
     * Spring Security 6 默认对匿名用户返回 403，通过此 EntryPoint 改为标准的 401 Unauthorized
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":401,\"message\":\"未登录或Token已过期，请先登录\",\"data\":null}");
        };
    }

    /**
     * CORS 跨域配置。
     * 允许来源由 {@code cors.allowed-origins} 配置属性决定：
     * 本地开发默认开放 localhost 常用端口，生产环境通过 CORS_ALLOWED_ORIGINS 环境变量
     * 注入具体前端域名（如 {@code https://your-app.com}），禁止通配符。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 来源白名单由配置文件/环境变量注入，生产环境应为具体前端域名
        configuration.setAllowedOriginPatterns(allowedOrigins);
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept"));
        // 允许前端携带 Cookie 或 Authorization 等凭证
        configuration.setAllowCredentials(true);
        // 预检请求（OPTIONS）的缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
