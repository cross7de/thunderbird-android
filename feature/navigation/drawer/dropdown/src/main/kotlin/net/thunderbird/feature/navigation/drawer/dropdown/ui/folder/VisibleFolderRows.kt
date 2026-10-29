package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import java.util.ArrayDeque
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder

internal data class FolderGuideSegment(
    val level: Int,
    val endsAtCenter: Boolean,
)

internal data class VisibleFolderRow(
    val treeFolder: DisplayTreeFolder,
    val depth: Int,
    val parentPrefix: String?,
    val guideSegments: List<FolderGuideSegment>,
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

    // For each depth, remember whether that node has a following sibling.
    fun addTree(
        folder: DisplayTreeFolder,
        depth: Int,
        parentPrefix: String?,
        hasNextSibling: List<Boolean>,
    ) {
        val displayFolder = folder.displayFolder ?: return
        val expanded = initiallyExpanded != (folderExpansionKey(displayFolder) in toggledFolderKeys)
        val children = if (expanded) folder.children.filter { it.displayFolder != null } else emptyList()
        val guides = buildList {
            for (level in 0 until depth) {
                // Keep every ancestor line visible throughout its expanded subtree.
                // Only its final visible descendant ends the line at mid-row.
                val isLastVisibleDescendant = children.isEmpty() &&
                    (level + 1..depth).none { hasNextSibling[it] }
                add(FolderGuideSegment(level, endsAtCenter = isLastVisibleDescendant))
            }
        }
        visible += VisibleFolderRow(folder, depth, parentPrefix, guides)
        if (expanded) {
            val prefix = (displayFolder as? MailDisplayFolder)?.folder?.name
            children.forEachIndexed { index, child ->
                addTree(child, depth + 1, prefix, hasNextSibling + (index < children.lastIndex))
            }
        }
    }

    val topLevel = rootFolder.children.filter { it.displayFolder != null }
    topLevel.forEachIndexed { index, child ->
        addTree(child, 0, null, listOf(index < topLevel.lastIndex))
    }
    return visible
}

/** Set a branch and every descendant to the opposite of the selected folder's current state. */
internal fun toggleFolderSubtree(
    rootFolder: DisplayTreeFolder,
    folderKey: String,
    initiallyExpanded: Boolean,
    toggledFolderKeys: Set<String>,
): Set<String>? {
    val pending = ArrayDeque<DisplayTreeFolder>()
    pending.addLast(rootFolder)
    var branch: DisplayTreeFolder? = null

    while (pending.isNotEmpty()) {
        val folder = pending.removeLast()
        if (folder.displayFolder?.let(::folderExpansionKey) == folderKey) {
            branch = folder
            break
        }
        folder.children.forEach { pending.addLast(it) }
    }
    val selected = branch ?: return null
    val expand = initiallyExpanded == (folderKey in toggledFolderKeys)
    val updated = toggledFolderKeys.toMutableSet()
    // Only the selected branch may be changed, not unfinished search branches.
    pending.clear()
    pending.addLast(selected)
    while (pending.isNotEmpty()) {
        val folder = pending.removeLast()
        folder.displayFolder?.let { display ->
            val key = folderExpansionKey(display)
            if (expand == initiallyExpanded) updated.remove(key) else updated.add(key)
        }
        folder.children.forEach { pending.addLast(it) }
    }
    return updated
}

/** Open only a selected folder's direct children. Their descendants keep their saved states. */
internal fun expandDirectChildren(
    rootFolder: DisplayTreeFolder,
    folderKey: String,
    initiallyExpanded: Boolean,
    toggledFolderKeys: Set<String>,
): Set<String>? {
    val pending = ArrayDeque<DisplayTreeFolder>()
    pending.addLast(rootFolder)
    while (pending.isNotEmpty()) {
        val folder = pending.removeLast()
        if (folder.displayFolder?.let(::folderExpansionKey) == folderKey) {
            val updated = toggledFolderKeys.toMutableSet()
            folder.children.forEach { child ->
                if (child.children.isNotEmpty()) {
                    child.displayFolder?.let { display ->
                        val key = folderExpansionKey(display)
                        if (initiallyExpanded) updated.remove(key) else updated.add(key)
                    }
                }
            }
            return updated
        }
        folder.children.forEach { pending.addLast(it) }
    }
    return null
}
