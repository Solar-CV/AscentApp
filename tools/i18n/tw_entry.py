#!/usr/bin/env python3
"""繁體中文「入口層 + 技能書」片段產生器（課程名／單元名／山峰名／分類／技能書 h32–h35）。

用法：
    python3 tools/i18n/tw_entry.py > /tmp/tw_entry.kt
    python3 tools/i18n/tw_entry.py --check      # 只列出將產生的鍵

中文原文不在 bank.json 裡（只有課程名），所以直接從 Kotlin 資料檔抓：
    Curriculum.kt → 單元 title、山峰名
    HandbookF.kt  → h32–h35 的 title / summary / 各節
需要 opencc（見 tw.py）。
"""
import importlib.util
import os
import re
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
from lib import ROOT, DATA, load, lessons, bank

spec = importlib.util.spec_from_file_location("tw", os.path.join(os.path.dirname(__file__), "tw.py"))
tw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tw)


def unit_titles():
    out = {}
    for name in ("Curriculum.kt", "TrekLessons.kt"):
        s = open(os.path.join(DATA, name), encoding="utf-8").read()
        for m in re.finditer(r'id = "([a-z0-9]+)",\s*index = \d+,[^)]*?title = "([^"]+)"', s, re.S):
            out[m.group(1)] = m.group(2)
    return out


def handbooks():
    """回傳 {article_id: (title, summary, [(heading, body)...])}"""
    s = open(os.path.join(DATA, "HandbookF.kt"), encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'id = "(h\d+)",\s*title = "([^"]+)",\s*code = "[^"]*",\s*icon = "[^"]*",\s*category = "[^"]*",\s*summary = "([^"]+)"', s, re.S):
        aid, title, summary = m.group(1), tw.unescape(m.group(2)), tw.unescape(m.group(3))
        rest = s[m.end():]
        stop = rest.find("HandbookArticle(")
        block = rest if stop < 0 else rest[:stop]
        secs = [(tw.unescape(a), tw.unescape(b)) for a, b in
                re.findall(r'HandbookSection\(\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)"\s*\)', block, re.S)]
        out[aid] = (title, summary, secs)
    return out


def main():
    have = load("zh-TW")
    en = load("en")
    rows = []

    # 課程名（中文在 bank.json）
    for lid, l in lessons().items():
        k = "lesson.%s.title" % lid
        if k not in have:
            rows.append((k, l["title"]))

    # 單元名
    for uid, title in unit_titles().items():
        k = "unit.%s.title" % uid
        if k not in have:
            rows.append((k, title))

    # 山峰名：值 = 鍵去掉前綴（中文原名）轉繁
    for k in en:
        if k.startswith("peak.") and k not in have:
            rows.append((k, k[5:]))

    # 分類
    for k in en:
        if k.startswith("cat.") and k not in have:
            rows.append((k, k[4:]))

    # 技能書
    for aid, (title, summary, secs) in handbooks().items():
        base = "hb.%s" % aid
        if base + ".title" not in have:
            rows.append((base + ".title", title))
        if base + ".summary" not in have:
            rows.append((base + ".summary", summary))
        for i, (h, b) in enumerate(secs, 1):
            if "%s.s%d.h" % (base, i) not in have:
                rows.append(("%s.s%d.h" % (base, i), h))
            if "%s.s%d.b" % (base, i) not in have:
                rows.append(("%s.s%d.b" % (base, i), b))

    if "--check" in sys.argv:
        for k, v in rows:
            print(k, "|", v[:60])
        print("共 %d 條" % len(rows))
        return 0

    for k, v in rows:
        print('    "%s" to "%s",' % (k, tw.escape(tw.conv(v))))
    print("// %d 條" % len(rows), file=sys.stderr)


if __name__ == "__main__":
    sys.exit(main())
