"""生成"精选商城"商品配图。

目标：替换仓库里 6 张与商品对不上的旧图（watch-main 配扫地机、shoe-main 配四件套），
并为颜色规格生成可区分的色相变体 —— 让"白色/沙色/深空灰"这类 SKU 差异在视觉上真实成立。

设计原则：
  - 同一套视觉语言（浅色渐变背景 + 白色卡片 + 深色文字），8 个商品风格统一
  - 每个品类一个主色调，便于在列表页区分
  - 不含真实品牌 logo，避免商标风险
  - 输出 900x900 JPEG，与现有 phone-main.jpg 尺寸一致

用法：python gen_product_images.py <输出目录>
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

SIZE = 900

# 品类 → (背景主色, 强调色)
PALETTE = {
    "phone":   ((198, 216, 240), (58, 96, 152)),
    "laptop":  ((206, 214, 226), (62, 76, 100)),
    "vacuum":  ((200, 224, 220), (44, 108, 100)),
    "fryer":   ((240, 218, 196), (150, 92, 44)),
    "tumbler": ((226, 216, 236), (96, 68, 140)),
    "bedding": ((232, 216, 214), (140, 84, 82)),
    "coffee":  ((226, 210, 190), (104, 72, 44)),
    "bag":     ((212, 220, 210), (66, 96, 70)),
}

# 商品定义：key, 中文名, 英文副标题, 规格行, [(图片标注, 调色名)]
#
# 图片标注必须与 gen_catalog.py 里该商品的**变体种类**逐一对应、顺序一致，
# 否则商品图会引用到不存在的文件。变体数 = 颜色种类数，不是 SKU 数
# （同一颜色可能有多个存储规格，但共用一张图）。
#
# 调色名决定卡片主色；容量/规格这类非颜色变体用 "原味"（不调色）。
PRODUCTS = [
    ("phone",   "探索者 Pro 5G 手机",  "EXPLORER PRO",   "6.7 英寸 · 5000mAh · 5000 万像素",
     [("白色", "白色"), ("沙色", "沙色"), ("深空灰", "深空灰")]),
    ("laptop",  "轻羽 Air 14 笔记本",  "FEATHER AIR 14", "14 英寸 2.8K · 1.29kg · 背光键盘",
     [("星银", "星银")]),
    ("vacuum",  "洁净 X10 扫地机器人", "PURE X10",       "激光导航 · 自动集尘 · 拖扫一体",
     [("白色", "白色"), ("浅绿", "浅绿"), ("深灰", "深灰")]),
    ("fryer",   "悦食 空气炸锅",       "TASTY FRYER",    "可视窗 · 双旋钮 · 不粘涂层",
     [("4.5L", "原味"), ("6L", "原味")]),
    ("tumbler", "随行保温杯 500ml",    "TRAVEL MUG",     "316 不锈钢 · 12 小时保温",
     [("雾蓝", "雾蓝"), ("藕粉", "藕粉")]),
    ("bedding", "纯棉四件套 1.8m",     "COTTON BEDDING", "60 支长绒棉 · 被套 200x230",
     [("月白", "月白"), ("豆沙", "豆沙")]),
    ("coffee",  "醇香挂耳咖啡",        "DRIP COFFEE",    "中深烘焙 · 现磨锁鲜",
     [("10 片装", "原味"), ("20 片装", "原味")]),
    ("bag",     "商务双肩包 15.6 寸",  "URBAN BACKPACK", "防泼水 · 独立电脑仓 · USB 接口",
     [("深灰", "深灰")]),
]

# 颜色名 → 卡片主体的调整方式：(目标色相覆盖 或 None, 饱和度系数, 明度系数)
# 只做受控调整，保证"深空灰"看起来是灰、"沙色"偏暖米、"雾蓝"偏冷蓝。
COLOR_TUNING = {
    "白色":   (None, 0.10, 1.16),
    "奶油白": (48,   0.22, 1.14),
    "月白":   (210,  0.12, 1.14),
    "星银":   (210,  0.10, 1.10),
    "沙色":   (36,   0.42, 1.02),
    "深空灰": (215,  0.06, 0.66),
    "深灰":   (215,  0.06, 0.62),
    "浅绿":   (150,  0.34, 1.02),
    "雾蓝":   (205,  0.52, 0.96),
    "藕粉":   (335,  0.34, 1.06),
    "豆沙":   (8,    0.40, 0.86),
    "原味":   (None, 1.00, 1.00),
}


def load_font(size, bold=False):
    """找一个能显示中文的字体。缺字体时退回默认字体（中文会显示为方块，但不会崩）。"""
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                continue
    return ImageFont.load_default()


def tune_color(color, variant):
    """按颜色名把卡片主色调到名实相符。"""
    import colorsys
    target_hue, sat_k, light_k = COLOR_TUNING.get(variant, (None, 1.0, 1.0))
    r, g, b = [c / 255 for c in color]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    if target_hue is not None:
        h = (target_hue % 360) / 360.0
    s = max(0.0, min(1.0, s * sat_k))
    l = max(0.0, min(1.0, l * light_k))
    r, g, b = colorsys.hls_to_rgb(h, l, s)
    return (int(r * 255), int(g * 255), int(b * 255))


def make_gradient(bg, top_lighten=0.35):
    """竖向渐变背景：顶部更亮，底部接近主色。"""
    img = Image.new("RGB", (SIZE, SIZE), bg)
    draw = ImageDraw.Draw(img)
    for y in range(SIZE):
        t = y / SIZE
        k = 1 - t * top_lighten
        color = tuple(min(255, int(c + (255 - c) * k * 0.5)) for c in bg)
        draw.line([(0, y), (SIZE, y)], fill=color)
    return img.filter(ImageFilter.GaussianBlur(0.6))


def rounded_card(img, accent):
    """中央白色圆角卡片 + 柔和阴影，作为商品"照片"的承载面。"""
    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    margin, top, bottom = 110, 130, SIZE - 210
    d.rounded_rectangle([margin, top, SIZE - margin, bottom], radius=54,
                        fill=(255, 255, 255, 236))
    img.paste(Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB"), (0, 0))

    # 卡片上一道品类色装饰条，让不同品类的卡片易于区分
    d2 = ImageDraw.Draw(img)
    d2.rounded_rectangle([margin + 40, top + 40, margin + 148, top + 52],
                         radius=6, fill=accent)
    return img


def draw_device(img, key, accent):
    """在卡片内画一个极简的商品剪影。不追求写实，只求形态可辨、留白干净。"""
    d = ImageDraw.Draw(img, "RGBA")
    cx, cy = SIZE // 2, 400
    soft = tuple(list(accent) + [46])
    if key == "phone":
        d.rounded_rectangle([cx - 118, cy - 196, cx + 118, cy + 196], radius=32, fill=soft)
        d.rounded_rectangle([cx - 104, cy - 176, cx + 104, cy + 168], radius=24,
                            fill=tuple(list(accent) + [255]))
    elif key == "laptop":
        d.rounded_rectangle([cx - 210, cy - 150, cx + 210, cy + 96], radius=18, fill=soft)
        d.rounded_rectangle([cx - 196, cy - 136, cx + 196, cy + 74], radius=12,
                            fill=tuple(list(accent) + [255]))
        d.rounded_rectangle([cx - 250, cy + 96, cx + 250, cy + 122], radius=12, fill=soft)
    elif key == "vacuum":
        d.ellipse([cx - 168, cy - 122, cx + 168, cy + 122], fill=tuple(list(accent) + [255]))
        d.ellipse([cx - 118, cy - 74, cx + 118, cy + 74], fill=(255, 255, 255, 238))
        d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], fill=soft)
    elif key == "fryer":
        d.rounded_rectangle([cx - 150, cy - 110, cx + 150, cy + 150], radius=34, fill=soft)
        d.rounded_rectangle([cx - 132, cy - 26, cx + 132, cy + 132], radius=26,
                            fill=tuple(list(accent) + [255]))
        d.rounded_rectangle([cx - 62, cy - 92, cx + 62, cy - 40], radius=16, fill=soft)
    elif key == "tumbler":
        d.rounded_rectangle([cx - 82, cy - 170, cx + 82, cy + 170], radius=40, fill=soft)
        d.rounded_rectangle([cx - 68, cy - 154, cx + 68, cy + 150], radius=32,
                            fill=tuple(list(accent) + [255]))
        d.rounded_rectangle([cx - 92, cy - 196, cx + 92, cy - 158], radius=16, fill=soft)
    elif key == "bedding":
        d.rounded_rectangle([cx - 210, cy - 130, cx + 210, cy + 140], radius=26, fill=soft)
        d.rounded_rectangle([cx - 196, cy - 116, cx + 196, cy + 124], radius=20,
                            fill=tuple(list(accent) + [255]))
        d.line([(cx - 196, cy + 10), (cx + 196, cy + 10)], fill=soft, width=6)
    elif key == "coffee":
        d.rounded_rectangle([cx - 130, cy - 150, cx + 130, cy + 150], radius=22, fill=soft)
        for i in range(3):
            y = cy - 90 + i * 66
            d.rounded_rectangle([cx - 112, y, cx + 112, y + 46], radius=10,
                                fill=tuple(list(accent) + [255]))
    elif key == "bag":
        d.rounded_rectangle([cx - 158, cy - 110, cx + 158, cy + 168], radius=40, fill=soft)
        d.rounded_rectangle([cx - 142, cy - 96, cx + 142, cy + 154], radius=32,
                            fill=tuple(list(accent) + [255]))
        d.arc([cx - 76, cy - 172, cx + 76, cy - 60], start=180, end=360,
              fill=soft, width=16)
    return img


def render(key, name, subtitle, spec, label, tune, out_path):
    bg, accent = PALETTE[key]
    bg = tune_color(bg, tune)
    accent = tune_color(accent, tune)

    img = make_gradient(bg)
    img = rounded_card(img, accent)
    img = draw_device(img, key, accent)

    d = ImageDraw.Draw(img)
    f_name = load_font(46, bold=True)
    f_sub = load_font(24)

    # 商品名（居中，超长自动缩号）
    text = name
    while d.textlength(text, font=f_name) > SIZE - 260 and f_name.size > 26:
        f_name = load_font(f_name.size - 2, bold=True)
    d.text((SIZE // 2, 726), text, font=f_name, fill=(32, 38, 48), anchor="mm")

    d.text((SIZE // 2, 776), subtitle, font=f_sub, fill=(132, 142, 156), anchor="mm")

    # 规格行 + 变体标注
    label_line = f"{spec} · {label}"
    f_spec = load_font(26)
    while d.textlength(label_line, font=f_spec) > SIZE - 150 and f_spec.size > 16:
        f_spec = load_font(f_spec.size - 1)
    d.text((SIZE // 2, 830), label_line, font=f_spec, fill=(96, 106, 122), anchor="mm")

    img.save(out_path, "JPEG", quality=88, optimize=True)
    return out_path


def main():
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    written = []
    for key, name, subtitle, spec, variants in PRODUCTS:
        for idx, (label, tune) in enumerate(variants):
            fname = f"{key}-v{idx + 1}.jpg"
            render(key, name, subtitle, spec, label, tune, os.path.join(out_dir, fname))
            written.append((fname, name, label))
    # 主图 = 第一个变体
    for key, name, subtitle, spec, variants in PRODUCTS:
        src = os.path.join(out_dir, f"{key}-v1.jpg")
        dst = os.path.join(out_dir, f"{key}-main.jpg")
        if os.path.exists(src):
            Image.open(src).save(dst, "JPEG", quality=88, optimize=True)
            written.append((f"{key}-main.jpg", name, "主图"))
    print(f"generated {len(written)} images to {out_dir}")
    for f, n, v in written:
        print(f"  {f:<22} [{v}]")


if __name__ == "__main__":
    main()
