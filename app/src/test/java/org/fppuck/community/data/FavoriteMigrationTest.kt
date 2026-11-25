package org.fppuck.community.data
import org.junit.Assert.*
import org.junit.Test
class FavoriteMigrationTest {
    @Test fun mergesAllProfilesAndNormalizesTeams() {
        val result = FavoriteMigration.plan(emptyMap<String, Any>(), mapOf("teams.1" to setOf(" dht ", ""), "teams.2" to setOf("DHT", "car"), "player.1.a.b" to "Alpha", "player.2.c" to "Beta"), emptySet(), emptyMap<String, Any>())
        assertEquals(setOf("DHT", "CAR"), result.teams)
        assertEquals(mapOf("a.b" to "Alpha", "c" to "Beta"), result.players)
    }
    @Test fun activeProfileWinsDuplicateNameAndCurrentWinsBoth() {
        val old = mapOf("last_active_profile" to 2L, "player.1.a" to "One", "player.2.a" to "Two")
        assertEquals("One", FavoriteMigration.plan(emptyMap<String, Any>(), old + ("last_active_profile" to 1L), emptySet(), emptyMap<String, Any>()).players["a"])
        assertEquals("Two", FavoriteMigration.plan(emptyMap<String, Any>(), old, emptySet(), emptyMap<String, Any>()).players["a"])
        assertEquals("Current", FavoriteMigration.plan(mapOf("player.a" to "Current"), old, emptySet(), emptyMap<String, Any>()).players["a"])
    }
    @Test fun completedMigrationNeverResurrectsRemovedFavorites() {
        val result = FavoriteMigration.plan(mapOf("profile_migration_v1" to true), mapOf("teams.1" to setOf("DHT"), "player.1.a" to "Old"), setOf("CAR"), mapOf("b" to "Older"))
        assertTrue(result.teams.isEmpty()); assertTrue(result.players.isEmpty())
    }
    @Test fun alreadyScopedLegacyDoesNotResurrectUnscopedEntries() {
        val result = FavoriteMigration.plan(emptyMap<String, Any>(), mapOf("migrated" to true), setOf("CAR"), mapOf("a" to "Old"))
        assertTrue(result.teams.isEmpty()); assertTrue(result.players.isEmpty())
    }
    @Test fun legacyGuestFavoritesPreservedAndCorruptValuesIgnored() {
        val result = FavoriteMigration.plan(emptyMap<String, Any>(), mapOf("teams.bad" to setOf("BAD"), "teams.0" to setOf("BAD"), "teams.2" to 42, "player.1." to "Blank", "player.1.a" to 3, "player.-2.b" to "Bad"), setOf("car"), mapOf("good" to "Good", "" to "Bad", "bad" to 42))
        assertEquals(setOf("CAR"), result.teams); assertEquals(mapOf("good" to "Good"), result.players)
    }
}
