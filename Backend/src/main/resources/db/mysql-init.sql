-- ================================================================
-- 校园图书借阅管理系统 - MySQL 初始化脚本
-- 在 docker-compose 启动 MySQL 容器时自动执行（首次启动有效）
-- 数据库 librarydb 由 MYSQL_DATABASE 环境变量自动创建
-- ================================================================

USE librarydb;

-- ----------------------------------------------------------------
-- 用户表
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID（自增主键）',
    username   VARCHAR(50)  NOT NULL UNIQUE          COMMENT '用户名（唯一，用于登录）',
    password   VARCHAR(255) NOT NULL                 COMMENT 'BCrypt 加密密码',
    email      VARCHAR(100)                          COMMENT '邮箱地址',
    phone      VARCHAR(20)                           COMMENT '手机号码',
    role       VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色：ROLE_ADMIN / ROLE_USER',
    status     TINYINT      NOT NULL DEFAULT 1       COMMENT '状态：1正常 0禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP            COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------------------------------------------
-- 图书表
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '图书ID',
    isbn            VARCHAR(20)                             COMMENT 'ISBN 编号',
    title           VARCHAR(200) NOT NULL                   COMMENT '图书名称',
    author          VARCHAR(100) NOT NULL                   COMMENT '作者',
    publisher       VARCHAR(100)                            COMMENT '出版社',
    category        VARCHAR(50)                             COMMENT '图书分类',
    total_stock     INT          NOT NULL DEFAULT 0         COMMENT '馆藏总数',
    available_stock INT          NOT NULL DEFAULT 0         COMMENT '当前可借数量',
    description     VARCHAR(1000)                           COMMENT '图书简介',
    status          TINYINT      NOT NULL DEFAULT 1         COMMENT '上架状态：1正常 0下架',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '入库时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP                          COMMENT '更新时间',
    INDEX idx_title    (title),
    INDEX idx_isbn     (isbn),
    INDEX idx_category (category),
    INDEX idx_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- ----------------------------------------------------------------
-- 借阅记录表
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow_records (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY COMMENT '借阅记录ID',
    user_id     BIGINT      NOT NULL                   COMMENT '借阅用户ID（外键）',
    book_id     BIGINT      NOT NULL                   COMMENT '图书ID（外键）',
    borrow_date DATE        NOT NULL                   COMMENT '借阅日期',
    due_date    DATE        NOT NULL                   COMMENT '应还日期（借阅日+30天）',
    return_date DATE                                   COMMENT '实际归还日期（未归还为NULL）',
    status      VARCHAR(20) NOT NULL DEFAULT 'BORROWED' COMMENT '状态：BORROWED/RETURNED/OVERDUE',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status  (status),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';
