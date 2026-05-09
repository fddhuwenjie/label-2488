-- ================================================================
-- 校园图书借阅管理系统 - 数据库初始化脚本（H2 兼容版本）
-- 本脚本在本地开发环境（H2 内存数据库）中自动执行
-- Docker 环境（MySQL）使用 db/mysql-init.sql
-- ================================================================

-- 用户表：存储系统用户信息，支持管理员和普通用户两种角色
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(100),
    phone      VARCHAR(20),
    role       VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    status     TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 图书表：记录馆藏图书基本信息和库存状态
-- status=0 表示逻辑删除（下架），查询时过滤 status=1
CREATE TABLE IF NOT EXISTS books (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn            VARCHAR(20),
    title           VARCHAR(200) NOT NULL,
    author          VARCHAR(100) NOT NULL,
    publisher       VARCHAR(100),
    category        VARCHAR(50),
    total_stock     INT          NOT NULL DEFAULT 0,
    available_stock INT          NOT NULL DEFAULT 0,
    description     VARCHAR(1000),
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 借阅记录表：记录每次借阅的完整生命周期
-- status: BORROWED（借阅中）/ RETURNED（已归还）/ OVERDUE（已逾期）
CREATE TABLE IF NOT EXISTS borrow_records (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    book_id     BIGINT      NOT NULL,
    borrow_date DATE        NOT NULL,
    due_date    DATE        NOT NULL,
    return_date DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (book_id) REFERENCES books (id)
);
