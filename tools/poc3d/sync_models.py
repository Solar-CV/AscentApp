#!/usr/bin/env python3
"""从模型库同步徽章：glb 进 assets，正面渲染图做成透明底成就图标。

模型库约定（见 ~/DSH-workzone/Model/README.md）：
  <badge_id>_vN/
    *.glb                 # 已按 achievement 命名过的直接对应
    renders/top.png       # 正面视角，纯黑背景、无投影

徽章目录名去掉 _vN 后再做少量对齐（first_step→first_steps 等），
图标用四邻域 flood fill 只吃掉与画布边缘连通的黑色，避免挖空徽章内部的深色区域。
"""
import json
import os
import shutil
import struct
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

MODEL_DIR = Path("/Users/lolo/DSH-workzone/Model")
ASSETS = Path("app/src/main/assets/badges")
ICONS = Path("app/src/main/res/drawable-nodpi")
ICON_SIZE = 256
BG_TOLERANCE = 26

# 目录名去掉 _vN 后仍与成就 id 不一致的
ALIAS = {
    "first_step": "first_steps",
    "crampon_graduation": "crampon_ready",
    "ascent_badge_self_arrest": "self_arrest",
}


def achievement_id(badge_id: str) -> str:
    base = badge_id
    for suffix in ("_v1", "_v2", "_v3", "_v4"):
        if base.endswith(suffix):
            base = base[: -len(suffix)]
            break
    return ALIAS.get(base, base)


def knockout(src: Path, dst: Path) -> tuple[int, int]:
    """把纯黑背景抠成透明，裁到内容框后居中放到方形画布上。

    做法：缩到 1/4 后从画布边缘做四邻域 flood fill，只吃掉与边缘连通的黑色，
    徽章内部的深色区域不会被打孔；再把蒙版放大回去，边缘羽化一点点。
    """
    im = Image.open(src).convert("RGB")
    w, h = im.size
    small = im.resize((w // 4, h // 4), Image.LANCZOS)
    arr = np.asarray(small, dtype=np.uint8)
    dark = arr.max(axis=2) <= BG_TOLERANCE
    sw, sh = small.size

    seen = np.zeros((sh, sw), dtype=bool)
    queue = deque()
    for x in range(sw):
        queue.append((x, 0)); queue.append((x, sh - 1))
    for y in range(sh):
        queue.append((0, y)); queue.append((sw - 1, y))
    while queue:
        x, y = queue.popleft()
        if seen[y, x] or not dark[y, x]:
            continue
        seen[y, x] = True
        if x + 1 < sw: queue.append((x + 1, y))
        if x: queue.append((x - 1, y))
        if y + 1 < sh: queue.append((x, y + 1))
        if y: queue.append((x, y - 1))

    mask = Image.fromarray(np.where(seen, 0, 255).astype(np.uint8), "L").resize((w, h), Image.LANCZOS)
    mask = mask.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(0.9))

    out = im.convert("RGBA")
    out.putalpha(mask)
    bbox = out.getbbox()
    if not bbox:
        raise SystemExit(f"空图：{src}")
    out = out.crop(bbox)

    size = ICON_SIZE
    content = int(size * 0.88)
    cw, ch = out.size
    scale = content / max(cw, ch)
    out = out.resize((max(1, round(cw * scale)), max(1, round(ch * scale))), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(out, ((size - out.width) // 2, (size - out.height) // 2))
    canvas.save(dst, "WEBP", quality=92, method=6)
    return out.size


def main():
    index = json.loads((MODEL_DIR / "INDEX.json").read_text())
    ASSETS.mkdir(parents=True, exist_ok=True)
    ICONS.mkdir(parents=True, exist_ok=True)

    rows = []
    for item in index["badges"]:
        badge_id = item["badge_id"]
        folder = MODEL_DIR / badge_id
        if not folder.is_dir():
            # self_arrest 的目录名与 badge_id 不同
            folder = next((p for p in MODEL_DIR.iterdir() if p.is_dir() and (p / item["glb"]["file"]).exists()), None)
        glb = folder / item["glb"]["file"]
        aid = achievement_id(badge_id)
        if not glb.exists():
            print("跳过（找不到 glb）:", badge_id)
            continue
        shutil.copyfile(glb, ASSETS / f"{aid}.glb")
        top = folder / "renders" / "top.png"
        icon_kb = 0
        if top.exists():
            dst = ICONS / f"badge_{aid}.webp"
            knockout(top, dst)
            icon_kb = dst.stat().st_size // 1024
        rows.append((aid, glb.stat().st_size / 1048576, icon_kb))

    total = sum(r[1] for r in rows)
    print(f"同步 {len(rows)} 枚，glb 合计 {total:.1f} MB")
    for aid, mb, kb in rows:
        print(f"  {aid:16s} {mb:5.2f} MB  icon {kb:3d} KB")


if __name__ == "__main__":
    main()
