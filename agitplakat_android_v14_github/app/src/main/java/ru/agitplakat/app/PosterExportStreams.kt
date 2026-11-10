package ru.agitplakat.app

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.util.zip.CRC32

/** Stream-based exporters used by Android's system save-file dialog. */
object PosterExportStreams {
    private const val PPM_120_DPI = 4724
    private val PNG_SIGNATURE = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)

    fun png(bitmap: Bitmap, output: OutputStream) {
        validate(bitmap)
        val temp = java.io.ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, temp)) { "Ошибка кодирования PNG" }
        val bytes = temp.toByteArray()
        require(bytes.size >= 33 && bytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)) {
            "Некорректный PNG"
        }
        output.write(bytes, 0, 33)
        output.write(physChunk(PPM_120_DPI))
        output.write(bytes, 33, bytes.size - 33)
        output.flush()
    }

    fun pdf(bitmap: Bitmap, output: OutputStream) {
        validate(bitmap)
        val document = PdfDocument()
        try {
            val info = PdfDocument.PageInfo.Builder(842, 1191, 1).create()
            val page = document.startPage(info)
            page.canvas.drawBitmap(bitmap, null, Rect(0, 0, info.pageWidth, info.pageHeight), null)
            document.finishPage(page)
            document.writeTo(output)
            output.flush()
        } finally {
            document.close()
        }
    }

    private fun validate(bitmap: Bitmap) {
        require(bitmap.width == PosterSpec.WIDTH_PX && bitmap.height == PosterSpec.HEIGHT_PX) {
            "Ожидается А3: 1403×1984 px при 120 DPI"
        }
    }

    private fun physChunk(ppm: Int): ByteArray {
        val type = byteArrayOf(112, 72, 89, 115)
        val data = java.nio.ByteBuffer.allocate(9).putInt(ppm).putInt(ppm).put(1).array()
        val crc = CRC32().apply { update(type); update(data) }.value
        return java.nio.ByteBuffer.allocate(21).putInt(9).put(type).put(data).putInt(crc.toInt()).array()
    }
}
