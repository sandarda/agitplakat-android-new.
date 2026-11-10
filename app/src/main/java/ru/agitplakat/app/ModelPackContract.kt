package ru.agitplakat.app

/**
 * Ожидаемая структура локального пакета Stable Diffusion 1.5 для MNN-Diffusion.
 * Это контракт проверки архива, а не гарантия совместимости конкретных весов.
 */
object ModelPackContract {
    const val ENGINE = "MNN-Diffusion"
    const val MODEL_FAMILY = "Stable Diffusion 1.5"
    const val STATUS = "BACKEND_PENDING"
    const val MAX_ARCHIVE_BYTES = 8L * 1024 * 1024 * 1024
    const val MAX_UNPACKED_BYTES = 8L * 1024 * 1024 * 1024
    const val MAX_ENTRIES = 20_000

    val REQUIRED_FILES = listOf(
        "text_encoder.mnn",
        "unet.mnn",
        "vae_decoder.mnn",
        "tokenizer.mtok"
    )

    const val NOTE =
        "Проверка структуры не подтверждает совместимость весов с версией MNN runtime."
}
