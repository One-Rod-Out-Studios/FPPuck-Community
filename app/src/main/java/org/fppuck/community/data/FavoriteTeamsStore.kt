package org.fppuck.community.data

import android.content.Context

object FavoriteTeamsStore {

    fun load(context: Context): Set<String> {
        return DeviceFavoritesStore.prefs(context)
            .getStringSet("teams", emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun toggle(
        context: Context,
        currentFavorites: Set<String>,
        abbreviation: String
    ): Set<String> {
        val normalized = abbreviation.trim().uppercase(java.util.Locale.ROOT)
        if (normalized.isBlank()) return currentFavorites

        val stored = DeviceFavoritesStore.prefs(context)
            .getStringSet("teams", emptySet())
            ?.toSet()
            ?: emptySet()

        val updated = stored.toMutableSet()
        if (normalized in updated) {
            updated.remove(normalized)
        } else {
            updated.add(normalized)
        }

        DeviceFavoritesStore.prefs(context)
            .edit()
            .putStringSet("teams", updated.toSet())
            .apply()

        return updated.toSet()
    }
}
