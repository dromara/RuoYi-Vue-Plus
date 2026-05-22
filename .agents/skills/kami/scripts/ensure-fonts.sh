#!/usr/bin/env bash
set -euo pipefail

# Portable across bash 3.2+ (macOS stock /bin/bash) and bash 4+ (Linux, Homebrew).
# Avoids `declare -A` so the script runs on a fresh macOS without `brew install bash`.

FONT_DIR="$(cd "$(dirname "$0")/../assets/fonts" && pwd)"
MIN_SIZE=10000000  # 10MB, prevents truncated downloads

# Parallel arrays: index N pairs CN_NAMES[N] with LOCAL_NAMES[N].
CN_NAMES=("仓耳今楷02-W04.ttf" "仓耳今楷02-W05.ttf")
LOCAL_NAMES=("TsangerJinKai02-W04.ttf" "TsangerJinKai02-W05.ttf")

# Mirror order is intentionally jsdmirror-first here, opposite of the
# templates' @font-face fallback (which lists jsdelivr first). Reasoning:
# this script runs interactively when fonts are missing locally, often from
# China where jsdmirror is reachable and faster than jsdelivr; templates run
# anywhere and prioritize jsdelivr's broader global coverage.
MIRROR_SOURCES=(
  "https://cdn.jsdmirror.com/gh/tw93/Kami@main/assets/fonts"
  "https://cdn.jsdelivr.net/gh/tw93/Kami@main/assets/fonts"
)

check_size() {
  local file="$1"
  [[ -f "$file" ]] || return 1
  local size
  size=$(wc -c < "$file" | tr -d ' ')
  [[ "$size" -ge "$MIN_SIZE" ]]
}

download_font() {
  local cn_name="$1"
  local local_name="$2"
  local target="$FONT_DIR/$local_name"

  # Source 1: official tsanger.cn
  local official_url="https://tsanger.cn/download/${cn_name}"
  echo "  Trying: tsanger.cn (official)"
  if curl --retry 2 --connect-timeout 15 --max-time 300 -fSL "$official_url" -o "$target.tmp" 2>/dev/null; then
    if check_size "$target.tmp"; then
      mv "$target.tmp" "$target"
      echo "  OK: $local_name downloaded ($(du -h "$target" | cut -f1))"
      return 0
    else
      rm -f "$target.tmp"
    fi
  else
    rm -f "$target.tmp"
  fi

  # Source 2+: CDN mirrors (already named TsangerJinKai02-W0x.ttf)
  for src in "${MIRROR_SOURCES[@]}"; do
    local url="$src/$local_name"
    echo "  Trying: $url"
    if curl --retry 2 --connect-timeout 15 --max-time 300 -fSL "$url" -o "$target.tmp" 2>/dev/null; then
      if check_size "$target.tmp"; then
        mv "$target.tmp" "$target"
        echo "  OK: $local_name downloaded ($(du -h "$target" | cut -f1))"
        return 0
      else
        rm -f "$target.tmp"
      fi
    else
      rm -f "$target.tmp"
    fi
  done

  echo "  ERROR: all sources failed for $local_name"
  return 1
}

mkdir -p "$FONT_DIR"

all_present=true
for local_name in "${LOCAL_NAMES[@]}"; do
  if ! check_size "$FONT_DIR/$local_name"; then
    all_present=false
    break
  fi
done

if $all_present; then
  echo "OK: TsangerJinKai fonts present"
  exit 0
fi

echo "Downloading TsangerJinKai fonts..."
failed=0
for i in "${!CN_NAMES[@]}"; do
  cn_name="${CN_NAMES[$i]}"
  local_name="${LOCAL_NAMES[$i]}"
  if check_size "$FONT_DIR/$local_name"; then
    echo "  OK: $local_name already present"
    continue
  fi
  if ! download_font "$cn_name" "$local_name"; then
    failed=$((failed + 1))
  fi
done

if [[ "$failed" -gt 0 ]]; then
  echo ""
  echo "Some fonts could not be downloaded. Alternatives:"
  echo "  1. Install Source Han Serif SC: brew install --cask font-source-han-serif-sc"
  echo "  2. Copy TsangerJinKai02-W04.ttf and W05.ttf manually into $FONT_DIR"
  exit 1
fi

echo "OK: all fonts ready"
