#!/usr/bin/env python3
"""Draft the next Kami release notes from git log.

Pulls commit subjects in a rev range and pours them into the V1.4.0-style
template (centered logo + bilingual changelog). The output is a starting
point: regroup the commits into 5-8 product-themed bullets and translate
each to Chinese before publishing.

Usage:
    python3 scripts/draft-release-notes.py
    python3 scripts/draft-release-notes.py V1.4.0..HEAD
    python3 scripts/draft-release-notes.py \\
        --version V1.5.0 \\
        --title "Steadier Hand" \\
        --subtitle-en "Plugin install fix and audit cleanup." \\
        --subtitle-cn "插件安装修复，审计清理沉淀。"

The default rev range is `<latest tag>..HEAD`. Output goes to stdout; pipe
to a file or `pbcopy` to feed `gh release create --notes-file`.
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from textwrap import dedent

_HEADER = dedent("""\
    <div align="center">
      <img src="https://gw.alipayobjects.com/zos/k/vl/logo.svg" alt="Kami Logo" width="120" />
      <h1 style="margin: 12px 0 6px;">Kami {version}</h1>
      <p><em>{subtitle_en}</em></p>
    </div>
""")

_FOOTER = dedent("""\
    > Kami is a quiet design system for professional documents, one constraint set that any agent can trust. https://github.com/tw93/Kami
""")

# Conventional-commit prefix to a short product label, used as a hint when
# the user reorganizes the auto-listed commits into themed bullets.
_PREFIX_HINT = {
    "build": "build",
    "ci": "ci",
    "feat": "feature",
    "fix": "fix",
    "docs": "docs",
    "chore": "chore",
    "refactor": "refactor",
    "test": "test",
    "perf": "perf",
    "revert": "revert",
}


def _run(cmd: list[str]) -> str:
    result = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"{' '.join(cmd)}: {result.stderr.strip() or 'failed'}")
    return result.stdout


def latest_tag() -> str | None:
    try:
        out = _run(["git", "describe", "--tags", "--abbrev=0"]).strip()
    except RuntimeError:
        return None
    return out or None


def commits_in(rev_range: str) -> list[tuple[str, str]]:
    """Return [(short_sha, subject), ...] in chronological order."""
    out = _run(["git", "log", rev_range, "--format=%h\t%s", "--reverse"])
    rows: list[tuple[str, str]] = []
    for line in out.splitlines():
        if "\t" in line:
            sha, subject = line.split("\t", 1)
            rows.append((sha.strip(), subject.strip()))
    return rows


def classify(subject: str) -> str:
    """Return a one-word commit category derived from the conventional-commit prefix."""
    head = subject.split(":", 1)[0].split("(", 1)[0].strip().lower()
    return _PREFIX_HINT.get(head, "other")


def render(
    version: str,
    title: str,
    subtitle_en: str,
    subtitle_cn: str,
    rev_range: str,
    commits: list[tuple[str, str]],
) -> str:
    out: list[str] = []
    out.append(_HEADER.format(version=version, subtitle_en=subtitle_en))
    out.append(f"<!-- title: {version} {title} -->")
    out.append(f"<!-- range: {rev_range} ({len(commits)} commits) -->")
    out.append("<!-- regroup the bullets below into 5-8 product-themed items -->")
    out.append("")
    out.append("### Changelog")
    out.append("")
    for i, (sha, subject) in enumerate(commits, 1):
        out.append(f"{i}. **TODO**: {subject}  <!-- {sha} {classify(subject)} -->")
    out.append("")
    out.append("### 更新日志")
    out.append("")
    out.append(f"<!-- 副标题: {subtitle_cn} -->")
    out.append("<!-- 翻译并对齐到上面英文条目，保持一一对应 -->")
    out.append("")
    for i in range(1, len(commits) + 1):
        out.append(f"{i}. **TODO**：（对应英文第 {i} 条）")
    out.append("")
    out.append(_FOOTER)
    return "\n".join(out)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "rev_range",
        nargs="?",
        default=None,
        help="Git rev range (default: <latest tag>..HEAD)",
    )
    parser.add_argument("--version", default="VX.Y.Z", help="Version label, e.g. V1.5.0")
    parser.add_argument("--title", default="<title>", help="Release title, e.g. 'Steadier Hand'")
    parser.add_argument(
        "--subtitle-en",
        default="<one-line subtitle>",
        help="Short English subtitle for the centered hero block",
    )
    parser.add_argument(
        "--subtitle-cn",
        default="<一句话中文副标题>",
        help="Short Chinese subtitle, kept as a comment hint for translators",
    )
    return parser.parse_args(argv[1:])


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    rev_range = args.rev_range
    if rev_range is None:
        tag = latest_tag()
        if not tag:
            print("ERROR: no tag found; pass an explicit rev range like V1.0.0..HEAD",
                  file=sys.stderr)
            return 2
        rev_range = f"{tag}..HEAD"

    try:
        commits = commits_in(rev_range)
    except RuntimeError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    if not commits:
        print(f"ERROR: no commits in {rev_range}", file=sys.stderr)
        return 1

    out = render(
        version=args.version,
        title=args.title,
        subtitle_en=args.subtitle_en,
        subtitle_cn=args.subtitle_cn,
        rev_range=rev_range,
        commits=commits,
    )
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
