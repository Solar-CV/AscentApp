#!/usr/bin/env python3
"""简中 → 繁體中文（台灣用語）題庫片段產生器。

用法：
    python3 tools/i18n/tw.py l26 l27 > /tmp/tw.kt      # 產生片段
    python3 tools/i18n/tw.py --check                    # 自檢：能否復現既有譯文
    python3 tools/i18n/tw.py --check-keys               # 只看差異鍵清單

轉換 = OpenCC s2twp（簡→繁，台灣詞彙）+ 本檔 TERMFIX 校正表。
TERMFIX 是從既有 3,954 條譯文反推出來的房屋用語，只有 6 條，
`--check` 必須 0 差異才能拿去產新片段。

注意：需要 opencc，裝在隔離 venv：
    ~/.workbuddy/binaries/python/envs/default/bin/pip install opencc-python-reimplemented
"""
import re
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
from lib import lessons, load, quiz_keys

try:
    import opencc
except ImportError:
    sys.exit("缺少 opencc：~/.workbuddy/binaries/python/envs/default/bin/pip install opencc-python-reimplemented")

cc = opencc.OpenCC("s2twp")

# 既有譯文的用詞習慣（順序有意義：先長詞，再單字）
TERMFIX = [
    ("千米", "公里"),
    ("釐米", "公分"),
    ("厘米", "公分"),
    ("平方米", "平方公尺"),
    ("高原反應", "高山症反應"),
    ("高原病", "高山症"),
    ("巖", "岩"),
    ("視窗", "窗口"),
    ("物件", "對象"),
    ("保護站", "確保站"),
]

# 「米」只在计量语境改成「公尺」：前面必须是数字或空格，避免動到 米飯 / 玉米
MI = re.compile(r"(?<=[0-9０-９一二三四五六七八九十百千萬万兩两幾几半\s])米")


def conv(s):
    s = cc.convert(s)
    for a, b in TERMFIX:
        s = s.replace(a, b)
    return MI.sub("公尺", s)


def escape(s):
    return (s.replace("\\", "\\\\").replace('"', '\\"')
             .replace("\n", "\\n"))


def unescape(s):
    """從 Kotlin 字串常量還原：\\n → 換行、\\" → 引號、\\\\ → 反斜線。"""
    out = []
    i = 0
    while i < len(s):
        if s[i] == "\\" and i + 1 < len(s):
            n = s[i + 1]
            out.append({"n": "\n", "t": "\t", '"': '"', "\\": "\\"}.get(n, "\\" + n))
            i += 2
        else:
            out.append(s[i])
            i += 1
    return "".join(out)


def source(lid, lesson):
    """按 quiz_keys 的顺序产出 (key, 原文)。"""
    out = []
    for i, q in enumerate(lesson["questions"]):
        if q.get("review"):
            continue
        b = "q.%s#%d" % (lid, i)
        out.append((b + ".p", q["prompt"]))
        t = q["type"]
        if t in ("single", "multi", "order"):
            for n, o in enumerate(q["options"]):
                out.append(("%s.o%d" % (b, n), o))
        elif t in ("fill", "calc"):
            for n, a in enumerate(q["answers"]):
                out.append(("%s.a%d" % (b, n), a))
        elif t == "match":
            for n, x in enumerate(q["pairsLeft"]):
                out.append(("%s.l%d" % (b, n), x))
            for n, x in enumerate(q["pairsRight"]):
                out.append(("%s.r%d" % (b, n), x))
        elif t == "scenario":
            if q.get("context"):
                out.append((b + ".c", q["context"]))
            for si, st in enumerate(q.get("steps", [])):
                out.append(("%s.s%d.p" % (b, si), st["prompt"]))
                for n, o in enumerate(st["options"]):
                    out.append(("%s.s%d.o%d" % (b, si, n), o))
                out.append(("%s.s%d.e" % (b, si), st["explanation"]))
        out.append((b + ".e", q["explanation"]))
    return out


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    ls = lessons()
    have = load("zh-TW")

    if "--check" in sys.argv or "--check-keys" in sys.argv:
        diffs = []
        for lid, l in ls.items():
            for k, v in source(lid, l):
                if k in have and escape(conv(v)) != have[k]:
                    diffs.append((k, escape(conv(v)), have[k]))
        print("既有譯文 %d 條，復現差異 %d 條" % (len(have), len(diffs)))
        for k, c, h in diffs[:10]:
            print("K", k)
            print("  conv:", c[:80])
            print("  have:", h[:80])
        return 0

    if not args:
        sys.exit("用法：tw.py <lesson...> 或 tw.py --check")

    lines = []
    for lid in args:
        for k, v in source(lid, ls[lid]):
            if k in have:
                continue
            lines.append('    "%s" to "%s",' % (k, escape(conv(v))))
    print("\n".join(lines))


if __name__ == "__main__":
    sys.exit(main())
