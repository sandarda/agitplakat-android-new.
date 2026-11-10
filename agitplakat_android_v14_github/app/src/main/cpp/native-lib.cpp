#include <jni.h>
#include <string>
#include <memory>
#include <exception>

#ifdef AGITPLAKAT_WITH_MNN
#include "diffusion/diffusion.hpp"
using MNN::DIFFUSION::Diffusion;
using MNN::DIFFUSION::DiffusionModelType;
#endif

extern "C" JNIEXPORT jboolean JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeIsMnnLinked(JNIEnv*, jobject) {
#ifdef AGITPLAKAT_WITH_MNN
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

#ifdef AGITPLAKAT_WITH_MNN
struct DiffusionHandle {
    std::unique_ptr<Diffusion> engine;
};

extern "C" JNIEXPORT jlong JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeCreate(
        JNIEnv* env, jobject, jstring modelPath, jint memoryMode, jint backend) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    try {
        auto handle = std::make_unique<DiffusionHandle>();
        handle->engine.reset(Diffusion::createDiffusion(
            std::string(path),
            DiffusionModelType::STABLE_DIFFUSION_1_5,
            static_cast<MNNForwardType>(backend),
            memoryMode
        ));
        env->ReleaseStringUTFChars(modelPath, path);
        if (!handle->engine || !handle->engine->load()) return 0;
        return reinterpret_cast<jlong>(handle.release());
    } catch (...) {
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeGenerate(
        JNIEnv* env, jobject, jlong ptr, jstring prompt, jstring outputPath,
        jint steps, jint seed, jobject callback) {
    auto* handle = reinterpret_cast<DiffusionHandle*>(ptr);
    if (!handle || !handle->engine) return JNI_FALSE;
    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    const char* outputChars = env->GetStringUTFChars(outputPath, nullptr);
    jclass cls = env->GetObjectClass(callback);
    // Kotlin function type (Int) -> Unit is Function1<Object, Object> at JNI level.
    jmethodID method = env->GetMethodID(cls, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID valueOf = env->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");
    bool ok = false;
    try {
        ok = handle->engine->run(
            std::string(promptChars), std::string(outputChars), steps, seed,
            [env, callback, method, integerClass, valueOf](int progress) {
                if (callback && method && valueOf) {
                    jobject boxed = env->CallStaticObjectMethod(integerClass, valueOf, progress);
                    jobject ignored = env->CallObjectMethod(callback, method, boxed);
                    if (ignored) env->DeleteLocalRef(ignored);
                    if (boxed) env->DeleteLocalRef(boxed);
                }
            }
        );
    } catch (...) {
        ok = false;
    }
    env->ReleaseStringUTFChars(prompt, promptChars);
    env->ReleaseStringUTFChars(outputPath, outputChars);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeRelease(JNIEnv*, jobject, jlong ptr) {
    delete reinterpret_cast<DiffusionHandle*>(ptr);
}
#else
extern "C" JNIEXPORT jlong JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeCreate(JNIEnv*, jobject, jstring, jint, jint) {
    return 0;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeGenerate(JNIEnv*, jobject, jlong, jstring, jstring, jint, jint, jobject) {
    return JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_ru_agitplakat_app_MnnDiffusionNative_nativeRelease(JNIEnv*, jobject, jlong) {}
#endif
