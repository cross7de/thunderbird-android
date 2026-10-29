package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

/** Distinguish two quick taps from the normal, immediately acting first tap. */
internal class FolderTapSequence(private val intervalMs: Long = 450L) {
    private var recentlyOpenedKey: String? = null
    private var openedAtMs: Long = 0L

    /** Returns true only for a second tap on the same, just-opened folder. */
    fun onTap(folderKey: String, wasExpanded: Boolean, nowMs: Long): Boolean {
        val elapsed = nowMs - openedAtMs
        if (wasExpanded && folderKey == recentlyOpenedKey && elapsed in 0..intervalMs) {
            reset()
            return true
        }
        if (!wasExpanded) {
            recentlyOpenedKey = folderKey
            openedAtMs = nowMs
        } else {
            reset()
        }
        return false
    }

    fun reset() {
        recentlyOpenedKey = null
    }
}
