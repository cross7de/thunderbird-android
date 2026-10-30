package net.thunderbird.feature.navigation.drawer.dropdown.data

import android.content.Context

/** Persists folder expansion overrides, keyed by account and full folder path. */
internal interface FolderExpansionStore {
    fun read(): Set<String>
    fun readForDefault(expandAllFolder: Boolean): Set<String>
    fun write(keys: Set<String>)
}

internal class SharedPreferencesFolderExpansionStore(context: Context) : FolderExpansionStore {
    private val preferences = context.getSharedPreferences("folder_expansion", Context.MODE_PRIVATE)

    override fun read(): Set<String> = preferences.getStringSet("toggled_keys", emptySet()).orEmpty().toSet()

    /**
     * Overrides have the opposite meaning when the global default changes. Discard them in that case.
     * Old installations lack a saved default, so migrate them to the current global setting once.
     */
    override fun readForDefault(expandAllFolder: Boolean): Set<String> {
        val previousDefault = if (preferences.contains(KEY_EXPAND_ALL_DEFAULT)) {
            preferences.getBoolean(KEY_EXPAND_ALL_DEFAULT, false)
        } else {
            null
        }

        if (previousDefault != expandAllFolder) {
            preferences.edit()
                .remove("toggled_keys")
                .putBoolean(KEY_EXPAND_ALL_DEFAULT, expandAllFolder)
                .apply()
            return emptySet()
        }

        return read()
    }

    override fun write(keys: Set<String>) {
        preferences.edit().putStringSet("toggled_keys", keys.toSet()).apply()
    }

    private companion object {
        const val KEY_EXPAND_ALL_DEFAULT = "expand_all_default"
    }
}
