#!/usr/bin/env bash
set -Eeuo pipefail

# Package shared objects from a completed official MNN Android ARM64 build.
# Usage: ./scripts/package_mnn_android.sh /path/to/MNN
MNN_SRC="${1:-${MNN_SRC:-}}"
if [[ -z "$MNN_SRC" || ! -d "$MNN_SRC/project/android/build_64" ]]; then
  echo "ERROR: pass the MNN root containing project/android/build_64." >&2
  exit 2
fi
command -v readelf >/dev/null || { echo "ERROR: readelf is required for ABI validation." >&2; exit 2; }
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$(cd "$MNN_SRC/project/android/build_64" && pwd)"
DEST="$ROOT/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$DEST"

# Remove only previous packaged MNN libraries/manifest; keep README and other assets.
find "$DEST" -maxdepth 1 -type f -name 'lib*.so' -delete
rm -f "$DEST/MNN_BUILD_MANIFEST.txt"
declare -A SEEN=()
COUNT=0
while IFS= read -r -d '' lib; do
  name="$(basename "$lib")"
  machine="$(readelf -h "$lib" | awk -F: '/Machine:/ {sub(/^ +/, "", $2); print $2}')"
  if [[ "$machine" != *AArch64* ]]; then
    echo "ERROR: $lib is not an AArch64 ELF (reported: $machine)." >&2
    exit 3
  fi
  if [[ -n "${SEEN[$name]:-}" ]]; then
    if ! cmp -s "$lib" "${SEEN[$name]}"; then
      echo "ERROR: conflicting copies of $name found:" >&2
      echo "  ${SEEN[$name]}" >&2
      echo "  $lib" >&2
      exit 4
    fi
    continue
  fi
  SEEN[$name]="$lib"
  cp "$lib" "$DEST/$name"
  COUNT=$((COUNT + 1))
done < <(find "$BUILD_DIR" -type f -name '*.so' -print0 | sort -z)

if [[ $COUNT -eq 0 ]]; then
  echo "ERROR: no .so files found; refusing to create an empty runtime package." >&2
  exit 5
fi
DEMO="$(find "$BUILD_DIR" -type f -name diffusion_demo -perm -u+x -print -quit)"
if [[ -z "$DEMO" ]]; then
  echo "ERROR: diffusion_demo missing; build is not accepted as a Diffusion build." >&2
  exit 6
fi
REVISION="$(git -C "$MNN_SRC" rev-parse HEAD 2>/dev/null || echo unknown)"
{
  echo "MNN revision: $REVISION"
  echo "ABI: arm64-v8a (each ELF verified as AArch64 with readelf)"
  echo "Diffusion demo: $DEMO"
  echo "Libraries copied: $COUNT"
  echo
  (cd "$DEST" && sha256sum ./*.so | sort)
} > "$DEST/MNN_BUILD_MANIFEST.txt"
echo "Packaged $COUNT AArch64 libraries into $DEST"
echo "Manifest: $DEST/MNN_BUILD_MANIFEST.txt"
echo "NOTE: this verifies ABI and build artifacts, not Kotlin/JNI API compatibility or successful inference."
