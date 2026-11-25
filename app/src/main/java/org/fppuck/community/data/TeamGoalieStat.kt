package org.fppuck.community.data

data class TeamGoalieStat(
    val number: String?,
    val name: String,
    val rookie: Boolean,
    val gamesPlayed: String,
    val wins: String,
    val losses: String,
    val ties: String,
    val overtimeLosses: String,
    val shotsAgainst: String,
    val goalsAgainst: String,
    val goalsAgainstAverage: String,
    val saves: String,
    val savePercent: String,
    val shutouts: String,
    val minutesPlayed: String
)