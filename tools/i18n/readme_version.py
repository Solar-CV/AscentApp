#!/usr/bin/env python3
"""把十份 README 里的版本号同步到 app/build.gradle.kts 的当前值。

README 顶部那句版本行是发版时唯一要跟着改的地方，其余历史章节一律不动
（README 是滚动累积的更新日志，旧段落是记录不是现状）。

各语言的版本行写法不同（Current release / 当前版本 / Aktuelle Version …），
所以不按整句匹配，只替换**每份文件里第一次出现**的 `vX.Y.Z` 与 `versionCode N`：
这两处在任何一份 README 里都只出现在顶部版本行。

用法：
  python3 tools/i18n/readme_version.py           # 只报告差异
  python3 tools/i18n/readme_version.py --write   # 实际写入
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GRADLE = ROOT / "app/build.gradle.kts"

VERSION_RE = re.compile(r"v(\d+\.\d+\.\d+)")
CODE_RE = re.compile(r"versionCode (\d+)")


def current() -> tuple[str, str]:
    text = GRADLE.read_text(encoding="utf-8")
    name = re.search(r'versionName\s*=\s*"([\d.]+)"', text)
    code = re.search(r"versionCode\s*=\s*(\d+)", text)
    if not name or not code:
        raise SystemExit("cannot read versionName / versionCode from app/build.gradle.kts")
    return name.group(1), code.group(1)


def main() -> None:
    write = "--write" in sys.argv
    name, code = current()
    changed = 0
    for path in sorted(ROOT.glob("README*.md")):
        text = path.read_text(encoding="utf-8")
        old_name = VERSION_RE.search(text)
        old_code = CODE_RE.search(text)
        if not old_name or not old_code:
            print(f"!! {path.name}: version line not found")
            continue
        if old_name.group(1) == name and old_code.group(1) == code:
            print(f"ok {path.name}  v{name} ({code})")
            continue
        new = VERSION_RE.sub(f"v{name}", text, count=1)
        new = CODE_RE.sub(f"versionCode {code}", new, count=1)
        changed += 1
        print(f"-> {path.name}  v{old_name.group(1)} ({old_code.group(1)}) → v{name} ({code})")
        if write:
            path.write_text(new, encoding="utf-8")
    if not write and changed:
        print("\nrerun with --write to apply")


if __name__ == "__main__":
    main()
