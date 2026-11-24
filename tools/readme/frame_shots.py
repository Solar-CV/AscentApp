#!/usr/bin/env python3
"""给模拟器截图套圆角、描边与投影，输出 README 用的展示图。

输入：原始 1080×1920 截图（adb screencap 直接拿到的）
输出：assets/readme/screen-*.png（1128×1968，透明底，可直接放 README）

用法：python3 tools/readme/frame_shots.py <原始目录>
原始目录下需有 path.png / quiz.png / handbook.png / training.png
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "assets/readme"

PAD = 24          # 画布四周留白，给投影用
RADIUS = 46       # 屏幕圆角（原图尺度）
NAMES = ["path", "quiz", "handbook", "training"]


def frame(src: Path, dst: Path) -> None:
    shot = Image.open(src).convert("RGB")
    w, h = shot.size

    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], RADIUS, fill=255)

    canvas = Image.new("RGBA", (w + PAD * 2, h + PAD * 2), (0, 0, 0, 0))

    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [PAD, PAD + 14, PAD + w, PAD + h + 14], RADIUS, fill=(23, 38, 58, 66)
    )
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(20)))

    contact = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(contact).rounded_rectangle(
        [PAD, PAD + 2, PAD + w, PAD + h + 2], RADIUS, fill=(23, 38, 58, 20)
    )
    canvas.alpha_composite(contact.filter(ImageFilter.GaussianBlur(3)))

    body = shot.convert("RGBA")
    body.putalpha(mask)
    canvas.alpha_composite(body, (PAD, PAD))

    ImageDraw.Draw(canvas).rounded_rectangle(
        [PAD, PAD, PAD + w - 1, PAD + h - 1], RADIUS, outline=(206, 217, 229, 190), width=2
    )

    canvas.save(dst, optimize=True)


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("usage: frame_shots.py <raw-dir>")
    src_dir = Path(sys.argv[1])
    OUT.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        src = src_dir / f"{name}.png"
        if not src.is_file():
            raise SystemExit(f"missing {src}")
        dst = OUT / f"screen-{name}.png"
        frame(src, dst)
        print(f"wrote {dst.relative_to(ROOT)} ({dst.stat().st_size / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
