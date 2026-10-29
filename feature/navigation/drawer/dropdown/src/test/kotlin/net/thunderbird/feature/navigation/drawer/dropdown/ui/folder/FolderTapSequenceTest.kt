package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

internal class FolderTapSequenceTest {
    @Test
    fun `first tap acts immediately and quick second tap is recognized`() {
        val sequence = FolderTapSequence()
        assertThat(sequence.onTap("folder", false, 100L)).isEqualTo(false)
        assertThat(sequence.onTap("folder", true, 250L)).isEqualTo(true)
        assertThat(sequence.onTap("folder", true, 260L)).isEqualTo(false)
    }

    @Test
    fun `second tap after timeout is a normal click`() {
        val sequence = FolderTapSequence()
        assertThat(sequence.onTap("folder", false, 100L)).isEqualTo(false)
        assertThat(sequence.onTap("folder", true, 600L)).isEqualTo(false)
    }

    @Test
    fun `other folder and long press cancel a potential second tap`() {
        val sequence = FolderTapSequence()
        sequence.onTap("one", false, 100L)
        assertThat(sequence.onTap("two", true, 150L)).isEqualTo(false)
        sequence.onTap("one", false, 200L)
        sequence.reset()
        assertThat(sequence.onTap("one", true, 220L)).isEqualTo(false)
    }
}
