package com.library.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.library.common.PageResult;
import com.library.dto.RegisterRequest;
import com.library.dto.UpdateUserRequest;
import com.library.entity.User;
import com.library.mapper.UserMapper;
import com.library.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户业务逻辑实现类
 *
 * <p>使用 {@code @Transactional(readOnly = true)} 为查询操作开启只读事务，
 * 提升数据库连接利用率；写操作通过方法级 {@code @Transactional} 覆盖。
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册实现。
     * 依次检查用户名和邮箱唯一性，BCrypt 加密密码后写入数据库。
     * 默认角色为 ROLE_USER，账号状态为正常（1）
     */
    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // 用户名唯一性校验
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在：" + request.getUsername());
        }
        // 邮箱唯一性校验（邮箱可选）
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userMapper.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册：" + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // BCrypt 加密密码，10 轮盐值，安全性高且抗彩虹表攻击
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("ROLE_USER");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("新用户注册成功: {}", user.getUsername());

        // 返回前清空密码字段，防止密码泄露
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在：ID=" + id);
        }
        return user;
    }

    @Override
    public User getCurrentUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在：" + username);
        }
        return user;
    }

    /**
     * 分页查询用户列表实现。
     *
     * <p>PageHelper 的使用方式：在查询方法调用前调用 {@code PageHelper.startPage()}，
     * PageHelper 会通过 MyBatis 拦截器自动在 SQL 后追加 LIMIT 子句实现物理分页。
     * 将结果包装为 {@link PageInfo} 即可获取分页元信息（总数、总页数等）。
     */
    @Override
    public PageResult<User> getAllUsers(String keyword, int pageNum, int pageSize) {
        // startPage 必须紧接在查询方法调用之前，中间不能有其他数据库操作
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.findAll(keyword);
        PageInfo<User> pageInfo = new PageInfo<>(users);
        return PageResult.of(pageInfo);
    }

    /**
     * 更新用户信息实现。
     * 使用 {@link User} 实体的动态 SQL 更新，只更新非空字段，避免覆盖未变更数据
     */
    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User existing = userMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在：ID=" + id);
        }

        User updateEntity = new User();
        updateEntity.setId(id);
        updateEntity.setEmail(request.getEmail());
        updateEntity.setPhone(request.getPhone());
        // 只有提供了新密码才更新密码
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            updateEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userMapper.update(updateEntity);
        log.info("用户信息已更新: ID={}", id);

        return userMapper.findById(id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (userMapper.findById(id) == null) {
            throw new RuntimeException("用户不存在：ID=" + id);
        }
        userMapper.deleteById(id);
        log.info("用户已删除: ID={}", id);
    }
}
