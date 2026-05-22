-- 电商购物网站数据库DDL
-- MySQL 8.0, InnoDB, utf8mb4
SET NAMES utf8mb4;

-- 用户表
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `age` int DEFAULT NULL COMMENT '年龄',
  `gender` tinyint DEFAULT NULL COMMENT '性别 0:未知 1:男 2:女',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_city` (`city`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `code` varchar(50) NOT NULL COMMENT '角色代码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 商品分类表
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `description` varchar(500) DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品表（支持逻辑删除）
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `sku` varchar(50) NOT NULL COMMENT 'SKU编码',
  `description` text COMMENT '商品描述',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `price` decimal(10,2) NOT NULL COMMENT '当前价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存数量',
  `sales_count` int NOT NULL DEFAULT 0 COMMENT '销售数量',
  `attributes_json` json DEFAULT NULL COMMENT '商品属性JSON',
  `image` varchar(500) DEFAULT NULL COMMENT '商品主图URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0:删除 1:正常 2:下架',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 商品图片表
CREATE TABLE `product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `is_main` tinyint NOT NULL DEFAULT 0 COMMENT '是否主图 0:否 1:是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sort` (`product_id`, `sort_order`),
  FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

-- 购物车主表
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_items` int NOT NULL DEFAULT 0 COMMENT '商品总数',
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_updated_at` (`updated_at`),
  FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车主表';

-- 购物车商品表
CREATE TABLE `cart_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `cart_id` bigint NOT NULL COMMENT '购物车ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `price` decimal(10,2) NOT NULL COMMENT '加入时价格',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_product` (`cart_id`, `product_id`),
  KEY `idx_product_id` (`product_id`),
  FOREIGN KEY (`cart_id`) REFERENCES `cart` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车商品表';

-- 订单表
CREATE TABLE `order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '实付金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态 0:待付款 1:已付款 2:已发货 3:已完成 4:已取消',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '支付方式',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `shipping_address` varchar(500) DEFAULT NULL COMMENT '收货地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '订单备注',
  `received_time` datetime DEFAULT NULL COMMENT '收货时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单商品表（含商品快照）
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(200) NOT NULL COMMENT '商品名称（快照）',
  `sku` varchar(50) NOT NULL COMMENT 'SKU编码（快照）',
  `price_at_purchase` decimal(10,2) NOT NULL COMMENT '购买时价格（快照）',
  `quantity` int NOT NULL COMMENT '购买数量',
  `subtotal` decimal(10,2) NOT NULL COMMENT '小计金额',
  `snapshot_json` json NOT NULL COMMENT '完整商品快照JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`),
  FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单商品表';

-- 销售记录表（用于推荐系统）
CREATE TABLE `sales_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `quantity` int NOT NULL COMMENT '购买数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_product` (`user_id`, `product_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_created_at` (`created_at`),
  FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售记录表';

-- 行为日志表
CREATE TABLE `behavior_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（可为空）',
  `path` varchar(500) NOT NULL COMMENT '请求路径',
  `method` varchar(10) NOT NULL COMMENT '请求方法',
  `params` text COMMENT '请求参数（脱敏后）',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` text COMMENT 'User-Agent',
  `duration` int NOT NULL DEFAULT 0 COMMENT '处理时长(ms)',
  `status_code` int NOT NULL COMMENT '响应状态码',
  `product_id` bigint DEFAULT NULL COMMENT '相关商品ID',
  `category_id` bigint DEFAULT NULL COMMENT '相关分类ID',
  `referer` varchar(500) DEFAULT NULL COMMENT '来源页面',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_path` (`path`(100)),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行为日志表';

-- 登录日志表
CREATE TABLE `login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `role` varchar(20) DEFAULT NULL COMMENT '用户角色 CUSTOMER/SALES/ADMIN',
  `ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `user_agent` text COMMENT '浏览器UA',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '登录状态 1:成功 0:失败',
  `failure_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 操作日志表（管理员/销售人员操作记录）
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `username` varchar(50) NOT NULL COMMENT '操作用户名',
  `role` varchar(20) NOT NULL COMMENT '用户角色 SALES/ADMIN',
  `operation` varchar(100) NOT NULL COMMENT '操作类型 create/update/delete',
  `resource` varchar(100) NOT NULL COMMENT '操作资源 product/order/category/user',
  `resource_id` bigint DEFAULT NULL COMMENT '资源ID',
  `content` text COMMENT '操作内容描述',
  `detail_json` json DEFAULT NULL COMMENT '操作详情JSON',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_operation` (`operation`),
  KEY `idx_resource` (`resource`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 每日汇总表
CREATE TABLE `daily_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汇总ID',
  `summary_date` date NOT NULL COMMENT '汇总日期',
  `summary_type` varchar(20) NOT NULL COMMENT '汇总类型 daily/hourly',
  `total_users` int NOT NULL DEFAULT 0 COMMENT '总用户数',
  `new_users` int NOT NULL DEFAULT 0 COMMENT '新增用户',
  `total_orders` int NOT NULL DEFAULT 0 COMMENT '总订单数',
  `total_sales` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '总销售额',
  `top_products_json` json DEFAULT NULL COMMENT '热销商品JSON',
  `user_distribution_json` json DEFAULT NULL COMMENT '用户分布JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_type` (`summary_date`, `summary_type`),
  KEY `idx_summary_date` (`summary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日汇总表';

-- 初始化角色数据
INSERT INTO `role` (`name`, `code`, `description`) VALUES
('顾客', 'CUSTOMER', '普通购物用户'),
('销售', 'SALES', '商品管理销售人员'),
('管理员', 'ADMIN', '系统管理员');