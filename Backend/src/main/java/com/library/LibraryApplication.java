package com.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园图书借阅管理系统 - 主启动类
 *
 * <p>系统涵盖三大核心业务模块：
 * <ul>
 *   <li>用户管理模块：注册、登录、个人信息维护、用户权限管理</li>
 *   <li>图书管理模块：图书CRUD、库存管理、分类检索</li>
 *   <li>借阅管理模块：借书、还书、借阅历史查询、逾期管理</li>
 * </ul>
 *
 * <p>技术栈：Spring Boot 3.2 + Spring MVC + MyBatis + Spring Security + JWT
 *
 * @author campus-library
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.library.mapper")
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
