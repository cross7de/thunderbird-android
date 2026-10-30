package com.fsck.k9.widget

import android.content.Context
import android.content.Intent

/** Settings for all instances of the classic compact message-list widget. */
object WidgetRowHeightSettings {
    const val MIN_HEIGHT_DP = 32
    const val MAX_HEIGHT_DP = 64
    const val DEFAULT_HEIGHT_DP = 32
    const val ACTION_ROW_HEIGHT_CHANGED = "com.fsck.k9.widget.ROW_HEIGHT_CHANGED"

    private const val PREFS_NAME = "compact_message_widget"
    private const val PREF_ROW_HEIGHT = "row_height_dp"
    private const val PROVIDER_CLASS_NAME = "net.thunderbird.android.widget.provider.MessageListWidgetProvider"

    fun get(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_ROW_HEIGHT, DEFAULT_HEIGHT_DP)
            .coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    }

    fun set(context: Context, heightDp: Int) {
        val height = heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
        if (get(context) == height) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(PREF_ROW_HEIGHT, height).apply()

        // The receiver owns the widget resource ID. No dependency on the widget module is needed here.
        val intent = Intent(ACTION_ROW_HEIGHT_CHANGED).apply {
            setClassName(context.packageName, PROVIDER_CLASS_NAME)
        }
        context.sendBroadcast(intent)
    }
}
