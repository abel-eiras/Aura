package com.aura.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

/** Pre-renders a rotated bitmap of a drawable, since neither QS tiles nor RemoteViews widgets
 * support rotating an icon declaratively - the Quick Settings tile and the home screen widget
 * both animate "recording" by swapping in a new rotated frame every tick. */
object IconRotation {
    fun rotatedBitmap(
        context: Context,
        @DrawableRes drawableRes: Int,
        angleDegrees: Float,
        sizePx: Int,
        tintColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = ContextCompat.getDrawable(context, drawableRes)!!.mutate()
        DrawableCompat.setTint(drawable, tintColor)
        drawable.setBounds(0, 0, sizePx, sizePx)
        canvas.save()
        canvas.rotate(angleDegrees, sizePx / 2f, sizePx / 2f)
        drawable.draw(canvas)
        canvas.restore()
        return bitmap
    }
}
