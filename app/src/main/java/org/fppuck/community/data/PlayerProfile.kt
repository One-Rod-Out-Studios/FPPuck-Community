package org.fppuck.community.data

data class PlayerSeasonStat(
    val season: String,
    val teamName: String,
    val division: String,
    val position: String,
    val gamesPlayed: String,
    val goals: String,
    val assists: String,
    val points: String,
    val pointsPerGame: String,
    val penaltyMinutes: String,
    val shootingPercent: String,
    val powerPlayGoals: String,
    val shortHandedGoals: String,
    val gameWinningGoals: String,
    val plusMinus: String,
    val shotsOnGoal: String
)

data class PlayerProfile(
    val playerId: String,
    val name: String,
    val number: String?,
    val position: String,
    val teamName: String,
    val seasons: List<PlayerSeasonStat>,
    val careerTotals: PlayerSeasonStat?
)