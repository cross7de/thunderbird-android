package app.k9mail.feature.widget.message.list

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.PendingIntentCompat
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.activity.MessageHomeActivity
import com.fsck.k9.activity.MessageHomeActivity.Companion.intentDisplaySearch
import com.fsck.k9.controller.MessagingControllerWrapper
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity
import com.fsck.k9.widget.WidgetRowHeightSettings
import net.thunderbird.feature.search.legacy.SearchAccount.Companion.createUnifiedInboxSearch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseMessageListWidgetProvider : AppWidgetProvider(), KoinComponent {
    private val messageListWidgetManager: MessageListWidgetManager by inject()
    private val coreResourceProvider: CoreResourceProvider by inject()
    private val controller: MessagingControllerWrapper by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MessageListWidgetActions.ACTION_SYNC, MessageListWidgetActions.ACTION_TOGGLE_DELETE -> {
                handleHeaderClick(context, intent)
            }
            WidgetRowHeightSettings.ACTION_ROW_HEIGHT_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
                if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.listView)
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun handleHeaderClick(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        if (appWidgetId !in manager.getAppWidgetIds(ComponentName(context, javaClass))) return

        val action = intent.action ?: return
        when (action) {
            MessageListWidgetActions.ACTION_SYNC -> controller.checkMail(
                id = null,
                ignoreLastCheckedTime = true,
                useManualWakeLock = true,
                notify = false,
                listener = null,
            )
            MessageListWidgetActions.ACTION_TOGGLE_DELETE -> {
                val enabled = MessageListWidgetActions.toggleDeleteMode(context, appWidgetId)
                manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.listView)
                Toast.makeText(
                    context,
                    if (enabled) R.string.message_list_widget_delete_enabled else R.string.message_list_widget_delete_disabled,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    override fun onEnabled(context: Context) {
        messageListWidgetManager.onWidgetAdded()
    }

    override fun onDisabled(context: Context) {
        messageListWidgetManager.onWidgetRemoved()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { MessageListWidgetActions.clear(context, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.message_list_widget_layout)

        val intent = Intent(context, MessageListWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("widget://message-list/$appWidgetId")
        }
        views.setRemoteAdapter(R.id.listView, intent)

        val viewAction = viewActionTemplatePendingIntent(context, appWidgetId)
        views.setPendingIntentTemplate(R.id.listView, viewAction)

        val composeAction = composeActionPendingIntent(context)
        views.setOnClickPendingIntent(R.id.new_message, composeAction)
        views.setOnClickPendingIntent(
            R.id.widget_settings,
            PendingIntentCompat.getActivity(
                context,
                -2,
                GeneralSettingsActivity.widgetSettingsIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )!!,
        )

        val headerClickAction = viewUnifiedFoldersPendingIntent(context)
        views.setOnClickPendingIntent(R.id.open_inbox, headerClickAction)
        views.setOnClickPendingIntent(
            R.id.sync_mail,
            headerActionPendingIntent(context, appWidgetId, MessageListWidgetActions.ACTION_SYNC),
        )
        views.setOnClickPendingIntent(
            R.id.toggle_delete,
            headerActionPendingIntent(context, appWidgetId, MessageListWidgetActions.ACTION_TOGGLE_DELETE),
        )

        val showExtraActions = appWidgetManager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) >= EXTRA_ACTIONS_MIN_WIDTH_DP
        val visibility = if (showExtraActions) View.VISIBLE else View.GONE
        views.setViewVisibility(R.id.open_inbox, visibility)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun viewActionTemplatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, MessageListWidgetActionActivity::class.java).apply {
            data = Uri.parse("widget://message-list/action/$appWidgetId")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntentCompat.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT, true)!!
    }

    private fun headerActionPendingIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, javaClass).apply {
            this.action = action
            data = Uri.parse("widget://message-list/header/$appWidgetId/$action")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun viewUnifiedFoldersPendingIntent(context: Context): PendingIntent {
        val unifiedFoldersSearch = createUnifiedInboxSearch(
            title = coreResourceProvider.searchUnifiedFoldersTitle(),
            detail = coreResourceProvider.searchUnifiedFoldersDetail(),
        )
        val intent = intentDisplaySearch(
            context = context,
            search = unifiedFoldersSearch.relatedSearch,
            noThreading = true,
            newTask = true,
            clearTop = true,
        )
        return PendingIntentCompat.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)!!
    }

    private fun composeActionPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MessageCompose::class.java).apply {
            action = MessageCompose.ACTION_COMPOSE
        }
        return PendingIntentCompat.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)!!
    }

    private companion object {
        const val EXTRA_ACTIONS_MIN_WIDTH_DP = 210
    }
}
