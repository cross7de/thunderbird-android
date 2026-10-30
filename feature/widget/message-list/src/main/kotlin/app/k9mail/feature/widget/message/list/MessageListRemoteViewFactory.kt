package app.k9mail.feature.widget.message.list

import android.content.Context
import android.content.Intent
import android.view.View
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.content.ContextCompat
import com.fsck.k9.CoreResourceProvider
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("TooManyFunctions")
internal class MessageListRemoteViewFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsFactory, KoinComponent {
    private val messageListLoader: MessageListLoader by inject()
    private val coreResourceProvider: CoreResourceProvider by inject()
    private val generalSettingsManager: GeneralSettingsManager by inject()

    private lateinit var unifiedInboxFolders: LocalMessageSearch

    private var messageListItems = emptyList<MessageListItem>()
    private var senderAboveSubject = false
    private var readTextColor = 0
    private var unreadTextColor = 0

    override fun onCreate() {
        unifiedInboxFolders = SearchAccount.createUnifiedInboxSearch(
            title = coreResourceProvider.searchUnifiedFoldersTitle(),
            detail = coreResourceProvider.searchUnifiedFoldersDetail(),
        ).relatedSearch

        senderAboveSubject = generalSettingsManager.getConfig().display.inboxSettings.isMessageListSenderAboveSubject
        readTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_read)
        unreadTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_unread)
    }

    override fun onDataSetChanged() {
        loadMessageList()
    }

    private fun loadMessageList() {
        // TODO: Use same sort order that is used for the Unified Inbox inside the app
        val messageListConfig = MessageListConfig(
            search = unifiedInboxFolders,
            showingThreadedList = generalSettingsManager.getConfig()
                .display
                .inboxSettings
                .isThreadedViewEnabled,
            sortType = SortType.SORT_DATE,
            sortAscending = false,
            sortDateAscending = false,
        )

        messageListItems = messageListLoader.getMessageList(messageListConfig)
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = messageListItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.message_list_widget_list_item)

        val item = messageListItems[position]

        val displayName = if (item.isRead) item.displayName else bold(item.displayName)
        val subject = if (item.isRead) item.subject else bold(item.subject)

        if (senderAboveSubject) {
            remoteView.setTextViewText(R.id.sender, displayName)
            remoteView.setTextViewText(R.id.mail_subject, subject)
        } else {
            remoteView.setTextViewText(R.id.sender, subject)
            remoteView.setTextViewText(R.id.mail_subject, displayName)
        }

        remoteView.setTextViewText(R.id.mail_date, item.displayDate)

        val textColor = getTextColor(item)
        remoteView.setTextColor(R.id.sender, textColor)
        remoteView.setTextColor(R.id.mail_subject, textColor)
        remoteView.setTextColor(R.id.mail_date, textColor)

        val reference = item.messageReference.toIdentityString()
        remoteView.setOnClickFillInIntent(R.id.mail_list_item, Intent().apply {
            action = MessageListWidgetActions.ACTION_OPEN_MESSAGE
            putExtra(MessageListWidgetActions.EXTRA_MESSAGE_REFERENCE, reference)
        })

        val deleteMode = MessageListWidgetActions.isDeleteMode(context, appWidgetId)
        remoteView.setViewVisibility(R.id.mail_date, if (deleteMode) View.GONE else View.VISIBLE)
        remoteView.setViewVisibility(R.id.delete_item, if (deleteMode) View.VISIBLE else View.GONE)
        if (deleteMode) {
            remoteView.setOnClickFillInIntent(R.id.delete_item, Intent().apply {
                action = MessageListWidgetActions.ACTION_DELETE_MESSAGE
                putExtra(MessageListWidgetActions.EXTRA_MESSAGE_REFERENCE, reference)
            })
        }

        remoteView.setInt(R.id.chip, "setBackgroundColor", item.accountColor)

        return remoteView
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.message_list_widget_list_item_loading).apply {
            // Set the text here instead of in the layout so the app language override is used
            setTextViewText(R.id.loadingText, context.getString(R.string.message_list_widget_list_item_loading))
        }
    }

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = messageListItems[position].uniqueId

    override fun hasStableIds(): Boolean = true

    private fun bold(text: String): CharSequence {
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        }
    }

    private fun getTextColor(messageListItem: MessageListItem): Int {
        return if (messageListItem.isRead) readTextColor else unreadTextColor
    }
}
