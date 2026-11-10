#!/usr/bin/env bash
set -Eeuo pipefail

# Build the official MNN Android ARM64 runtime with SD 1.5 Diffusion support.
# Usage: ANDROID_NDK=/path/to/ndk ./scripts/build_mnn_android.sh /path/to/MNN
MNN_SRC="${1:-${MNN_SRC:-}}"

if [[ -z "${ANDROID_NDK:-}" || ! -d "$ANDROID_NDK" ]]; then
  echo "ERROR: set ANDROID_NDK to an installed Android NDK directory." >&2
  exit 2
fi
if [[ -z "$MNN_SRC" || ! -d "$MNN_SRC/project/android" ]]; then
  echo "ERROR: pass the root directory of a complete Alibaba MNN checkout." >&2
  exit 2
fi
if [[ ! -f "$MNN_SRC/project/android/build_64.sh" ]]; then
  echo "ERROR: upstream project/android/build_64.sh is missing." >&2
  exit 2
fi

MNN_SRC="$(cd "$MNN_SRC" && pwd)"
REVISION="$(git -C "$MNN_SRC" rev-parse HEAD 2>/dev/null || echo unknown)"
BUILD_DIR="$MNN_SRC/project/android/build_64"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# These options follow the upstream MNN Diffusion Android documentation.
# MNN_BUILD_LLM is required for tokenizer.mtok. OpenCL is enabled, while
# SEP_BUILD=OFF keeps the first integration pass in a simpler shared-library set.
"$MNN_SRC/project/android/build_64.sh" \
  -DMNN_LOW_MEMORY=ON \
  -DMNN_BUILD_DIFFUSION=ON \
  -DMNN_BUILD_LLM=ON \
  -DMNN_BUILD_OPENCV=ON \
  -DMNN_IMGCODECS=ON \
  -DMNN_OPENCL=ON \
  -DMNN_SEP_BUILD=OFF \
  -DMNN_SUPPORT_TRANSFORMER_FUSE=ON

mapfile -t LIBS < <(find "$BUILD_DIR" -type f -name '*.so' -print | sort)
if [[ ${#LIBS[@]} -eq 0 ]]; then
  echo "ERROR: build finished but no shared libraries were found under $BUILD_DIR" >&2
  exit 3
fi
DEMO="$(find "$BUILD_DIR" -type f -name diffusion_demo -perm -u+x -print -quit)"
if [[ -z "$DEMO" ]]; then
  echo "ERROR: diffusion_demo was not produced; do not package this build as Diffusion-ready." >&2
  exit 4
fi
printf 'MNN revision: %s\n' "$REVISION"
printf 'Diffusion demo: %s\n' "$DEMO"
printf 'Shared libraries found: %s\n' "${#LIBS[@]}"
printf '%s\n' "${LIBS[@]}"
echo "NEXT: run scripts/package_mnn_android.sh $MNN_SRC"
