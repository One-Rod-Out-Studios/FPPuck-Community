package org.fppuck.community.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreboardGameTest {
    @Test
    fun resolvesHockeyShiftTickerId() {
        val game = ScoreboardGame(LocalDate.of(2026, 11, 7), "BRB", "TCT", 3, 4, "FINAL")
        val id = resolveHockeyShiftTickerId("TICKER|||1309148|||SAT NOV 7|||BRB 3 TCT 4 FINAL", game)
        assertEquals("1309148", id)
    }

    @Test
    fun parsesShotsOnGoalTotals() {
        val rows = listOf(
            "SHOTS",
            "Team\t1st\t2nd\t3rd\tT",
            "BRB\t2\t2\t2\t6",
            "TCT\t3\t3\t3\t9"
        )
        val shots = parseShotsOnGoal(rows)
        assertEquals(listOf(6, 9), shots.map { it.total })

        val more = listOf(
            "SHOTS",
            "Team\t1st\t2nd\t3rd\tT",
            "BRB\t4\t4\t3\t11",
            "TCT\t5\t4\t5\t14"
        )
        assertEquals(listOf(11, 14), parseShotsOnGoal(more).map { it.total })
    }
}
