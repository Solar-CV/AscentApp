#!/usr/bin/env python3
"""生成 3D 徽章展示页的亮色风景背景（重模糊，只剩色块与光感）。

不用照片：模糊到这个程度后，程序化的天空/山脊/雾带与实拍几乎无差别，
而且色调可控、体积小、不带版权问题。

输出：1080x1920 JPEG，约 150–300 KB。
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


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/bg/badge_bg.jpg"
    im = build()
    import os
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out, "JPEG", quality=84, optimize=True, progressive=True)
    print("wrote", out, os.path.getsize(out), "bytes")
