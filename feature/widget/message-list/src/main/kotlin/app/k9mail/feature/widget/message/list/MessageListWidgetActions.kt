package app.k9mail.feature.widget.message.list

import android.content.Context

internal object MessageListWidgetActions {
    const val ACTION_SYNC = "app.k9mail.widget.SYNC"
    const val ACTION_TOGGLE_DELETE = "app.k9mail.widget.TOGGLE_DELETE"
    const val ACTION_OPEN_MESSAGE = "app.k9mail.widget.OPEN_MESSAGE"
    const val ACTION_DELETE_MESSAGE = "app.k9mail.widget.DELETE_MESSAGE"
    const val EXTRA_MESSAGE_REFERENCE = "message_reference"

    private const val PREFS_NAME = "compact_message_widget"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDeleteMode(context: Context, appWidgetId: Int): Boolean {
        return prefs(context).getBoolean("delete_$appWidgetId", false)
    }

    fun toggleDeleteMode(context: Context, appWidgetId: Int): Boolean {
        val enabled = !isDeleteMode(context, appWidgetId)
        prefs(context).edit().putBoolean("delete_$appWidgetId", enabled).apply()
        return enabled
    }

    fun clear(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove("delete_$appWidgetId")
            .apply()
    }
}
