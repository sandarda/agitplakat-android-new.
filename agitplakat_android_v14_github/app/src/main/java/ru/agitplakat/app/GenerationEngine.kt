package ru.agitplakat.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface ImageGenerationEngine {
    suspend fun generate(
        prompt: String,
        negativePrompt: String,
        seed: Long,
        steps: Int,
        width: Int,
        height: Int,
        onProgress: (Int) -> Unit
    ): Bitmap
    fun isReady(): Boolean
}

/**
 * SD 1.5 on MNN. The current upstream text-to-image API writes a file and
 * accepts prompt/steps/seed; image dimensions are determined by that model build.
 * PosterRenderer performs the final A3 crop/scale and exact text composition.
 */
class MnnDiffusionEngine(context: Context) : ImageGenerationEngine {
    private val appContext = context.applicationContext
    private val modelStore = LocalModelStore(appContext)
    private val outputDir = File(appContext.cacheDir, "mnn-output").apply { mkdirs() }

    override fun isReady(): Boolean =
        MnnDiffusionNative.isAvailable() && modelStore.installedModel() != null

    override suspend fun generate(
        prompt: String,
        negativePrompt: String,
        seed: Long,
        steps: Int,
        width: Int,
        height: Int,
        onProgress: (Int) -> Unit
    ): Bitmap = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "Опишите сюжет плаката" }
        check(isReady()) { MnnDiffusionNative.status() }
        val modelDir = requireNotNull(modelStore.installedModel())
        val out = File(outputDir, "poster-${System.currentTimeMillis()}.png")
        try {
            MnnDiffusionNative.generate(
                modelDir.absolutePath, prompt, out.absolutePath,
                steps.coerceIn(1, 30), seed.toInt()
            ) { value -> onProgress(value.coerceIn(0, 100)) }
            check(out.isFile && out.length() > 0L) { "MNN не создал выходное изображение" }
            BitmapFactory.decodeFile(out.absolutePath)
                ?: error("Не удалось прочитать изображение, созданное MNN")
        } finally {
            out.delete()
        }
    }
}

/** JNI calls are declared against the matching native C++ implementation in this project. */
private object MnnDiffusionNative {
    private val loaded: Boolean
    private val loadFailure: String?
    init {
        var ok = false
        var failure: String? = null
        try {
            System.loadLibrary("agitplakat_native")
            ok = true
        } catch (e: Throwable) {
            failure = e.message ?: e.javaClass.simpleName
        }
        loaded = ok
        loadFailure = failure
    }

    fun isAvailable(): Boolean = loaded && nativeIsMnnLinked()
    fun status(): String = when {
        !loaded -> "Не удалось загрузить JNI: ${loadFailure ?: "ошибка"}"
        !nativeIsMnnLinked() -> "MNN runtime не подключён. Соберите библиотеки и JNI с MNN_ROOT."
        else -> "MNN JNI загружен; проверьте совместимость пакета SD 1.5."
    }

    fun generate(
        modelPath: String, prompt: String, outputPath: String,
        steps: Int, seed: Int, onProgress: (Int) -> Unit
    ) {
        check(isAvailable()) { status() }
        val handle = nativeCreate(modelPath, 0, 3)
        check(handle != 0L) { "Не удалось создать MNN Diffusion session" }
        try {
            check(nativeGenerate(handle, prompt, outputPath, steps, seed, onProgress)) {
                "MNN Diffusion вернул ошибку генерации"
            }
        } finally {
            nativeRelease(handle)
        }
    }

    private external fun nativeIsMnnLinked(): Boolean
    private external fun nativeCreate(modelPath: String, memoryMode: Int, backend: Int): Long
    private external fun nativeGenerate(
        handle: Long, prompt: String, outputPath: String, steps: Int, seed: Int,
        callback: (Int) -> Unit
    ): Boolean
    private external fun nativeRelease(handle: Long)
}
