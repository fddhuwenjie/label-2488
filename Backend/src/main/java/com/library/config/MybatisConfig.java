package com.library.config;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis 全局配置类
 *
 * <p>通过 {@link ConfigurationCustomizer} Bean 以编程方式定制 MyBatis 核心配置，
 * 体现 Spring Boot 轻配置原则：使用 Java 代码替代 XML 配置。
 *
 * <p>关键配置说明：
 * <ul>
 *   <li><b>延迟加载（lazyLoadingEnabled）</b>：全局开启，关联对象默认按需加载，
 *       减少不必要的数据库查询</li>
 *   <li><b>积极加载（aggressiveLazyLoading）</b>：关闭，访问任意属性时不触发全部关联加载，
 *       与延迟加载配合实现真正的按需查询</li>
 *   <li><b>二级缓存（cacheEnabled）</b>：全局开启，各 Mapper 通过 &lt;cache&gt; 标签
 *       独立配置缓存策略，减少重复查询的数据库压力</li>
 *   <li><b>驼峰映射（mapUnderscoreToCamelCase）</b>：自动将数据库下划线命名（created_at）
 *       映射为 Java 驼峰命名（createdAt），简化 ResultMap 配置</li>
 * </ul>
 */
@org.springframework.context.annotation.Configuration
public class MybatisConfig {

    /**
     * 自定义 MyBatis 配置 Bean。
     * Spring Boot 会在 {@link org.apache.ibatis.session.SqlSessionFactory} 创建时应用此自定义配置，
     * 配置的优先级高于 application.yml 中的同名 mybatis.configuration.* 属性
     */
    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return (Configuration configuration) -> {
            // 全局开启延迟加载（Lazy Loading），减少不必要的关联查询
            configuration.setLazyLoadingEnabled(true);

            // 关闭积极加载：只有显式访问关联属性时才触发加载，而非访问任意属性时就全部加载
            configuration.setAggressiveLazyLoading(false);

            // 全局开启二级缓存（各 Mapper 还需在 XML 中配置 <cache> 标签才生效）
            configuration.setCacheEnabled(true);

            // 开启数据库下划线命名到 Java 驼峰命名的自动转换
            configuration.setMapUnderscoreToCamelCase(true);
        };
    }
}
