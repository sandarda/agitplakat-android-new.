package ru.agitplakat.app

/**
 * Проверяет только загрузку нашего JNI-моста.
 * Успешная загрузка НЕ означает, что MNN или модель доступны.
 */
object MnnNativeBridge {
    private val loaded: Boolean
    private val loadError: String?

    init {
        var ok = false
        var error: String? = null
        try {
            System.loadLibrary("agitplakat_native")
            ok = true
        } catch (e: UnsatisfiedLinkError) {
            error = e.message ?: "Не удалось загрузить JNI-библиотеку"
        } catch (e: SecurityException) {
            error = e.message ?: "Загрузка JNI запрещена системой"
        }
        loaded = ok
        loadError = error
    }

    private external fun nativeRuntimeStatus(): String
    private external fun nativeAbiVersion(): Int

    fun diagnostic(): String {
        if (!loaded) return "JNI недоступен: ${loadError ?: "неизвестная ошибка"}"
        return try {
            "${nativeRuntimeStatus()} (ABI bridge ${nativeAbiVersion()})"
        } catch (e: UnsatisfiedLinkError) {
            "JNI загружен частично: ${e.message ?: "нет native-метода"}"
        }
    }

    fun isBridgeLoaded(): Boolean = loaded
}
