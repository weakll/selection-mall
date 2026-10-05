-- 精选商城自营商品目录（8 商品 / 20 SKU）
-- 由 scripts/gen_catalog.py 生成，请勿手工编辑
--
-- 定位：自营严选模式。全部商品使用自营品牌「甄选自营」，不含第三方商家商品，
-- 因此品牌表收敛为单一品牌，商品覆盖 8 个一级品类。
--
-- 为什么保留 SKU ID 1-9 而不是删表重建：
-- order_item 反规范化存储了 sku_id / sku_name / thumb_img / sku_price，
-- 历史订单对这些 ID 有引用（共 100+ 条明细）。删旧建新会让订单详情指向不存在的行。
-- 因此把新商品的 SKU 挂到这些 ID 上，并在本脚本末尾回填订单明细的名称、图片与单价。

USE `selection_mall`;
SET NAMES utf8mb4;
START TRANSACTION;

-- 移除开源模板遗留的演示商品（华为 / 小米 / 苹果 / NIKE），它们与自营定位冲突，
-- 且会形成第二份商品数据源。其分类与品牌一并清理。
DELETE FROM product_details WHERE product_id IN (101, 102, 103, 104);
DELETE FROM product_sku     WHERE product_id IN (101, 102, 103, 104);
DELETE FROM product         WHERE id         IN (101, 102, 103, 104);
DELETE FROM category_brand  WHERE id         IN (101, 102, 103, 104);
DELETE FROM category        WHERE id         IN (101, 102, 103, 104);
DELETE FROM brand           WHERE id         IN (101, 102, 103, 104);

-- 幂等前置清理：删掉本目录涉及的旧商品与 SKU，再重新插入。
-- 没有这一步时脚本不可重复执行 —— 上一次运行可能残留属于同一商品的多余 SKU
-- （例如笔记本本应 2 个 SKU，残留后变成 3 个），而 ON DUPLICATE KEY UPDATE
-- 只更新不删除，无法自愈。
DELETE FROM product_sku     WHERE product_id IN (1,2,3,11,12,13,14,15);
DELETE FROM product         WHERE id         IN (1,2,3,11,12,13,14,15);
DELETE FROM product_details WHERE product_id IN (1,2,3,11,12,13,14,15);

-- 清理不再属于本目录的遗留占位商品（含其 SKU 与详情图）。
-- 必须连商品一起删：只删 SKU 会留下没有 SKU 的空商品，前台会出现无价无货的条目。
DELETE FROM product_sku     WHERE product_id IN (5,6,8,9,10);
DELETE FROM product         WHERE id         IN (5,6,8,9,10);
DELETE FROM product_details WHERE product_id IN (5,6,8,9,10);

-- 自营模式：清掉历史遗留的测试品牌（'14' 'opop4' 'ihone17' 等无效数据），
-- 统一为自营品牌。旧商品已下架，故可安全改绑。
DELETE FROM brand WHERE id <> 1;
UPDATE brand SET name = '甄选自营' WHERE id = 1;

-- 关于 SKU ID：下面复用 1-9、19、20 这几个 ID，而不是另起新号。
-- 原因是 order_item 反规范化存储了 sku_id，历史订单对这些 ID 有引用；
-- 另起新号会让旧 ID 变成悬空引用，订单详情取不到商品。
-- 19/20 原属已下架的 ROG 商品，这里改挂到扫地机器人的颜色变体上。

