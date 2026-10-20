#!/usr/bin/env python3
"""内容层多语言覆盖率自检。

用法：
    python3 tools/i18ncheck.py

检查三件事：
  1. 每个语言包相对英文包缺哪些 key（缺失 = 运行时回退英文）
  2. 数据层实际存在的课程 / 成就 / 技能书 / 单元，是否都有条目
  3. 各语言覆盖率百分比

退出码：存在「结构性缺失」（数据层有实体但英文包没有）时返回 1，
仅有语言间差异时返回 0 —— 因为回退英文是设计行为，不是错误。
"""
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK_DIR = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/i18n")

LANGS = [
    ("en", ["PackEn", "PackEnBrief", "PackEnHandbook", "PackEnQuiz"]),
    ("zh-TW", ["PackZhTw", "PackZhTwBrief", "PackZhTwEntry", "PackZhTwHandbook", "PackZhTwQuiz"]),
    ("ja", ["PackJa", "PackJaBrief", "PackJaEntry", "PackJaHandbook", "PackJaQuiz"]),
    ("ru", ["PackRu", "PackRuBrief", "PackRuEntry", "PackRuHandbook", "PackRuQuiz"]),
    ("fr", ["PackFr", "PackFrBrief", "PackFrEntry", "PackFrHandbook", "PackFrQuiz"]),
    ("sv", ["PackSv", "PackSvBrief", "PackSvEntry", "PackSvHandbook", "PackSvQuiz"]),
    ("de", ["PackDe", "PackDeBrief", "PackDeEntry", "PackDeHandbook", "PackDeQuiz"]),
    ("es", ["PackEs", "PackEsBrief", "PackEsEntry", "PackEsHandbook", "PackEsQuiz"]),
    ("it", ["PackIt", "PackItBrief", "PackItEntry", "PackItHandbook", "PackItQuiz"]),
]

ENTRY = re.compile(r'"([a-z]+\.[^"]+)" to "((?:[^"\\]|\\.)*)"')


def read_pack(names):
    out = {}
    for name in names:
        path = os.path.join(PACK_DIR, f"{name}.kt")
        if os.path.exists(path):
            out.update(dict(ENTRY.findall(open(path, encoding="utf-8").read())))
    return out


def collect_ids():
    data = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data")
    src = ""
    for f in glob.glob(os.path.join(data, "*.kt")):
        if f.endswith(".bak-chatbox-0930"):
            continue
        src += open(f, encoding="utf-8").read() + "\n"

    lessons = set(re.findall(r'Lesson\(\s*\n?\s*id = "([^"]+)"', src))
    units = set()
    for m in re.finditer(r'Unit\(([\s\S]{0,900}?)\)', src):
        u = re.search(r'id = "(u\d+)"', m.group(1))
        if u:
            units.add(u.group(1))

    ach = set(re.findall(r'\n    Achievement\(\n\s*"([a-z0-9_]+)"', src))
    hb = set(re.findall(r'HandbookArticle\(\s*id = "([^"]+)"', src))
    return lessons, units, ach, hb


def collect_handbook_sections():
    """技能书 id → 章节数。按文章边界切片，避免把下一篇的章节算进来。"""
    data = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data")
    out = {}
    for path in sorted(glob.glob(os.path.join(data, "Handbook*.kt"))):
        src = open(path, encoding="utf-8").read()
        starts = [m.start() for m in re.finditer(r'HandbookArticle\(', src)] + [len(src)]
        for i in range(len(starts) - 1):
            seg = src[starts[i]:starts[i + 1]]
            aid = re.search(r'id = "([^"]+)"', seg)
            if not aid:
                continue
            out[aid.group(1)] = len(re.findall(r'HandbookSection\(', seg))
    return out


def main():
    packs = {tag: read_pack(name) for tag, name in LANGS}
    base = packs["en"]
    if not base:
        print("找不到英文包，检查路径:", PACK_DIR)
        return 1

    lessons, units, ach, hb = collect_ids()
    hb_secs = collect_handbook_sections()

    structural = []
    for i in sorted(lessons):
        if f"lesson.{i}.title" not in base:
            structural.append(f"lesson.{i}.title")
    for i in sorted(units):
        if f"unit.{i}.title" not in base:
            structural.append(f"unit.{i}.title")
    for i in sorted(ach):
        for fld in ("title", "desc"):
            if f"ach.{i}.{fld}" not in base:
                structural.append(f"ach.{i}.{fld}")
    for i in sorted(hb):
        for fld in ("title", "summary"):
            if f"hb.{i}.{fld}" not in base:
                structural.append(f"hb.{i}.{fld}")
        # 正文按章节检查；英文正文尚未建立时不报结构性缺失
        if any(".s1.h" in k for k in base):
            for n in range(1, hb_secs.get(i, 0) + 1):
                for fld in ("h", "b"):
                    if f"hb.{i}.s{n}.{fld}" not in base:
                        structural.append(f"hb.{i}.s{n}.{fld}")

    print("=== 数据层实体 ===")
    print(f"  课程 {len(lessons)} · 单元 {len(units)} · 成就 {len(ach)} · 技能书 {len(hb)}")
    print(f"  英文包条目 {len(base)}")
    if structural:
        print(f"\n!! 英文包结构性缺失 {len(structural)} 项：")
        for k in structural[:20]:
            print("   ", k)
    else:
        print("  英文包覆盖完整（所有实体均有条目）")

    def pct_of(k, pred):
        total = [x for x in base if pred(x)]
        hit = [x for x in total if x in k]
        return len(hit), len(total)

    print("\n=== 各语言覆盖 ===")
    print(f"{'语言':7s} {'导航/说明':>10s} {'课程要点':>10s} {'技能书正文':>10s} {'题库':>10s} {'总覆盖率':>9s}")
    total_body = sum(hb_secs.values()) * 2
    for tag, _ in LANGS:
        if tag == "en":
            continue
        k = packs[tag]
        # 技能书章节 key 是 hb.*.sN.h/.b；题库子问题是 q.*.sN.*，不能混为一谈
        body = lambda x: x.startswith("hb.") and re.search(r'\.s\d+\.', x) is not None
        quiz = lambda x: x.startswith("q.")
        a, at = pct_of(k, lambda x: ".brief." not in x and not body(x) and not quiz(x))
        b, bt = pct_of(k, lambda x: ".brief." in x)
        c, ct = pct_of(k, body)
        d, dt = pct_of(k, quiz)
        pct = 100.0 * len(set(base) & set(k)) / max(1, len(base))
        print(f"{tag:7s} {a:4d}/{at:<4d} {b:4d}/{bt:<4d} {c:4d}/{ct:<4d} {d:4d}/{dt:<4d} {pct:8.1f}%")

    print("\n说明：缺失条目运行时回退英文（不是中文），属设计行为。")
    return 1 if structural else 0


if __name__ == "__main__":
    sys.exit(main())
