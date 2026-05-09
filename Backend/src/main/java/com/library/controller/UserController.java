package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.UpdateUserRequest;
import com.library.entity.User;
import com.library.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * <p>提供用户信息查询、更新和删除的 RESTful 接口。
 * 权限控制原则：普通用户只能操作自己的数据，管理员可操作所有用户数据。
 *
 * <p>权限控制实现方式：
 * <ul>
 *   <li>路由级：SecurityConfig 中的 requestMatchers 配置（粗粒度）</li>
 *   <li>方法级：{@code @PreAuthorize} 注解（细粒度）</li>
 *   <li>业务级：Controller 中手动判断当前用户与目标用户的关系</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户信息（GET /api/users/me）
     * 从 JWT 中提取用户名（Authentication.getName()），查询并返回用户信息
     *
     * @param authentication Spring Security 注入的认证对象
     * @return 当前用户信息（密码字段为 null）
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(Authentication authentication) {
        User user = userService.getCurrentUser(authentication.getName());
        return Result.success(user);
    }

    /**
     * 查询用户列表（GET /api/users）
     * 仅管理员可访问（已在 SecurityConfig 中配置 + @PreAuthorize 双重保护）
     *
     * @param keyword  搜索关键字（用户名/邮箱模糊匹配），可选
     * @param pageNum  页码，默认 1
     * @param pageSize 每页记录数，默认 10
     * @return 分页用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<User>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<User> result = userService.getAllUsers(keyword, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 根据 ID 查询用户信息（GET /api/users/{id}）
     * 管理员可查任意用户，普通用户只能查询自己
     *
     * @param id             目标用户 ID
     * @param authentication 当前认证信息
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        // 非管理员只能查看自己的信息
        if (!"ROLE_ADMIN".equals(currentUser.getRole()) && !currentUser.getId().equals(id)) {
            return Result.error(403, "无权查看其他用户的信息");
        }
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 更新用户信息（PUT /api/users/{id}）
     * 管理员可修改任意用户，普通用户只能修改自己的信息
     *
     * @param id             目标用户 ID
     * @param request        更新请求（@Valid 触发参数格式校验）
     * @param authentication 当前认证信息
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id,
                                   @Valid @RequestBody UpdateUserRequest request,
                                   Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication.getName());
        // 权限检查：非管理员不能修改他人信息
        if (!"ROLE_ADMIN".equals(currentUser.getRole()) && !currentUser.getId().equals(id)) {
            return Result.error(403, "无权修改其他用户的信息");
        }
        User updated = userService.updateUser(id, request);
        return Result.success("用户信息更新成功", updated);
    }

    /**
     * 删除用户（DELETE /api/users/{id}）
     * 仅管理员可操作（SecurityConfig + @PreAuthorize 双重保护）
     *
     * @param id 目标用户 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }
}
