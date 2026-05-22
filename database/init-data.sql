-- 初始化数据
SET NAMES utf8mb4;

-- 插入角色（忽略重复）
INSERT IGNORE INTO role (name, code, description, created_at) VALUES
('管理员', 'ADMIN', '系统管理员', NOW()),
('销售员', 'SALES', '销售专员', NOW()),
('普通用户', 'CUSTOMER', '普通用户', NOW());

-- 创建管理员账号 (密码: admin123)
INSERT INTO user (username, password, email, phone, city, status, created_at, updated_at) VALUES
('admin', '$2a$10$aevw9EX91z9Di4oRCCZ/Q.rKhl14HFIcIo2hnOHUmUfqVafTkicPW', 'admin@example.com', '13800138000', '北京', 1, NOW(), NOW());

-- 创建销售员账号 (密码: sales123)
INSERT INTO user (username, password, email, phone, city, status, created_at, updated_at) VALUES
('sales', '$2a$10$5YffufsiUUH47OZuNVcoxeSzEtcuBxMTqIOhJFa7iB9D.oVWDKh3K', 'sales@example.com', '13800138001', '上海', 1, NOW(), NOW());

-- 创建测试用户 (密码: user123)
INSERT INTO user (username, password, email, phone, city, status, created_at, updated_at) VALUES
('testuser', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'user@example.com', '13800138002', '广州', 1, NOW(), NOW()),
('zhangsan', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'zhangsan@test.com', '13800000001', '深圳', 1, NOW(), NOW()),
('lisi', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'lisi@test.com', '13800000002', '杭州', 1, NOW(), NOW()),
('wangwu', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'wangwu@test.com', '13800000003', '成都', 1, NOW(), NOW()),
('zhaoliu', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'zhaoliu@test.com', '13800000004', '北京', 1, NOW(), NOW()),
('sunqi', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'sunqi@test.com', '13800000005', '上海', 1, NOW(), NOW()),
('zhouba', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'zhouba@test.com', '13800000006', '广州', 1, NOW(), NOW()),
('wujiu', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'wujiu@test.com', '13800000007', '成都', 1, NOW(), NOW()),
('zhengshi', '$2a$10$C1nb3Y0v/6pGGRrPU7vquuGIWxhd4KKzul.C/MmmwKHLxGEzhBE62', 'zhengshi@test.com', '13800000008', '武汉', 1, NOW(), NOW());

-- 关联用户角色
INSERT INTO user_role (user_id, role_id, created_at) VALUES
(1, 3, NOW()),  -- admin -> ADMIN (role_id=3)
(2, 2, NOW()),  -- sales -> SALES (role_id=2)
(3, 1, NOW()),  -- testuser -> CUSTOMER (role_id=1)
(4, 1, NOW()),  -- zhangsan -> CUSTOMER
(5, 1, NOW()),  -- lisi -> CUSTOMER
(6, 1, NOW()),  -- wangwu -> CUSTOMER
(7, 1, NOW()),  -- zhaoliu -> CUSTOMER
(8, 1, NOW()),  -- sunqi -> CUSTOMER
(9, 1, NOW()),  -- zhouba -> CUSTOMER
(10, 1, NOW()), -- wujiu -> CUSTOMER
(11, 1, NOW()); -- zhengshi -> CUSTOMER

-- 插入商品分类
INSERT INTO category (name, description, sort_order, status, created_at, updated_at) VALUES
('电子产品', '手机、电脑、数码配件等', 1, 1, NOW(), NOW()),
('服装鞋帽', '男装、女装、鞋靴、配饰等', 2, 1, NOW(), NOW()),
('食品饮料', '零食、饮料、生鲜等', 3, 1, NOW(), NOW()),
('家居用品', '家具、厨具、家纺等', 4, 1, NOW(), NOW()),
('图书文具', '图书、办公用品、文具等', 5, 1, NOW(), NOW());

-- 插入商品数据
INSERT INTO product (name, sku, description, price, original_price, stock, category_id, image, status, sales_count, created_at, updated_at) VALUES
('iPhone 15 Pro', 'PHONE-001', '苹果最新旗舰手机，钛金属设计', 8999.00, 9999.00, 100, 1, 'https://via.placeholder.com/300x300?text=iPhone15', 1, 50, NOW(), NOW()),
('MacBook Pro 14', 'LAPTOP-001', '专业级笔记本电脑，M3芯片', 14999.00, 16999.00, 50, 1, 'https://via.placeholder.com/300x300?text=MacBook', 1, 30, NOW(), NOW()),
('AirPods Pro 2', 'AUDIO-001', '主动降噪无线耳机', 1899.00, 2299.00, 200, 1, 'https://via.placeholder.com/300x300?text=AirPods', 1, 150, NOW(), NOW()),
('纯棉T恤', 'CLOTH-001', '舒适透气的纯棉T恤', 99.00, 129.00, 500, 2, 'https://via.placeholder.com/300x300?text=T恤', 1, 300, NOW(), NOW()),
('运动鞋', 'SHOE-001', '轻便透气跑步鞋', 399.00, 499.00, 300, 2, 'https://via.placeholder.com/300x300?text=运动鞋', 1, 200, NOW(), NOW()),
('薯片大礼包', 'SNACK-001', '多种口味薯片组合', 59.00, 79.00, 1000, 3, 'https://via.placeholder.com/300x300?text=薯片', 1, 500, NOW(), NOW()),
('可乐 330ml*24', 'DRINK-001', '经典碳酸饮料', 45.00, 55.00, 800, 3, 'https://via.placeholder.com/300x300?text=可乐', 1, 400, NOW(), NOW()),
('四件套床品', 'HOME-001', '纯棉舒适四件套', 299.00, 399.00, 150, 4, 'https://via.placeholder.com/300x300?text=床品', 1, 80, NOW(), NOW()),
('保温杯', 'HOME-002', '304不锈钢保温杯', 79.00, 99.00, 400, 4, 'https://via.placeholder.com/300x300?text=保温杯', 1, 250, NOW(), NOW()),
('Java编程思想', 'BOOK-001', '经典Java编程书籍', 108.00, 128.00, 200, 5, 'https://via.placeholder.com/300x300?text=Java', 1, 120, NOW(), NOW());