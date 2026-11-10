#!/usr/bin/env bash
set -Eeuo pipefail
# Runs the upstream diffusion_demo on an Android device via adb.
# Usage: ./scripts/smoke_test_mnn_android.sh /path/to/MNN
MNN_SRC="${1:-${MNN_SRC:-}}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODEL="${2:-}"
ADB="${ADB:-adb}"
[[ -n "$MNN_SRC" && -d "$MNN_SRC/project/android/build_64" ]] || { echo "Pass a completed MNN checkout." >&2; exit 2; }
[[ -n "$MODEL" && -d "$MODEL" ]] || { echo "Pass a local SD1.5 model directory." >&2; exit 2; }
command -v "$ADB" >/dev/null || { echo "adb is required." >&2; exit 2; }
DEMO="$(find "$MNN_SRC/project/android/build_64" -type f -name diffusion_demo -perm -u+x -print -quit)"
[[ -n "$DEMO" ]] || { echo "diffusion_demo not found; MNN Diffusion build incomplete." >&2; exit 3; }
"$ADB" get-state >/dev/null
"$ADB" shell mkdir -p /data/local/tmp/agitplakat/model /data/local/tmp/agitplakat/lib
"$ADB" push "$DEMO" /data/local/tmp/agitplakat/diffusion_demo >/dev/null
"$ADB" push "$MODEL/." /data/local/tmp/agitplakat/model/ >/dev/null
while IFS= read -r -d '' lib; do
  "$ADB" push "$lib" /data/local/tmp/agitplakat/lib/ >/dev/null
done < <(find "$MNN_SRC/project/android/build_64" -type f -name '*.so' -print0)
"$ADB" shell chmod 755 /data/local/tmp/agitplakat/diffusion_demo
"$ADB" shell 'cd /data/local/tmp/agitplakat && LD_LIBRARY_PATH=/data/local/tmp/agitplakat/lib ./diffusion_demo model 0 0 3 10 42 /data/local/tmp/agitplakat/smoke.png "vintage Soviet poster illustration, detailed worker portrait, no text"'
"$ADB" pull /data/local/tmp/agitplakat/smoke.png "$ROOT/mnn_smoke_test.png"
echo "If mnn_smoke_test.png exists and is a valid image, the upstream demo passed on this device."
