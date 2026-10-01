CREATE DATABASE IF NOT EXISTS `selection_mall`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `selection_mall`;

CREATE TABLE IF NOT EXISTS `brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `logo` VARCHAR(1024) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_brand_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `image_url` VARCHAR(1024) DEFAULT NULL,
    `parent_id` BIGINT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `order_num` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category_parent_status` (`parent_id`, `status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `category_brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `brand_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category_brand_category` (`category_id`, `is_deleted`),
    KEY `idx_category_brand_brand` (`brand_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product_unit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_unit_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product_spec` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `spec_name` VARCHAR(100) NOT NULL,
    `spec_value` TEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_spec_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `brand_id` BIGINT DEFAULT NULL,
    `category1_id` BIGINT DEFAULT NULL,
    `category2_id` BIGINT DEFAULT NULL,
    `category3_id` BIGINT DEFAULT NULL,
    `unit_name` VARCHAR(50) DEFAULT NULL,
    `slider_urls` MEDIUMTEXT,
    `spec_value` MEDIUMTEXT,
    `status` TINYINT NOT NULL DEFAULT 0,
    `audit_status` TINYINT NOT NULL DEFAULT 0,
    `audit_message` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_brand` (`brand_id`),
    KEY `idx_product_category1` (`category1_id`),
    KEY `idx_product_status_audit` (`status`, `audit_status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product_sku` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sku_code` VARCHAR(100) DEFAULT NULL,
    `sku_name` VARCHAR(255) NOT NULL,
    `product_id` BIGINT NOT NULL,
    `thumb_img` VARCHAR(1024) DEFAULT NULL,
    `sale_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `market_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `cost_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `stock_num` INT NOT NULL DEFAULT 0,
    `sale_num` INT NOT NULL DEFAULT 0,
    `sku_spec` TEXT,
    `weight` VARCHAR(32) DEFAULT NULL,
    `volume` VARCHAR(32) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_sku_product` (`product_id`, `status`, `is_deleted`),
    KEY `idx_product_sku_code` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product_details` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT NOT NULL,
    `image_urls` MEDIUMTEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_details_product` (`product_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `region` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(32) NOT NULL,
    `parent_code` BIGINT NOT NULL DEFAULT 0,
    `name` VARCHAR(128) NOT NULL,
    `level` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_region_parent_code` (`parent_code`, `is_deleted`),
    KEY `idx_region_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(100) DEFAULT NULL,
    `password` VARCHAR(255) DEFAULT NULL,
    `nick_name` VARCHAR(100) DEFAULT NULL,
    `avatar` VARCHAR(1024) DEFAULT NULL,
    `sex` TINYINT DEFAULT NULL,
    `phone` VARCHAR(32) DEFAULT NULL,
    `memo` VARCHAR(500) DEFAULT NULL,
    `open_id` VARCHAR(128) DEFAULT NULL,
    `union_id` VARCHAR(128) DEFAULT NULL,
    `last_login_ip` VARCHAR(64) DEFAULT NULL,
    `last_login_time` DATETIME DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_info_username` (`username`),
    KEY `idx_user_info_phone` (`phone`),
    KEY `idx_user_info_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(32) NOT NULL,
    `tag_name` VARCHAR(64) DEFAULT NULL,
    `province_code` VARCHAR(32) DEFAULT NULL,
    `city_code` VARCHAR(32) DEFAULT NULL,
    `district_code` VARCHAR(32) DEFAULT NULL,
    `address` VARCHAR(512) NOT NULL,
    `full_address` VARCHAR(1024) DEFAULT NULL,
    `is_default` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_address_user` (`user_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_collect` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `sku_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_collect_user` (`user_id`, `is_deleted`),
    KEY `idx_user_collect_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_browse_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `sku_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_browse_history_user` (`user_id`, `update_time`),
    KEY `idx_user_browse_history_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `coupon_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `coupon_type` TINYINT NOT NULL DEFAULT 0,
    `coupon_name` VARCHAR(100) NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `condition_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `start_time` DATETIME DEFAULT NULL,
    `end_time` DATETIME DEFAULT NULL,
    `range_type` TINYINT NOT NULL DEFAULT 0,
    `range_desc` VARCHAR(500) DEFAULT NULL,
    `publish_count` INT NOT NULL DEFAULT 0,
    `per_limit` INT NOT NULL DEFAULT 0,
    `use_count` INT NOT NULL DEFAULT 0,
    `receive_count` INT NOT NULL DEFAULT 0,
    `expire_time` DATETIME DEFAULT NULL,
    `publish_status` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_info_publish_window` (`publish_status`, `start_time`, `end_time`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `coupon_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `coupon_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `order_id` BIGINT DEFAULT NULL,
    `coupon_status` TINYINT NOT NULL DEFAULT 0,
    `get_type` TINYINT NOT NULL DEFAULT 0,
    `get_time` DATETIME DEFAULT NULL,
    `using_time` DATETIME DEFAULT NULL,
    `used_time` DATETIME DEFAULT NULL,
    `expire_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_user_user_status` (`user_id`, `coupon_status`, `is_deleted`),
    KEY `idx_coupon_user_coupon` (`coupon_id`),
    KEY `idx_coupon_user_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `name` VARCHAR(100) DEFAULT NULL,
    `phone` VARCHAR(32) DEFAULT NULL,
    `avatar` VARCHAR(1024) DEFAULT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_user_username` (`username`),
    KEY `idx_sys_user_status` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_name` VARCHAR(100) NOT NULL,
    `role_code` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_role_code` (`role_code`),
    KEY `idx_sys_role_name` (`role_name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `parent_id` BIGINT NOT NULL DEFAULT 0,
    `title` VARCHAR(100) NOT NULL,
    `component` VARCHAR(255) DEFAULT NULL,
    `sort_value` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_menu_parent_sort` (`parent_id`, `sort_value`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_user_role_user` (`user_id`),
    KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `is_half` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_role_menu_role` (`role_id`, `is_deleted`, `is_half`),
    KEY `idx_sys_role_menu_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_oper_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) DEFAULT NULL,
    `method` VARCHAR(255) DEFAULT NULL,
    `request_method` VARCHAR(32) DEFAULT NULL,
    `operator_type` VARCHAR(64) DEFAULT NULL,
    `oper_name` VARCHAR(100) DEFAULT NULL,
    `oper_url` VARCHAR(1024) DEFAULT NULL,
    `oper_ip` VARCHAR(64) DEFAULT NULL,
    `oper_param` MEDIUMTEXT,
    `json_result` MEDIUMTEXT,
    `status` TINYINT NOT NULL DEFAULT 0,
    `error_msg` TEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sys_oper_log_create_time` (`create_time`),
    KEY `idx_sys_oper_log_name` (`oper_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `nick_name` VARCHAR(100) DEFAULT NULL,
    `order_no` VARCHAR(64) NOT NULL,
    `coupon_id` BIGINT DEFAULT NULL,
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `coupon_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `original_total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `feight_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `pay_type` TINYINT DEFAULT NULL,
    `order_status` TINYINT NOT NULL DEFAULT 0,
    `receiver_name` VARCHAR(100) DEFAULT NULL,
    `receiver_phone` VARCHAR(32) DEFAULT NULL,
    `receiver_tag_name` VARCHAR(64) DEFAULT NULL,
    `receiver_province` VARCHAR(100) DEFAULT NULL,
    `receiver_city` VARCHAR(100) DEFAULT NULL,
    `receiver_district` VARCHAR(100) DEFAULT NULL,
    `receiver_address` VARCHAR(1024) DEFAULT NULL,
    `payment_time` DATETIME DEFAULT NULL,
    `delivery_time` DATETIME DEFAULT NULL,
    `receive_time` DATETIME DEFAULT NULL,
    `remark` TEXT,
    `cancel_time` DATETIME DEFAULT NULL,
    `cancel_reason` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_info_order_no` (`order_no`),
    KEY `idx_order_info_user_time` (`user_id`, `create_time`),
    KEY `idx_order_info_user_status` (`user_id`, `order_status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `sku_id` BIGINT NOT NULL,
    `sku_name` VARCHAR(255) NOT NULL,
    `thumb_img` VARCHAR(1024) DEFAULT NULL,
    `sku_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `sku_num` INT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`, `is_deleted`),
    KEY `idx_order_item_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `operate_user` VARCHAR(100) DEFAULT NULL,
    `process_status` TINYINT DEFAULT NULL,
    `note` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_log_order_time` (`order_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `stock_restore_compensation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) DEFAULT NULL,
    `stock_items_json` TEXT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `retry_count` INT NOT NULL DEFAULT 0,
    `error_message` VARCHAR(500) DEFAULT NULL,
    `next_retry_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_stock_restore_compensation_pending` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_date` DATE NOT NULL,
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `total_num` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_statistics_date` (`order_date`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payment_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `order_no` VARCHAR(64) NOT NULL,
    `pay_type` TINYINT NOT NULL DEFAULT 0,
    `out_trade_no` VARCHAR(128) DEFAULT NULL,
    `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `content` MEDIUMTEXT,
    `payment_status` TINYINT NOT NULL DEFAULT 0,
    `callback_time` DATETIME DEFAULT NULL,
    `callback_content` MEDIUMTEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_payment_info_order_status` (`order_no`, `payment_status`, `is_deleted`),
    KEY `idx_payment_info_out_trade_no` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
