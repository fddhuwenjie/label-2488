-- 测试用户（使用高 ID 避免与 DataInitializer 冲突）
INSERT INTO users (id, username, password, email, phone, role, status)
VALUES (100, 'testuser', '$2a$10$hashedSecret', 'test@example.com', '13800138000', 'ROLE_USER', 1);

-- 测试图书：库存为 3 本（使用高 ID 避免与 DataInitializer 冲突）
INSERT INTO books (id, isbn, title, author, publisher, category, total_stock, available_stock, description, status)
VALUES (100, '978-1234567890', '并发编程实战', 'Brian Goetz', '机械工业出版社', '计算机', 3, 3, 'Java 并发编程经典', 1);
