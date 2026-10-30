package com.fsck.k9.ui.choosefolder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderType
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@Suppress("TooManyFunctions")
class ChooseFolderActivity : BaseActivity() {
    private val viewModel: ChooseFolderViewModel by viewModel()
    private val messagingController: MessagingController by inject()
    private val folderNameFormatter: FolderNameFormatter by inject()
    private val folderIconProvider: FolderIconProvider by inject { parametersOf(theme) }

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter<FolderListItem>
    private lateinit var accountId: AccountId
    private lateinit var action: Action
    private var currentFolderId: Long? = null
    private var scrollToFolderId: Long? = null
    private var messageReference: String? = null
    private var folderTree: List<ChooseFolderNode> = emptyList()
    private val expandedPaths = mutableSetOf<String>()
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.folder_list)

        if (!decodeArguments(savedInstanceState)) {
            finish()
            return
        }

        when (action) {
            Action.MOVE -> setTitle(R.string.choose_folder_move_title)
            Action.COPY -> setTitle(R.string.choose_folder_copy_title)
            else -> setTitle(R.string.choose_folder_title)
        }

        initializeActionBar()
        initializeFolderList()

        viewModel.getFolders().observe(this) { folders ->
            updateFolderList(folders)
        }

        val savedShowHiddenFolders = savedInstanceState?.getBoolean(STATE_SHOW_HIDDEN_FOLDERS)
        val showHiddenFolders = savedShowHiddenFolders ?: false

        viewModel.setDisplayMode(accountId, showHiddenFolders)
    }

    private fun decodeArguments(savedInstanceState: Bundle?): Boolean {
        action = intent.action?.toAction() ?: error("Missing Intent action")

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: return false
        accountId = AccountIdFactory.of(accountUuid)

        messageReference = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
        currentFolderId = intent.getLongExtraOrNull(EXTRA_CURRENT_FOLDER_ID)
        savedInstanceState?.getStringArrayList(STATE_EXPANDED_PATHS)?.let(expandedPaths::addAll)
        searchQuery = savedInstanceState?.getString(STATE_SEARCH_QUERY).orEmpty()

        // Begin in the message's source folder, not in the last used destination.
        // When the source folder is unknown (e.g. a unified view), keep the old fallback.
        scrollToFolderId = if (savedInstanceState != null) {
            savedInstanceState.getLongOrNull(STATE_SCROLL_TO_FOLDER_ID)
        } else {
            currentFolderId ?: intent.getLongExtraOrNull(EXTRA_SCROLL_TO_FOLDER_ID)
        }

        return true
    }

    private fun initializeActionBar() {
        val actionBar = supportActionBar ?: error("Action bar missing")
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(Icons.Outlined.Close)
    }

    private fun initializeFolderList() {
        itemAdapter = ItemAdapter()

        val folderListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item: FolderListItem, _ ->
                when (chooseFolderRowAction(item.hasChildren, item.canSelectDestination, searchQuery.isNotBlank())) {
                    ChooseFolderRowAction.EXPAND -> toggleFolder(item.path)
                    ChooseFolderRowAction.SELECT -> confirmDestination(item)
                    ChooseFolderRowAction.NONE -> Unit
                }
                true
            }
        }

        recyclerView = findViewById(R.id.folderList)
        recyclerView.adapter = folderListAdapter
    }

    private fun updateFolderList(displayFolders: List<DisplayFolder>) {
        folderTree = buildChooseFolderHierarchy(displayFolders) { folder ->
            folderNameFormatter.displayName(folder.folder)
        }

        // Reveal the initial folder's ancestors. For the source folder also expand its
        // own children so that navigation can continue immediately from there.
        scrollToFolderId?.let { targetId ->
            findChooseFolderNode(folderTree, targetId)?.let { node ->
                expandedPaths.addAll(node.ancestors)
                if (targetId == currentFolderId) expandedPaths.add(node.path)
            }
        }
        renderFolderList()
    }

    private fun renderFolderList() {
        val searching = searchQuery.isNotBlank()
        val rows = visibleChooseFolderNodes(folderTree, expandedPaths, searchQuery)
        val folderListItems = rows.map { (node, depth) ->
            val folder = node.folder
            val databaseId = folder?.folder?.id
            FolderListItem(
                databaseId = databaseId,
                folderIconResource = folderIconProvider.getFolderIcon(folder?.folder?.type ?: FolderType.REGULAR),
                displayName = folder?.let { folderNameFormatter.displayName(it.folder) } ?: node.path,
                label = node.label,
                path = node.path,
                depth = depth,
                hasChildren = node.children.isNotEmpty(),
                showExpansionButton = !searching && node.children.isNotEmpty(),
                isExpanded = node.path in expandedPaths,
                canSelectDestination = databaseId != null && databaseId != currentFolderId,
                onToggleExpansion = ::toggleFolder,
                onSelectDestination = ::confirmDestination,
            )
        }

        itemAdapter.set(folderListItems)
        scrollToFolder(folderListItems)
    }

    private fun toggleFolder(path: String) {
        if (!expandedPaths.add(path)) expandedPaths.remove(path)
        renderFolderList()
    }

    private fun scrollToFolder(folders: List<FolderListItem>) {
        val target = scrollToFolderId ?: return
        val index = folders.indexOfFirst { it.databaseId == target }
        if (index != -1) {
            recyclerView.scrollToPosition(index)
            scrollToFolderId = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        scrollToFolderId?.let { folderId -> outState.putLong(STATE_SCROLL_TO_FOLDER_ID, folderId) }
        outState.putBoolean(STATE_SHOW_HIDDEN_FOLDERS, viewModel.isShowHiddenFolders)
        outState.putStringArrayList(STATE_EXPANDED_PATHS, ArrayList(expandedPaths))
        outState.putString(STATE_SEARCH_QUERY, searchQuery)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.choose_folder_option, menu)
        configureFolderSearchView(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.toggle_hidden_folders)?.setChecked(viewModel.isShowHiddenFolders)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun configureFolderSearchView(menu: Menu) {
        val folderMenuItem = menu.findItem(R.id.filter_folders)
        val folderSearchView = folderMenuItem.actionView as SearchView
        folderSearchView.maxWidth = Int.MAX_VALUE
        folderSearchView.queryHint = getString(R.string.folder_list_filter_hint)
        folderSearchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean = true

                override fun onQueryTextChange(newText: String): Boolean {
                    searchQuery = newText
                    renderFolderList()
                    return true
                }
            },
        )
        if (searchQuery.isNotEmpty()) {
            folderMenuItem.expandActionView()
            folderSearchView.setQuery(searchQuery, false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.toggle_hidden_folders -> setShowHiddenFolders(item.isChecked.not())
            R.id.list_folders -> refreshFolderList()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun refreshFolderList() {
        messagingController.refreshFolderList(accountId)
    }

    private fun setShowHiddenFolders(enabled: Boolean) {
        viewModel.setDisplayMode(accountId, enabled)
    }

    /** Only an explicit destination action followed by confirmation can return a destination. */
    private fun confirmDestination(item: FolderListItem) {
        val folderId = item.databaseId ?: return
        if (!item.canSelectDestination) return

        val title = when (action) {
            Action.MOVE -> R.string.choose_folder_move_title
            Action.COPY -> R.string.choose_folder_copy_title
            Action.CHOOSE -> R.string.choose_folder_title
        }
        val confirmButton = when (action) {
            Action.MOVE -> R.string.choose_folder_confirm_move_action
            Action.COPY -> R.string.choose_folder_confirm_copy_action
            Action.CHOOSE -> R.string.choose_folder_confirm_choose_action
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(getString(R.string.choose_folder_confirm_destination, item.displayName))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(confirmButton) { _, _ -> returnResult(folderId, item.displayName) }
            .show()
    }

    private fun returnResult(folderId: Long, displayName: String) {
        val result = Intent().apply {
            putExtra(RESULT_SELECTED_FOLDER_ID, folderId)
            putExtra(RESULT_FOLDER_DISPLAY_NAME, displayName)
            putExtra(RESULT_MESSAGE_REFERENCE, messageReference)
        }

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun Intent.getLongExtraOrNull(name: String): Long? {
        if (!hasExtra(name)) return null

        val value = getLongExtra(name, -1L)
        return if (value != -1L) value else null
    }

    private fun Bundle.getLongOrNull(name: String): Long? {
        return if (containsKey(name)) getLong(name) else null
    }

    private fun String.toAction() = Action.valueOf(this)

    enum class Action {
        MOVE,
        COPY,
        CHOOSE,
    }

    companion object {
        private const val STATE_SCROLL_TO_FOLDER_ID = "scrollToFolderId"
        private const val STATE_SHOW_HIDDEN_FOLDERS = "showHiddenFolders"
        private const val STATE_EXPANDED_PATHS = "expandedFolderPaths"
        private const val STATE_SEARCH_QUERY = "folderSearchQuery"
        private const val EXTRA_ACCOUNT = "accountUuid"
        private const val EXTRA_CURRENT_FOLDER_ID = "currentFolderId"
        private const val EXTRA_SCROLL_TO_FOLDER_ID = "scrollToFolderId"
        private const val EXTRA_MESSAGE_REFERENCE = "messageReference"
        const val RESULT_SELECTED_FOLDER_ID = "selectedFolderId"
        const val RESULT_FOLDER_DISPLAY_NAME = "folderDisplayName"
        const val RESULT_MESSAGE_REFERENCE = "messageReference"

        @JvmStatic
        fun buildLaunchIntent(
            context: Context,
            action: Action,
            accountId: AccountId,
            currentFolderId: Long? = null,
            scrollToFolderId: Long? = null,
            messageReference: MessageReference? = null,
        ): Intent {
            return Intent(context, ChooseFolderActivity::class.java).apply {
                this.action = action.toString()
                putExtra(EXTRA_ACCOUNT, accountId.toString())
                currentFolderId?.let { putExtra(EXTRA_CURRENT_FOLDER_ID, currentFolderId) }
                scrollToFolderId?.let { putExtra(EXTRA_SCROLL_TO_FOLDER_ID, scrollToFolderId) }
                messageReference?.let { putExtra(EXTRA_MESSAGE_REFERENCE, it.toIdentityString()) }
            }
        }
    }
}
