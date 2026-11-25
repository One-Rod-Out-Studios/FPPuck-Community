package org.fppuck.community.data

/** Pure migration: merge every legacy profile, with the active profile winning name conflicts. */
internal object FavoriteMigration {
    data class Snapshot(val teams: Set<String>, val players: Map<String, String>)
    fun plan(current: Map<String, *>, profiles: Map<String, *>, oldTeams: Set<String>, oldPlayers: Map<String, *>): Snapshot {
        val teams = linkedSetOf<String>()
        val players = linkedMapOf<String, String>()
        fun addTeams(value: Any?) {
            (value as? Set<*>)?.filterIsInstance<String>()?.forEach {
                val team = it.trim().uppercase(java.util.Locale.ROOT)
                if (team.isNotBlank()) teams.add(team)
            }
        }
        fun addPlayer(id: String, value: Any?) {
            if (id.isNotBlank() && value is String && value.isNotBlank()) players[id] = value
        }
        if (current["profile_migration_v1"] != true) {
            if (profiles["migrated"] != true) {
                addTeams(oldTeams)
                oldPlayers.toSortedMap().forEach { (id, name) -> addPlayer(id, name) }
            }
            val active = (profiles["last_active_profile"] as? Number)?.toLong()
            val ordered = profiles.entries.sortedWith(compareBy<Map.Entry<String, *>> {
                it.key.substringAfter('.').substringBefore('.').toLongOrNull() == active
            }.thenBy { it.key })
            ordered.forEach { (key, value) ->
                if (key.startsWith("teams.") && key.substringAfter('.').toLongOrNull()?.let { it > 0 } == true) addTeams(value)
                if (key.startsWith("player.")) {
                    val suffix = key.removePrefix("player.")
                    if (suffix.substringBefore('.').toLongOrNull()?.let { it > 0 } == true && '.' in suffix)
                        addPlayer(suffix.substringAfter('.'), value)
                }
            }
        }
        addTeams(current["teams"])
        current.toSortedMap().forEach { (key, value) ->
            if (key.startsWith("player.")) addPlayer(key.removePrefix("player."), value)
        }
        return Snapshot(teams, players)
    }
}
