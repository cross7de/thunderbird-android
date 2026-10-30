package com.fsck.k9.ui.choosefolder

import app.k9mail.legacy.ui.folder.DisplayFolder
import java.util.ArrayDeque
import java.util.Locale
import net.thunderbird.feature.mail.folder.FolderType

/** Builds a presentation tree from real folder paths, without creating destination folder IDs. */
internal data class ChooseFolderNode(
    val path: String,
    var label: String,
    val ancestors: List<String>,
    var folder: DisplayFolder? = null,
    val children: MutableList<ChooseFolderNode> = mutableListOf(),
)

internal data class VisibleChooseFolderNode(val node: ChooseFolderNode, val depth: Int)

internal fun buildChooseFolderHierarchy(
    folders: List<DisplayFolder>,
    displayName: (DisplayFolder) -> String,
): List<ChooseFolderNode> {
    val roots = mutableListOf<ChooseFolderNode>()
    val byPath = mutableMapOf<String, ChooseFolderNode>()

    folders.filterNot { it.folder.type == FolderType.OUTBOX }.forEach { folder ->
        val delimiter = folder.pathDelimiter
        val segments = if (delimiter.isEmpty()) listOf(folder.folder.name) else folder.folder.name.split(delimiter)
        var parent: ChooseFolderNode? = null
        val ancestors = mutableListOf<String>()

        segments.forEachIndexed { index, segment ->
            val currentParent = parent
            val path = if (currentParent == null) segment else "${currentParent.path}$delimiter$segment"
            val node = byPath.getOrPut(path) {
                ChooseFolderNode(path, segment.ifEmpty { "(Unnamed)" }, ancestors.toList()).also {
                    if (currentParent == null) roots.add(it) else currentParent.children.add(it)
                }
            }
            if (index == segments.lastIndex) {
                node.folder = folder
                node.label = if (folder.folder.isLocalOnly || folder.folder.type == FolderType.INBOX) {
                    displayName(folder)
                } else {
                    segment.ifEmpty { "(Unnamed)" }
                }
            }
            ancestors.add(path)
            parent = node
        }
    }

    return roots
}

/** With a query, matched folders and their complete ancestor chains are shown regardless of expansion state. */
internal fun visibleChooseFolderNodes(
    roots: List<ChooseFolderNode>,
    expandedPaths: Set<String>,
    query: String,
): List<VisibleChooseFolderNode> {
    val terms = query.trim().lowercase(Locale.getDefault()).split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun visit(node: ChooseFolderNode, depth: Int): List<VisibleChooseFolderNode> {
        val row = VisibleChooseFolderNode(node, depth)
        if (terms.isEmpty()) {
            return if (node.path in expandedPaths) {
                listOf(row) + node.children.flatMap { visit(it, depth + 1) }
            } else {
                listOf(row)
            }
        }

        val children = node.children.flatMap { visit(it, depth + 1) }
        val matches = node.folder != null && terms.any { term ->
            term in node.path.lowercase(Locale.getDefault()) || term in node.label.lowercase(Locale.getDefault())
        }
        return if (matches || children.isNotEmpty()) listOf(row) + children else emptyList()
    }

    return roots.flatMap { visit(it, 0) }
}

internal fun findChooseFolderNode(roots: List<ChooseFolderNode>, folderId: Long): ChooseFolderNode? {
    val nodes = ArrayDeque<ChooseFolderNode>()
    roots.forEach(nodes::addLast)
    while (nodes.isNotEmpty()) {
        val node = nodes.removeFirst()
        if (node.folder?.folder?.id == folderId) return node
        node.children.forEach(nodes::addLast)
    }
    return null
}

/** The row itself is navigation for parents; only leaf rows can initiate destination selection. */
internal enum class ChooseFolderRowAction {
    EXPAND,
    SELECT,
    NONE,
}

internal fun chooseFolderRowAction(
    hasChildren: Boolean,
    canSelectDestination: Boolean,
    isSearching: Boolean,
): ChooseFolderRowAction = when {
    hasChildren && !isSearching -> ChooseFolderRowAction.EXPAND
    hasChildren -> ChooseFolderRowAction.NONE
    canSelectDestination -> ChooseFolderRowAction.SELECT
    else -> ChooseFolderRowAction.NONE
}
