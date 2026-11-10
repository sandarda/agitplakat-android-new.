#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MNN_ROOT="${1:-${MNN_ROOT:-}}"
LIB="$ROOT/app/src/main/jniLibs/arm64-v8a/libMNN.so"
HEADER="${MNN_ROOT:+$MNN_ROOT/transformers/diffusion/engine/include/diffusion/diffusion.hpp}"
[[ -n "$MNN_ROOT" && -f "$HEADER" ]] || { echo "Missing matching MNN diffusion.hpp; set MNN_ROOT." >&2; exit 2; }
[[ -f "$LIB" ]] || { echo "Missing packaged libMNN.so; run package_mnn_android.sh." >&2; exit 3; }
command -v readelf >/dev/null || { echo "readelf is required." >&2; exit 4; }
readelf -h "$LIB" | grep -q AArch64 || { echo "libMNN.so is not AArch64." >&2; exit 5; }
echo "MNN header and AArch64 libMNN.so are present. This is a pre-build check, not an inference test."
