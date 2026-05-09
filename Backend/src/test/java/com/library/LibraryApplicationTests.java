package com.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 集成测试主入口
 * 验证应用上下文加载成功（使用默认 H2 内存数据库，无需外部服务）
 */
@SpringBootTest
class LibraryApplicationTests {

    /** 验证 Spring 上下文能够正常加载所有 Bean */
    @Test
    void contextLoads() {
        // 此测试方法为空：只要上下文加载不抛出异常即视为通过
    }
}
