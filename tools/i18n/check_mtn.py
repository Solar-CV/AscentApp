#!/usr/bin/env python3
"""山峰内容层自检：键集合、空值、重复键、以及不该出现的汉字。

    python3 tools/i18n/check_mtn.py

退出码 1 = 有硬错误（缺键 / 空值 / 非中日语言里混入汉字）。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_mtn_packs import parse  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(HERE))
PACK_DIR = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/i18n")
ENTRY = re.compile(r'"([^"]+)" to "((?:[^"\\]|\\.)*)"')

FILES = {
    "en": "PackEnMtn.kt", "zh-TW": "PackZhTwMtn.kt", "ja": "PackJaMtn.kt",
    "ru": "PackRuMtn.kt", "de": "PackDeMtn.kt", "es": "PackEsMtn.kt",
    "fr": "PackFrMtn.kt", "sv": "PackSvMtn.kt", "it": "PackItMtn.kt",
}
CJK = re.compile(r"[\u4e00-\u9fff]")
# 日文里这些汉字是正常的（山名用汉字书写），俄/欧语言里出现汉字就是漏翻
CJK_OK = {"ja", "zh-TW"}

rows = parse()
want = {"mtn.%s.name" % r["id"] for r in rows}
want |= {"mtn.range.%s" % r["range"] for r in rows}
want.add("mtn.snow.none")

bad = False
for lang, fname in FILES.items():
    path = os.path.join(PACK_DIR, fname)
    raw = open(path, encoding="utf-8").read()
    pairs = ENTRY.findall(raw)
    keys = [k for k, _ in pairs]
    got = set(keys)
    dup = sorted({k for k in keys if keys.count(k) > 1})
    empty = sorted(k for k, v in pairs if not v.strip())
    miss = sorted(want - got)
    extra = sorted(got - want)
    cjk = sorted(k for k, v in pairs if lang not in CJK_OK and CJK.search(v))
    line = "%-6s %3d 键  缺%d 多%d 重%d 空%d 汉字%d" % (
        lang, len(keys), len(miss), len(extra), len(dup), len(empty), len(cjk))
    print(line)
    for label, items in (("缺", miss), ("多", extra), ("重", dup), ("空", empty), ("汉字", cjk)):
        if items:
            bad = True
            print("   %s: %s" % (label, ", ".join(items[:12])))

print("期望 %d 键" % len(want))
sys.exit(1 if bad else 0)
