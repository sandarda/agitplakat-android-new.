package ru.agitplakat.app

import android.content.Context
import java.io.File

/** Local-only poster collection. Stores exported PNG files in app-private storage. */
class PosterHistoryStore(context: Context) {
    private val directory = File(context.filesDir, "posters").apply { mkdirs() }

    fun savePng(name: String, bytes: ByteArray): File {
        val safeName = name.replace(Regex("[^A-Za-zА-Яа-я0-9_-]"), "_")
            .trim('_').ifBlank { "poster" }.take(48)
        val file = File(directory, "$safeName-${System.currentTimeMillis()}.png")
        file.writeBytes(bytes)
        return file
    }

    fun list(): List<File> =
        directory.listFiles { file -> file.isFile && file.extension.equals("png", true) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun delete(file: File): Boolean {
        require(file.parentFile == directory) { "Можно удалять только файлы коллекции приложения" }
        return file.delete()
    }
}
