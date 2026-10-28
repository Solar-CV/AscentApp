#!/usr/bin/env python3
"""从 .github/RELEASE_NOTES.md 里抽出「本次发布」的正文。

问题：release.yml 直接把整个 RELEASE_NOTES.md 当作 Release 正文（body_path），
于是每发一版，历史章节都会重复出现在页面上，越积越长。

做法：
  1. 文件头部（第一个 `## v...` 之前，即安装说明）原样保留；
  2. 当前 tag 对应的章节整段保留；
  3. 介于「上一个已发布的 tag」和「当前 tag」之间、从来没单独发过的章节，
     只取一级小标题，压成一行式要点——这些东西本次一并交付，读者需要知道
     但不需要读全文；
  4. 比上一个 tag 更老的章节一律不出现。

「已发布」的判定用 git tag：发布流程本身就是打 tag，所以 tag 就是事实来源。
没有 tag 可比时（首个版本）不列历史要点。

用法：
  python3 .github/scripts/release_body.py \
      --tag v0.20.11 --notes .github/RELEASE_NOTES.md --out out/release_body.md
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

SECTION_RE = re.compile(r"^##\s+v?(\d+\.\d+\.\d+)\b.*$", re.MULTILINE)
HEADING_RE = re.compile(r"^###\s+(.+)$", re.MULTILINE)
TAG_RE = re.compile(r"^v?(\d+\.\d+\.\d+)$")


def semver(text: str) -> tuple[int, int, int] | None:
    m = TAG_RE.match(text.strip())
    if not m:
        return None
    return tuple(int(p) for p in m.group(1).split("."))  # type: ignore[return-value]


def split_notes(text: str) -> tuple[str, list[tuple[tuple[int, int, int], str]]]:
    """返回（头部, [(版本, 章节原文)]），章节按文件顺序（新 → 旧）。"""
    marks = list(SECTION_RE.finditer(text))
    if not marks:
        return text, []
    header = text[: marks[0].start()].rstrip() + "\n"
    sections = []
    for i, m in enumerate(marks):
        end = marks[i + 1].start() if i + 1 < len(marks) else len(text)
        sections.append((semver(m.group(1)), text[m.start():end].rstrip() + "\n"))
    return header, sections  # type: ignore[return-value]


def git_tags() -> list[tuple[int, int, int]]:
    try:
        out = subprocess.run(
            ["git", "tag", "--list", "v*"],
            capture_output=True, text=True, check=True,
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError):
        return []
    tags = []
    for line in out.splitlines():
        v = semver(line)
        if v:
            tags.append(v)
    return sorted(tags)


def trim_rule(text: str) -> str:
    """去掉末尾的空行和水平分隔线，避免拼接后出现两条 ---。"""
    lines = text.rstrip().splitlines()
    while lines and (not lines[-1].strip() or lines[-1].strip() == "---"):
        lines.pop()
    return "\n".join(lines)


def first_headings(body: str, limit: int = 1) -> list[str]:
    """取章节的一级小标题（`### ...`）；没有就退回第一条列表项。"""
    heads = [h.strip() for h in HEADING_RE.findall(body)]
    if heads:
        return heads[:limit]
    for line in body.splitlines():
        s = line.strip()
        if s.startswith(("- ", "* ")):
            return [s[2:].strip().rstrip("。.")]
    return []


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--tag", required=True)
    ap.add_argument("--notes", default=".github/RELEASE_NOTES.md")
    ap.add_argument("--out", default="out/release_body.md")
    ap.add_argument("--max-briefs", type=int, default=8)
    args = ap.parse_args()

    text = Path(args.notes).read_text(encoding="utf-8")
    current = semver(args.tag)
    header, sections = split_notes(text)

    # 找不到对应章节就整份照发：宁可页面长一点，也不要发一份空正文
    if current is None or not sections:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(text, encoding="utf-8")
        print("[release_body] 未解析出版本章节，回退为整份 notes")
        return 0

    mine = [s for v, s in sections if v == current]
    if not mine:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(text, encoding="utf-8")
        print(f"[release_body] notes 里没有 {args.tag} 的章节，回退为整份 notes")
        return 0

    # 上一个已发布的版本：所有小于当前的 tag 里最大的那个
    published = [v for v in git_tags() if v < current]
    last = max(published) if published else None

    # 介于两者之间、且自己没有 tag 的章节 = 本次一并交付但从未单独发过的内容
    pending: list[tuple[tuple[int, int, int], str]] = []
    for v, body in sections:
        if v == current:
            continue
        if last is not None and last < v < current:
            pending.append((v, body))
        elif last is None:
            continue

    out = [trim_rule(header), "", "---", "", trim_rule(mine[0])]

    if pending:
        # 新的在上，与正文顺序一致；条目过多时只留最近的几条
        pending.sort(reverse=True)
        shown = pending[: args.max_briefs]
        dropped = len(pending) - len(shown)
        out += [
            "",
            "### 本次一并发布的版本（此前未单独发版，只列要点）",
            "",
        ]
        for v, body in shown:
            ver = "v%d.%d.%d" % v
            heads = first_headings(body)
            if heads:
                for h in heads:
                    out.append(f"- **{ver}** — {h}")
            else:
                out.append(f"- **{ver}**")
        if dropped:
            out.append(f"- 另有 {dropped} 个更早的未发布版本，详见仓库 `RELEASE_NOTES.md`")

    body = "\n".join(out).rstrip() + "\n"
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out).write_text(body, encoding="utf-8")
    print(
        f"[release_body] {args.tag}: 正文 {len(body.splitlines())} 行；"
        f"一并发布 {len(pending)} 个旧版本（列出 {min(len(pending), args.max_briefs)} 个）"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
