"""生成"精选商城"自营商品数据（8 商品 / 20 SKU）。

## 为什么不是简单的 DELETE + INSERT

order_item 表里**反规范化**地存了 sku_id、sku_name、thumb_img、sku_price，
共 105 条历史明细引用了旧商品。若直接删旧建新：
  - 历史订单会出现"小米 红米Note10"这类已下架商品名（真实电商也会这样，可接受）
  - 但更糟的是 sku_id 指向已删除的行，订单详情页取图取值会出错

因此策略是**保留订单引用最多的 SKU ID（1-9）**，把新商品的 SKU 挂到这些 ID 上，
再按新 SKU 回填订单明细的名称、图片、单价，并重算订单金额。
这样历史订单与商品目录始终自洽。

## ID 分配

  商品 1  探索者 Pro 5G 手机   → 复用 SKU 1-6（订单引用最多，32+12+11+8+4+2 次）
  商品 2  轻羽 Air 14 笔记本   → 复用 SKU 7-9
  商品 3  洁净 X10 扫地机器人  → 复用 SKU 10
  商品 11 悦食 空气炸锅        → 新建 SKU 21-22
  商品 12 随行保温杯 500ml     → 新建 SKU 23-24
  商品 13 纯棉四件套 1.8m      → 新建 SKU 25-26
  商品 14 醇香挂耳咖啡         → 新建 SKU 27-28
  商品 15 商务双肩包 15.6 寸   → 新建 SKU 29

SKU 11-20 属于旧商品 5/6/8/9/10，全部删除。

用法：python gen_catalog.py <输出 sql 文件>
"""
import sys

BRAND_NAME = "甄选自营"

# 商品 ID → 图片文件名前缀。图片由 gen_product_images.py 生成。
PRODUCT_KEY = {
    1: "phone", 2: "laptop", 3: "vacuum", 11: "fryer", 12: "tumbler",
    13: "bedding", 14: "coffee", 15: "bag",
}

# (product_id, 名称, 一级, 二级, 三级, 单位, spec 维度, [(SKU id, 颜色, 规格, 售价, 市场价, 成本价, 库存, 销量)])
# spec 维度顺序决定 sku_name 的拼接顺序
CATALOG = [
    (1, "甄选自营 探索者 Pro 5G 手机", 1, 2, 3, "台", ["颜色", "存储"],
     [(1, "白色", "8GB+128GB", 1899, 2099, 1180, 120, 18),
      (2, "白色", "12GB+256GB", 2299, 2599, 1420, 96, 11),
      (3, "沙色", "8GB+128GB", 1899, 2099, 1180, 88, 7),
      (4, "沙色", "12GB+256GB", 2299, 2599, 1420, 74, 4),
      (5, "深空灰", "8GB+128GB", 1899, 2099, 1180, 130, 22),
      (6, "深空灰", "12GB+256GB", 2799, 2999, 1740, 62, 9)]),

    (2, "甄选自营 轻羽 Air 14 笔记本", 1, 41, 42, "台", ["处理器"],
     [(7, None, "i5 16GB+512GB", 4299, 4699, 3180, 45, 6),
      (8, None, "i7 16GB+1TB", 5799, 6299, 4260, 28, 3)]),

    (3, "甄选自营 洁净 X10 扫地机器人", 119, 133, 135, "台", ["颜色"],
     [(10, "白色", None, 1699, 1999, 1120, 56, 12),
      (20, "浅绿", None, 1699, 1999, 1120, 42, 6),
      (19, "深灰", None, 1799, 2099, 1180, 38, 9)]),

    (11, "甄选自营 悦食 空气炸锅", 119, 146, 148, "台", ["容量"],
     [(21, None, "4.5L", 399, 459, 248, 140, 26),
      (22, None, "6L", 499, 569, 310, 96, 15)]),

    (12, "甄选自营 随行保温杯 500ml", 475, 497, 500, "个", ["颜色"],
     [(23, "雾蓝", None, 129, 159, 62, 210, 48),
      (24, "藕粉", None, 149, 179, 72, 168, 31)]),

    (13, "甄选自营 纯棉四件套 1.8m", 519, 520, 523, "套", ["颜色"],
     [(25, "月白", None, 349, 429, 196, 88, 14),
      (26, "豆沙", None, 429, 499, 240, 64, 8)]),

    (14, "甄选自营 醇香挂耳咖啡", 316, 351, 356, "盒", ["规格"],
     [(27, None, "10 片装", 69, 89, 34, 320, 96),
      (28, None, "20 片装", 109, 139, 54, 240, 62)]),

    (15, "甄选自营 商务双肩包 15.6 寸", 385, 398, 401, "个", ["颜色"],
     [(29, "深灰", None, 299, 369, 158, 110, 19)]),
]

