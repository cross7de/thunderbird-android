package com.fsck.k9.ui.settings.general

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.fsck.k9.widget.WidgetRowHeightSettings

/** Four reference marks, without snapping; every intermediate dp value remains selectable. */
@Suppress("MagicNumber")
class WidgetRowHeightMarksView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = density
        textSize = 11 * resources.displayMetrics.scaledDensity
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat()
        val available = (width - paddingLeft - paddingRight).toFloat()
        val stops = intArrayOf(WidgetRowHeightSettings.MIN_HEIGHT_DP, 40, 48, WidgetRowHeightSettings.MAX_HEIGHT_DP)
        for (value in stops) {
            val fraction = (value - WidgetRowHeightSettings.MIN_HEIGHT_DP).toFloat() /
                (WidgetRowHeightSettings.MAX_HEIGHT_DP - WidgetRowHeightSettings.MIN_HEIGHT_DP)
            val x = left + available * fraction
            paint.strokeWidth = if (value == 40) 2 * density else density
            canvas.drawLine(x, 0f, x, 8 * density, paint)
            val label = value.toString()
            val textX = (x - paint.measureText(label) / 2).coerceIn(0f, width - paint.measureText(label))
            canvas.drawText(label, textX, 24 * density, paint)
        }
    }
}
