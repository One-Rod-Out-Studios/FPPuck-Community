package org.fppuck.community.data

enum class GameStatus {
    LIVE,
    FINAL,
    FINAL_OT,
    UPCOMING
}

data class HockeyGame(
    val status: GameStatus,
    val time: String = "",
    val awayTeam: String,
    val awayScore: Int? = null,
    val homeTeam: String,
    val homeScore: Int? = null
)