# 需要清理的旧商品（其 SKU 全部删除）
#
# 含两类：
#   5/6/8/9/10 —— 原始遗留的占位商品（可口可乐 / 可乐 / a / ROG显卡 / ROG）
#   3          —— 旧商品 3（华为手机最新款1），新目录要把扫地机器人挂到 ID 3 上
#
# 注意：清理必须**同时删商品与它的 SKU**。只删 SKU 会留下没有 SKU 的空商品，
# 在前台表现为列表里出现无价无货的条目。
LEGACY_PRODUCT_IDS = [5, 6, 8, 9, 10]

# 旧商品图片 → 新商品图片的映射（订单明细要换图，否则历史订单显示旧图）
LEGACY_IMAGE_MAP = {
    1: "/static/products/phone-v1.jpg",
    2: "/static/products/phone-v2.jpg",
    3: "/static/products/phone-v3.jpg",
    4: "/static/products/phone-v2.jpg",
    5: "/static/products/phone-v3.jpg",
    6: "/static/products/phone-v3.jpg",
    7: "/static/products/laptop-v1.jpg",
    8: "/static/products/laptop-v1.jpg",
    9: "/static/products/laptop-v1.jpg",
    10: "/static/products/vacuum-v1.jpg",
}


def sql_escape(s):
    return s.replace("'", "''")


def variant_image(product_id, color_idx):
    """按商品与变体序号取图片路径。"""
    return f"/static/products/{PRODUCT_KEY[product_id]}-v{color_idx}.jpg"


# 每种商品的图片变体数，必须与 gen_product_images.py 的 PRODUCTS 变体列表长度一致。
#
# 为什么单独维护而不是从 SKU 推导：图片变体数取决于商品**有几种外观**，
# 而不是有几个规格取值。笔记本只有"星银"一种外观（2 个处理器共用 1 张图），
# 咖啡只有一种包装外观，若从规格取值推导会算出 2 张不存在的图。
# gen_product_images.py 与本表的一致性由 gen_catalog.py 生成后的交叉校验兜底。
IMAGE_VARIANTS = {
    1: 3,    # 手机：白 / 沙 / 深空灰
    2: 1,    # 笔记本：星银
    3: 3,    # 扫地机器人：白 / 浅绿 / 深灰
    11: 2,   # 空气炸锅：4.5L / 6L
    12: 2,   # 保温杯：雾蓝 / 藕粉
    13: 2,   # 四件套：月白 / 豆沙
    14: 2,   # 咖啡：10 片装 / 20 片装
    15: 1,   # 双肩包：深灰
}

# 图片变体的选取键：该商品按哪个维度决定外观。
# 颜色类商品按颜色取图；非颜色类（处理器/容量/规格）只有一个图片变体，全部取第 1 张。
IMAGE_VARIANT_DIM = {
    1: "颜色", 2: "处理器", 3: "颜色", 11: "容量",
    12: "颜色", 13: "颜色", 14: "规格", 15: "颜色",
}


def image_index(product_id, skus, dims, color, spec):
    """算出某个 SKU 该用第几张图（从 1 开始）。"""
    dim = IMAGE_VARIANT_DIM[product_id]
    if dim not in dims:
        return 1
    # 颜色维度按取值在颜色列表中的位置；非颜色维度只有一个变体，恒为 1
    if dim != "颜色":
        return 1
    colors = list(dict.fromkeys(s[1] for s in skus if s[1]))
    value = color if dim == "颜色" else spec
    return colors.index(value) + 1 if value in colors else 1


