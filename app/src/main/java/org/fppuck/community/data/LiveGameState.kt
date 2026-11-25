package org.fppuck.community.data

data class LiveGameState(
    val awayScore: Int? = null,
    val homeScore: Int? = null,
    val clock: String? = null,
    val period: String? = null,
    val lastPlay: LivePlay? = null
) {
    fun statusLabel(): String? {

        val cleanPeriod =
            period
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?.uppercase()

        val cleanClock =
            clock
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }

        return when {

            cleanPeriod == null ->
                null

            cleanPeriod.contains(
                "FINAL",
                ignoreCase = true
            ) ->
                "FINAL"

            cleanClock != null ->
                "$cleanPeriod $cleanClock"

            else ->
                cleanPeriod
        }
    }
}

data class LivePlay(
    val type: String,
    val teamAbbreviation: String? = null,
    val time: String? = null,
    val description: String = ""
)
