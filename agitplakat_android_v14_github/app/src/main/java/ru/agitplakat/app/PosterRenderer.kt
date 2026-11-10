package ru.agitplakat.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/** Print canvas: A3 portrait at 120 DPI (rounded to whole pixels). */
object PosterSpec {
    const val WIDTH_PX = 1403
    const val HEIGHT_PX = 1984
    const val DPI = 120
    const val WIDTH_MM = 297
    const val HEIGHT_MM = 420
}

/**
 * Composes the AI-generated artwork with user-supplied Cyrillic text.
 * The image model must receive no lettering prompt; all exact wording is drawn here.
 */
object PosterRenderer {
    fun render(baseArtwork: Bitmap, headline: String): Bitmap {
        val output = Bitmap.createBitmap(
            PosterSpec.WIDTH_PX,
            PosterSpec.HEIGHT_PX,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val src = centerCropRect(baseArtwork.width, baseArtwork.height)
        val dst = Rect(0, 0, PosterSpec.WIDTH_PX, PosterSpec.HEIGHT_PX)
        canvas.drawBitmap(baseArtwork, src, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

        // A restrained paper strip keeps lettering legible without asking the model to draw text.
        val stripHeight = PosterSpec.HEIGHT_PX * 0.23f
        val strip = RectF(
            PosterSpec.WIDTH_PX * 0.045f,
            PosterSpec.HEIGHT_PX - stripHeight - PosterSpec.HEIGHT_PX * 0.035f,
            PosterSpec.WIDTH_PX * 0.955f,
            PosterSpec.HEIGHT_PX - PosterSpec.HEIGHT_PX * 0.035f
        )
        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(242, 226, 194) }
        canvas.drawRect(strip, paperPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(164, 43, 34)
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        }
        drawWrappedText(canvas, headline.trim(), strip, textPaint)
        return output
    }

    private fun centerCropRect(width: Int, height: Int): Rect {
        val targetRatio = PosterSpec.WIDTH_PX.toFloat() / PosterSpec.HEIGHT_PX
        val sourceRatio = width.toFloat() / height
        return if (sourceRatio > targetRatio) {
            val cropWidth = (height * targetRatio).toInt().coerceAtLeast(1)
            val left = (width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, height)
        } else {
            val cropHeight = (width / targetRatio).toInt().coerceAtLeast(1)
            val top = (height - cropHeight) / 2
            Rect(0, top, width, top + cropHeight)
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, bounds: RectF, paint: Paint) {
        if (text.isBlank()) return
        val maxWidth = bounds.width() * 0.92f
        var textSize = bounds.height() * 0.30f
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        var lines: List<String>
        while (true) {
            paint.textSize = textSize
            val result = mutableListOf<String>()
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth || line.isEmpty()) line = candidate
                else { result += line; line = word }
            }
            if (line.isNotEmpty()) result += line
            lines = result
            val lineHeight = textSize * 1.08f
            if (lines.size * lineHeight <= bounds.height() * 0.82f || textSize <= 22f) break
            textSize *= 0.92f
        }
        val lineHeight = textSize * 1.08f
        val totalHeight = lines.size * lineHeight
        var y = bounds.centerY() - totalHeight / 2f + textSize
        lines.forEach { line ->
            canvas.drawText(line, bounds.centerX(), y, paint)
            y += lineHeight
        }
    }
}
