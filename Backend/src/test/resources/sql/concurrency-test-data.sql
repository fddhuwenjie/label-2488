INSERT INTO users (id, username, password, email, phone, role, status)
VALUES (1001, 'testuser1', '$2a$10$hashedPassword', 'testuser1@example.com', '13800139001', 'ROLE_USER', 1),
       (1002, 'testuser2', '$2a$10$hashedPassword', 'testuser2@example.com', '13800139002', 'ROLE_USER', 1),
       (1003, 'testuser3', '$2a$10$hashedPassword', 'testuser3@example.com', '13800139003', 'ROLE_USER', 1),
       (1004, 'testuser4', '$2a$10$hashedPassword', 'testuser4@example.com', '13800139004', 'ROLE_USER', 1),
       (1005, 'testuser5', '$2a$10$hashedPassword', 'testuser5@example.com', '13800139005', 'ROLE_USER', 1),
       (1006, 'testuser6', '$2a$10$hashedPassword', 'testuser6@example.com', '13800139006', 'ROLE_USER', 1),
       (1007, 'testuser7', '$2a$10$hashedPassword', 'testuser7@example.com', '13800139007', 'ROLE_USER', 1),
       (1008, 'testuser8', '$2a$10$hashedPassword', 'testuser8@example.com', '13800139008', 'ROLE_USER', 1),
       (1009, 'testuser9', '$2a$10$hashedPassword', 'testuser9@example.com', '13800139009', 'ROLE_USER', 1),
       (1010, 'testuser10', '$2a$10$hashedPassword', 'testuser10@example.com', '13800139010', 'ROLE_USER', 1);

INSERT INTO books (id, isbn, title, author, publisher, category, total_stock, available_stock, description, status)
VALUES (1000, '978-1234567890', '并发编程实战', 'Brian Goetz', '机械工业出版社', '计算机', 3, 3, 'Java并发编程经典著作', 1);
