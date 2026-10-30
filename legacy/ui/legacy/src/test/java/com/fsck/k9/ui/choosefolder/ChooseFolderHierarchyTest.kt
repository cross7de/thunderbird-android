package com.fsck.k9.ui.choosefolder

import app.k9mail.legacy.ui.folder.DisplayFolder
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.Folder
import org.junit.Test

class ChooseFolderHierarchyTest {
    @Test
    fun `missing intermediate folders are navigation nodes without destination ids`() {
        val roots = buildChooseFolderHierarchy(listOf(folder(42, "Parent/Absent/Child")), ::displayName)

        val parent = roots.single()
        val absent = parent.children.single()
        val child = absent.children.single()
        assertThat(parent.folder).isEqualTo(null)
        assertThat(absent.folder).isEqualTo(null)
        assertThat(child.folder?.folder?.id).isEqualTo(42L)
        assertThat(child.ancestors).isEqualTo(listOf("Parent", "Parent/Absent"))
    }

    @Test
    fun `real parent arriving after its child replaces the placeholder`() {
        val roots = buildChooseFolderHierarchy(
            listOf(folder(2, "Inbox/Project"), folder(1, "Inbox", FolderType.INBOX)),
            ::displayName,
        )

        assertThat(roots.single().folder?.folder?.id).isEqualTo(1L)
        assertThat(roots.single().label).isEqualTo("Inbox")
        assertThat(roots.single().children.single().folder?.folder?.id).isEqualTo(2L)
    }

    @Test
    fun `collapsed folders hide descendants and expansion reveals them`() {
        val roots = buildChooseFolderHierarchy(listOf(folder(1, "A"), folder(2, "A/B"), folder(3, "A/B/C")), ::displayName)

        assertThat(visibleChooseFolderNodes(roots, emptySet(), "").map { it.node.path })
            .isEqualTo(listOf("A"))
        assertThat(visibleChooseFolderNodes(roots, setOf("A", "A/B"), "").map { it.node.path })
            .isEqualTo(listOf("A", "A/B", "A/B/C"))
    }

    @Test
    fun `search reveals a deep match and every ancestor without expanding unrelated folders`() {
        val roots = buildChooseFolderHierarchy(
            listOf(folder(1, "Mail/Work/2026/Invoices"), folder(2, "Other/Private")),
            ::displayName,
        )
        val rows = visibleChooseFolderNodes(roots, emptySet(), "invoices")

        assertThat(rows.map { it.node.path }).isEqualTo(
            listOf("Mail", "Mail/Work", "Mail/Work/2026", "Mail/Work/2026/Invoices"),
        )
        assertThat(rows.map { it.depth }).isEqualTo(listOf(0, 1, 2, 3))
    }

    @Test
    fun `search includes full path and excludes outbox as a destination`() {
        val roots = buildChooseFolderHierarchy(
            listOf(folder(1, "Projects/Invoice"), folder(2, "Outbox", FolderType.OUTBOX)),
            ::displayName,
        )

        assertThat(visibleChooseFolderNodes(roots, emptySet(), "projects").map { it.node.path })
            .isEqualTo(listOf("Projects", "Projects/Invoice"))
        assertThat(roots.map { it.path }).isEqualTo(listOf("Projects"))
    }

    @Test
    fun `previous destination is found with ancestor paths`() {
        val roots = buildChooseFolderHierarchy(listOf(folder(44, "A/B/C/D")), ::displayName)

        val destination = findChooseFolderNode(roots, 44L)
        assertThat(destination?.ancestors).isEqualTo(listOf("A", "A/B", "A/B/C"))
        assertThat(findChooseFolderNode(roots, 999L)).isEqualTo(null)
    }

    @Test
    fun `tapping a selectable parent expands instead of selecting it as destination`() {
        assertThat(chooseFolderRowAction(hasChildren = true, canSelectDestination = true, isSearching = false))
            .isEqualTo(ChooseFolderRowAction.EXPAND)
    }

    @Test
    fun `virtual parent expands and nonselectable leaf cannot be chosen`() {
        assertThat(chooseFolderRowAction(hasChildren = true, canSelectDestination = false, isSearching = false))
            .isEqualTo(ChooseFolderRowAction.EXPAND)
        assertThat(chooseFolderRowAction(hasChildren = false, canSelectDestination = false, isSearching = false))
            .isEqualTo(ChooseFolderRowAction.NONE)
    }

    @Test
    fun `selecting a leaf requires an explicit confirmation action`() {
        assertThat(chooseFolderRowAction(hasChildren = false, canSelectDestination = true, isSearching = false))
            .isEqualTo(ChooseFolderRowAction.SELECT)
    }

    @Test
    fun `search results containing a parent do not become directly selectable by row tap`() {
        assertThat(chooseFolderRowAction(hasChildren = true, canSelectDestination = true, isSearching = true))
            .isEqualTo(ChooseFolderRowAction.NONE)
    }

    private fun folder(id: Long, path: String, type: FolderType = FolderType.REGULAR): DisplayFolder {
        return DisplayFolder(
            folder = Folder(id = id, name = path, type = type, isLocalOnly = false),
            isInTopGroup = false,
            unreadMessageCount = 0,
            starredMessageCount = 0,
            pathDelimiter = "/",
        )
    }

    private fun displayName(folder: DisplayFolder): String = folder.folder.name
}
