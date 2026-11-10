package ru.agitplakat.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class ModelInstallResult(
    val directory: File,
    val sha256: String,
    val bytes: Long,
    val components: List<String>
)

/**
 * Безопасно распаковывает ZIP-пакет SD 1.5 в приватное хранилище.
 * Проверяет обязательные файлы и SHA-256 архива. Это НЕ проверка бинарной
 * совместимости MNN и не включает генерацию до подключения runtime.
 */
class LocalModelStore(private val context: Context) {
    private val modelDir = File(context.filesDir, "models").apply { mkdirs() }
    private val installedDir get() = File(modelDir, "sd15")

    fun installedModel(): File? =
        installedDir.takeIf { it.isDirectory && findRequiredFiles(it).size == ModelPackContract.REQUIRED_FILES.size }

    fun installedComponents(): List<String> =
        installedModel()?.let { findRequiredFiles(it).keys.sorted() } ?: emptyList()

    fun importPack(uri: Uri, expectedSha256: String? = null): ModelInstallResult {
        val archive = File.createTempFile("model-", ".zip", modelDir)
        val staging = File(modelDir, "sd15.staging")
        val backup = File(modelDir, "sd15.previous")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Не удалось открыть ZIP-пакет модели" }
                archive.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= ModelPackContract.MAX_ARCHIVE_BYTES) {
                            "Архив превышает допустимый размер 8 ГБ"
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(archive.length() > 0L) { "Выбранный ZIP-архив пуст" }

            val hash = sha256(archive)
            if (expectedSha256 != null) {
                require(hash.equals(expectedSha256.trim(), ignoreCase = true)) {
                    "Контрольная сумма SHA-256 не совпадает"
                }
            }

            deleteRecursively(staging)
            check(staging.mkdirs()) { "Не удалось создать временный каталог модели" }
            unpackSafely(archive, staging)

            val components = findRequiredFiles(staging)
            val missing = ModelPackContract.REQUIRED_FILES.filterNot { components.containsKey(it) }
            require(missing.isEmpty()) {
                "В архиве не хватает компонентов: ${missing.joinToString()}"
            }

            // Убираем посторонние файлы? Нет: сохраняем структуру архива для runtime.
            deleteRecursively(backup)
            if (installedDir.exists()) {
                check(installedDir.renameTo(backup)) { "Не удалось сохранить предыдущую модель" }
            }
            if (!staging.renameTo(installedDir)) {
                if (backup.exists()) backup.renameTo(installedDir)
                error("Не удалось установить пакет модели")
            }
            deleteRecursively(backup)

            val finalComponents = findRequiredFiles(installedDir)
            val unpackedBytes = installedDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            return ModelInstallResult(installedDir, hash, unpackedBytes, finalComponents.keys.sorted())
        } finally {
            archive.delete()
            deleteRecursively(staging)
        }
    }

    fun deleteModel(): Boolean = deleteRecursively(installedDir)

    private fun unpackSafely(archive: File, destination: File) {
        var entries = 0
        var totalBytes = 0L
        val canonicalRoot = destination.canonicalFile
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                require(entries <= ModelPackContract.MAX_ENTRIES) { "В архиве слишком много файлов" }

                val normalized = entry.name.replace('\\', '/')
                require(normalized.isNotBlank() && !normalized.startsWith("/") &&
                    !normalized.split('/').any { it == ".." }) {
                    "В архиве обнаружен небезопасный путь"
                }
                val out = File(destination, normalized).canonicalFile
                require(out.toPath().startsWith(canonicalRoot.toPath())) {
                    "Путь файла выходит за пределы каталога модели"
                }

                if (entry.isDirectory) {
                    check(out.isDirectory || out.mkdirs()) { "Не удалось создать каталог модели" }
                } else {
                    check(out.parentFile?.isDirectory == true || out.parentFile?.mkdirs() == true) {
                        "Не удалось создать каталог компонента модели"
                    }
                    out.outputStream().buffered().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            require(totalBytes <= ModelPackContract.MAX_UNPACKED_BYTES) {
                                "Распакованный пакет превышает 8 ГБ"
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun findRequiredFiles(root: File): Map<String, File> {
        val found = mutableMapOf<String, File>()
        root.walkTopDown().filter { it.isFile }.forEach { file ->
            if (file.name in ModelPackContract.REQUIRED_FILES && file.length() > 0L) {
                found[file.name] = file
            }
        }
        return found
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun deleteRecursively(file: File): Boolean =
        !file.exists() || file.deleteRecursively()
}
