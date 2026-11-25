package org.fppuck.community.data

import org.jsoup.Jsoup
import java.util.Locale

/** Read named columns: the provider's W is total wins, not regulation wins. */
internal fun parseLeagueStandings(html: String): List<LeagueStanding> {
    val result = mutableListOf<LeagueStanding>()
    val divisions = listOf("Atlantic", "Great Lakes", "Delta", "Frontier")
    for (table in Jsoup.parse(html).select("table")) {
        val headers = table.select("thead tr").firstOrNull()?.children()
            ?.map { it.text().trim().lowercase(Locale.ROOT) } ?: continue
        val required = listOf("rk", "team", "gp", "w", "l", "ot", "pts")
        if (!required.all { it in headers } || headers.distinct().size != headers.size) continue
        var division = ""
        for (row in table.select("tbody > tr")) {
            val cells = row.children().filter { it.tagName() == "td" }
            if (cells.size == 1) {
                division = divisions.firstOrNull {
                    cells[0].text().trim().equals("$it DIVISION", ignoreCase = true)
                }.orEmpty()
                continue
            }
            if (division.isEmpty() || cells.size != headers.size) continue
            fun number(name: String): Int? = cells[headers.indexOf(name)].text().trim()
                .toIntOrNull()?.takeIf { it >= 0 }
            val rank = number("rk")?.takeIf { it > 0 } ?: continue
            val played = number("gp") ?: continue
            val wins = number("w") ?: continue
            val losses = number("l") ?: continue
            val overtime = number("ot") ?: continue
            val points = number("pts") ?: continue
            if (wins.toLong() + losses + overtime > played) continue
            val teamText = cells[headers.indexOf("team")].text()
            val team = sampleTeams.firstOrNull {
                teamText.contains("${it.city} ${it.name}", ignoreCase = true)
            } ?: continue
            result += LeagueStanding(rank, "${team.city} ${team.name}", division,
                if (division in listOf("Atlantic", "Great Lakes")) "Eastern" else "Western",
                played, wins, losses, overtime, points)
        }
    }
    return result.distinctBy { it.teamName }
        .sortedWith(compareBy<LeagueStanding> { divisions.indexOf(it.division) }.thenBy { it.rank })
}
