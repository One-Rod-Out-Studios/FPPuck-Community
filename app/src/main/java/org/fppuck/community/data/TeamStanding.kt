package org.fppuck.community.data

data class TeamStanding(
    val rank: Int,
    val teamName: String,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val overtimeLosses: Int,
    val points: Int
)