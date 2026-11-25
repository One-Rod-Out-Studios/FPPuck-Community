package org.fppuck.community.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ScheduledGame(
    val gameNumber: Int,
    val date: LocalDate,
    val awayTeam: String,
    val homeTeam: String,
    val sourceStartDateTime: ZonedDateTime? = null,
    val sourceTimeZoneAbbreviation: String? = null,
    val watchUrl: String? = null
) {

    fun localStartDateTime(): ZonedDateTime? =
        sourceStartDateTime
            ?.withZoneSameInstant(
                ZoneId.systemDefault()
            )

    fun localStartTimeLabel(): String? =
        localStartDateTime()
            ?.format(
                DateTimeFormatter.ofPattern(
                    "h:mm a z",
                    Locale.US
                )
            )
}