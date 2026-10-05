# 演示数据与商品配图

## 商品配图

`mall-h5/public/static/products/` 下的商品图由 `scripts/gen_product_images.py` 生成，
**不是下载的第三方图片**。

```bash
python scripts/gen_product_images.py mall-h5/public/static/products
```

生成逻辑与设计取舍：

- 同一套视觉语言（浅色渐变背景 + 白色卡片 + 深色文字），8 个商品风格统一
- 每个品类一个主色调，便于在列表页区分
- 同一商品的多个颜色规格生成不同色相的变体，**让「白色 / 沙色 / 深空灰」这类
  规格差异在视觉上真实成立**，而不是所有规格共用一张图
- 颜色名与渲染颜色受控对应（"深空灰"就是灰），不随机调色相 —— 否则会出现
  规格名与图片颜色不符的情况
- 不含任何真实品牌 logo 与商标，避免公开仓库的商标风险

输出规格：900×900 JPEG，quality 88。

### 图片与数据的对应关系

图片文件名与 `deploy/mysql/catalog.sql` 里 SKU 的 `thumb_img` 字段一一对应，
命名规则为 `{商品key}-v{变体序号}.jpg`，另有 `{商品key}-main.jpg` 作为列表页主图。

商品有几个图片变体由 `gen_catalog.py` 的 `IMAGE_VARIANTS` 声明，必须与
`gen_product_images.py` 的 `PRODUCTS` 变体列表长度一致 —— 两者不一致时会引用到
不存在的文件。生成 catalog.sql 后可自检引用完整性：

```bash
grep -o '/static/products/[a-z0-9-]*\.jpg' deploy/mysql/catalog.sql \
  | sort -u | sed 's|/static/products/||' \
  | while read f; do [ -f "mall-h5/public/static/products/$f" ] || echo "缺失: $f"; done
```

注意：图片变体数取决于商品**有几种外观**，不是有几个规格取值。
例如笔记本只有「星银」一种外观，两个处理器规格共用同一张图。

## 商品数据

商品目录的唯一数据源是 `deploy/mysql/catalog.sql`（8 商品 / 20 SKU / 1 个自营品牌），
由 `scripts/gen_catalog.py` 生成：

```bash
python scripts/gen_catalog.py deploy/mysql/catalog.sql
```

定位是**自营严选**：全部商品使用自营品牌「甄选自营」，不含第三方商家商品。

该脚本可重复执行（幂等）：前置清理会先删除本目录涉及的商品与 SKU 再重新插入，
只靠 `ON DUPLICATE KEY UPDATE` 无法清除上一次运行残留的多余 SKU。

它还负责把历史订单明细里的 `sku_name` / `thumb_img` / `sku_price` 同步到新目录 ——
`order_item` 反规范化存储了这些字段，不同步会出现「订单写着旧商品名，
但点进商品页是另一件商品」的矛盾。脚本末尾附有三种完整性校验查询，应全部返回 0 行。

## 历史遗留素材

`mall-h5/public/static/products/` 下仍保留早期素材
（`phone-main.jpg`、`phone-detail-1.jpg`、`phone-detail-2.jpg`、`shoe-main.jpg`、
`laptop-main.jpg`、`watch-main.jpg`）。它们是本地复制的 Unsplash 公开图片，
曾用于开源模板的演示商品（华为 P20 / 小米 8 / iPhone 8 Plus / NIKE 气垫鞋）。

那批商品已从 `demo-data.sql` 移除，这些图目前不被任何数据引用，确认无用可直接删除。

来源与许可：

- Phone: https://images.unsplash.com/photo-1511707171634-5f897ff02aa9
- Phone detail 1: https://images.unsplash.com/photo-1592899677977-9c10ca588bbd
- Phone detail 2: https://images.unsplash.com/photo-1556656793-08538906a9f8
- Shoes: https://images.unsplash.com/photo-1542291026-7eec264c27ff
- Laptop: https://images.unsplash.com/photo-1496181133206-80ce9b88a853
- Watch: https://images.unsplash.com/photo-1523275335684-37898b6baf30
- Unsplash license: https://unsplash.com/license
