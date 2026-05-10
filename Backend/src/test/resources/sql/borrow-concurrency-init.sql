INSERT INTO users (id, username, password, email, phone, role, status, created_at, updated_at)
VALUES (99, 'concurrent_user', '$2a$10$dummyHashForTest', 'concurrent@library.com', '13900000000', 'ROLE_USER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO books (id, isbn, title, author, publisher, category, total_stock, available_stock, description, status, created_at, updated_at)
VALUES (99, '978-CONCURRENT-001', '并发测试图书', '测试作者', '测试出版社', '计算机', 3, 3, '并发借阅测试专用图书', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
