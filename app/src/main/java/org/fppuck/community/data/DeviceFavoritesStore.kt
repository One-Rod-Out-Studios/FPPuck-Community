package org.fppuck.community.data

import android.content.Context
import android.content.SharedPreferences

/** One device collection. Legacy identities are retired only after the merged snapshot reaches disk. */
object DeviceFavoritesStore {
    @Synchronized
    internal fun prefs(context: Context): SharedPreferences {
        val app = context.applicationContext
        val target = app.getSharedPreferences("fppuck_favorites", Context.MODE_PRIVATE)
        val legacy = app.getSharedPreferences("fppuck_profile_favorites", Context.MODE_PRIVATE)
        val oldPlayers = app.getSharedPreferences("fppuck_favorite_players", Context.MODE_PRIVATE)
        val settings = app.getSharedPreferences("fphl_preferences", Context.MODE_PRIVATE)
        if (!target.getBoolean("profile_migration_v1", false) || legacy.all.isNotEmpty() ||
            oldPlayers.all.isNotEmpty() || settings.contains("favorite_teams") || app.getDatabasePath("fppuck.db").exists()) {
            val snapshot = FavoriteMigration.plan(target.all, legacy.all,
                (settings.all["favorite_teams"] as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty(), oldPlayers.all)
            val editor = target.edit().putStringSet("teams", snapshot.teams)
                .putBoolean("profile_migration_v1", true)
                // Force a disk write even when a prior failed commit changed the in-memory snapshot.
                .putString("migration_write_nonce", java.util.UUID.randomUUID().toString())
            snapshot.players.forEach { (id, name) -> editor.putString("player.$id", name) }
            check(editor.commit()) { "Unable to safely save favorites. Please retry." }
            legacy.edit().clear().commit()
            oldPlayers.edit().clear().commit()
            settings.edit().remove("favorite_teams").commit()
            app.deleteDatabase("fppuck.db")
        }
        return target
    }

    fun initialize(context: Context) { prefs(context) }
}
