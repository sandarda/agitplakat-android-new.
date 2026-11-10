# Device validation checklist — Honor 20 / Kirin 980

This checklist is deliberately separate from a successful compile. A build is not
considered device-ready until each applicable test has passed on the physical phone.

## Before installing

- [ ] Build MNN from one recorded upstream commit with `MNN_BUILD_DIFFUSION=ON`,
      `MNN_BUILD_LLM=ON`, `MNN_OPENCL=ON`, and `MNN_SEP_BUILD=OFF`.
- [ ] Confirm every packaged `.so` is ELF `AArch64` (`readelf -h`).
- [ ] Confirm the native Diffusion JNI wrapper methods in Kotlin have matching
      exported/registered native implementations in the packaged libraries.
- [ ] Confirm the model pack was converted for the same MNN API/revision.
- [ ] Build and install the debug APK; capture `adb logcat` from cold start.

## Runtime tests

- [ ] App opens with no `UnsatisfiedLinkError` or missing-library errors.
- [ ] Model files load; report the actual backend selected (OpenCL or CPU fallback).
- [ ] Generate one 256×384 test image at the configured low-memory profile.
- [ ] Repeat generation three times; verify no OOM, crash, or corrupted output.
- [ ] Record wall-clock generation time, peak memory, battery drain, and device temperature.
- [ ] Disable/re-enable OpenCL or force CPU fallback and verify the app reports the change.
- [ ] Cancel/leave generation safely; reopen app and confirm recovery.

## Poster pipeline tests

- [ ] Render a supplied illustration with a long Cyrillic headline.
- [ ] Verify every letter, punctuation mark, and line break in the exported PNG.
- [ ] Verify PNG dimensions are 1403×1984 and DPI metadata is 120.
- [ ] Verify PDF page is exactly 297×420 mm, portrait.
- [ ] Reopen both exported files on another device/viewer.

## Acceptance rule

Do not mark generation as ready merely because the APK builds, the MNN `.so` files
load, or the four model files exist. Readiness requires a successful image inference
on the Honor 20 and a verified poster export. If OpenCL is unavailable, a tested CPU
fallback may be offered, but performance must be measured before presenting it as
usable on this device.
