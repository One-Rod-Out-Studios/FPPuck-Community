package org.fppuck.community.data
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
@RunWith(AndroidJUnit4::class)
class DeviceFavoritesPersistenceTest {
    @Test fun migratesPersistsAndDoesNotReimportRemovedFavorites() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "favorite-test-${UUID.randomUUID()}-"
        val names = mutableSetOf<String>()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                names.add(prefix + name)
                return base.getSharedPreferences(prefix + name, mode)
            }
            override fun getDatabasePath(name: String): File = base.getDatabasePath(prefix + name)
            override fun deleteDatabase(name: String): Boolean = base.deleteDatabase(prefix + name)
        }
        try {
            val old = context.getSharedPreferences("fppuck_profile_favorites", Context.MODE_PRIVATE)
            assertTrue(old.edit().putBoolean("migrated", true).putStringSet("teams.1", setOf("DHT")).putStringSet("teams.2", setOf("CAR")).putString("player.1.p", "Player").commit())
            val settings = context.getSharedPreferences("fphl_preferences", Context.MODE_PRIVATE)
            assertTrue(settings.edit().putString("appearance", "DARK").commit())
            assertEquals(setOf("DHT", "CAR"), FavoriteTeamsStore.load(context))
            assertEquals(mapOf("p" to "Player"), FavoritePlayersStore.load(context))
            assertTrue(old.all.isEmpty())
            assertEquals("DARK", settings.getString("appearance", null))
            FavoritePlayersStore.remove(context, "p")
            FavoritePlayersStore.set(context, "new", "New Player")
            assertTrue(DeviceFavoritesStore.prefs(context).edit().commit())
            // A leftover source after a cleanup interruption must never reintroduce old data.
            assertTrue(old.edit().putString("player.1.p", "Player").commit())
            assertEquals(mapOf("new" to "New Player"), FavoritePlayersStore.load(context))
            assertTrue(old.all.isEmpty())
        } finally {
            names.forEach { base.deleteSharedPreferences(it) }
            base.deleteDatabase(prefix + "fppuck.db")
        }
    }
}
