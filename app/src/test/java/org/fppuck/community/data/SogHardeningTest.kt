package org.fppuck.community.data

import org.junit.Assert.assertEquals
import org.junit.Test

private const val H = "Team\t1st\t2nd\t3rd\tOT\tT"

class SogHardeningTest {

    private fun table(rowA: String, rowB: String) =
        listOf("SHOTS", H, rowA, rowB, "", "NEXT SECTION", "7\t1\t2\t3\t4")

    @Test fun shotsRowsSurviveInBothOrders() {
        val good = "Wings\t1\t2\t3\t0\t6"
        val bad = "Bruins\tx\t2\t3\t0\t5"
        assertEquals(listOf("Wings"), parseShotsOnGoal(table(good, bad)).map { it.teamName })
        assertEquals(listOf("Wings"), parseShotsOnGoal(table(bad, good)).map { it.teamName })
    }

    @Test fun shotsRowsSurviveInBothOrdersZeroTotal() {
        val zero = "Wings\t0\t0\t0\t0\t0"
        val bad = "Bruins\tx\t2\t3\t0\t5"
        val rows = parseShotsOnGoal(table(zero, bad))
        assertEquals(1, rows.size)
        assertEquals("Wings", rows[0].teamName)
        assertEquals(0, rows[0].total)
    }

    @Test fun mixedCaseHeadingSpacersAndExplicitStop() {
        val lines = listOf(
            "Shots",
            "", "   ",
            H,
            "", "\t", "  \t  ",
            "Wings\t1\t2\t3\t0\t6",
            "NEXT SECTION",
            "7\t1\t2\t3\t4"
        )
        val rows = parseShotsOnGoal(lines)
        assertEquals(1, rows.size)
        assertEquals("Wings", rows[0].teamName)
        assertEquals(6, rows[0].total)
    }

    @Test fun profileKeepsSeasonsAndBlankSog() {
        val text = "Jean-Luc Dupont\n#9 F\nIce Bears\nPlayer Career Stats\n" +
            "Pos\tGP\tG\tA\tPts\tPPG\tPIM\tSh%\tPPG goals\tSHG\tGWG\t+/-\tSOG\n" +
            "F\t10\t4\t6\t10\t1\t12\t50.0\t3\t0\t1\t+2\t\n" +
            "F\t12\t5\t7\t12\t2\t18\t55.0\t4\t0\t2\t-1\t0\n" +
            "Season Team Division\n" +
            "FPHL 2023\nIce Bears\tDiv A\n" +
            "FPHL 2024\nIce Bears\tDiv B"
        val m = PlayerRepository::class.java.getDeclaredMethod(
            "parseProfile", String::class.java, String::class.java)
        m.isAccessible = true
        val profile = m.invoke(PlayerRepository, "7", text) as PlayerProfile
        val seasons = profile.seasons
        assertEquals(2, seasons.size)
        assertEquals("FPHL 2023", seasons[0].season)
        assertEquals("FPHL 2024", seasons[1].season)
        assertEquals("", seasons[0].shotsOnGoal)
        assertEquals("0", seasons[1].shotsOnGoal)
        // Existing stat stays aligned with its header column.
        assertEquals("10", seasons[0].points)
        assertEquals("12", seasons[1].points)
    }
}
