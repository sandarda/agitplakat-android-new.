package ru.agitplakat.app

import android.graphics.Bitmap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32

/**
 * Exports a print-ready PNG and writes PNG pHYs metadata for 120 DPI.
 * Pixel dimensions remain exactly 1403x1984; DPI metadata tells print software
 * the intended physical scale. PNG stores pixel density in pixels per metre.
 */
object PosterPngExporter {
    private const val PIXELS_PER_METRE_120_DPI = 4724

    fun exportA3(bitmap: Bitmap, destination: File) {
        require(bitmap.width == PosterSpec.WIDTH_PX && bitmap.height == PosterSpec.HEIGHT_PX) {
            "Ожидается плакат А3 1403×1984 px при 120 DPI"
        }
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile ?: destination.absoluteFile.parentFile, "poster-png.tmp")

        try {
            FileOutputStream(temporary).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Не удалось закодировать PNG"
                }
            }

            // Insert pHYs immediately after IHDR, as required by the PNG chunk order.
            val physChunk = createPhysChunk(PIXELS_PER_METRE_120_DPI)
            BufferedInputStream(FileInputStream(temporary)).use { input ->
                BufferedOutputStream(FileOutputStream(destination)).use { output ->
                    val header = ByteArray(33) // signature (8) + IHDR chunk (25)
                    val read = input.read(header)
                    require(read == header.size && header.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)) {
                        "Получен некорректный PNG"
                    }
                    output.write(header)
                    output.write(physChunk)
                    input.copyTo(output)
                    output.flush()
                }
            }
        } finally {
            temporary.delete()
        }
    }

    private fun createPhysChunk(pixelsPerMetre: Int): ByteArray {
        val type = byteArrayOf('p'.code.toByte(), 'H'.code.toByte(), 'Y'.code.toByte(), 's'.code.toByte())
        val data = java.nio.ByteBuffer.allocate(9)
            .putInt(pixelsPerMetre)
            .putInt(pixelsPerMetre)
            .put(1) // unit = metre
            .array()
        val crc = CRC32().apply {
            update(type)
            update(data)
        }.value
        return java.nio.ByteBuffer.allocate(4 + 4 + data.size + 4)
            .putInt(data.size)
            .put(type)
            .put(data)
            .putInt(crc.toInt())
            .array()
    }

    private val PNG_SIGNATURE = byteArrayOf(
        137.toByte(), 80, 78, 71, 13, 10, 26, 10
    )
}
