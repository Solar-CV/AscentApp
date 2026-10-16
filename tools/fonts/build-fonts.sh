#!/usr/bin/env bash
#
# ASCENT 字体内嵌构建脚本（子集化）
# ---------------------------------------------------------------
# 产出：app/src/main/res/font/ 下的 4 个字体文件
#   ascent_sans_regular.ttf    Noto Sans SC 400   （≈ 思源黑体，覆盖中文 + 拉丁 + 数字）
#   ascent_sans_semibold.ttf   Noto Sans SC 600
#   ascent_mono_regular.ttf    JetBrains Mono 400 （海拔 / 坐标 / 编号 / meta）
#   ascent_mono_semibold.ttf   JetBrains Mono 600
#   ascent_display_bold.ttf    Nunito ExtraBold（数字/拉丁展示，多邻国同款）
#
# 依赖：python3、fontTools（pip3 install --user fonttools）、curl
# 用法：bash tools/fonts/build-fonts.sh
#
# 注意：字符集是从「App 源码 + bank/ + import/ + knowledge/」全量提取的
#       （CJK 约 3300 字，可覆盖在线题库后续新增内容）；
#       如果以后新内容引入了没出现过的新字，需要重跑本脚本。
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
DEST="$ROOT/app/src/main/res/font"
mkdir -p "$WORK/fonts" "$WORK/out" "$DEST"

UA="Mozilla/5.0 (X11; Linux x86_64)"

# ---------- 1. 下载静态 TTF 实例（Google Fonts；未识别的 UA 会返回 truetype） ----------
fetch() { # family  weights  outprefix
  local css url i=0
  css=$(curl -sS -m 30 "https://fonts.googleapis.com/css2?family=$1:wght@$2&display=swap" -H "User-Agent: $UA")
  for url in $(printf '%s' "$css" | grep -o 'https://fonts.gstatic.com/[^)]*'); do
    i=$((i+1))
    curl -sS -m 180 -o "$WORK/fonts/$3-$i.ttf" "$url"
  done
}
echo "· 下载 Noto Sans SC 400/600 …"
fetch "Noto+Sans+SC" "400;600" "NotoSansSC"
echo "· 下载 JetBrains Mono 400/600 …"
fetch "JetBrains+Mono" "400;600" "JetBrainsMono"
echo "· 下载 Nunito 800 …"
fetch "Nunito" "800" "Nunito"

# ---------- 2. 从语料提取字符集 ----------
echo "· 提取字符集 …"
python3 - "$ROOT" "$WORK" <<'PY'
import glob, os, sys
root, work = sys.argv[1], sys.argv[2]
chars = set()
for r in ["app/src/main/java", "bank", "import", "knowledge"]:
    for p in glob.glob(os.path.join(root, r, "**", "*"), recursive=True):
        if os.path.isfile(p) and p.lower().endswith((".kt", ".json", ".md", ".mjs", ".txt", ".tsv", ".csv")):
            try:
                chars |= set(open(p, encoding="utf-8", errors="ignore").read())
            except Exception:
                pass
for c in range(0x20, 0x7F):
    chars.add(chr(c))
chars |= set("、。，；：！？（）【】《》「」『』“”‘’…—·×÷±≈≤≥°℃‰→←↑↓★☆✓✕•　／％")
chars = {c for c in chars if c.isprintable() and ord(c) >= 0x20 and c != "\u00a0"}
open(os.path.join(work, "charset.txt"), "w", encoding="utf-8").write("".join(sorted(chars)))
cjk = len([c for c in chars if "\u4e00" <= c <= "\u9fff"])
print(f"  字符总数 {len(chars)}（其中 CJK {cjk}）")
latin = {chr(c) for c in range(0x20, 0x7F)}
latin |= set("–—·×÷±≈≤≥°%‰→←↑↓★☆✓✕•，。、：；！？（）")
latin = {c for c in latin if c.isprintable() and ord(c) >= 0x20}
open(os.path.join(work, "latin.txt"), "w", encoding="utf-8").write("".join(sorted(latin)))
print(f"  拉丁子集 {len(latin)}")
PY

# ---------- 3. 子集化 ----------
echo "· 子集化 …"
for pair in "NotoSansSC-1:ascent_sans_regular" "NotoSansSC-2:ascent_sans_semibold" \
            "JetBrainsMono-1:ascent_mono_regular" "JetBrainsMono-2:ascent_mono_semibold"; do
  src="${pair%%:*}"; dst="${pair##*:}"
  python3 -m fontTools.subset "$WORK/fonts/$src.ttf" \
    --text-file="$WORK/charset.txt" --output-file="$WORK/out/$dst.ttf" \
    --name-IDs='*' --name-legacy --name-languages='*' \
    --notdef-outline --recommended-glyphs --drop-tables+=DSIG >/dev/null
done

# Nunito（拉丁子集）
python3 -m fontTools.subset "$WORK/fonts/Nunito-1.ttf" \
  --text-file="$WORK/latin.txt" --output-file="$WORK/out/ascent_display_bold.ttf" \
  --name-IDs='*' --name-legacy --name-languages='*' \
  --notdef-outline --recommended-glyphs --drop-tables+=DSIG >/dev/null

# ---------- 4. 落盘 ----------
cp "$WORK"/out/*.ttf "$DEST"/
echo "· 完成 → $DEST"
ls -la "$DEST"
du -ch "$DEST"/*.ttf | tail -1