-- 商品主记录
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (1, '甄选自营 探索者 Pro 5G 手机', 1, 1, 2, 3, '台', '["/static/products/phone-v1.jpg", "/static/products/phone-v2.jpg", "/static/products/phone-v3.jpg"]', '[{"key":"颜色","valueList":["白色","沙色","深空灰"]},{"key":"存储","valueList":["8GB+128GB","12GB+256GB"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (2, '甄选自营 轻羽 Air 14 笔记本', 1, 1, 41, 42, '台', '["/static/products/laptop-v1.jpg"]', '[{"key":"处理器","valueList":["i5 16GB+512GB","i7 16GB+1TB"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (3, '甄选自营 洁净 X10 扫地机器人', 1, 119, 133, 135, '台', '["/static/products/vacuum-v1.jpg", "/static/products/vacuum-v2.jpg", "/static/products/vacuum-v3.jpg"]', '[{"key":"颜色","valueList":["白色","浅绿","深灰"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (11, '甄选自营 悦食 空气炸锅', 1, 119, 146, 148, '台', '["/static/products/fryer-v1.jpg", "/static/products/fryer-v2.jpg"]', '[{"key":"容量","valueList":["4.5L","6L"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (12, '甄选自营 随行保温杯 500ml', 1, 475, 497, 500, '个', '["/static/products/tumbler-v1.jpg", "/static/products/tumbler-v2.jpg"]', '[{"key":"颜色","valueList":["雾蓝","藕粉"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (13, '甄选自营 纯棉四件套 1.8m', 1, 519, 520, 523, '套', '["/static/products/bedding-v1.jpg", "/static/products/bedding-v2.jpg"]', '[{"key":"颜色","valueList":["月白","豆沙"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (14, '甄选自营 醇香挂耳咖啡', 1, 316, 351, 356, '盒', '["/static/products/coffee-v1.jpg", "/static/products/coffee-v2.jpg"]', '[{"key":"规格","valueList":["10 片装","20 片装"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;
INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES
  (15, '甄选自营 商务双肩包 15.6 寸', 1, 385, 398, 401, '个', '["/static/products/bag-v1.jpg"]', '[{"key":"颜色","valueList":["深灰"]}]', 1, 1, 0)
  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), status=1, audit_status=1, is_deleted=0;

-- SKU：图片索引按变体种类计算，同一颜色的多个规格共用一张图
INSERT INTO product_sku (id, sku_code, sku_name, product_id, thumb_img, sale_price, market_price, cost_price, stock_num, sale_num, sku_spec, weight, status, is_deleted) VALUES
  (1, 'ZX010001', '甄选自营 探索者 Pro 5G 手机 颜色:白色 存储:8GB+128GB', 1, '/static/products/phone-v1.jpg', 1899, 2099, 1180, 120, 18, '[{"key":"颜色","value":"白色"},{"key":"存储","value":"8GB+128GB"}]', 0.45, 1, 0),
  (2, 'ZX010002', '甄选自营 探索者 Pro 5G 手机 颜色:白色 存储:12GB+256GB', 1, '/static/products/phone-v1.jpg', 2299, 2599, 1420, 96, 11, '[{"key":"颜色","value":"白色"},{"key":"存储","value":"12GB+256GB"}]', 0.45, 1, 0),
  (3, 'ZX010003', '甄选自营 探索者 Pro 5G 手机 颜色:沙色 存储:8GB+128GB', 1, '/static/products/phone-v2.jpg', 1899, 2099, 1180, 88, 7, '[{"key":"颜色","value":"沙色"},{"key":"存储","value":"8GB+128GB"}]', 0.45, 1, 0),
  (4, 'ZX010004', '甄选自营 探索者 Pro 5G 手机 颜色:沙色 存储:12GB+256GB', 1, '/static/products/phone-v2.jpg', 2299, 2599, 1420, 74, 4, '[{"key":"颜色","value":"沙色"},{"key":"存储","value":"12GB+256GB"}]', 0.45, 1, 0),
  (5, 'ZX010005', '甄选自营 探索者 Pro 5G 手机 颜色:深空灰 存储:8GB+128GB', 1, '/static/products/phone-v3.jpg', 1899, 2099, 1180, 130, 22, '[{"key":"颜色","value":"深空灰"},{"key":"存储","value":"8GB+128GB"}]', 0.45, 1, 0),
  (6, 'ZX010006', '甄选自营 探索者 Pro 5G 手机 颜色:深空灰 存储:12GB+256GB', 1, '/static/products/phone-v3.jpg', 2799, 2999, 1740, 62, 9, '[{"key":"颜色","value":"深空灰"},{"key":"存储","value":"12GB+256GB"}]', 0.45, 1, 0),
  (7, 'ZX020007', '甄选自营 轻羽 Air 14 笔记本 处理器:i5 16GB+512GB', 2, '/static/products/laptop-v1.jpg', 4299, 4699, 3180, 45, 6, '[{"key":"处理器","value":"i5 16GB+512GB"}]', 1.29, 1, 0),
  (8, 'ZX020008', '甄选自营 轻羽 Air 14 笔记本 处理器:i7 16GB+1TB', 2, '/static/products/laptop-v1.jpg', 5799, 6299, 4260, 28, 3, '[{"key":"处理器","value":"i7 16GB+1TB"}]', 1.29, 1, 0),
  (10, 'ZX030010', '甄选自营 洁净 X10 扫地机器人 颜色:白色', 3, '/static/products/vacuum-v1.jpg', 1699, 1999, 1120, 56, 12, '[{"key":"颜色","value":"白色"}]', 1.0, 1, 0),
  (20, 'ZX030020', '甄选自营 洁净 X10 扫地机器人 颜色:浅绿', 3, '/static/products/vacuum-v2.jpg', 1699, 1999, 1120, 42, 6, '[{"key":"颜色","value":"浅绿"}]', 1.0, 1, 0),
  (19, 'ZX030019', '甄选自营 洁净 X10 扫地机器人 颜色:深灰', 3, '/static/products/vacuum-v3.jpg', 1799, 2099, 1180, 38, 9, '[{"key":"颜色","value":"深灰"}]', 1.0, 1, 0),
  (21, 'ZX110021', '甄选自营 悦食 空气炸锅 容量:4.5L', 11, '/static/products/fryer-v1.jpg', 399, 459, 248, 140, 26, '[{"key":"容量","value":"4.5L"}]', 1.0, 1, 0),
  (22, 'ZX110022', '甄选自营 悦食 空气炸锅 容量:6L', 11, '/static/products/fryer-v1.jpg', 499, 569, 310, 96, 15, '[{"key":"容量","value":"6L"}]', 1.0, 1, 0),
  (23, 'ZX120023', '甄选自营 随行保温杯 500ml 颜色:雾蓝', 12, '/static/products/tumbler-v1.jpg', 129, 159, 62, 210, 48, '[{"key":"颜色","value":"雾蓝"}]', 1.0, 1, 0),
  (24, 'ZX120024', '甄选自营 随行保温杯 500ml 颜色:藕粉', 12, '/static/products/tumbler-v2.jpg', 149, 179, 72, 168, 31, '[{"key":"颜色","value":"藕粉"}]', 1.0, 1, 0),
  (25, 'ZX130025', '甄选自营 纯棉四件套 1.8m 颜色:月白', 13, '/static/products/bedding-v1.jpg', 349, 429, 196, 88, 14, '[{"key":"颜色","value":"月白"}]', 1.0, 1, 0),
  (26, 'ZX130026', '甄选自营 纯棉四件套 1.8m 颜色:豆沙', 13, '/static/products/bedding-v2.jpg', 429, 499, 240, 64, 8, '[{"key":"颜色","value":"豆沙"}]', 1.0, 1, 0),
  (27, 'ZX140027', '甄选自营 醇香挂耳咖啡 规格:10 片装', 14, '/static/products/coffee-v1.jpg', 69, 89, 34, 320, 96, '[{"key":"规格","value":"10 片装"}]', 1.0, 1, 0),
  (28, 'ZX140028', '甄选自营 醇香挂耳咖啡 规格:20 片装', 14, '/static/products/coffee-v1.jpg', 109, 139, 54, 240, 62, '[{"key":"规格","value":"20 片装"}]', 1.0, 1, 0),
  (29, 'ZX150029', '甄选自营 商务双肩包 15.6 寸 颜色:深灰', 15, '/static/products/bag-v1.jpg', 299, 369, 158, 110, 19, '[{"key":"颜色","value":"深灰"}]', 1.0, 1, 0)
ON DUPLICATE KEY UPDATE sku_code=VALUES(sku_code), sku_name=VALUES(sku_name), product_id=VALUES(product_id), thumb_img=VALUES(thumb_img), sale_price=VALUES(sale_price), market_price=VALUES(market_price), cost_price=VALUES(cost_price), stock_num=VALUES(stock_num), sale_num=VALUES(sale_num), sku_spec=VALUES(sku_spec), status=1, is_deleted=0;

-- 商品详情图：与轮播图同源同数量，保证详情页与列表页视觉一致
DELETE FROM product_details WHERE product_id IN (1,2,3,11,12,13,14,15);
INSERT INTO product_details (product_id, image_urls) VALUES (1, '["/static/products/phone-v1.jpg", "/static/products/phone-v2.jpg", "/static/products/phone-v3.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (2, '["/static/products/laptop-v1.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (3, '["/static/products/vacuum-v1.jpg", "/static/products/vacuum-v2.jpg", "/static/products/vacuum-v3.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (11, '["/static/products/fryer-v1.jpg", "/static/products/fryer-v2.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (12, '["/static/products/tumbler-v1.jpg", "/static/products/tumbler-v2.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (13, '["/static/products/bedding-v1.jpg", "/static/products/bedding-v2.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (14, '["/static/products/coffee-v1.jpg", "/static/products/coffee-v2.jpg"]');
INSERT INTO product_details (product_id, image_urls) VALUES (15, '["/static/products/bag-v1.jpg"]');

-- 回填历史订单明细：把旧的 SKU 名称/图片/单价换成新目录的值。
-- 订单明细反规范化存储了这些字段，不一起改就会出现「订单写着小米红米Note10，
-- 但点进商品页是另一件商品」的矛盾。
UPDATE order_item oi
  JOIN product_sku s ON s.id = oi.sku_id
SET oi.sku_name  = s.sku_name,
    oi.thumb_img = s.thumb_img,
    oi.sku_price = s.sale_price;

-- 重映射悬空明细。
--
-- 背景：新旧目录的 SKU 集合并不重合。旧华为笔记本的最后两档规格在新目录中没有
-- 对应配置，而 3 条历史订单明细引用了其中的 SKU 9；该行已被前置清理删除，
-- 订单详情会因此指向不存在的 SKU。
--
-- 处理方式分两步：
--   1) 显式迁移已知的旧 SKU：旧华为笔记本的最后一档规格（SKU 9）在新目录中没有
--      对应配置，把引用它的订单改挂到同品类的 i7 款（SKU 8）。这是真实业务里
--      「下架高配订单转为同品类在售配置」的常见处理。
--   2) 兜底：任何仍指向不存在 SKU 的明细，按旧名里的商品词匹配新 SKU，
--      匹配不到则取全局最低价 SKU，保证订单详情可渲染而不是指向空行。
--
-- 为什么不只靠自动匹配：新目录的商品名带「甄选自营」品牌前缀且型号全新，
-- 与旧名（如「华为笔记本 内存:32G」）几乎没有共同词，自动匹配会退化成兜底逻辑，
-- 把笔记本订单挂到咖啡上 —— 数据自洽但业务上说不通。
UPDATE order_item SET sku_id = 8
 WHERE sku_id = 9
   AND EXISTS (SELECT 1 FROM product_sku WHERE id = 8 AND is_deleted = 0);

UPDATE order_item oi
  JOIN (
    SELECT item_id, new_sku_id FROM (
      SELECT oi2.id AS item_id, s.id AS new_sku_id,
             ROW_NUMBER() OVER (PARTITION BY oi2.id ORDER BY
                 (s.sku_name LIKE CONCAT('%', SUBSTRING_INDEX(oi2.sku_name, ' ', 1), '%')) DESC,
                 s.sale_price ASC) AS rn
        FROM order_item oi2
        CROSS JOIN product_sku s
       WHERE s.is_deleted = 0
         AND oi2.sku_id NOT IN (SELECT id FROM product_sku)
    ) ranked WHERE rn = 1
  ) pick ON pick.item_id = oi.id
  JOIN product_sku tgt ON tgt.id = pick.new_sku_id
SET oi.sku_id    = tgt.id,
    oi.sku_name  = tgt.sku_name,
    oi.thumb_img = tgt.thumb_img,
    oi.sku_price = tgt.sale_price;

-- 迁移后同步订单明细的名称/图片/单价（含上面显式迁移的 SKU 8）
UPDATE order_item oi
  JOIN product_sku s ON s.id = oi.sku_id
SET oi.sku_name  = s.sku_name,
    oi.thumb_img = s.thumb_img,
    oi.sku_price = s.sale_price;

-- 重算订单金额：原价 = Σ(单价×数量)，总额 = 原价 + 运费 − 优惠券
UPDATE order_info oi
  JOIN (SELECT order_id, SUM(sku_price * sku_num) AS amt
          FROM order_item WHERE is_deleted = 0 GROUP BY order_id) t
    ON t.order_id = oi.id
SET oi.original_total_amount = t.amt,
    oi.total_amount = ROUND(t.amt + oi.feight_fee - oi.coupon_amount, 2)
WHERE oi.coupon_amount <= t.amt;

COMMIT;

-- 校验：以下查询应全部返回 0 行
-- 1) 订单明细指向不存在或已删除的 SKU
SELECT oi.id FROM order_item oi LEFT JOIN product_sku s ON s.id = oi.sku_id
  WHERE s.id IS NULL OR s.is_deleted = 1;
-- 2) 订单明细名称与 SKU 目录不一致
SELECT oi.id FROM order_item oi JOIN product_sku s ON s.id = oi.sku_id
  WHERE oi.sku_name <> s.sku_name;
-- 3) 商品缺少 SKU 或 SKU 缺少商品
SELECT p.id FROM product p LEFT JOIN product_sku s ON s.product_id = p.id
  WHERE p.is_deleted = 0 AND s.id IS NULL;
SELECT s.id FROM product_sku s LEFT JOIN product p ON p.id = s.product_id
  WHERE s.is_deleted = 0 AND p.id IS NULL;
