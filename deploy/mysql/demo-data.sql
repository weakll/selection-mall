-- Demo data converted to selection-mall schema.
-- Source reference: https://github.com/macrozheng/mall-swarm
-- This file contains public demo catalog data only. It does not import users, orders or credentials.

USE `selection_mall`;

SET NAMES utf8mb4;

INSERT INTO brand (id, name, logo, is_deleted) VALUES
    (101, '华为', 'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180607/huawei.png', 0),
    (102, '小米', 'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/xiaomi.jpg', 0),
    (103, '苹果', 'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/apple.jpg', 0),
    (104, 'NIKE', 'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/nike.jpg', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), logo = VALUES(logo), is_deleted = 0;

INSERT INTO category (id, name, image_url, parent_id, status, order_num, is_deleted) VALUES
    (101, '手机数码', 'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/phone.png', 0, 1, 1, 0),
    (102, '手机通讯', NULL, 101, 1, 1, 0),
    (103, '运动鞋服', 'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/shoes.png', 0, 1, 2, 0),
    (104, '运动鞋', NULL, 103, 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = VALUES(image_url), parent_id = VALUES(parent_id), status = 1, is_deleted = 0;

INSERT INTO category_brand (id, brand_id, category_id, is_deleted) VALUES
    (101, 101, 102, 0),
    (102, 102, 102, 0),
    (103, 103, 102, 0),
    (104, 104, 104, 0)
ON DUPLICATE KEY UPDATE brand_id = VALUES(brand_id), category_id = VALUES(category_id), is_deleted = 0;

INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name,
                    slider_urls, spec_value, status, audit_status, audit_message, is_deleted) VALUES
    (101, '华为 HUAWEI P20 全面屏智能手机', 101, 101, 102, NULL, '部',
     '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180607/5ac1bf58Ndefaac16.jpg"]',
     '[{"key":"颜色","value":"亮黑色"},{"key":"容量","value":"64GB"}]', 1, 1, NULL, 0),
    (102, '小米8 全面屏游戏智能手机', 102, 101, 102, NULL, '部',
     '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/xiaomi.jpg"]',
     '[{"key":"颜色","value":"黑色"},{"key":"容量","value":"64GB"}]', 1, 1, NULL, 0),
    (103, 'Apple iPhone 8 Plus', 103, 101, 102, NULL, '部',
     '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5acc5248N6a5f81cd.jpg"]',
     '[{"key":"颜色","value":"红色"},{"key":"容量","value":"64GB"}]', 1, 1, NULL, 0),
    (104, 'NIKE 男子气垫休闲鞋', 104, 103, 104, NULL, '双',
     '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5b19403eN9f0b3cb8.jpg"]',
     '[{"key":"颜色","value":"白色"},{"key":"尺寸","value":"41"}]', 1, 1, NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), brand_id = VALUES(brand_id), category1_id = VALUES(category1_id),
    category2_id = VALUES(category2_id), unit_name = VALUES(unit_name), slider_urls = VALUES(slider_urls),
    spec_value = VALUES(spec_value), status = 1, audit_status = 1, is_deleted = 0;

INSERT INTO product_sku (id, sku_code, sku_name, product_id, thumb_img, sale_price, market_price, cost_price,
                        stock_num, sale_num, sku_spec, weight, volume, status, is_deleted) VALUES
    (101, 'DEMO-HUAWEI-P20-64', '华为 HUAWEI P20 64GB 亮黑色', 101,
     'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180607/5ac1bf58Ndefaac16.jpg', 3788.00, 4288.00, 3200.00,
     100, 120, '[{"key":"颜色","value":"亮黑色"},{"key":"容量","value":"64GB"}]', '0.2kg', '0.01m3', 1, 0),
    (102, 'DEMO-XIAOMI-8-64', '小米8 6GB+64GB 黑色', 102,
     'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/xiaomi.jpg', 2699.00, 2999.00, 2200.00,
     100, 180, '[{"key":"颜色","value":"黑色"},{"key":"容量","value":"64GB"}]', '0.2kg', '0.01m3', 1, 0),
    (103, 'DEMO-IPHONE8P-64', 'Apple iPhone 8 Plus 64GB 红色', 103,
     'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5acc5248N6a5f81cd.jpg', 4999.00, 5499.00, 4300.00,
     100, 90, '[{"key":"颜色","value":"红色"},{"key":"容量","value":"64GB"}]', '0.3kg', '0.01m3', 1, 0),
    (104, 'DEMO-NIKE-AIRMAX-41', 'NIKE AIR MAX 90 白色 41码', 104,
     'https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5b19403eN9f0b3cb8.jpg', 599.00, 799.00, 420.00,
     100, 60, '[{"key":"颜色","value":"白色"},{"key":"尺寸","value":"41"}]', '0.8kg', '0.02m3', 1, 0)
ON DUPLICATE KEY UPDATE sku_name = VALUES(sku_name), product_id = VALUES(product_id), thumb_img = VALUES(thumb_img),
    sale_price = VALUES(sale_price), market_price = VALUES(market_price), cost_price = VALUES(cost_price),
    stock_num = VALUES(stock_num), sku_spec = VALUES(sku_spec), status = 1, is_deleted = 0;

INSERT INTO product_details (id, product_id, image_urls, is_deleted) VALUES
    (101, 101, '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180607/5ac1bf58Ndefaac16.jpg"]', 0),
    (102, 102, '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/xiaomi.jpg"]', 0),
    (103, 103, '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5acc5248N6a5f81cd.jpg"]', 0),
    (104, 104, '["https://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5b19403eN9f0b3cb8.jpg"]', 0)
ON DUPLICATE KEY UPDATE image_urls = VALUES(image_urls), is_deleted = 0;

-- A small coupon set for local UI verification. Coupon status 1 means published.
INSERT INTO coupon_info (id, coupon_type, coupon_name, amount, condition_amount, publish_count, per_limit,
                         publish_status, expire_time, is_deleted) VALUES
    (101, 1, '演示新人券', 10.00, 0.00, 9999, 1, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), 0),
    (102, 2, '演示满50减15', 15.00, 50.00, 9999, 1, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), 0)
ON DUPLICATE KEY UPDATE coupon_name = VALUES(coupon_name), amount = VALUES(amount), condition_amount = VALUES(condition_amount),
    publish_status = 1, expire_time = VALUES(expire_time), is_deleted = 0;
