package com.library.security;

import com.library.entity.User;
import com.library.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security 用户详情服务实现
 *
 * <p>Spring Security 在认证流程中通过此服务根据用户名加载用户信息。
 * 实现 {@link UserDetailsService} 接口，将数据库中的用户数据转换为
 * Spring Security 所需的 {@link UserDetails} 格式。
 *
 * <p>认证流程：
 * <pre>
 * AuthController.login() → AuthenticationManager.authenticate()
 *   → DaoAuthenticationProvider → UserDetailsService.loadUserByUsername()
 *   → 比对 BCrypt 密码 → 认证成功/失败
 * </pre>
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据用户名从数据库加载用户信息，供 Spring Security 认证使用
     *
     * @param username 用户输入的用户名
     * @return 包含用户名、加密密码和权限列表的 UserDetails 对象
     * @throws UsernameNotFoundException 用户名不存在或账号已禁用时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户（UserMapper 已开启 MyBatis 二级缓存，重复认证时可命中缓存）
        User user = userMapper.findByUsername(username);

        if (user == null) {
            log.warn("认证失败，用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("认证失败，账号已禁用: {}", username);
            throw new UsernameNotFoundException("账号已被禁用: " + username);
        }

        // 将自定义 User 转换为 Spring Security 的 UserDetails
        // 角色字段（如 ROLE_ADMIN）直接作为 GrantedAuthority，Spring Security 约定 ROLE_ 前缀
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())  // BCrypt 加密的密码，由 DaoAuthenticationProvider 验证
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority(user.getRole())))
                .build();
    }
}
