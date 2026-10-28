#!/usr/bin/env python3
"""生成 3D 徽章展示页的背景（重模糊，只剩色块与光感）。

两个变体：
  默认      亮色，清晨山脊      → res/drawable-nodpi/badge_bg.jpg
  --dark    暗色，合成波落日    → res/drawable-nodpi/badge_bg_dark.jpg

不用照片：模糊到这个程度后，程序化的天空/山脊/雾带与实拍几乎无差别，
而且色调可控、体积小、不带版权问题。

输出：1080x1920 JPEG，各约 30–300 KB。

用法：
  python3 tools/poc3d/gen_badge_bg.py            # 亮色
  python3 tools/poc3d/gen_badge_bg.py --dark     # 暗色
"""
import math
import random
import sys
from PIL import Image, ImageDraw, ImageFilter

W, H = 1080, 1920
random.seed(20261023)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def build_sky():
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    # 天顶偏冷蓝 → 地平线暖米白（清晨）
    top = (138, 178, 210)
    hor = (248, 236, 212)
    # 单段余弦插值：避免分段线性的导数断点（模糊后会留下横向接缝）
    for y in range(H):
        t = min(1.0, y / (H * 0.60))
        e = 0.5 - 0.5 * math.cos(math.pi * (t ** 0.92))
        c = lerp(top, hor, e)
        d.line([(0, y), (W, y)], fill=tuple(int(v) for v in c))
    return img


def add_sun(img):
    glow = Image.new("L", (W, H), 0)
    gd = ImageDraw.Draw(glow)
    cx, cy, r = int(W * 0.26), int(H * 0.20), 260
    for i in range(18, 0, -1):
        rr = int(r * i / 12)
        v = int(26 * (1 - i / 20))
        gd.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=v)
    img.paste(Image.new("RGB", (W, H), (255, 250, 232)), (0, 0), glow)
    return img


def ridge(img, base_y, amp, color, seed, roughness=5):
    """一层山脊：随机游走 + 低频起伏，画到 base_y 以下。"""
    rnd = random.Random(seed)
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    pts = []
    phase = rnd.random() * 10
    for x in range(0, W + 1, 6):
        t = x / W
        h = (math.sin(t * math.pi * 1.7 + phase) * 0.5 + 0.5) * amp
        h += (math.sin(t * math.pi * 4.3 + phase * 2.1) * 0.5 + 0.5) * amp * 0.55
        h += (rnd.random() - 0.5) * roughness
        pts.append((x, base_y - h))
    pts += [(W, H), (0, H)]
    d.polygon(pts, fill=color)
    img.alpha_composite(layer)
    return img


def build():
    img = build_sky().convert("RGBA")
    img = add_sun(img)

    # 远 → 近：饱和度递增、明度递减（大气透视）
    img = ridge(img, int(H * 0.60), 150, (188, 204, 212, 255), 11, 3)
    img = ridge(img, int(H * 0.66), 190, (162, 186, 180, 255), 22, 4)
    img = ridge(img, int(H * 0.73), 230, (126, 158, 138, 255), 33, 5)
    # 雾带：压在前两层之间，制造纵深
    fog = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fog)
    for i in range(120):
        y = int(H * 0.655) + i
        a = int(70 * (1 - abs(i - 60) / 60))
        fd.line([(0, y), (W, y)], fill=(240, 244, 240, max(0, a)))
    img.alpha_composite(fog.filter(ImageFilter.GaussianBlur(22)))
    img = ridge(img, int(H * 0.82), 260, (96, 124, 112, 255), 44, 6)
    img = ridge(img, int(H * 0.93), 240, (72, 96, 90, 255), 55, 5)

    rgb = img.convert("RGB")
    # 重模糊：山形完全糊掉，只留色块与光晕
    rgb = rgb.filter(ImageFilter.GaussianBlur(54))
    # 再叠一层更淡的雾，让对比度整体偏低（文字与徽章压得住）
    veil = Image.new("RGB", (W, H), (250, 250, 246))
    rgb = Image.blend(rgb, veil, 0.12)

    # 轻微暗角，避免四角过亮
    vig = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vig)
    vd.ellipse([-W * 0.25, -H * 0.12, W * 1.25, H * 1.12], fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(180))
    dark = Image.new("RGB", (W, H), (196, 202, 198))
    rgb = Image.composite(rgb, Image.blend(rgb, dark, 0.35), vig)
    return rgb


# ---------------------------------------------------------------- 暗色版
# 合成波（synthwave）落日：靛紫天顶 → 品红中层 → 橙红地平线，
# 一轮被切开的落日，下面压两层近黑山脊。全程重度模糊，
# 最后只剩一片暖紫的色场与光晕，正好衬金属徽章。


