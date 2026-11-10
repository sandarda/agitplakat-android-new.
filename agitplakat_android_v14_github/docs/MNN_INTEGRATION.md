# MNN-Diffusion integration plan

## Confirmed upstream contract

The upstream MNN Diffusion guide documents Stable Diffusion 1.5 as model type `0`,
with low-memory mode `0`, CPU backend `0` or OpenCL backend `3`, and a 10-step demo.
The SD 1.5 package contains `text_encoder.mnn`, `unet.mnn`, `vae_decoder.mnn`, and
`tokenizer.mtok`. The tokenizer requires `MNN_BUILD_LLM=ON`.

Official references:
- https://github.com/alibaba/MNN/blob/master/transformers/diffusion/README.md
- https://github.com/alibaba/MNN/blob/master/docs/transformers/diffusion.md
- https://github.com/alibaba/MNN/tree/master/apps/Android/MnnLlmChat

## Reproducible native build

1. Clone the official Alibaba MNN repository and record its exact commit hash.
2. Set `ANDROID_NDK` to the installed Android NDK directory.
3. Run `scripts/build_mnn_android.sh /path/to/MNN`.
4. Confirm that the build produced the upstream `diffusion_demo` executable. The
   build script now stops if that target is missing.
5. Run `scripts/package_mnn_android.sh /path/to/MNN` to copy only AArch64 ELF shared
   libraries and create a SHA-256 manifest in `jniLibs/arm64-v8a`. The packager
   requires `readelf` and checks every copied library machine type.
6. Inspect ELF dependency closure and exact JNI exports before attempting an APK build.
7. Run the upstream `diffusion_demo` smoke test in a suitable Android/Linux test
   environment with the exact converted SD 1.5 model package.

The script intentionally uses `MNN_SEP_BUILD=OFF` to avoid a separately packaged
OpenCL backend in the first integration pass. If the selected upstream revision
changes option names or build output layout, stop and reconcile against that exact
revision rather than silently continuing.

## JNI integration rule

Do not copy the LLM Chat app's class declarations or guess native signatures. The
Diffusion JNI implementation must be taken from the same MNN revision as the native
libraries. Match Kotlin declarations to the native registrations/symbols exactly,
then add an instrumentation smoke test that initializes a session and generates
one image. An `UnsatisfiedLinkError` issue has been reported for SD 1.5 in MNN Chat;
therefore library loading alone is not acceptance evidence.

## App-side contract

`ImageGenerationEngine` is the stable application boundary. The poster headline,
body copy, labels, and decorative text must never be passed to the image model. The
model receives only the illustration prompt; `PosterRenderer` composes exact text
after inference. Generation stays disabled until the JNI smoke test and a physical
Honor 20 inference test pass.

## Current honest status

- This source archive does not include MNN source code or compiled MNN runtime `.so`.
- The native bridge currently diagnoses only the app's own JNI library.
- `MnnDiffusionEngine.isReady()` remains false and `generate()` deliberately fails
  with an explanatory message.
- Model ZIP validation checks expected filenames and safe extraction, not MNN graph
  compatibility or the exact upstream conversion revision.
- No APK has been built or tested on the target phone yet.


## Build script verification (v1.2.0)

The Android build flags now include `MNN_BUILD_OPENCV=ON`, `MNN_IMGCODECS=ON`,
`MNN_BUILD_LLM=ON`, and the official Diffusion/OpenCL options. The packaging step
checks AArch64 ELF headers and requires the `diffusion_demo` artifact. These checks
prevent packaging an ordinary MNN build as if it contained Diffusion support; they
still do not prove that the application JNI layer is compatible or that inference
works on the target handset.


## JNI implementation added in v1.3.0

The project now contains a small JNI adapter using the public MNN Diffusion C++ API:
`Diffusion::createDiffusion(..., STABLE_DIFFUSION_1_5, backend, memoryMode)`,
`load()`, and `run(prompt, outputPath, steps, seed, callback)`. The implementation
is conditionally compiled only when both `MNN_ROOT` and the packaged
`app/src/main/jniLibs/arm64-v8a/libMNN.so` exist. Without them, the app builds a
diagnostic-only bridge and reports that inference is unavailable.

Build sequence after obtaining a matching MNN checkout and Android NDK:

1. `export ANDROID_NDK=/absolute/path/to/ndk`
2. `./scripts/build_mnn_android.sh /absolute/path/to/MNN`
3. `./scripts/package_mnn_android.sh /absolute/path/to/MNN`
4. Build the app with `-PMNN_ROOT=/absolute/path/to/MNN` (for example,
   `./gradlew assembleDebug -PMNN_ROOT=/absolute/path/to/MNN`).
5. Verify that `libMNN.so` and the other required runtime libraries are packaged,
   install on device, and run a real inference smoke test before enabling generation UI.

The native adapter currently requests OpenCL backend id 3 and low-memory mode 0.
This is a first real SD 1.5 adapter, not a guarantee that OpenCL is available or
that a given converted model package is compatible. CPU fallback selection and
hardware validation remain outstanding. The MNN API's text-to-image overload
does not expose width/height in this wrapper; the output is normalized by the
existing A3 poster renderer after inference.

Upstream source references (Alibaba MNN, Apache-2.0 project):
- `transformers/diffusion/engine/include/diffusion/diffusion.hpp`
- `apps/Android/MnnLlmChat/app/src/main/cpp/diffusion_jni.cpp`
- `apps/Android/MnnLlmChat/app/src/main/cpp/diffusion_session.cpp`
- `apps/Android/MnnLlmChat/app/src/main/cpp/diffusion_session.h`

This project uses the public Diffusion C++ API directly rather than copying the
MnnLlmChat app's Java package or its JNI method names. Upstream MNN Chat has had
reported JNI binding failures; successful library loading alone is not accepted
as proof of inference.


## v1.4 validation fixes

- Corrected the JNI progress callback capture so the boxed Kotlin `Int` can be passed to
  `Function1.invoke(Object)` without referencing uncaptured C++ locals.
- Added `kotlinx-coroutines-android`, required by the app's `Dispatchers.Main`/Android coroutine usage.
- Added `smoke_test_mnn_android.sh` to run upstream `diffusion_demo` directly on a connected
  Android device with the same SD 1.5 model package before debugging the app's JNI layer.
- This smoke test requires an attached device, `adb`, a built ARM64 MNN demo, and a compatible
  model directory. It is not run in this environment.
