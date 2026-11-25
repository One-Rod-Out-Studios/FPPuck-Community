package org.fppuck.community.data

data class LeagueStanding(
    val rank: Int,
    val teamName: String,
    val division: String,
    val conference: String,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val overtimeLosses: Int,
    val points: Int
)