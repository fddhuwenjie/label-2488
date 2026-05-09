package com.library;

import com.library.entity.Book;
import com.library.entity.User;
import com.library.mapper.BookMapper;
import com.library.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据初始化器
 *
 * <p>应用启动完成后自动执行（实现 {@link CommandLineRunner}），
 * 检查并插入测试账号和示例图书数据。具有幂等性：已存在的数据不会重复插入。
 *
 * <p>初始化内容：
 * <ul>
 *   <li>管理员账号：admin / admin123（ROLE_ADMIN）</li>
 *   <li>普通用户账号：user1 / user123（ROLE_USER）</li>
 *   <li>普通用户账号：user2 / user123（ROLE_USER）</li>
 *   <li>10 本示例图书（涵盖计算机、文学、历史、数学等分类）</li>
 * </ul>
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("开始初始化系统基础数据...");
        initUsers();
        initBooks();
        log.info("系统基础数据初始化完成");
    }

    /**
     * 初始化测试用户账号
     * 使用 BCryptPasswordEncoder 对密码进行加密，确保密码安全存储
     */
    private void initUsers() {
        // 初始化管理员账号（ROLE_ADMIN，拥有图书管理和用户管理权限）
        if (!userMapper.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@library.com");
            admin.setPhone("13800138000");
            admin.setRole("ROLE_ADMIN");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("初始化管理员账号成功: admin / admin123");
        }

        // 初始化普通用户1（ROLE_USER，可借还图书）
        if (!userMapper.existsByUsername("user1")) {
            User user1 = new User();
            user1.setUsername("user1");
            user1.setPassword(passwordEncoder.encode("user123"));
            user1.setEmail("user1@library.com");
            user1.setPhone("13900139001");
            user1.setRole("ROLE_USER");
            user1.setStatus(1);
            userMapper.insert(user1);
            log.info("初始化普通用户账号成功: user1 / user123");
        }

        // 初始化普通用户2
        if (!userMapper.existsByUsername("user2")) {
            User user2 = new User();
            user2.setUsername("user2");
            user2.setPassword(passwordEncoder.encode("user123"));
            user2.setEmail("user2@library.com");
            user2.setPhone("13900139002");
            user2.setRole("ROLE_USER");
            user2.setStatus(1);
            userMapper.insert(user2);
            log.info("初始化普通用户账号成功: user2 / user123");
        }
    }

    /**
     * 初始化示例图书数据（10 本，涵盖多个分类）
     * 仅在图书表为空时执行，避免重复插入
     */
    private void initBooks() {
        if (bookMapper.count() > 0) {
            log.info("图书数据已存在，跳过初始化");
            return;
        }

        // 示例图书数据：[ISBN, 书名, 作者, 出版社, 分类, 馆藏数量, 简介]
        String[][] bookData = {
            {"978-7-115-54742-4", "Spring Boot 实战（第5版）",
                "克雷格·沃斯", "人民邮电出版社", "计算机", "5",
                "Spring Boot 权威实战指南，涵盖 Web、数据访问、安全、测试等核心主题"},
            {"978-7-111-64703-4", "深入理解Java虚拟机（第3版）",
                "周志明", "机械工业出版社", "计算机", "3",
                "JVM 原理深度解析，Java 开发者必读经典"},
            {"978-7-115-47743-1", "Java编程思想（第4版）",
                "Bruce Eckel", "机械工业出版社", "计算机", "4",
                "Java 编程语言全面指南，面向对象设计经典教材"},
            {"978-7-302-45230-8", "数据结构与算法分析（Java语言描述）",
                "Mark Allen Weiss", "清华大学出版社", "计算机", "6",
                "经典算法与数据结构教材，附 Java 实现代码"},
            {"978-7-115-42876-7", "设计模式：可复用面向对象软件的基础",
                "GoF", "机械工业出版社", "计算机", "3",
                "软件设计模式 23 种经典模式，编程必读经典"},
            {"978-7-020-08912-5", "红楼梦",
                "曹雪芹", "人民文学出版社", "文学", "8",
                "中国四大名著之一，中国古典文学巅峰之作"},
            {"978-7-020-09071-8", "百年孤独",
                "加西亚·马尔克斯", "南海出版公司", "文学", "5",
                "魔幻现实主义文学巅峰之作，诺贝尔文学奖获奖作品"},
            {"978-7-01-011104-9", "中国通史",
                "白寿彝", "上海人民出版社", "历史", "4",
                "权威中国历史通识读本，从远古到近代完整梳理"},
            {"978-7-5399-5649-0", "人类简史",
                "尤瓦尔·赫拉利", "中信出版社", "历史", "6",
                "从认知革命到 21 世纪，重新审视人类的过去与未来"},
            {"978-7-04-053521-1", "高等数学（第七版）上册",
                "同济大学数学系", "高等教育出版社", "数学", "10",
                "国内高校使用最广泛的高等数学教材，理工科必备"}
        };

        for (String[] data : bookData) {
            Book book = new Book();
            book.setIsbn(data[0]);
            book.setTitle(data[1]);
            book.setAuthor(data[2]);
            book.setPublisher(data[3]);
            book.setCategory(data[4]);
            int stock = Integer.parseInt(data[5]);
            book.setTotalStock(stock);
            book.setAvailableStock(stock);
            book.setDescription(data[6]);
            bookMapper.insert(book);
        }

        log.info("初始化图书数据完成，共插入 {} 本图书", bookData.length);
    }
}
