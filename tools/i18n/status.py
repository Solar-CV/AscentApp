#!/usr/bin/env python3
"""内容层进度与下一批建议（按语言）。

用法：
    python3 tools/i18n/status.py                 # 英文概览 + 下一批
    python3 tools/i18n/status.py --lang zh-TW    # 繁中
    python3 tools/i18n/status.py --all           # 列出全部缺口课
"""
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
from lib import bank, lessons, load, quiz_keys, brief_keys

LANG = "en"
for i, a in enumerate(sys.argv):
    if a == "--lang" and i + 1 < len(sys.argv):
        LANG = sys.argv[i + 1]

NAME = {"en": "英文", "zh-TW": "繁體中文", "ja": "日文", "ru": "俄文", "fr": "法文",
        "sv": "瑞典文", "de": "德文", "es": "西班牙文", "it": "義大利文"}.get(LANG, LANG)

ls = lessons()
have = load(LANG)
order = [l["id"] for tr in bank()["tracks"] for u in tr["units"] for l in u["lessons"]]
rows = []
for lid in order:
    q = [k for k in quiz_keys(lid, ls[lid]) if k not in have]
    b = [k for k in brief_keys(lid, ls[lid]) if k not in have]
    if q or b:
        rows.append((lid, ls[lid]["title"], len(q), len(b), len(ls[lid]["questions"])))

total = sum(r[2] for r in rows)
print("%s 内容层缺口：%d 条（题库）+ %d 条（要点），涉及 %d 课"
      % (NAME, total, sum(r[3] for r in rows), len(rows)))
print()
if rows:
    print("%-6s %-22s %6s %6s" % ("课", "标题", "缺键", "题数"))
    show = rows if "--all" in sys.argv else rows[:12]
    for lid, title, q, b, n in show:
        print("%-6s %-22s %6d %6d" % (lid, title[:20], q, n))
    if len(rows) > len(show):
        print("… 另 %d 课，加 --all 查看" % (len(rows) - len(show)))
    print()
    print("下一批建议：%s" % " ".join(r[0] for r in rows[:3]))
