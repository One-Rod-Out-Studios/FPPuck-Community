package org.fppuck.community.data

data class TeamPlayerStat(
    val number: String?,
    val name: String,
    val position: String,
    val rookie: Boolean,
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