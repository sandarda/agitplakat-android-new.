# v1.3.0

- Added a real MNN Diffusion C++ JNI adapter based on the public Stable Diffusion 1.5 API.
- Added conditional CMake linking to a matching MNN checkout and packaged `libMNN.so`.
- Added a Kotlin engine wrapper that runs inference on Dispatchers.IO and reads the generated image.
- Kept generation unavailable unless the native MNN runtime and model package are present.
- Important: this version is not yet built or tested; the local environment lacks Android NDK/SDK and MNN binaries.
