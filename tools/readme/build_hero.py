#!/usr/bin/env python3
"""生成 README 顶部横幅（1600×620，2x 截图 → 3200×1240 PNG）。

两套构图，用 --mode 选：
  badge        右侧放「全弧」成就的珠峰地形块（3D 徽章渲染），产品照式留白
  typographic  不放图，纯排版 + 等高线底纹，居中

风格：Premium Alpine / Outdoor Technology —— 冷灰白底、留白、细线、等宽数字。

用法：python3 tools/readme/build_hero.py [--mode badge|typographic]
依赖：Chrome（headless 截图）
"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res/drawable-nodpi"
CONTOUR = RES / "mtn_haba.webp"
PLAQUE = RES / "badge_full_route.webp"
OUT = ROOT / "assets/readme"
CHROME = os.environ.get("CHROME", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")

W, H = 1600, 620
SCALE = 2

LIGHT = dict(
    bg_top="#FDFEFF", bg_bot="#EDF3FA",
    ink="#0D1622", sub="#46586B", mute="#8FA0B2", accent="#0E7BEA",
    rule="#D8E2ED", contour_op=0.085, contour_tint="multiply",
    plaque_filter="contrast(1.03)",
)
DARK = dict(
    bg_top="#0A0F16", bg_bot="#131C27",
    ink="#F3F7FB", sub="#A9B9C9", mute="#6E8093", accent="#4AA6FF",
    rule="#25313F", contour_op=0.16, contour_tint="screen",
    plaque_filter="none",
)

STATS = [("91", "LESSONS"), ("1,563", "QUESTIONS"), ("88", "PEAKS"), ("40", "BADGES")]

FONT = '"Helvetica Neue",Helvetica,Arial,sans-serif'


def page_badge(t: dict, contour: Path, plaque: Path) -> str:
    stats = "".join(
        f'<div class="stat" style="left:{112 + i * 158}px;top:414px">'
        f'<div class="num" style="color:{t["ink"]}">{n}</div>'
        f'<div class="lab" style="color:{t["mute"]}">{lab}</div></div>'
        for i, (n, lab) in enumerate(STATS)
    )
    return f"""<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>
  html,body{{margin:0;padding:0;width:{W}px;height:{H}px;overflow:hidden;}}
  .wrap{{position:relative;width:{W}px;height:{H}px;
        background:linear-gradient(180deg,{t["bg_top"]} 0%,{t["bg_bot"]} 100%);}}
  .contour{{position:absolute;left:-120px;top:-260px;width:1900px;
           opacity:{t["contour_op"]};mix-blend-mode:{t["contour_tint"]};
           -webkit-mask-image:radial-gradient(115% 105% at 46% 42%,#000 52%,transparent 100%);}}
  .plaque{{position:absolute;left:1068px;top:112px;width:436px;filter:{t["plaque_filter"]};}}
  .txt{{position:absolute;left:112px;font-family:{FONT};}}
  .kicker{{top:150px;font-size:17px;letter-spacing:6.2px;font-weight:500;color:{t["mute"]};}}
  .mark{{top:198px;font-size:94px;line-height:1;letter-spacing:7px;font-weight:700;color:{t["ink"]};}}
  .tag{{top:316px;font-size:29px;letter-spacing:0.4px;font-weight:400;color:{t["sub"]};}}
  .rule{{position:absolute;left:112px;top:386px;width:286px;height:1px;background:{t["rule"]};}}
  .stat{{position:absolute;height:70px;}}
  .num{{font-size:40px;line-height:46px;letter-spacing:0.5px;font-weight:700;
       font-variant-numeric:tabular-nums;}}
  .lab{{font-size:14px;letter-spacing:3.2px;font-weight:500;margin-top:6px;}}
  .foot{{position:absolute;left:112px;top:552px;font-size:13px;letter-spacing:1.9px;
        font-weight:400;color:{t["mute"]};font-family:{FONT};}}
  .ic{{position:absolute;left:112px;top:96px;}}
</style></head><body><div class="wrap">
  <img class="contour" src="{contour.as_uri()}">
  <img class="plaque" src="{plaque.as_uri()}">
  <svg class="ic" width="34" height="34" viewBox="0 0 32 32" fill="none">
    <path d="M6 23l7-11 5 7 3-4 5 8z" fill="{t["accent"]}"/>
    <path d="M19 5v6" stroke="#F0B429" stroke-width="2.4" stroke-linecap="round"/>
    <path d="M20.4 3.4L27 5.6 20.4 8z" fill="#F0B429"/>
  </svg>
  <div class="txt kicker">HIGH-ALTITUDE TRAINING · ANDROID</div>
  <div class="txt mark">ASCENTROAM</div>
  <div class="txt tag">From your first crampon to 8,000 metres.</div>
  <div class="rule"></div>
  {stats}
  <div class="foot">FULL ROUTE · MOUNT EVEREST, 8,848.86 M · 3D BADGE, 40 IN ALL</div>
</div></body></html>
"""


def page_typo(t: dict, contour: Path) -> str:
    stats = "".join(
        f'<div class="stat"><div class="num" style="color:{t["ink"]}">{n}</div>'
        f'<div class="lab" style="color:{t["mute"]}">{lab}</div></div>'
        for n, lab in STATS
    )
    return f"""<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>
  html,body{{margin:0;padding:0;width:{W}px;height:{H}px;overflow:hidden;}}
  .wrap{{position:relative;width:{W}px;height:{H}px;
        background:linear-gradient(180deg,{t["bg_top"]} 0%,{t["bg_bot"]} 100%);}}
  .contour{{position:absolute;left:-260px;top:-300px;width:2100px;
           opacity:{t["contour_op"] * 1.15:.3f};mix-blend-mode:{t["contour_tint"]};}}
  .c{{position:absolute;left:0;width:{W}px;text-align:center;font-family:{FONT};}}
  .kicker{{top:158px;font-size:16px;letter-spacing:7px;font-weight:500;color:{t["mute"]};}}
  .mark{{top:206px;font-size:92px;line-height:1;letter-spacing:8px;font-weight:700;color:{t["ink"]};}}
  .tag{{top:326px;font-size:28px;letter-spacing:0.6px;font-weight:400;color:{t["sub"]};}}
  .stats{{position:absolute;left:0;top:424px;width:{W}px;text-align:center;font-family:{FONT};
         font-size:0;}}
  .stat{{display:inline-block;width:190px;vertical-align:top;}}
  .num{{font-size:38px;line-height:44px;letter-spacing:0.5px;font-weight:700;
       font-variant-numeric:tabular-nums;}}
  .lab{{font-size:13px;letter-spacing:3.2px;font-weight:500;margin-top:6px;}}
</style></head><body><div class="wrap">
  <img class="contour" src="{contour.as_uri()}">
  <div class="c kicker">HIGH-ALTITUDE TRAINING · ANDROID</div>
  <div class="c mark">ASCENTROAM</div>
  <div class="c tag">From your first crampon to 8,000 metres.</div>
  <div class="stats">{stats}</div>
</div></body></html>
"""


def shoot(html: Path, out: Path) -> None:
    profile = html.parent / ("chrome-" + out.stem)
    subprocess.run(
        [CHROME, "--headless=new", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
         "--allow-file-access-from-files", "--virtual-time-budget=1500",
         f"--force-device-scale-factor={SCALE}", f"--user-data-dir={profile}",
         f"--window-size={W},{H}", f"--screenshot={out}", html.as_uri()],
        check=True, capture_output=True,
    )


def main() -> None:
    mode = "badge"
    if "--mode" in sys.argv:
        mode = sys.argv[sys.argv.index("--mode") + 1]
    if mode not in ("badge", "typographic"):
        raise SystemExit("mode must be badge or typographic")

    OUT.mkdir(parents=True, exist_ok=True)
    tmp = Path(tempfile.mkdtemp(prefix="ascent-hero-"))
    try:
        for name, theme in (("hero", LIGHT), ("hero-dark", DARK)):
            html = tmp / f"{name}.html"
            html.write_text(
                page_badge(theme, CONTOUR, PLAQUE) if mode == "badge"
                else page_typo(theme, CONTOUR),
                encoding="utf-8")
            shot = tmp / f"{name}.png"
            shoot(html, shot)
            final = OUT / f"{name}.png"
            shutil.copyfile(shot, final)
            print(f"wrote {final.relative_to(ROOT)} ({final.stat().st_size / 1024:.0f} KB)")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
