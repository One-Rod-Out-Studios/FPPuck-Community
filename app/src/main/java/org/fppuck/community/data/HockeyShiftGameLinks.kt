package org.fppuck.community.data

import java.time.format.DateTimeFormatter
import java.util.Locale

private val TICKER_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)

private fun tickerMatches(rendered: String, game: ScoreboardGame): Set<String> {
    val ids = mutableSetOf<String>()
    for (line in rendered.split("\n")) {
        val parts = line.split("|||")
        if (parts.size != 4) continue
        if (parts[0] != "TICKER") continue
        val id = parts[1]
        val date = parts[2]
        val text = parts[3]
        if (!id.all { it in '0'..'9' } || id.isEmpty()) continue
        if (id.toIntOrNull() == null) continue
        if (id.startsWith("0")) continue
        val tokens = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.size < 4) continue
        val awayAbbrev = tokens[0]
        val awayScoreText = tokens[1]
        val homeAbbrev = tokens[2]
        val homeScoreText = tokens[3]
        val awayScore = awayScoreText.toIntOrNull() ?: continue
        val homeScore = homeScoreText.toIntOrNull() ?: continue
        if (awayScore < 0 || homeScore < 0) continue
        if (game.awayScore != null && game.awayScore != awayScore) continue
        if (game.homeScore != null && game.homeScore != homeScore) continue
        if (!awayAbbrev.equals(game.awayAbbreviation, ignoreCase = true)) continue
        if (!homeAbbrev.equals(game.homeAbbreviation, ignoreCase = true)) continue
        val expectedDate = game.date.format(TICKER_DATE_FORMAT)
        if (!date.equals(expectedDate, true)) continue
        ids.add(id)
    }
    return ids
}

internal fun resolveHockeyShiftTickerId(rendered: String, game: ScoreboardGame): String? {
    return tickerMatches(rendered, game).singleOrNull()
}

internal fun chooseHockeyShiftGameId(official: List<String>, rendered: String, game: ScoreboardGame): String? {
    val officialIds = official.toSet()
    val tickerIds = tickerMatches(rendered, game)
    if (officialIds.size > 1 || tickerIds.size > 1) return null
    return tickerIds.singleOrNull() ?: officialIds.singleOrNull()
}
