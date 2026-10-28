#!/usr/bin/env python3
"""从模型库同步徽章：glb 进 assets，正面渲染图做成透明底成就图标。

模型库约定（见 ~/DSH-workzone/Model/README.md）：
  <badge_id>_vN/
    *.glb                 # 已按 achievement 命名过的直接对应
    renders/top.png       # 正面视角，纯黑背景、无投影

徽章目录名去掉 _vN 后再做少量对齐（first_step→first_steps 等），
图标用四邻域 flood fill 只吃掉与画布边缘连通的黑色，避免挖空徽章内部的深色区域。
"""
import hashlib
import json
import os
import shutil
import struct
import sys
import tempfile
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from bake_emissive import bake  # noqa: E402  （自发光烘焙并入同步，两个脚本不会走散）

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

    第二轮处理「被边框围住的背景」：卡扣（safety_first）的中心孔、冲孔卡片
    （runs_20）的通孔，与画布外背景同色但被包住，第一轮够不到。判据是
    **整块均匀**（标准差 ≤ 2、与角点背景均值差 ≤ 3）——设计里的深色区域
    总有渐变或噪点，标准差不达标，不会被误挖。
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

    bg = arr[2, 2].astype(np.int16)
    near = np.abs(arr.astype(np.int16) - bg).max(axis=2) <= 6
    visited = np.zeros((sh, sw), dtype=bool)
    for y0 in range(sh):
        for x0 in range(sw):
            if seen[y0, x0] or visited[y0, x0] or not near[y0, x0]:
                continue
            comp = []
            stack = [(x0, y0)]
            visited[y0, x0] = True
            while stack:
                x, y = stack.pop()
                comp.append((x, y))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < sw and 0 <= ny < sh and not visited[ny, nx] and near[ny, nx]:
                        visited[ny, nx] = True
                        stack.append((nx, ny))
            # 太小的多半是描线之间的缝，不动它
            if len(comp) < 60:
                continue
            pix = np.array([arr[y, x] for x, y in comp], dtype=np.int16)
            if pix.std(axis=0).max() <= 2.0 and np.abs(pix.mean(axis=0) - bg).max() <= 3.0:
                for x, y in comp:
                    seen[y, x] = True

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


def collect() -> list[tuple[str, Path]]:
    """扫模型库目录，返回 (成就 id, 模型目录)。

    不依赖 INDEX.json 的完整性——2026-10-28 那批里 `safety_first_v1` 就漏登记了，
    按目录扫才不会静默少一枚。同一成就若有多份（不同 _vN 同时存在），取最新的一份。
    """
    by_id: dict[str, Path] = {}
    for folder in sorted(MODEL_DIR.iterdir()):
        if not folder.is_dir():
            continue
        glbs = list(folder.glob("*.glb"))
        if not glbs:
            continue
        aid = achievement_id(folder.name)
        prev = by_id.get(aid)
        if prev is None or max(f.stat().st_mtime for f in folder.glob("*.glb")) > \
                max(f.stat().st_mtime for f in prev.glob("*.glb")):
            by_id[aid] = folder
    return sorted(by_id.items())


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    ICONS.mkdir(parents=True, exist_ok=True)

    rows = []
    for aid, folder in collect():
        glb = next(folder.glob("*.glb"))
        target = ASSETS / f"{aid}.glb"
        state = "新增" if not target.exists() else "更新"
        # 先在临时文件里烘焙，再与现有资产比内容：
        # mtime 不可靠（git checkout 会把时间刷成现在），而直接比源文件又必然不等
        # （资产是烘过自发光的），只有「烘完再比」才能既不漏更新、又不留假改动。
        with tempfile.NamedTemporaryFile(suffix=".glb", delete=False) as tmp:
            tmp_path = Path(tmp.name)
        try:
            shutil.copyfile(glb, tmp_path)
            bake(tmp_path)
            same = target.exists() and (
                hashlib.md5(target.read_bytes()).digest()
                == hashlib.md5(tmp_path.read_bytes()).digest()
            )
            if not same:
                shutil.copyfile(tmp_path, target)
                state = "新增" if state == "新增" else "更新"
            else:
                state = "不变"
        finally:
            tmp_path.unlink(missing_ok=True)

        top = folder / "renders" / "top.png"
        icon_kb = 0
        dst = ICONS / f"badge_{aid}.webp"
        stale = not dst.exists() or top.stat().st_mtime > dst.stat().st_mtime
        if not top.exists():
            print(f"  !! {aid} 缺少 renders/top.png，图标未生成")
        elif stale or "--force" in sys.argv:
            knockout(top, dst)
            icon_kb = dst.stat().st_size // 1024
        else:
            icon_kb = -1  # 未重生成，沿用既有图标
        rows.append((aid, glb.stat().st_size / 1048576, icon_kb, state))

    total = sum(r[1] for r in rows)
    print(f"同步 {len(rows)} 枚，glb 合计 {total:.1f} MB")
    for aid, mb, kb, state in rows:
        icon = "沿用" if kb == -1 else f"{kb:3d} KB"
        print(f"  {aid:16s} {state:4s} {mb:5.2f} MB  icon {icon}")


if __name__ == "__main__":
    main()