def _grad(img, stops):
    """stops: [(t, (r,g,b)), ...]，t ∈ [0,1]，按余弦缓动在相邻色之间插值。"""
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / (H - 1)
        for i in range(len(stops) - 1):
            t0, c0 = stops[i]
            t1, c1 = stops[i + 1]
            if t0 <= t <= t1:
                k = (t - t0) / max(1e-6, t1 - t0)
                k = 0.5 - 0.5 * math.cos(math.pi * k)
                c = lerp(c0, c1, k)
                d.line([(0, y), (W, y)], fill=tuple(int(v) for v in c))
                break
    return img


def _sun_disc(img, cx, cy, r, core, glow_col, glow=1.1):
    """落日：本体 + 多层外发光。径向叠加，避免出现硬边圆盘。"""
    # 外发光
    halo = Image.new("L", (W, H), 0)
    hd = ImageDraw.Draw(halo)
    R = int(r * (1.0 + glow) * 1.9)
    for i in range(26, 0, -1):
        rr = int(R * i / 26)
        v = int(26 * (1 - i / 28) ** 3.0)
        hd.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=v)
    img.paste(Image.new("RGB", (W, H), glow_col), (0, 0), halo.filter(ImageFilter.GaussianBlur(30)))

    # 本体：中心亮、边缘略暗，模拟低空的太阳
    disc = Image.new("L", (W, H), 0)
    dd = ImageDraw.Draw(disc)
    dd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    disc = disc.filter(ImageFilter.GaussianBlur(r * 0.09))
    img.paste(Image.new("RGB", (W, H), core), (0, 0), disc)
    return img


def _sun_cuts(img, cx, cy, r):
    """合成波标志性的横向切线：在日轮下缘切出几道深色条。"""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    y = cy + r * 0.06
    gap = r * 0.055
    hgt = r * 0.030
    while y < cy + r * 0.96:
        d.rectangle([cx - r * 1.05, y, cx + r * 1.05, y + hgt], fill=(38, 12, 44, 235))
        y += gap + hgt
        gap *= 1.16
        hgt *= 1.10
    return Image.alpha_composite(img, layer)


def build_dark():
    # 天顶深靛 → 品红 → 地平线橙红；地平线以下直接压暗
    img = _grad(
        Image.new("RGB", (W, H)),
        [
            (0.00, (14, 8, 32)),
            (0.18, (31, 14, 58)),
            (0.38, (74, 22, 86)),
            (0.52, (146, 40, 96)),
            (0.62, (226, 82, 74)),
            (0.66, (255, 146, 84)),
            (0.70, (150, 58, 96)),
            (0.82, (46, 18, 54)),
            (1.00, (12, 7, 22)),
        ],
    ).convert("RGBA")

    # 落日压在 0.60 高度，略偏左，让右侧留出徽章主体
    sun_x, sun_y, sun_r = int(W * 0.37), int(H * 0.605), int(W * 0.185)
    img = _sun_disc(img, sun_x, sun_y, sun_r,
                    core=(255, 198, 118), glow_col=(255, 122, 74), glow=1.40)
    # 合成波的横向切线在 40px 模糊下只会糊成一团灰，这里不加；
    # 想要"切日"效果得把模糊降到 12px 以下，那种清晰度会抢徽章。

    # 地平线光带：一条更亮的暖橙，给整幅图一个"落日"的定位
    band = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    bd = ImageDraw.Draw(band)
    for i in range(200):
        y = int(H * 0.640) + i - 100
        a = int(150 * (1 - abs(i - 100) / 100) ** 1.5)
        bd.line([(0, y), (W, y)], fill=(255, 176, 108, max(0, a)))
    img.alpha_composite(band.filter(ImageFilter.GaussianBlur(46)))

    # 山脊：越近越暗，最后几层几乎融进底色
    img = ridge(img, int(H * 0.735), 190, (58, 20, 56, 255), 71, 5)
    img = ridge(img, int(H * 0.815), 240, (34, 12, 40, 255), 82, 6)
    img = ridge(img, int(H * 0.905), 220, (20, 8, 26, 255), 93, 5)

    rgb = img.convert("RGB")
    rgb = rgb.filter(ImageFilter.GaussianBlur(40))
    # 轻微压一层暗紫，整体明度落到亮色版的一半左右
    veil = Image.new("RGB", (W, H), (26, 14, 38))
    rgb = Image.blend(rgb, veil, 0.18)

    # 暗角：四角更沉，中间留一圈暖光
    vig = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vig)
    vd.ellipse([-W * 0.30, -H * 0.10, W * 1.30, H * 1.10], fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(200))
    deep = Image.new("RGB", (W, H), (8, 5, 16))
    rgb = Image.composite(rgb, Image.blend(rgb, deep, 0.62), vig)
    return rgb


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    dark = "--dark" in sys.argv
    out = args[0] if args else (
        "app/src/main/res/drawable-nodpi/badge_bg_dark.jpg" if dark
        else "app/src/main/res/drawable-nodpi/badge_bg.jpg"
    )
    im = build_dark() if dark else build()
    import os
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out, "JPEG", quality=84, optimize=True, progressive=True)
    print("wrote", out, os.path.getsize(out), "bytes")
