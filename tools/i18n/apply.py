#!/usr/bin/env python3
"""校验并合并一个翻译片段。

用法：
    python3 tools/i18n/apply.py /tmp/q_m01.kt            # 默认题库层、英文
    python3 tools/i18n/apply.py /tmp/b.kt --layer brief
    python3 tools/i18n/apply.py /tmp/q_l26.kt --lang zh-TW

做的事：
  1. 检查重复键、空值、以及「不在期望集合里」的键（多半是拼错）
  2. 检查该片段的课程是否覆盖完整（缺了会提示，不阻断）
  3. 给上一条补逗号后插入目标 mapOf
"""
import argparse
import os
import re
import sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from lib import PACK_DIR, ENTRY, lessons, load, quiz_keys, brief_keys, target


def expected(layer):
    ls = lessons()
    keys = set()
    for lid, l in ls.items():
        keys.update(quiz_keys(lid, l) if layer == "quiz" else brief_keys(lid, l))
    return keys


def insert(path, frag):
    lines = open(path, encoding="utf-8").read().split("\n")
    idx = max(i for i, l in enumerate(lines) if l.strip() == ")")
    prev = idx - 1
    while prev >= 0 and not lines[prev].strip():
        prev -= 1
    if not lines[prev].rstrip().endswith((",", "(", "{")):
        lines[prev] = lines[prev].rstrip() + ","
    lines.insert(idx, frag)
    open(path, "w", encoding="utf-8").write("\n".join(lines))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("fragment")
    ap.add_argument("--layer", default="quiz", choices=["quiz", "brief", "entry", "handbook"])
    ap.add_argument("--allow-missing", action="store_true")
    ap.add_argument("--lang", default="en")
    a = ap.parse_args()

    frag = open(a.fragment, encoding="utf-8").read().strip("\n")
    # 允许片段自带 mapOf( ... ) 包裹：去掉首尾包裹行，只保留成对的条目
    fl = frag.split("\n")
    if fl and fl[0].strip().startswith("mapOf("):
        fl = fl[1:]
    while fl and not fl[-1].strip():
        fl.pop()
    if fl and fl[-1].strip() == "),":
        fl[-1] = ")"
    if fl and fl[-1].strip() == ")":
        fl = fl[:-1]
    frag = "\n".join(fl)
    pairs = ENTRY.findall(frag)
    keys = [k for k, _ in pairs]
    problems = []
    if len(keys) != len(set(keys)):
        problems.append("重复键 %d 个" % (len(keys) - len(set(keys))))
    if any(not v.strip() for _, v in pairs):
        problems.append("空值 %d 个" % sum(1 for _, v in pairs if not v.strip()))
    if a.layer in ("quiz", "brief"):
        unknown = [k for k in keys if k not in expected(a.layer)]
        if unknown:
            problems.append("未知键 %d 个：%s" % (len(unknown), unknown[:5]))

    have = load(a.lang)
    covered = {k.split(".")[1].split("#")[0] for k in keys if a.layer == "quiz"}
    missing = []
    for lid in covered:
        exp = quiz_keys(lid, lessons()[lid]) if a.layer == "quiz" else brief_keys(lid, lessons()[lid])
        missing += [k for k in exp if k not in have and k not in keys]
    if missing and not a.allow_missing:
        problems.append("本课仍缺 %d 条，例如 %s" % (len(missing), missing[:3]))

    if problems:
        print("片段有问题，未合并：")
        for p in problems:
            print("  -", p)
        return 1

    if not frag.endswith(","):
        frag += ","
    dest = target(a.lang, a.layer)
    insert(os.path.join(PACK_DIR, dest), frag)
    print("已合并 %d 条到 %s（%s）" % (len(pairs), dest, a.lang))
    return 0


if __name__ == "__main__":
    sys.exit(main())
