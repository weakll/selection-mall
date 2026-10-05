-- 本地演示用的优惠券数据。
--
-- 说明：本文件原本还包含一组来自开源模板的演示商品（华为 P20 / 小米8 / iPhone 8 Plus /
-- NIKE 气垫鞋）及其品牌与分类。那批数据与"自营严选"定位冲突，且与自营商品目录
-- 形成两份商品数据源，已在重构中移除。
--
-- 商品目录的唯一数据源是 deploy/mysql/catalog.sql（8 商品 / 20 SKU），
-- 由 scripts/gen_catalog.py 生成。

USE `selection_mall`;

SET NAMES utf8mb4;

-- 少量优惠券，用于本地验证领券与下单抵扣。publish_status = 1 表示已发布。
INSERT INTO coupon_info (id, coupon_type, coupon_name, amount, condition_amount, publish_count, per_limit,
                         publish_status, expire_time, is_deleted) VALUES
    (101, 1, '新人无门槛券', 10.00, 0.00, 9999, 1, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), 0),
    (102, 2, '满 99 减 20', 20.00, 99.00, 9999, 1, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), 0),
    (103, 2, '满 299 减 50', 50.00, 299.00, 9999, 1, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), 0),
    (104, 2, '满 999 减 150', 150.00, 999.00, 9999, 1, 1, DATE_ADD(NOW(), INTERVAL 90 DAY), 0)
ON DUPLICATE KEY UPDATE coupon_name = VALUES(coupon_name), amount = VALUES(amount),
    condition_amount = VALUES(condition_amount), publish_status = 1,
    expire_time = VALUES(expire_time), is_deleted = 0;
