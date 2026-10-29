package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder

internal class VisibleFolderRowsTest {
    private val accountId = AccountIdFactory.create()

    @Test
    fun `collapsed parent hides descendants but remembers their expansion`() {
        val grandparent = folder(1, "parent")
        val parent = folder(2, "parent/child")
        val grandchild = folder(3, "parent/child/grandchild")
        val root = tree(null, tree(grandparent, tree(parent, tree(grandchild))))
        val parentKey = folderExpansionKey(grandparent)
        val childKey = folderExpansionKey(parent)

        assertThat(visibleFolderRows(root, false, emptySet()).size).isEqualTo(1)
        assertThat(visibleFolderRows(root, false, setOf(parentKey, childKey)).map { it.depth })
            .isEqualTo(listOf(0, 1, 2))
        assertThat(visibleFolderRows(root, false, setOf(childKey)).size).isEqualTo(1)
        assertThat(visibleFolderRows(root, false, setOf(parentKey, childKey)).size).isEqualTo(3)
    }

    @Test
    fun `placeholder key is stable despite changing negative ID`() {
        val first = folder(-1, "missing/parent")
        val second = folder(-100, "missing/parent")
        assertThat(folderExpansionKey(first)).isEqualTo(folderExpansionKey(second))
    }

    @Test
    fun `initial expansion can be inverted for a single branch`() {
        val parent = folder(1, "top")
        val child = folder(2, "top/child")
        val root = tree(null, tree(parent, tree(child)))
        assertThat(visibleFolderRows(root, true, emptySet()).size).isEqualTo(2)
        assertThat(visibleFolderRows(root, true, setOf(folderExpansionKey(parent))).size).isEqualTo(1)
    }

    @Test
    fun `long press expands all descendants and preserves unrelated folder states`() {
        val parent = folder(1, "parent")
        val child = folder(2, "parent/child")
        val grandchild = folder(3, "parent/child/grandchild")
        val other = folder(4, "other")
        val root = tree(null, tree(parent, tree(child, tree(grandchild))), tree(other))
        val parentKey = folderExpansionKey(parent)
        val childKey = folderExpansionKey(child)
        val grandchildKey = folderExpansionKey(grandchild)
        val otherKey = folderExpansionKey(other)

        val expanded = toggleFolderSubtree(root, parentKey, false, setOf(childKey, otherKey))
        assertThat(expanded).isEqualTo(setOf(parentKey, childKey, grandchildKey, otherKey))

        val collapsed = toggleFolderSubtree(root, parentKey, false, requireNotNull(expanded))
        assertThat(collapsed).isEqualTo(setOf(otherKey))
    }

    @Test
    fun `long press respects global expand all setting`() {
        val parent = folder(1, "parent")
        val child = folder(2, "parent/child")
        val root = tree(null, tree(parent, tree(child)))
        val parentKey = folderExpansionKey(parent)
        val childKey = folderExpansionKey(child)

        val collapsed = toggleFolderSubtree(root, parentKey, true, emptySet())
        assertThat(collapsed).isEqualTo(setOf(parentKey, childKey))

        val expanded = toggleFolderSubtree(root, parentKey, true, requireNotNull(collapsed))
        assertThat(expanded).isEqualTo(emptySet())
    }

    @Test
    fun `unknown folder key leaves existing expansion state untouched`() {
        val root = tree(null, tree(folder(1, "parent")))
        assertThat(toggleFolderSubtree(root, "not-in-tree", false, setOf("old"))).isEqualTo(null)
    }

    @Test
    fun `long press does not modify ancestors or neighboring branches`() {
        val ancestor = folder(10, "ancestor")
        val parent = folder(11, "ancestor/parent")
        val target = folder(12, "ancestor/parent/target")
        val leaf = folder(13, "ancestor/parent/target/leaf")
        val siblingBefore = folder(14, "ancestor/parent/before")
        val siblingAfter = folder(15, "ancestor/parent/after")
        val other = folder(16, "other")
        val root = tree(
            null,
            tree(other),
            tree(
                ancestor,
                tree(
                    parent,
                    tree(siblingBefore),
                    tree(target, tree(leaf)),
                    tree(siblingAfter),
                ),
            ),
        )
        val targetKey = folderExpansionKey(target)
        val leafKey = folderExpansionKey(leaf)
        val ancestorKey = folderExpansionKey(ancestor)
        val parentKey = folderExpansionKey(parent)
        val siblingBeforeKey = folderExpansionKey(siblingBefore)
        val siblingAfterKey = folderExpansionKey(siblingAfter)
        val otherKey = folderExpansionKey(other)

        val unchanged = setOf(ancestorKey, parentKey, siblingAfterKey)
        val opened = requireNotNull(toggleFolderSubtree(root, targetKey, false, unchanged))
        assertThat(opened).isEqualTo(unchanged + targetKey + leafKey)
        val closed = toggleFolderSubtree(root, targetKey, false, opened)
        assertThat(closed).isEqualTo(unchanged)
        // The other branches are not accidentally added to the expanded set.
        assertThat(opened.contains(siblingBeforeKey)).isEqualTo(false)
        assertThat(opened.contains(otherKey)).isEqualTo(false)
    }

