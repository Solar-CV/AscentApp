#!/usr/bin/env python3
"""导出指定课程尚未翻译的中文原文，供翻译。

用法：
    python3 tools/i18n/prep.py m01 m02 > /tmp/batch.txt
    python3 tools/i18n/prep.py l1 --lang ja        # 指定语言（默认 en）
"""
import json
import sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from lib import lessons, load

LANG = "en"
for i, a in enumerate(sys.argv):
    if a == "--lang" and i + 1 < len(sys.argv):
        LANG = sys.argv[i + 1]

ls = lessons()
have = load(LANG)
for lid in [a for a in sys.argv[1:] if not a.startswith("--") and a != LANG]:
    l = ls[lid]
    print("### %s | %s" % (lid, l["title"]))
    for n, b in enumerate(l.get("brief", []), 1):
        if "lesson.%s.brief.%d" % (lid, n) not in have:
            print("  B%d: %s" % (n, b))
    for i, q in enumerate(l["questions"]):
        if q.get("review"):
            continue
        base = "q.%s#%d" % (lid, i)
        if base + ".p" in have:
            continue
        t = q["type"]
        print("\n[%d] %s" % (i, t))
        print("  p: %s" % q["prompt"])
        if q.get("context"):
            print("  c: %s" % q["context"])
        if t in ("single", "multi", "order"):
            for n, o in enumerate(q["options"]):
                print("  o%d: %s" % (n, o))
        elif t in ("fill", "calc"):
            print("  a: %s" % " / ".join(q["answers"]))
        elif t == "match":
            for n, x in enumerate(q["pairsLeft"]):
                print("  l%d: %s" % (n, x))
            for n, x in enumerate(q["pairsRight"]):
                print("  r%d: %s" % (n, x))
        elif t == "scenario":
            for si, st in enumerate(q.get("steps", [])):
                print("  s%d.p: %s" % (si, st["prompt"]))
                for n, o in enumerate(st["options"]):
                    print("  s%d.o%d: %s" % (si, n, o))
                print("  s%d.e: %s" % (si, st["explanation"]))
        print("  e: %s" % q["explanation"])
