#!/usr/bin/env python3
"""内容层多语言覆盖率自检。

用法：
    bash tools/contentcheck/run.sh      # 先跑一次，生成 bank/bank.json
    python3 tools/i18ncheck.py

为什么先要 contentcheck：题库的期望键由「课程 + 题目结构」推算（题干、选项数、
配对列、填空答案、情景步数），这些只有序列化后的题库才拿得准。早期版本把
英文包自己的 key 当分母，于是**新增课时永远不计入**，覆盖率会一直显示 100%。

检查四件事：
  1. 四层（入口 / 要点 / 技能书正文 / 题库）各自的期望键集合
  2. 英文包是否覆盖全部期望键（英文是回退基准，缺了就是结构性缺口）
  3. 每种语言相对期望键的覆盖情况
  4. 数据层实体（课程 / 单元 / 成就 / 技能书 / 山峰 / 分类）是否都有条目

退出码：英文包存在缺口时返回 1；仅其他语言有缺口时返回 0 ——
因为回退英文是设计行为，不是错误。
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK_DIR = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data/i18n")
DATA = os.path.join(ROOT, "app/src/main/java/com/ascent/climb/data")
BANK = os.path.join(ROOT, "bank/bank.json")

ENTRY = re.compile(r'"([^"]+)" to "((?:[^"\\]|\\.)*)"')

# 语言 → 包前缀。题库按语言拆成 Pack<X>Quiz / Quiz2 / Quiz3…（单文件约 2,500 条
# 会让 Kotlin 编译守护进程崩），所以题库那几支按目录实际文件自动展开，不要写死文件名。
_LANG_PREFIX = [
    ("en", "PackEn"),
    ("zh-TW", "PackZhTw"),
    ("ja", "PackJa"),
    ("ru", "PackRu"),
    ("fr", "PackFr"),
    ("sv", "PackSv"),
    ("de", "PackDe"),
    ("es", "PackEs"),
    ("it", "PackIt"),
]


def _quiz_part_no(name):
    """PackEnQuiz → 0、PackEnQuiz2 → 2、PackEnQuiz10 → 10（自然序，别按字符串排）。"""
    tail = name.split("Quiz", 1)[-1]
    return int(tail) if tail.isdigit() else 0


def pack_names(prefix):
    names = [prefix, f"{prefix}Brief", f"{prefix}Entry", f"{prefix}Handbook"]
    quiz = sorted(
        (os.path.basename(p)[:-3] for p in glob.glob(os.path.join(PACK_DIR, f"{prefix}Quiz*.kt"))),
        key=_quiz_part_no,
    )
    return names + quiz


LANGS = [(code, pack_names(prefix)) for code, prefix in _LANG_PREFIX]


def read_pack(names):
    out = {}
    for name in names:
        path = os.path.join(PACK_DIR, f"{name}.kt")
        if os.path.exists(path):
            out.update(dict(ENTRY.findall(open(path, encoding="utf-8").read())))
    return out


def load_bank():
    if not os.path.exists(BANK):
        print("找不到 bank/bank.json —— 先运行 bash tools/contentcheck/run.sh")
        sys.exit(2)
    return json.load(open(BANK, encoding="utf-8"))


def quiz_keys(lid, l):
    """按题目结构推出该课在题库层的全部键。"""
    ks = []
    for i, q in enumerate(l["questions"]):
        # 复习副本是运行时 copy 出来的，id 沿用原题，不需要单独的译文键
        if q.get("review"):
            continue
        base = f"q.{lid}#{i}"
        t = q["type"]
        ks.append(base + ".p")
        if t in ("single", "multi", "order"):
            ks += [f"{base}.o{n}" for n in range(len(q["options"]))]
        elif t in ("fill", "calc"):
            ks += [f"{base}.a{n}" for n in range(len(q["answers"]))]
        elif t == "match":
            ks += [f"{base}.l{n}" for n in range(len(q["pairsLeft"]))]
            ks += [f"{base}.r{n}" for n in range(len(q["pairsRight"]))]
        elif t == "scenario":
            if q.get("context"):
                ks.append(base + ".c")
            for si, st in enumerate(q.get("steps", [])):
                ks.append(f"{base}.s{si}.p")
                ks += [f"{base}.s{si}.o{n}" for n in range(len(st["options"]))]
                ks.append(f"{base}.s{si}.e")
        ks.append(base + ".e")
    return ks


def data_entities():
    lessons = []
    for tr in load_bank()["tracks"]:
        for u in tr["units"]:
            for l in u["lessons"]:
                lessons.append(l)

    src = ""
    for f in glob.glob(os.path.join(DATA, "*.kt")):
        if f.endswith(".bak-chatbox-0930"):
            continue
        src += open(f, encoding="utf-8").read() + "\n"

    units = re.findall(r'id = "(m[0-9a-z]+)", index = \d+', src)
    peaks = sorted(set(re.findall(r'Peak\("([^"]+)"', src)))
    cats = sorted(set(re.findall(r'category = "([^"]+)"', src)))
    ach = sorted(set(re.findall(r'\n    Achievement\(\n\s*"([a-z0-9_]+)"', src)))

    hb = {}
    for path in sorted(glob.glob(os.path.join(DATA, "Handbook*.kt"))):
        s = open(path, encoding="utf-8").read()
        starts = [m.start() for m in re.finditer(r'HandbookArticle\(', s)] + [len(s)]
        for i in range(len(starts) - 1):
            seg = s[starts[i]:starts[i + 1]]
            m = re.search(r'id = "([^"]+)"', seg)
            if m:
                hb[m.group(1)] = len(re.findall(r'HandbookSection\(', seg))
    return lessons, units, ach, hb, peaks, cats


def expectations():
    lessons, units, ach, hb, peaks, cats = data_entities()
    entry, brief, body, quiz = [], [], [], []
    for l in lessons:
        lid = l["id"]
        entry.append(f"lesson.{lid}.title")
        for n in range(len(l.get("brief", []))):
            brief.append(f"lesson.{lid}.brief.{n + 1}")
        quiz += quiz_keys(lid, l)
    for u in units:
        entry.append(f"unit.{u}.title")
    for a in ach:
        entry += [f"ach.{a}.title", f"ach.{a}.desc"]
    for hid, n in hb.items():
        entry += [f"hb.{hid}.title", f"hb.{hid}.summary"]
        for k in range(1, n + 1):
            body += [f"hb.{hid}.s{k}.h", f"hb.{hid}.s{k}.b"]
    entry += [f"cat.{c}" for c in cats] + [f"peak.{p}" for p in peaks]
    return {"入口": entry, "要点": brief, "正文": body, "题库": quiz}, lessons, hb


def main():
    expect, lessons, hb = expectations()
    packs = {tag: read_pack(name) for tag, name in LANGS}
    base = packs["en"]
    if not base:
        print("找不到英文包，检查路径:", PACK_DIR)
        return 1

    total = sum(len(v) for v in expect.values())
    print("=== 数据层实体 ===")
    print(f"  课程 {len(lessons)} · 技能书 {len(hb)} 篇 / {sum(hb.values())} 节")
    print(f"  期望条目 {total}（入口 {len(expect['入口'])} / 要点 {len(expect['要点'])} / "
          f"正文 {len(expect['正文'])} / 题库 {len(expect['题库'])}）")

    print("\n=== 英文包（回退基准）===")
    failed = False
    for name, keys in expect.items():
        miss = [k for k in keys if k not in base]
        flag = "完整" if not miss else f"缺 {len(miss)}"
        print(f"  {name:4s} {len(keys) - len(miss):5d}/{len(keys):<5d} {flag}")
        if miss:
            failed = True
            for k in miss[:4]:
                print("      ", k)
            if len(miss) > 4:
                print(f"       … 另 {len(miss) - 4} 条")

    print("\n=== 各语言覆盖（相对期望键）===")
    print(f"{'语言':7s} {'入口':>11s} {'要点':>11s} {'正文':>11s} {'题库':>13s} {'总覆盖率':>9s}")
    for tag, _ in LANGS:
        if tag == "en":
            continue
        k = packs[tag]
        cells, hit_all, exp_all = [], 0, 0
        for name in ("入口", "要点", "正文", "题库"):
            keys = expect[name]
            hit = sum(1 for x in keys if x in k)
            cells.append(f"{hit}/{len(keys)}")
            hit_all += hit
            exp_all += len(keys)
        pct = 100.0 * hit_all / max(1, exp_all)
        print(f"{tag:7s} {cells[0]:>11s} {cells[1]:>11s} {cells[2]:>11s} {cells[3]:>13s} {pct:8.1f}%")

    print("\n说明：缺失条目运行时回退英文（不是中文），属设计行为。")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