    @Test
    fun `ancestor guide continues through expanded last child to final descendant`() {
        val parent = folder(1, "p")
        val left = folder(2, "p/left")
        val leftLeaf = folder(3, "p/left/leaf")
        val right = folder(4, "p/right")
        val rightLeaf = folder(5, "p/right/leaf")
        val root = tree(null, tree(parent, tree(left, tree(leftLeaf)), tree(right, tree(rightLeaf))))
        val rows = visibleFolderRows(root, true, emptySet())

        assertThat(rows[0].guideSegments).isEqualTo(emptyList())
        assertThat(rows[1].guideSegments).isEqualTo(listOf(FolderGuideSegment(0, false)))
        assertThat(rows[2].guideSegments).isEqualTo(
            listOf(FolderGuideSegment(0, false), FolderGuideSegment(1, true)),
        )
        assertThat(rows[3].guideSegments).isEqualTo(listOf(FolderGuideSegment(0, false)))
        // Parent and last child's guides both end at the final visible descendant's midpoint.
        assertThat(rows[4].guideSegments).isEqualTo(
            listOf(FolderGuideSegment(0, true), FolderGuideSegment(1, true)),
        )
    }

    @Test
    fun `ancestor guide stops at midpoint of last collapsed descendant`() {
        val parent = folder(10, "parent")
        val last = folder(11, "parent/last")
        val grandchild = folder(12, "parent/last/grandchild")
        val root = tree(null, tree(parent, tree(last, tree(grandchild))))
        val rows = visibleFolderRows(root, false, setOf(folderExpansionKey(parent)))

        assertThat(rows.size).isEqualTo(2)
        assertThat(rows[1].guideSegments).isEqualTo(listOf(FolderGuideSegment(0, true)))
    }

    @Test
    fun `second quick tap expands direct children but not grandchildren`() {
        val parent = folder(1, "p")
        val first = folder(2, "p/first")
        val grandchild = folder(3, "p/first/child")
        val second = folder(4, "p/second")
        val greatGrandchild = folder(5, "p/first/child/deeper")
        val other = folder(6, "other")
        val root = tree(
            null,
            tree(parent, tree(first, tree(grandchild, tree(greatGrandchild))), tree(second, tree(folder(7, "leaf")))),
            tree(other),
        )
        val parentKey = folderExpansionKey(parent)
        val firstKey = folderExpansionKey(first)
        val secondKey = folderExpansionKey(second)
        val grandchildKey = folderExpansionKey(grandchild)
        val otherKey = folderExpansionKey(other)
        val updated = expandDirectChildren(root, parentKey, false, setOf(parentKey, otherKey))
        assertThat(updated).isEqualTo(setOf(parentKey, firstKey, secondKey, otherKey))
        assertThat(updated?.contains(grandchildKey)).isEqualTo(false)
        assertThat(expandDirectChildren(root, "missing", false, emptySet())).isEqualTo(null)
    }

    @Test
    fun `direct children expansion honors global expand-all preference`() {
        val parent = folder(1, "p")
        val child = folder(2, "p/child")
        val root = tree(null, tree(parent, tree(child, tree(folder(3, "p/child/leaf")))))
        val childKey = folderExpansionKey(child)
        assertThat(expandDirectChildren(root, folderExpansionKey(parent), true, setOf(childKey)))
            .isEqualTo(emptySet())
    }

    private fun folder(id: Long, name: String) = MailDisplayFolder(
        accountId = accountId,
        folder = Folder(id = id, name = name, type = FolderType.REGULAR, isLocalOnly = false),
        isInTopGroup = false,
        unreadMessageCount = 0,
        starredMessageCount = 0,
        pathDelimiter = "/",
        isPlaceholder = id < 0,
    )

    private fun tree(
        folder: MailDisplayFolder?,
        vararg children: DisplayTreeFolder,
    ) = DisplayTreeFolder(
        displayFolder = folder,
        displayName = folder?.folder?.name,
        totalUnreadCount = 0,
        totalStarredCount = 0,
        children = persistentListOf(*children),
    )
}