def build_sku_name(product_name, dimensions, values):
    """按维度拼接 sku_name，形如「甄选自营 探索者 Pro 5G 手机 颜色:白色 存储:8GB+128GB」。"""
    parts = [product_name]
    for dim, val in zip(dimensions, values):
        if val:
            parts.append(f"{dim}:{val}")
    return " ".join(parts)


def main():
    out_path = sys.argv[1]
    catalog_ids = ",".join(str(c[0]) for c in CATALOG)
    legacy_ids = ",".join(str(i) for i in LEGACY_PRODUCT_IDS)
    lines = []
    a = lines.append

    a("-- 精选商城自营商品目录（8 商品 / 20 SKU）")
    a("-- 由 scripts/gen_catalog.py 生成，请勿手工编辑")
    a("--")
    a("-- 定位：自营严选模式。全部商品使用自营品牌「甄选自营」，不含第三方商家商品，")
    a("-- 因此品牌表收敛为单一品牌，商品覆盖 8 个一级品类。")
    a("--")
    a("-- 为什么保留 SKU ID 1-9 而不是删表重建：")
    a("-- order_item 反规范化存储了 sku_id / sku_name / thumb_img / sku_price，")
    a("-- 历史订单对这些 ID 有引用（共 100+ 条明细）。删旧建新会让订单详情指向不存在的行。")
    a("-- 因此把新商品的 SKU 挂到这些 ID 上，并在本脚本末尾回填订单明细的名称、图片与单价。")
    a("")
    a("USE `selection_mall`;")
    a("SET NAMES utf8mb4;")
    a("START TRANSACTION;")
    a("")
    a("-- 移除开源模板遗留的演示商品（华为 / 小米 / 苹果 / NIKE），它们与自营定位冲突，")
    a("-- 且会形成第二份商品数据源。其分类与品牌一并清理。")
    a("DELETE FROM product_details WHERE product_id IN (101, 102, 103, 104);")
    a("DELETE FROM product_sku     WHERE product_id IN (101, 102, 103, 104);")
    a("DELETE FROM product         WHERE id         IN (101, 102, 103, 104);")
    a("DELETE FROM category_brand  WHERE id         IN (101, 102, 103, 104);")
    a("DELETE FROM category        WHERE id         IN (101, 102, 103, 104);")
    a("DELETE FROM brand           WHERE id         IN (101, 102, 103, 104);")
    a("")
    a("-- 幂等前置清理：删掉本目录涉及的旧商品与 SKU，再重新插入。")
    a("-- 没有这一步时脚本不可重复执行 —— 上一次运行可能残留属于同一商品的多余 SKU")
    a("-- （例如笔记本本应 2 个 SKU，残留后变成 3 个），而 ON DUPLICATE KEY UPDATE")
    a("-- 只更新不删除，无法自愈。")
    a(f"DELETE FROM product_sku     WHERE product_id IN ({catalog_ids});")
    a(f"DELETE FROM product         WHERE id         IN ({catalog_ids});")
    a(f"DELETE FROM product_details WHERE product_id IN ({catalog_ids});")
    a("")
    a("-- 清理不再属于本目录的遗留占位商品（含其 SKU 与详情图）。")
    a("-- 必须连商品一起删：只删 SKU 会留下没有 SKU 的空商品，前台会出现无价无货的条目。")
    a(f"DELETE FROM product_sku     WHERE product_id IN ({legacy_ids});")
    a(f"DELETE FROM product         WHERE id         IN ({legacy_ids});")
    a(f"DELETE FROM product_details WHERE product_id IN ({legacy_ids});")
    a("")

    # ---------- 品牌：收敛为自营单一品牌 ----------
    a("-- 自营模式：清掉历史遗留的测试品牌（'14' 'opop4' 'ihone17' 等无效数据），")
    a("-- 统一为自营品牌。旧商品已下架，故可安全改绑。")
    a("DELETE FROM brand WHERE id <> 1;")
    a(f"UPDATE brand SET name = '{BRAND_NAME}' WHERE id = 1;")
    a("")

    # ---------- SKU ID 复用说明 ----------
    a("-- 关于 SKU ID：下面复用 1-9、19、20 这几个 ID，而不是另起新号。")
    a("-- 原因是 order_item 反规范化存储了 sku_id，历史订单对这些 ID 有引用；")
    a("-- 另起新号会让旧 ID 变成悬空引用，订单详情取不到商品。")
    a("-- 19/20 原属已下架的 ROG 商品，这里改挂到扫地机器人的颜色变体上。")
    a("")

    # ---------- 商品 ----------
    a("-- 商品主记录")
    for (pid, name, c1, c2, c3, unit, dims, skus) in CATALOG:
        key = PRODUCT_KEY[pid]

        # 轮播图数量 = 该商品的图片变体数（见 IMAGE_VARIANTS），不是 SKU 数也不是规格取值数
        sliders = ", ".join(
            f'"/static/products/{key}-v{i + 1}.jpg"'
            for i in range(IMAGE_VARIANTS[pid]))

        # spec_value：单维度取该维度各取值；双维度（颜色+存储）分别去重
        if len(dims) == 1:
            values = list(dict.fromkeys(s[1] if dims[0] == "颜色" else s[2] for s in skus))
            spec_json = '[{"key":"%s","valueList":[%s]}]' % (
                dims[0], ",".join('"%s"' % v for v in values))
        else:
            colors = list(dict.fromkeys(s[1] for s in skus))
            stores = list(dict.fromkeys(s[2] for s in skus))
            spec_json = ('[{"key":"颜色","valueList":[%s]},{"key":"存储","valueList":[%s]}]'
                         % (",".join('"%s"' % c for c in colors),
                            ",".join('"%s"' % s for s in stores)))

        a(f"INSERT INTO product (id, name, brand_id, category1_id, category2_id, category3_id, "
          f"unit_name, slider_urls, spec_value, status, audit_status, is_deleted) VALUES")
        a(f"  ({pid}, '{sql_escape(name)}', 1, {c1}, {c2}, {c3}, '{unit}', "
          f"'[{sliders}]', '{spec_json}', 1, 1, 0)")
        a("  ON DUPLICATE KEY UPDATE name=VALUES(name), brand_id=1, "
          "category1_id=VALUES(category1_id), category2_id=VALUES(category2_id), "
          "category3_id=VALUES(category3_id), unit_name=VALUES(unit_name), "
          "slider_urls=VALUES(slider_urls), spec_value=VALUES(spec_value), "
          "status=1, audit_status=1, is_deleted=0;")
    a("")

    # ---------- SKU ----------
    a("-- SKU：图片索引按变体种类计算，同一颜色的多个规格共用一张图")
    sku_rows = []
    for (pid, name, c1, c2, c3, unit, dims, skus) in CATALOG:
        # 图片索引由 image_index 统一计算，见其说明。
        # 不能在遍历中边收集变体边取索引：笔记本两个 SKU 属同一外观，
        # 边收集会让第二个 SKU 拿到索引 2，从而引用到不存在的 laptop-v2.jpg。
        for (sid, color, spec, sale, market, cost, stock, sale_num) in skus:
            img = variant_image(pid, image_index(pid, skus, dims, color, spec))
            label = color or spec
            values = [color, spec] if len(dims) == 2 else [label]
            sku_name = build_sku_name(name, dims, values)
            sku_spec = "[" + ",".join(
                '{{"key":"{}","value":"{}"}}'.format(d, v)
                for d, v in zip(dims, values) if v) + "]"
            sku_code = f"ZX{pid:02d}{sid:04d}"
            weight = 0.45 if pid == 1 else (1.29 if pid == 2 else 1.0)
            sku_rows.append((sid, sku_code, sku_name, pid, img, sale, market, cost,
                             stock, sale_num, sku_spec, weight))

    a("INSERT INTO product_sku (id, sku_code, sku_name, product_id, thumb_img, sale_price, "
      "market_price, cost_price, stock_num, sale_num, sku_spec, weight, status, is_deleted) VALUES")
    for i, (sid, code, sname, pid, img, sale, market, cost, stock, sale_num, spec, weight) in enumerate(sku_rows):
        # 最后一行不能以分号收尾：ON DUPLICATE KEY UPDATE 子句必须挂在同一条 INSERT 上。
        # 早期版本在这里写成 "最后一行加分号"，导致语句提前终止、子句变成独立语句，
        # 执行时报 Duplicate entry '1' for key 'product_sku.PRIMARY'。
        tail = "," if i < len(sku_rows) - 1 else ""
        a(f"  ({sid}, '{code}', '{sql_escape(sname)}', {pid}, '{img}', {sale}, {market}, "
          f"{cost}, {stock}, {sale_num}, '{spec}', {weight}, 1, 0){tail}")
    a("ON DUPLICATE KEY UPDATE sku_code=VALUES(sku_code), sku_name=VALUES(sku_name), "
      "product_id=VALUES(product_id), thumb_img=VALUES(thumb_img), "
      "sale_price=VALUES(sale_price), market_price=VALUES(market_price), "
      "cost_price=VALUES(cost_price), stock_num=VALUES(stock_num), "
      "sale_num=VALUES(sale_num), sku_spec=VALUES(sku_spec), status=1, is_deleted=0;")
    a("")

    # ---------- 商品详情图 ----------
    a("-- 商品详情图：与轮播图同源同数量，保证详情页与列表页视觉一致")
    a("DELETE FROM product_details WHERE product_id IN "
      f"({','.join(str(c[0]) for c in CATALOG)});")
    for (pid, name, c1, c2, c3, unit, dims, skus) in CATALOG:
        key = PRODUCT_KEY[pid]
        urls = ", ".join(
            f'"/static/products/{key}-v{i + 1}.jpg"'
            for i in range(IMAGE_VARIANTS[pid]))
        a(f"INSERT INTO product_details (product_id, image_urls) VALUES ({pid}, '[{urls}]');")
    a("")

    # ---------- 重映射悬空订单明细 ----------
    # ---------- 回填与重映射历史订单明细 ----------
    a("-- 回填历史订单明细：把旧的 SKU 名称/图片/单价换成新目录的值。")
    a("-- 订单明细反规范化存储了这些字段，不一起改就会出现「订单写着小米红米Note10，")
    a("-- 但点进商品页是另一件商品」的矛盾。")
    a("UPDATE order_item oi")
    a("  JOIN product_sku s ON s.id = oi.sku_id")
    a("SET oi.sku_name  = s.sku_name,")
    a("    oi.thumb_img = s.thumb_img,")
    a("    oi.sku_price = s.sale_price;")
    a("")
    a("-- 重映射悬空明细。")
    a("--")
    a("-- 背景：新旧目录的 SKU 集合并不重合。旧华为笔记本的最后两档规格在新目录中没有")
    a("-- 对应配置，而 3 条历史订单明细引用了其中的 SKU 9；该行已被前置清理删除，")
    a("-- 订单详情会因此指向不存在的 SKU。")
    a("--")
    a("-- 处理方式分两步：")
    a("--   1) 显式迁移已知的旧 SKU：旧华为笔记本的最后一档规格（SKU 9）在新目录中没有")
    a("--      对应配置，把引用它的订单改挂到同品类的 i7 款（SKU 8）。这是真实业务里")
    a("--      「下架高配订单转为同品类在售配置」的常见处理。")
    a("--   2) 兜底：任何仍指向不存在 SKU 的明细，按旧名里的商品词匹配新 SKU，")
    a("--      匹配不到则取全局最低价 SKU，保证订单详情可渲染而不是指向空行。")
    a("--")
    a("-- 为什么不只靠自动匹配：新目录的商品名带「甄选自营」品牌前缀且型号全新，")
    a("-- 与旧名（如「华为笔记本 内存:32G」）几乎没有共同词，自动匹配会退化成兜底逻辑，")
    a("-- 把笔记本订单挂到咖啡上 —— 数据自洽但业务上说不通。")
    a("UPDATE order_item SET sku_id = 8")
    a(" WHERE sku_id = 9")
    a("   AND EXISTS (SELECT 1 FROM product_sku WHERE id = 8 AND is_deleted = 0);")
    a("")
    a("UPDATE order_item oi")
    a("  JOIN (")
    a("    SELECT item_id, new_sku_id FROM (")
    a("      SELECT oi2.id AS item_id, s.id AS new_sku_id,")
    a("             ROW_NUMBER() OVER (PARTITION BY oi2.id ORDER BY")
    a("                 (s.sku_name LIKE CONCAT('%', SUBSTRING_INDEX(oi2.sku_name, ' ', 1), '%')) DESC,")
    a("                 s.sale_price ASC) AS rn")
    a("        FROM order_item oi2")
    a("        CROSS JOIN product_sku s")
    a("       WHERE s.is_deleted = 0")
    a("         AND oi2.sku_id NOT IN (SELECT id FROM product_sku)")
    a("    ) ranked WHERE rn = 1")
    a("  ) pick ON pick.item_id = oi.id")
    a("  JOIN product_sku tgt ON tgt.id = pick.new_sku_id")
    a("SET oi.sku_id    = tgt.id,")
    a("    oi.sku_name  = tgt.sku_name,")
    a("    oi.thumb_img = tgt.thumb_img,")
    a("    oi.sku_price = tgt.sale_price;")
    a("")
    a("-- 迁移后同步订单明细的名称/图片/单价（含上面显式迁移的 SKU 8）")
    a("UPDATE order_item oi")
    a("  JOIN product_sku s ON s.id = oi.sku_id")
    a("SET oi.sku_name  = s.sku_name,")
    a("    oi.thumb_img = s.thumb_img,")
    a("    oi.sku_price = s.sale_price;")
    a("")

    a("-- 重算订单金额：原价 = Σ(单价×数量)，总额 = 原价 + 运费 − 优惠券")
    a("UPDATE order_info oi")
    a("  JOIN (SELECT order_id, SUM(sku_price * sku_num) AS amt")
    a("          FROM order_item WHERE is_deleted = 0 GROUP BY order_id) t")
    a("    ON t.order_id = oi.id")
    a("SET oi.original_total_amount = t.amt,")
    a("    oi.total_amount = ROUND(t.amt + oi.feight_fee - oi.coupon_amount, 2)")
    a("WHERE oi.coupon_amount <= t.amt;")
    a("")

    a("COMMIT;")
    a("")
    a("-- 校验：以下查询应全部返回 0 行")
    a("-- 1) 订单明细指向不存在或已删除的 SKU")
    a("SELECT oi.id FROM order_item oi LEFT JOIN product_sku s ON s.id = oi.sku_id")
    a("  WHERE s.id IS NULL OR s.is_deleted = 1;")
    a("-- 2) 订单明细名称与 SKU 目录不一致")
    a("SELECT oi.id FROM order_item oi JOIN product_sku s ON s.id = oi.sku_id")
    a("  WHERE oi.sku_name <> s.sku_name;")
    a("-- 3) 商品缺少 SKU 或 SKU 缺少商品")
    a("SELECT p.id FROM product p LEFT JOIN product_sku s ON s.product_id = p.id")
    a("  WHERE p.is_deleted = 0 AND s.id IS NULL;")
    a("SELECT s.id FROM product_sku s LEFT JOIN product p ON p.id = s.product_id")
    a("  WHERE s.is_deleted = 0 AND p.id IS NULL;")

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    print(f"wrote {out_path}")
    print(f"  products: {len(CATALOG)}")
    print(f"  skus: {len(sku_rows)}")


if __name__ == "__main__":
    main()
