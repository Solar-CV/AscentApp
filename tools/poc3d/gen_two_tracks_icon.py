#!/usr/bin/env python3
"""给还没有 3D 模型的成就画一张中性占位图标。

目前只有 `two_tracks`（双线并进）用它。原来这类成就共用 `badge_placeholder_agi.jpg`，
那是一张**真人照片**，当作成就图标既不对题也不该随包分发，所以换掉。

风格对齐现有徽章：深蓝珐琅底 + 黄铜外框 + 陶土色路线。双线并进就画两条从下方
汇到峰顶的路线。
"""
import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path("app/src/main/res/drawable-nodpi/badge_two_tracks.webp")
S = 512  # 先放大画，再降采样，边缘更干净
R = 64   # 圆角半径（相对 S）

ENAMEL = (22, 35, 58, 255)
ENAMEL_HI = (30, 47, 76, 255)
BRASS = (196, 156, 74, 255)
BRASS_DARK = (146, 111, 44, 255)
ROUTE = (219, 189, 143, 255)
ROUTE2 = (150, 187, 214, 255)
PEAK = (241, 228, 200, 255)


def rounded_mask(size: int, radius: int) -> Image.Image:
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return m


def dashed(draw: ImageDraw.ImageDraw, pts, color, width, dash=26, gap=16):
    """沿折线画虚线（手动切段，PIL 没有 dash 支持）。"""
    carry = 0.0
    on = True
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        seg = math.hypot(x1 - x0, y1 - y0)
        t = 0.0
        while t < seg:
            step = (dash if on else gap) - carry
            end = min(seg, t + step)
            if on:
                draw.line(
                    [x0 + (x1 - x0) * t / seg, y0 + (y1 - y0) * t / seg,
                     x0 + (x1 - x0) * end / seg, y0 + (y1 - y0) * end / seg],
                    fill=color, width=width,
                )
            carry += end - t
            if carry >= (dash if on else gap) - 1e-6:
                carry = 0.0
                on = not on
            t = end


def build() -> Image.Image:
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # 珐琅底：竖向渐变，避免死板的纯色块
    plate = Image.new("RGBA", (S, S), ENAMEL)
    gp = ImageDraw.Draw(plate)
    for y in range(S):
        t = y / S
        gp.line([(0, y), (S, y)], fill=tuple(
            int(ENAMEL_HI[i] + (ENAMEL[i] - ENAMEL_HI[i]) * t) for i in range(3)
        ) + (255,))
    img.paste(plate, (0, 0), rounded_mask(S, R))

    # 黄铜外框：先画粗环再挖空内圈
    d.rounded_rectangle([0, 0, S - 1, S - 1], radius=R, outline=BRASS_DARK, width=22)
    d.rounded_rectangle([0, 0, S - 1, S - 1], radius=R, outline=BRASS, width=14)
    d.rounded_rectangle([22, 22, S - 23, S - 23], radius=R - 16, outline=BRASS_DARK, width=3)

    # 两条路线：从底部两角向上收拢到峰顶
    left = [(118, 424), (168, 356), (196, 300), (232, 250), (256, 214)]
    right = [(394, 424), (344, 356), (316, 300), (278, 250), (256, 214)]
    dashed(d, left, ROUTE, 9)
    dashed(d, right, ROUTE2, 9)

    # 峰顶点 + 一点光晕
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([256 - 46, 214 - 46, 256 + 46, 214 + 46], fill=PEAK[:3] + (70,))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(16)))
    d = ImageDraw.Draw(img)
    d.ellipse([256 - 15, 214 - 15, 256 + 15, 214 + 15], fill=PEAK)
    d.ellipse([256 - 7, 214 - 7, 256 + 7, 214 + 7], fill=BRASS_DARK)

    # 只留圆角内的像素
    img.putalpha(Image.composite(img.getchannel("A"), Image.new("L", (S, S), 0),
                                rounded_mask(S, R)))
    return img.resize((256, 256), Image.LANCZOS)


def main():
    icon = build()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(OUT, "WEBP", quality=92, method=6)
    print(OUT, OUT.stat().st_size // 1024, "KB")

    # 真人照片占位图不再需要（只有 two_tracks 引用过）
    old = Path("app/src/main/res/drawable-nodpi/badge_placeholder_agi.jpg")
    if old.exists():
        old.unlink()
        print("removed", old)


if __name__ == "__main__":
    main()
