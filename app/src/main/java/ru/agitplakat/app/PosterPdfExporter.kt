package ru.agitplakat.app

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Writes a single-page A3 portrait PDF. PDF page units are points (1/72 inch).
 * PosterSpec is 297x420 mm; 120 DPI bitmap is embedded without resampling.
 */
object PosterPdfExporter {
    private const val PAGE_WIDTH_PT = 841.89
    private const val PAGE_HEIGHT_PT = 1190.55

    fun exportA3(bitmap: Bitmap, destination: File) {
        require(bitmap.width == PosterSpec.WIDTH_PX && bitmap.height == PosterSpec.HEIGHT_PX) {
            "Ожидается плакат А3 1403×1984 px при 120 DPI"
        }
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH_PT.toInt(), PAGE_HEIGHT_PT.toInt(), 1
            ).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(
                bitmap,
                null,
                Rect(0, 0, pageInfo.pageWidth, pageInfo.pageHeight),
                null
            )
            document.finishPage(page)
            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }
}
