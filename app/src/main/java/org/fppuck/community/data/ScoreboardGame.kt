package org.fppuck.community.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ScoreboardGame(
    val date: LocalDate,
    val awayAbbreviation: String,
    val homeAbbreviation: String,
    val awayScore: Int? = null,
    val homeScore: Int? = null,
    val status: String,
    val watchUrl: String? = null
) {

    fun localStatusLabel(): String {

        val match =
            TIME_STATUS_REGEX.find(
                status.trim()
            )
                ?: return status

        var hour =
            match.groupValues[1]
                .toInt()

        val minute =
            match.groupValues[2]
                .toInt()

        val amPm =
            match.groupValues[3]
                .uppercase(Locale.US)

        val zoneAbbreviation =
            match.groupValues[4]
                .uppercase(Locale.US)

        if (
            amPm == "AM" &&
            hour == 12
        ) {
            hour = 0
        }

        if (
            amPm == "PM" &&
            hour != 12
        ) {
            hour += 12
        }

        val sourceZone =
            zoneForAbbreviation(
                zoneAbbreviation
            )
                ?: return status

        val sourceDateTime =
            ZonedDateTime.of(
                date,
                LocalTime.of(
                    hour,
                    minute
                ),
                sourceZone
            )

        val localDateTime =
            sourceDateTime
                .withZoneSameInstant(
                    ZoneId.systemDefault()
                )

        return localDateTime.format(
            DateTimeFormatter.ofPattern(
                "h:mm a z",
                Locale.US
            )
        )
    }

    private fun zoneForAbbreviation(
        abbreviation: String
    ): ZoneId? {

        return when (
            abbreviation.uppercase(
                Locale.US
            )
        ) {

            "EST",
            "EDT" ->
                ZoneId.of(
                    "America/New_York"
                )

            "CST",
            "CDT" ->
                ZoneId.of(
                    "America/Chicago"
                )

            "MST",
            "MDT" ->
                ZoneId.of(
                    "America/Denver"
                )

            "PST",
            "PDT" ->
                ZoneId.of(
                    "America/Los_Angeles"
                )

            else ->
                null
        }
    }

    companion object {

        private val TIME_STATUS_REGEX =
            Regex(
                """(?i)(\d{1,2}):(\d{2})\s*(AM|PM)\s*([ECMP][DS]T)"""
            )
    }
}