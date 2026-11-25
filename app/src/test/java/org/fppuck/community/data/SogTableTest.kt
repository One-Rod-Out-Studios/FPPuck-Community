package org.fppuck.community.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fixture-based JVM tests for [parseShotsOnGoal] using the real historical sample.
 *
 * No mocks, no reflection: the actual parsing function is exercised directly on
 * tab-delimited lines exactly as they appear in a game-details feed.
 */
class SogTableTest {

    // The real historical sample (tab-delimited).
    private val realSample = listOf(
        "SHOTS",
        "Team\t1st\t2nd\t3rd\tOT\tT",
        "Indiana Sentinels\t8\t10\t9\t4\t32",
        "Watertown Wolves\t11\t4\t11\t2\t28",
        "3 STARS"
    )

    @Test
    fun realSampleParsesBothTeamsInOrder() {
        val rows = parseShotsOnGoal(realSample)
        assertEquals(2, rows.size)
        // Order must match the fixture order: home first, away second.
        assertEquals("Indiana Sentinels", rows[0].teamName)
        assertEquals("Watertown Wolves", rows[1].teamName)
    }

    @Test
    fun realSamplePerPeriodValuesAndTotals() {
        val rows = parseShotsOnGoal(realSample)

        // Indiana Sentinels: 8/10/9 total, T=32 (OT included in T).
        assertEquals(8, rows[0].first)
        assertEquals(10, rows[0].second)
        assertEquals(9, rows[0].third)
        assertEquals(32, rows[0].total)

        // Watertown Wolves: 11/4/11 total, T=28.
        assertEquals(11, rows[1].first)
        assertEquals(4, rows[1].second)
        assertEquals(11, rows[1].third)
        assertEquals(28, rows[1].total)
    }

    @Test
    fun totalComesFromTColumnNotOt() {
        val rows = parseShotsOnGoal(realSample)
        // T is the authoritative published total (32 and 28), not a calculated
        // regulation+OT sum. Indiana's printed T=32 differs from 8+10+9+4=31,
        // so the parser must trust the T column verbatim.
        assertEquals(32, rows[0].total)
        assertEquals(28, rows[1].total)
    }

    @Test
    fun noOtSampleParsesWithoutOtColumn() {
        val lines = listOf(
            "SHOTS",
            "Team\t1st\t2nd\t3rd\tT",
            "Indiana Sentinels\t8\t10\t9\t27",
            "Watertown Wolves\t11\t4\t11\t26",
            "3 STARS"
        )
        val rows = parseShotsOnGoal(lines)
        assertEquals(2, rows.size)
        assertEquals("Indiana Sentinels", rows[0].teamName)
        assertEquals(8, rows[0].first)
        assertEquals(10, rows[0].second)
        assertEquals(9, rows[0].third)
        assertEquals(27, rows[0].total)
        assertEquals("Watertown Wolves", rows[1].teamName)
        assertEquals(26, rows[1].total)
    }

    @Test
    fun missingShotsSectionReturnsUnavailable() {
        val lines = listOf(
            "PERIOD SCORES",
            "Team\t1st\t2nd\t3rd\tT",
            "Indiana Sentinels\t3\t2\t4\t9",
            "3 STARS"
        )
        // No SHOTS heading -> statistics unavailable, not fabricated zeros.
        assertEquals(emptyList<GamePeriodBreakdown>(), parseShotsOnGoal(lines))
    }

    @Test
    fun missingTotalColumnRejectsTable() {
        val lines = listOf(
            "SHOTS",
            "Team\t1st\t2nd\t3rd",
            "Indiana Sentinels\t8\t10\t9",
            "Watertown Wolves\t11\t4\t11",
            "3 STARS"
        )
        // Required T column is absent -> table unavailable, never fabricated.
        assertEquals(emptyList<GamePeriodBreakdown>(), parseShotsOnGoal(lines))
    }

    @Test
    fun trueZeroIsParsedAsZeroNotUnavailable() {
        val lines = listOf(
            "SHOTS",
            "Team\t1st\t2nd\t3rd\tOT\tT",
            "Indiana Sentinels\t0\t0\t0\t0\t0",
            "Watertown Wolves\t11\t4\t11\t2\t28",
            "3 STARS"
        )
        val rows = parseShotsOnGoal(lines)
        assertEquals(2, rows.size)
        // A genuine zero shot line must remain a real zero, not null.
        assertEquals(0, rows[0].first)
        assertEquals(0, rows[0].second)
        assertEquals(0, rows[0].third)
        assertEquals(0, rows[0].total)
    }

    @Test
    fun parsingIsIsolatedFromSubsequentSections() {
        // A following section (3 STARS and beyond) must never be consumed.
        val lines = listOf(
            "SHOTS",
            "Team\t1st\t2nd\t3rd\tOT\tT",
            "Indiana Sentinels\t8\t10\t9\t4\t32",
            "Watertown Wolves\t11\t4\t11\t2\t28",
            "3 STARS",
            "Player\tTeam\tG\tA\tP",
            "Sidney Crosby\tIndiana Sentinels\t2\t1\t3"
        )
        val rows = parseShotsOnGoal(lines)
        // Only the two shot rows are consumed; star/player rows are not.
        assertEquals(2, rows.size)
        assertEquals("Indiana Sentinels", rows[0].teamName)
        assertEquals("Watertown Wolves", rows[1].teamName)
    }

    @Test
    fun originalTeamNamesArePreservedVerbatim() {
        val rows = parseShotsOnGoal(realSample)
        // Names must be preserved exactly, no normalization.
        assertEquals("Indiana Sentinels", rows[0].teamName)
        assertEquals("Watertown Wolves", rows[1].teamName)
    }
}
