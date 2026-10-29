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
