package org.fppuck.community.data

import android.content.Context

/** Device-local player favorites, available without creating an account. */
object FavoritePlayersStore {
    fun load(context: Context): Map<String, String> = DeviceFavoritesStore.prefs(context).all
        .filter { (key, value) -> key.startsWith("player.") && key.removePrefix("player.").isNotBlank() && value is String && value.isNotBlank() }
        .mapKeys { it.key.removePrefix("player.") }.mapValues { it.value as String }

    fun set(context: Context, id: String, name: String) {
        if (id.isBlank() || name.isBlank()) return
        DeviceFavoritesStore.prefs(context).edit().putString("player.$id", name).apply()
    }

    fun remove(context: Context, id: String) {
        if (id.isBlank()) return
        DeviceFavoritesStore.prefs(context).edit().remove("player.$id").apply()
    }
}
