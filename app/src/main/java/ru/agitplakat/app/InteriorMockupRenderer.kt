package ru.agitplakat.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

enum class InteriorRoom(val title: String) {
    HOME_OFFICE("Домашний кабинет"),
    RETRO_LIVING("Гостиная в ретростиле"),
    LOFT_HALLWAY("Прихожая / лофт")
}

/**
 * Places the A3 poster into a frame area on a supplied room photograph.
 * Coordinates are normalized (0..1) so each included template can define its
 * own frame position. This is a compositing utility, not a source of room photos.
 */
object InteriorMockupRenderer {
    fun compose(
        roomTemplate: Bitmap,
        poster: Bitmap,
        frameLeft: Float,
        frameTop: Float,
        frameWidth: Float,
        frameHeight: Float
    ): Bitmap {
        require(listOf(frameLeft, frameTop, frameWidth, frameHeight).all { it in 0f..1f })
        require(frameLeft + frameWidth <= 1f && frameTop + frameHeight <= 1f)
        val output = roomTemplate.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val frame = RectF(
            frameLeft * output.width,
            frameTop * output.height,
            (frameLeft + frameWidth) * output.width,
            (frameTop + frameHeight) * output.height
        )
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55000000
            setShadowLayer(12f, 3f, 5f, 0x66000000)
        }
        canvas.drawRect(frame, shadow)
        val inset = (frame.width() * 0.035f).coerceAtLeast(2f)
        val posterRect = RectF(
            frame.left + inset,
            frame.top + inset,
            frame.right - inset,
            frame.bottom - inset
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(poster, null, posterRect, paint)
        return output
    }
}
