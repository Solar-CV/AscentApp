#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查成就文案（title / desc / cond）在九种翻译语言里的覆盖率。

i18ncheck.py 只统计课程入口 / 要点 / 正文 / 题库四层，成就文案不在它的口径里，
所以另开一个。加新成就时先跑这个，缺了会在运行时回退中文原文。

用法：
    python3 tools/check_ach.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "i18n"))
from lib import PACK_DIR, PACKS  # noqa: E402

ACH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app/src/main/java/com/ascent/climb/data/Achievements.kt")

FIELDS = ("title", "desc", "cond")


def achievement_ids():
    s = open(ACH, encoding="utf-8").read()
    pat = re.compile(
        r'"([a-z0-9_]+)",\s*"[^"]*",\s*"[^"]*",\s*\n\s*AchCategory\.\w+,\s*Rarity\.\w+,\s*"[^"]*",\s*"[^"]*"'
    )
    return [m.group(1) for m in pat.finditer(s)]


def load(lang):
    blob = ""
    for name in PACKS[lang]:
        p = os.path.join(PACK_DIR, name + ".kt")
        if os.path.isfile(p):
            blob += open(p, encoding="utf-8").read()
    return blob


def main():
    ids = achievement_ids()
    if len(ids) != len(set(ids)):
        dup = sorted({i for i in ids if ids.count(i) > 1})
        print(f"✗ 成就 id 重复：{', '.join(dup)}")
        return 1

    expect = {f"ach.{i}.{f}" for i in ids for f in FIELDS}
    print(f"成就 {len(ids)} 条 → 每种语言应有 {len(expect)} 个键\n")

    bad = 0
    print(f'{"语言":<8}{"title":>10}{"desc":>10}{"cond":>10}   状态')
    for lang in PACKS:
        blob = load(lang)
        counts, missing = {}, {}
        for f in FIELDS:
            have = sum(1 for i in ids if f'"ach.{i}.{f}" to ' in blob)
            counts[f] = have
            missing[f] = [i for i in ids if f'"ach.{i}.{f}" to ' not in blob]
        ok = all(counts[f] == len(ids) for f in FIELDS)
        status = "完整" if ok else "缺 " + ", ".join(f"{f}:{len(missing[f])}" for f in FIELDS if missing[f])
        if not ok:
            bad += 1
        print(f'{lang:<8}{counts["title"]:>10}{counts["desc"]:>10}{counts["cond"]:>10}   {status}')

    if bad:
        print(f"\n✗ {bad} 种语言有缺失。缺失项运行时回退中文原文（不是英文），请补翻译。")
        return 1
    print("\n✓ 全部语言完整")
    return 0


if __name__ == "__main__":
    sys.exit(main())
