package com.library.service;

import com.library.common.PageResult;
import com.library.dto.RegisterRequest;
import com.library.dto.UpdateUserRequest;
import com.library.entity.User;

/**
 * 用户业务服务接口
 * 定义用户模块的核心业务操作，由 {@link impl.UserServiceImpl} 提供具体实现
 */
public interface UserService {

    /**
     * 用户注册
     * 校验用户名/邮箱唯一性，BCrypt 加密密码后持久化
     *
     * @param request 注册请求（已通过参数校验）
     * @return 注册成功的用户信息（密码字段为 null）
     * @throws RuntimeException 用户名或邮箱已存在时抛出
     */
    User register(RegisterRequest request);

    /**
     * 根据 ID 查询用户信息
     *
     * @param id 用户 ID
     * @return 用户信息（密码字段为 null）
     * @throws RuntimeException 用户不存在时抛出
     */
    User getUserById(Long id);

    /**
     * 根据用户名查询当前登录用户信息（从 JWT 中提取用户名后调用）
     *
     * @param username 用户名
     * @return 用户信息（密码字段为 null）
     * @throws RuntimeException 用户不存在时抛出
     */
    User getCurrentUser(String username);

    /**
     * 分页查询用户列表（管理员专用）
     *
     * @param keyword  搜索关键字（用户名/邮箱模糊匹配），null 表示不过滤
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 分页结果，列表中的用户密码字段均为 null
     */
    PageResult<User> getAllUsers(String keyword, int pageNum, int pageSize);

    /**
     * 更新用户信息（局部更新，只更新非空字段）
     *
     * @param id      目标用户 ID
     * @param request 更新请求（可包含邮箱、手机、新密码）
     * @return 更新后的用户信息
     * @throws RuntimeException 用户不存在时抛出
     */
    User updateUser(Long id, UpdateUserRequest request);

    /**
     * 删除用户（物理删除，管理员专用）
     *
     * @param id 目标用户 ID
     * @throws RuntimeException 用户不存在时抛出
     */
    void deleteUser(Long id);
}
