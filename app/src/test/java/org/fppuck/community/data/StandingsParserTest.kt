package org.fppuck.community.data

import org.junit.Assert.*
import org.junit.Test

class StandingsParserTest {
    private fun table(headers: List<String>, values: List<String>, division: String = "ATLANTIC") =
        "<table><thead><tr>" + headers.joinToString("") { "<th>$it</th>" } +
            "</tr></thead><tbody><tr><td colspan='99'>$division DIVISION</td></tr><tr>" +
            values.joinToString("") { "<td>$it</td>" } + "</tr></tbody></table>"
    private val headers = listOf("Rk", "Team", "GP", "W", "L", "OT", "Pts", "W%", "GF", "GA")
    private val values = listOf("1", "Binghamton Black Bears BBB", "12", "11", "1", "0", "22", ".917", "61", "23")

    @Test fun currentProviderLayoutUsesTotalWinsAndPointsNotGoals() {
        val row = parseLeagueStandings(table(headers, values)).single()
        assertEquals(12, row.gamesPlayed)
        assertEquals(11, row.wins)
        assertEquals(1, row.losses)
        assertEquals(0, row.overtimeLosses)
        assertEquals(22, row.points)
        assertEquals("Atlantic", row.division)
        assertEquals("Eastern", row.conference)
    }

    @Test fun reorderedColumnsAndDuplicatedResponsiveTablesPreserveValues() {
        val order = listOf(1, 6, 8, 0, 5, 3, 9, 4, 2, 7)
        val html = table(order.map { headers[it] }, order.map { values[it] })
        assertEquals(parseLeagueStandings(table(headers, values)), parseLeagueStandings(html + html))
    }

    @Test fun malformedUnknownAndImpossibleRowsCannotBecomeInventedZeroStats() {
        assertTrue(parseLeagueStandings(table(headers, values.toMutableList().apply { this[3] = "33" })).isEmpty())
        assertTrue(parseLeagueStandings(table(headers, values.toMutableList().apply { this[6] = "?" })).isEmpty())
        assertTrue(parseLeagueStandings(table(headers.map { if (it == "Pts") "GF" else it }, values)).isEmpty())
        assertTrue(parseLeagueStandings(table(headers, values, "UNKNOWN")).isEmpty())
    }
}
