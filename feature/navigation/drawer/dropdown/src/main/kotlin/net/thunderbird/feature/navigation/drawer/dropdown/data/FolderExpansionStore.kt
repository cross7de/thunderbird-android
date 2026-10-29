package net.thunderbird.feature.navigation.drawer.dropdown.data

import android.content.Context

/** Persists folder expansion overrides, keyed by account and full folder path. */
internal interface FolderExpansionStore {
    fun read(): Set<String>
    fun write(keys: Set<String>)
}

internal class SharedPreferencesFolderExpansionStore(context: Context) : FolderExpansionStore {
    private val preferences = context.getSharedPreferences("folder_expansion", Context.MODE_PRIVATE)

    override fun read(): Set<String> = preferences.getStringSet("toggled_keys", emptySet()).orEmpty().toSet()

    override fun write(keys: Set<String>) {
        preferences.edit().putStringSet("toggled_keys", keys.toSet()).apply()
    }
}
