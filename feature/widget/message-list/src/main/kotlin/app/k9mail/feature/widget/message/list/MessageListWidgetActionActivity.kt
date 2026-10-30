package app.k9mail.feature.widget.message.list

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.activity.MessageHomeActivity
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccountManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** A tiny activity is needed because a RemoteViews collection can only have one PendingIntent template. */
class MessageListWidgetActionActivity : Activity(), KoinComponent {
    private val controller: MessagingController by inject()
    private val accountManager: LegacyAccountManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reference = MessageReference.parse(intent.getStringExtra(MessageListWidgetActions.EXTRA_MESSAGE_REFERENCE))
        if (reference != null) {
            when (intent.action) {
                MessageListWidgetActions.ACTION_OPEN_MESSAGE -> {
                    startActivity(MessageHomeActivity.actionDisplayMessageIntent(
                        context = this,
                        messageReference = reference,
                        openInUnifiedInbox = true,
                        messageViewOnly = true,
                    ))
                }
                MessageListWidgetActions.ACTION_DELETE_MESSAGE -> deleteSingleMessage(reference)
            }
        }
        finish()
    }

    private fun deleteSingleMessage(reference: MessageReference) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ||
            !MessageListWidgetActions.isDeleteMode(this, widgetId)
        ) return

        val trashId = accountManager.findById(reference.accountId)?.trashFolderId
        if (trashId == null || trashId == reference.folderId || !controller.isMoveCapable(reference)) {
            Toast.makeText(this, R.string.message_list_widget_cannot_move_to_trash, Toast.LENGTH_SHORT).show()
            return
        }

        controller.moveMessage(reference.accountId, reference.folderId, reference, trashId)
    }
}
