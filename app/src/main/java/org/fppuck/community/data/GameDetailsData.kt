package org.fppuck.community.data

data class GamePeriodBreakdown(
    val teamName: String,
    val first: Int,
    val second: Int,
    val third: Int,
    val total: Int
)

data class GameScoringEvent(
    val period: String,
    val time: String,
    val strength: String,
    val teamName: String,
    val description: String,
    val scoreAfterGoal: String
)

data class GamePenalty(
    val period: String,
    val time: String,
    val player: String,
    val teamName: String,
    val infraction: String,
    val length: String
)

data class GameStar(
    val rank: String,
    val playerName: String,
    val statLine: String,
    val teamName: String
)

data class GameOfficial(
    val type: String,
    val name: String
)

data class GameDetailsData(
    val gameId: String,
    val dateTimeVenue: String?,
    val periodScores: List<GamePeriodBreakdown>,
    val shots: List<GamePeriodBreakdown>,
    val scoringEvents: List<GameScoringEvent>,
    val penalties: List<GamePenalty>,
    val stars: List<GameStar>,
    val officials: List<GameOfficial>,
    val startTime: String?,
    val endTime: String?,
    val attendance: String?
)