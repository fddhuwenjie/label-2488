package com.library.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT 令牌工具类
 *
 * <p>负责 JWT 令牌的生成、解析和验证。使用 HMAC-SHA256（HS256）对称加密算法，
 * 密钥从配置文件读取（Base64 编码，长度 ≥ 256 位保证安全性）。
 *
 * <p>JWT 令牌格式：{@code Bearer <header>.<payload>.<signature>}
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * JWT 签名密钥（Base64 编码）。
     * 至少 32 字节（256 位）以满足 HMAC-SHA256 的安全要求
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** JWT 有效期（毫秒），从配置文件读取，默认 86400000ms = 24 小时 */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 从 Spring Security 的 {@link Authentication} 对象生成 JWT 令牌。
     * 登录成功后调用此方法获取令牌返回给客户端
     *
     * @param authentication Spring Security 认证成功后的 Authentication 对象
     * @return JWT 令牌字符串
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userDetails.getUsername());
    }

    /**
     * 根据用户名生成 JWT 令牌。
     * Payload 中存储：sub（用户名）、iat（签发时间）、exp（过期时间）
     *
     * @param username 用户名（存入 JWT subject）
     * @return JWT 令牌字符串
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(username)           // 设置 subject（用户名）
                .setIssuedAt(now)               // 签发时间
                .setExpiration(expiryDate)      // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // HS256 签名
                .compact();
    }

    /**
     * 从 JWT 令牌中提取用户名（即 subject 字段）
     *
     * @param token JWT 令牌字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * 验证 JWT 令牌的有效性（签名正确且未过期）
     *
     * @param token JWT 令牌字符串
     * @return true 表示令牌有效，false 表示无效（签名错误、过期、格式错误等）
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("JWT 签名无效: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT 令牌已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT 令牌格式不支持: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims 为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 将 Base64 编码的密钥字符串转换为 HMAC-SHA 签名密钥对象
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
