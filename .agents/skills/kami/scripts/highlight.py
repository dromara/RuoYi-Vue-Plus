"""Lightweight syntax highlighting for Kami HTML templates.

Scans HTML for <pre><code class="language-*"> blocks and applies
Pygments-based inline-style highlighting using Kami design tokens.
Blocks without a language- class pass through unchanged.
"""
from __future__ import annotations

import html as html_mod
import re

CODE_BLOCK_RE = re.compile(
    r'(<pre[^>]*>\s*<code\s+class="language-([\w+-]+)"[^>]*>)'
    r'(.*?)'
    r'(</code>\s*</pre>)',
    re.DOTALL,
)

KAMI_PALETTE = {
    "brand":      "#1B365D",
    "stone":      "#6b6a64",
    "olive":      "#504e49",
    "dark_warm":  "#3d3d3a",
    "near_black": "#141413",
}


def _build_kami_style():
    from pygments.style import Style
    from pygments.token import (
        Comment, Keyword, Literal, Name, Number, Operator,
        Punctuation, String, Token,
    )

    class KamiStyle(Style):
        background_color = ""
        default_style = ""
        styles = {
            Token:              "",
            Comment:            KAMI_PALETTE["stone"],
            Comment.Single:     KAMI_PALETTE["stone"],
            Comment.Multiline:  KAMI_PALETTE["stone"],
            Comment.Preproc:    KAMI_PALETTE["stone"],
            Keyword:            KAMI_PALETTE["brand"],
            Keyword.Constant:   KAMI_PALETTE["brand"],
            Keyword.Namespace:  KAMI_PALETTE["brand"],
            Keyword.Type:       KAMI_PALETTE["brand"],
            Name.Builtin:       KAMI_PALETTE["brand"],
            Name.Function:      KAMI_PALETTE["near_black"],
            Name.Class:         KAMI_PALETTE["near_black"],
            Name.Decorator:     KAMI_PALETTE["olive"],
            String:             KAMI_PALETTE["olive"],
            String.Doc:         KAMI_PALETTE["stone"],
            Number:             KAMI_PALETTE["dark_warm"],
            Number.Float:       KAMI_PALETTE["dark_warm"],
            Number.Integer:     KAMI_PALETTE["dark_warm"],
            Literal:            KAMI_PALETTE["dark_warm"],
            Operator:           "",
            Punctuation:        "",
        }

    return KamiStyle


def _highlight_block(match: re.Match[str]) -> str:
    from pygments import highlight as pyg_highlight
    from pygments.formatters import HtmlFormatter
    from pygments.lexers import get_lexer_by_name

    open_tag = match.group(1)
    language = match.group(2)
    code = match.group(3)
    close_tag = match.group(4)

    code_text = html_mod.unescape(code)

    try:
        lexer = get_lexer_by_name(language, stripall=False)
    except Exception:
        return match.group(0)

    formatter = HtmlFormatter(
        style=_build_kami_style(),
        noclasses=True,
        nowrap=True,
    )

    highlighted = pyg_highlight(code_text, lexer, formatter)
    return f'{open_tag}{highlighted}{close_tag}'


def highlight_code_blocks(html_text: str) -> str:
    """Apply syntax highlighting to language-tagged code blocks.

    Returns HTML unchanged if Pygments is not installed or no
    language-tagged blocks are found.
    """
    try:
        import pygments  # noqa: F401
    except ImportError:
        return html_text

    if not CODE_BLOCK_RE.search(html_text):
        return html_text

    return CODE_BLOCK_RE.sub(_highlight_block, html_text)
