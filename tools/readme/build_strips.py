#!/usr/bin/env python3
"""生成 README 里的两条真实素材带：山峰地形图、3D 徽章。

peaks.png   1600×430   四座山的地形图 + 名称 / 海拔 / 画法（relief 或 contour）
badges.png  1600×280   五枚 3D 徽章，横向排列

素材直接取自 App 自己的 drawable-nodpi / assets，不另画。
用法：python3 tools/readme/build_strips.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res/drawable-nodpi"
OUT = ROOT / "assets/readme"

INK = (13, 22, 34)
MUTE = (122, 138, 155)
LINE = (232, 238, 244)
BG = (251, 252, 254)

PEAKS = [
    ("k2", "K2", "8,611 M", "RELIEF"),
    ("matterhorn", "Matterhorn", "4,478 M", "RELIEF"),
    ("denali", "Denali", "6,190 M", "CONTOUR"),
    ("kilimanjaro", "Kilimanjaro", "5,895 M", "CONTOUR"),
]

BADGES = ["basecamp_grad", "altitude_6000", "avalanche_eye", "crampon_ready", "days_21"]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype("/System/Library/Fonts/HelveticaNeue.ttc", size,
                              index=1 if bold else 0)


def tracked(draw: ImageDraw.ImageDraw, xy, text, fnt, fill, tracking=0.0, anchor_right=None):
    """逐字绘制以支持字距（PIL 没有 letter-spacing）。"""
    widths = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)
    x = (anchor_right - total) if anchor_right is not None else xy[0]
    for ch, w in zip(text, widths):
        draw.text((x, xy[1]), ch, font=fnt, fill=fill)
        x += w + tracking
    return total


def peaks_strip(dst: Path) -> None:
    W, H = 1600, 430
    cell = W // len(PEAKS)
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)

    for i, (key, name, alt, kind) in enumerate(PEAKS):
        x0 = i * cell
        if i:
            d.line([(x0, 40), (x0, H - 40)], fill=LINE, width=1)

        m = Image.open(RES / f"mtn_{key}.webp").convert("RGBA")
        m = m.crop(m.getbbox())
        box_w, box_h = cell - 96, 240
        m.thumbnail((box_w, box_h), Image.LANCZOS)
        img.paste(m, (x0 + (cell - m.width) // 2, 40 + (box_h - m.height) // 2), m)

        tracked(d, (x0 + 48, 322), name, font(26, True), INK, tracking=0.2)
        tracked(d, (x0 + 48, 360), alt, font(15), MUTE, tracking=2.2)
        tracked(d, (0, 362), kind, font(12), (150, 168, 186), tracking=2.4,
                anchor_right=x0 + cell - 48)

    img.save(dst, optimize=True)


def badges_strip(dst: Path) -> None:
    W, H = 1000, 230
    size = 170
    gap = (W - size * len(BADGES)) // (len(BADGES) + 1)
    img = Image.new("RGBA", (W, H), BG + (255,))

    for i, key in enumerate(BADGES):
        b = Image.open(RES / f"badge_{key}.webp").convert("RGBA")
        b = b.resize((size, size), Image.LANCZOS)
        x = gap + i * (size + gap)
        img.alpha_composite(b, (x, (H - size) // 2))

    img.save(dst, optimize=True)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, fn in (("peaks.png", peaks_strip), ("badges.png", badges_strip)):
        p = OUT / name
        fn(p)
        print(f"wrote {p.relative_to(ROOT)} ({p.stat().st_size / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
