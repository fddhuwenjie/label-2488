package com.library.mapper;

import com.library.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层接口（MyBatis Mapper）
 *
 * <p>提供用户表的完整 CRUD 操作，SQL 定义在 resources/mapper/UserMapper.xml 中。
 * 二级缓存已在 XML 中通过 {@code <cache>} 标签启用，提升重复查询性能。
 */
public interface UserMapper {

    /**
     * 根据用户名查询用户（用于 Spring Security 认证）
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 null
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户实体，不存在时返回 null
     */
    User findById(@Param("id") Long id);

    /**
     * 查询用户列表（支持关键字搜索，管理员使用）
     *
     * @param keyword 搜索关键字，匹配用户名或邮箱，为 null 时查询全部
     * @return 用户列表（配合 PageHelper 实现物理分页）
     */
    List<User> findAll(@Param("keyword") String keyword);

    /**
     * 新增用户，执行后 user.id 自动回填（useGeneratedKeys）
     *
     * @param user 用户实体（密码已 BCrypt 加密）
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 动态更新用户信息（只更新非 null 字段）
     *
     * @param user 包含要更新字段的用户实体（id 不能为 null）
     * @return 影响行数
     */
    int update(User user);

    /**
     * 根据 ID 物理删除用户
     *
     * @param id 用户 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 检查用户名是否已被注册（注册时去重校验）
     *
     * @param username 待检查的用户名
     * @return true 表示已存在
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否已被注册
     *
     * @param email 待检查的邮箱
     * @return true 表示已存在
     */
    boolean existsByEmail(@Param("email") String email);
}
