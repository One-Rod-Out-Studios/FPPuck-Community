package org.fppuck.community.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TeamRepository {

    suspend fun fetchTeamOverviewStats(
        context: Context,
        teamAbbreviation: String
    ): TeamOverviewStats {

        val text =
            fetchTeamPage(
                context = context,
                teamAbbreviation = teamAbbreviation
            )

        return withContext(Dispatchers.Default) {
            parseTeamOverviewStats(text)
        }
    }
    private fun parseTeamOverviewStats(
        text: String
    ): TeamOverviewStats {

        val lines =
            cleanLines(text)

        fun valueAfter(label: String): String {

            val index =
                lines.indexOfFirst {
                    it.equals(
                        label,
                        ignoreCase = true
                    )
                }

            return if (
                index >= 0 &&
                index + 1 < lines.size
            ) {
                lines[index + 1]
            } else {
                "—"
            }
        }

        return TeamOverviewStats(
            streak =
                valueAfter("STREAK"),

            goalsForAverage =
                valueAfter("GOALS FOR AVERAGE"),

            goalDifferential =
                valueAfter("GOAL DIFFERENTIAL"),

            powerPlayPercent =
                valueAfter("POWER PLAY %"),

            penaltyKillPercent =
                valueAfter("PENALTY KILL %")
        )
    }
    private val teamIds =
        mapOf(
            "BBB" to "684207",
            "BRB" to "684208",
            "DHT" to "684210",
            "TCT" to "684205",
            "IND" to "684211",
            "PHP" to "684213",
            "MCR" to "684212",
            "WTW" to "684215",
            "MRM" to "684200",
            "BKF" to "684206",
            "CRD" to "684203",
            "MSM" to "684284",
            "FRF" to "684285",
            "TOP" to "684214",
            "SCK" to "684286",
            "MNL" to "684281",
            "OCS" to "684287"
        )

    private val pageCache =
        mutableMapOf<String, String>()

    suspend fun fetchRoster(
        context: Context,
        teamAbbreviation: String
    ): List<TeamPlayer> {

        val text =
            fetchTeamPage(
                context = context,
                teamAbbreviation = teamAbbreviation
            )

        return withContext(Dispatchers.Default) {
            parseRoster(text)
        }
    }

    suspend fun fetchPlayerStats(
        context: Context,
        teamAbbreviation: String
    ): List<TeamPlayerStat> {

        val text =
            fetchTeamPage(
                context = context,
                teamAbbreviation = teamAbbreviation
            )

        return withContext(Dispatchers.Default) {
            parsePlayerStats(text)
        }
    }

    private suspend fun fetchTeamPage(
        context: Context,
        teamAbbreviation: String
    ): String {

        pageCache[teamAbbreviation]?.let {
            return it
        }

        val teamId =
            teamIds[teamAbbreviation]
                ?: error(
                    "No HockeyShift team ID for $teamAbbreviation"
                )

        val url =
            "https://www.federalhockey.com/stats" +
                    "?mobile-app=true&theme=false" +
                    "#/182/team/$teamId"

        val text =
            renderPage(
                context = context,
                url = url
            )

        pageCache[teamAbbreviation] = text

        return text
    }

    private fun cleanLines(
        text: String
    ): List<String> {

        return text.lines()
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
    }

    private fun findStatsHeader(
        lines: List<String>
    ): Int {

        return lines.indexOfFirst {
            it.startsWith("Pos") &&
                    it.contains("GP") &&
                    it.contains("Pts")
        }
    }

    private fun findNamesHeader(
        lines: List<String>
    ): Int {

        return lines.indexOfFirst {
            it.startsWith("#") &&
                    it.contains("Name")
        }
    }

    private fun findStatRows(
        lines: List<String>
    ): List<List<String>> {

        val statsHeaderIndex =
            findStatsHeader(lines)

        val namesHeaderIndex =
            findNamesHeader(lines)

        if (
            statsHeaderIndex < 0 ||
            namesHeaderIndex <= statsHeaderIndex
        ) {
            return emptyList()
        }

        return lines
            .subList(
                statsHeaderIndex + 1,
                namesHeaderIndex
            )
            .map {
                it.split("\t")
                    .map(String::trim)
            }
            .filter { parts ->

                parts.size >= 13 &&
                        parts[1].toIntOrNull() != null
            }
    }

    private fun parseRoster(
        text: String
    ): List<TeamPlayer> {

        val lines =
            cleanLines(text)

        val statRows =
            findStatRows(lines)

        val namesHeaderIndex =
            findNamesHeader(lines)

        if (namesHeaderIndex < 0) {
            return emptyList()
        }

        val roster =
            mutableListOf<TeamPlayer>()

        lines
            .drop(namesHeaderIndex + 1)
            .takeWhile {
                !it.startsWith(
                    "Admin Login",
                    ignoreCase = true
                )
            }
            .forEach { line ->

                val parts =
                    line.split("\t")
                        .map(String::trim)

                if (parts.isEmpty()) {
                    return@forEach
                }

                val number =
                    parts
                        .firstOrNull()
                        ?.takeIf {
                            it.isNotBlank() &&
                                    it.all(Char::isDigit)
                        }

                val rawName =
                    parts
                        .lastOrNull()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: return@forEach

                val rookie =
                    rawName.contains(" - R")

                val cleanedName =
                    rawName
                        .replace(
                            Regex("#\\d+"),
                            ""
                        )
                        .replace(
                            " - R",
                            ""
                        )
                        .trim()

                if (
                    cleanedName.equals(
                        "Name",
                        ignoreCase = true
                    )
                ) {
                    return@forEach
                }

                val position =
                    statRows
                        .getOrNull(roster.size)
                        ?.getOrNull(0)
                        ?: ""

                roster.add(
                    TeamPlayer(
                        number = number,
                        name =
                            formatPlayerName(
                                cleanedName
                            ),
                        position = position,
                        rookie = rookie
                    )
                )
            }

        return roster
    }

    private fun parsePlayerStats(
        text: String
    ): List<TeamPlayerStat> {

        val lines =
            cleanLines(text)

        val roster =
            parseRoster(text)

        val statRows =
            findStatRows(lines)

        return statRows.mapIndexedNotNull {
                index,
                columns ->

            val player =
                roster.getOrNull(index)
                    ?: return@mapIndexedNotNull null

            TeamPlayerStat(
                number = player.number,
                name = player.name,
                position =
                    columns.getOrElse(0) { "" },
                rookie = player.rookie,
                gamesPlayed =
                    columns.getOrElse(1) { "" },
                goals =
                    columns.getOrElse(2) { "" },
                assists =
                    columns.getOrElse(3) { "" },
                points =
                    columns.getOrElse(4) { "" },
                pointsPerGame =
                    columns.getOrElse(5) { "" },
                penaltyMinutes =
                    columns.getOrElse(6) { "" },
                shootingPercent =
                    columns.getOrElse(7) { "" },
                powerPlayGoals =
                    columns.getOrElse(8) { "" },
                shortHandedGoals =
                    columns.getOrElse(9) { "" },
                gameWinningGoals =
                    columns.getOrElse(10) { "" },
                plusMinus =
                    columns.getOrElse(11) { "" },
                shotsOnGoal =
                    columns.getOrElse(12) { "" }
            )
        }
    }

    private fun formatPlayerName(
        sourceName: String
    ): String {

        if (!sourceName.contains(",")) {
            return sourceName
        }

        val parts =
            sourceName.split(
                ",",
                limit = 2
            )

        if (parts.size != 2) {
            return sourceName
        }

        val lastName =
            parts[0].trim()

        val firstName =
            parts[1].trim()

        return "$firstName $lastName"
    }

    private suspend fun renderPage(
        context: Context,
        url: String
    ): String =
        withContext(Dispatchers.Main) {

            suspendCancellableCoroutine { continuation ->

                val webView =
                    WebView(context)

                val handler =
                    Handler(
                        Looper.getMainLooper()
                    )

                var captureScheduled = false

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            if (captureScheduled) {
                                return
                            }

                            captureScheduled = true

                            handler.postDelayed(
                                {

                                    view.evaluateJavascript(
                                        """
                                        (function() {
                                            if (!document.body) {
                                                return "";
                                            }

                                            return document.body.innerText;
                                        })()
                                        """.trimIndent()
                                    ) { result ->

                                        try {

                                            val text =
                                                if (
                                                    result == null ||
                                                    result == "null"
                                                ) {
                                                    ""
                                                } else {

                                                    JSONTokener(result)
                                                        .nextValue()
                                                        .toString()
                                                }

                                            webView.destroy()

                                            if (
                                                continuation.isActive
                                            ) {
                                                continuation.resume(
                                                    text
                                                )
                                            }

                                        } catch (
                                            exception: Exception
                                        ) {

                                            webView.destroy()

                                            if (
                                                continuation.isActive
                                            ) {
                                                continuation
                                                    .resumeWithException(
                                                        exception
                                                    )
                                            }
                                        }
                                    }

                                },
                                6000
                            )
                        }
                    }

                continuation.invokeOnCancellation {

                    handler.removeCallbacksAndMessages(
                        null
                    )

                    webView.destroy()
                }

                webView.loadUrl(url)
            }
        }
}
