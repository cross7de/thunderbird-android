package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder

internal data class VisibleFolderRow(
    val treeFolder: DisplayTreeFolder,
    val depth: Int,
    val parentPrefix: String?,
)

/** Use the full path, not temporary negative IDs assigned to placeholder folders. */
internal fun folderExpansionKey(folder: DisplayFolder): String = when (folder) {
    is MailDisplayFolder -> "mail:${folder.accountId}:${folder.folder.name}"
    is UnifiedDisplayFolder -> "unified:${folder.folderId}"
    else -> "other:${folder.accountId}:${folder.folderId}"
}

/** Return only visible rows. The underlying hierarchy remains unchanged. */
internal fun visibleFolderRows(
    rootFolder: DisplayTreeFolder,
    initiallyExpanded: Boolean,
    toggledFolderKeys: Set<String>,
): List<VisibleFolderRow> {
    val visible = mutableListOf<VisibleFolderRow>()

    fun addTree(folder: DisplayTreeFolder, depth: Int, parentPrefix: String?) {
        val displayFolder = folder.displayFolder ?: return
        visible += VisibleFolderRow(folder, depth, parentPrefix)
        val expanded = initiallyExpanded != (folderExpansionKey(displayFolder) in toggledFolderKeys)
        if (expanded) {
            val prefix = (displayFolder as? MailDisplayFolder)?.folder?.name
            folder.children.forEach { child ->
                addTree(child, depth + 1, prefix)
            }
        }
    }

    rootFolder.children.forEach { addTree(it, 0, null) }
    return visible
}
