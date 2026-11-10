# v1.4.0

- Fixed JNI progress callback capture in C++.
- Added Android coroutine runtime dependency.
- Added an adb-based smoke test for the upstream MNN `diffusion_demo` on a real device.
- No APK or inference test is claimed: this environment has no Android SDK/NDK, built MNN runtime, model weights, or connected Honor 20.
