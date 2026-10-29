package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder

@Composable
internal fun FolderList(
    rootFolder: DisplayTreeFolder,
    selectedFolder: DisplayFolder?,
    onFolderClick: (DisplayFolder) -> Unit,
    onFolderExpandClick: (String) -> Unit = {},
    onFolderExpandLongClick: (String) -> Unit = {},
    toggledFolderKeys: Set<String> = emptySet(),
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
    isExpandedInitial: Boolean = false,
) {
    val resources = LocalResources.current
    val folderNameFormatter = remember { FolderNameFormatter(resources) }
    val listState = rememberLazyListState()
    val visibleRows = remember(rootFolder, isExpandedInitial, toggledFolderKeys) {
        visibleFolderRows(rootFolder, isExpandedInitial, toggledFolderKeys)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = BoltTheme.spacings.default),
    ) {
        items(
            items = visibleRows,
            key = { folderExpansionKey(requireNotNull(it.treeFolder.displayFolder)) },
        ) { row ->
            val folder = requireNotNull(row.treeFolder.displayFolder)
            val folderKey = folderExpansionKey(folder)
            FolderListItem(
                displayFolder = folder,
                treeFolder = row.treeFolder,
                showStarredCount = showStarredCount,
                onClick = onFolderClick,
                onExpandClick = { onFolderExpandClick(folderKey) },
                onExpandLongClick = { onFolderExpandLongClick(folderKey) },
                isExpanded = isExpandedInitial != (folderKey in toggledFolderKeys),
                folderNameFormatter = folderNameFormatter,
                selectedFolderId = selectedFolder?.folderId,
                parentPrefix = row.parentPrefix,
                indentationLevel = row.depth,
                guideSegments = row.guideSegments,
            )
        }
    }
}
