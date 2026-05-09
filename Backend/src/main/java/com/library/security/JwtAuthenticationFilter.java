package com.library.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * <p>继承 {@link OncePerRequestFilter}，保证每次 HTTP 请求只执行一次过滤逻辑。
 * 拦截所有请求，从 Authorization 请求头中提取 Bearer 令牌，
 * 验证有效性后将认证信息注入 Spring Security 上下文，实现无状态认证。
 *
 * <p>工作流程：
 * <pre>
 * 请求 → 提取 Authorization 头 → 截取 Bearer Token → 验证 JWT
 *      → 加载 UserDetails → 构建 Authentication → 存入 SecurityContext
 *      → 放行至后续过滤器
 * </pre>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * 核心过滤方法：每次请求执行一次 JWT 认证逻辑
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从 HTTP 请求头提取 JWT 令牌
            String jwt = extractTokenFromRequest(request);

            // 令牌非空且有效，则进行认证
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 从令牌中提取用户名
                String username = tokenProvider.getUsernameFromToken(jwt);

                // 从数据库加载用户详细信息（含权限列表）
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 构建认证对象（凭证设为 null，权限从 UserDetails 取）
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                // 设置请求详情（IP 地址、Session ID 等）
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证信息存入线程本地的 SecurityContext，后续代码可通过 SecurityContextHolder 访问
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // 认证失败不抛出异常，只记录日志，让后续的 Security 配置处理未认证请求
            log.error("无法设置用户认证信息: {}", ex.getMessage());
        }

        // 无论认证是否成功，都放行到下一个过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求的 Authorization 头提取 Bearer 令牌
     *
     * @param request HTTP 请求
     * @return JWT 令牌字符串（不含 "Bearer " 前缀），无效时返回 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // 标准 Bearer 令牌格式：Authorization: Bearer <token>
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
