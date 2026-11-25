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

object PlayerRepository {

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

    private val playerIdCache =
        mutableMapOf<String, Map<String, String>>()

    private val profileCache =
        mutableMapOf<String, PlayerProfile>()

    suspend fun fetchPlayerProfile(
        context: Context,
        teamAbbreviation: String,
        playerName: String
    ): PlayerProfile {

        val playerId =
            resolvePlayerId(
                context = context,
                teamAbbreviation = teamAbbreviation,
                playerName = playerName
            )

        profileCache[playerId]?.let {
            return it
        }

        val url =
            "https://www.federalhockey.com/stats" +
                    "?mobile-app=true&theme=false" +
                    "#/player/$playerId"

        val text =
            renderTextPage(
                context = context,
                url = url
            )

        val profile =
            withContext(Dispatchers.Default) {
                parseProfile(
                    playerId = playerId,
                    text = text
                )
            }

        profileCache[playerId] = profile

        return profile
    }

    private suspend fun resolvePlayerId(
        context: Context,
        teamAbbreviation: String,
        playerName: String
    ): String {

        val normalizedTarget =
            normalizeName(playerName)

        playerIdCache[teamAbbreviation]
            ?.get(normalizedTarget)
            ?.let {
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

        val rawLinks =
            renderPlayerLinks(
                context = context,
                url = url
            )

        val players =
            mutableMapOf<String, String>()

        rawLinks
            .lines()
            .forEach { line ->

                val parts =
                    line.split(
                        "|||",
                        limit = 2
                    )

                if (parts.size != 2) {
                    return@forEach
                }

                val sourceName =
                    parts[0].trim()

                val href =
                    parts[1].trim()

                val playerId =
                    Regex("""/player/(\d+)""")
                        .find(href)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?: return@forEach

                players[
                    normalizeName(
                        formatName(sourceName)
                    )
                ] = playerId
            }

        players[normalizedTarget]
            ?.let { playerId ->

                playerIdCache[teamAbbreviation] =
                    players

                return playerId
            }

        /*
         * Not found in the skater table.
         * Try HockeyShift's separate goalie table.
         */
        val goalieLinks =
            renderGoaliePlayerLinks(
                context = context,
                url = url
            )

        goalieLinks
            .lines()
            .forEach { line ->

                val parts =
                    line.split(
                        "|||",
                        limit = 2
                    )

                if (parts.size != 2) {
                    return@forEach
                }

                val sourceName =
                    parts[0].trim()

                val href =
                    parts[1].trim()

                val playerId =
                    Regex("""/player/(\d+)""")
                        .find(href)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?: return@forEach

                players[
                    normalizeName(
                        formatName(sourceName)
                    )
                ] = playerId
            }

        playerIdCache[teamAbbreviation] =
            players

        return players[normalizedTarget]
            ?: error(
                "Unable to find HockeyShift player ID for $playerName"
            )
    }

    private fun parseProfile(
        playerId: String,
        text: String
    ): PlayerProfile {

        val lines =
            text.lines()
                .map {
                    // Preserve tab separators at the start and end of TSV
                    // lines (e.g. an empty final SOG column). Kotlin trim(),
                    // trimStart() and trimEnd() with no predicate remove tabs,
                    // so use a predicate that excludes '\t' from removable
                    // whitespace. Return true only for ordinary whitespace
                    // other than a tab; return false for letters, digits and
                    // tabs (e.g. keep the leading 'F' and final tab).
                    it.trim { character ->
                        character.isWhitespace() && character != '\t'
                    }
                }
                .filter {
                    it.isNotEmpty()
                }

        val careerHeaderIndex =
            lines.indexOfFirst {
                it.startsWith(
                    "Player Career Stats",
                    ignoreCase = true
                )
            }

        if (careerHeaderIndex < 0) {
            error("Unable to parse player profile.")
        }

        val metaIndex =
            (careerHeaderIndex - 1 downTo 0)
                .firstOrNull { index ->
                    Regex(
                        """^#\S+\s+.+$"""
                    ).matches(
                        lines[index]
                    )
                }
                ?: error(
                    "Unable to locate player information."
                )

        val name =
            lines.getOrNull(
                metaIndex - 1
            ) ?: "Unknown Player"

        val meta =
            lines[metaIndex]

        val metaMatch =
            Regex(
                """^#(\S+)\s+(.+)$"""
            ).find(meta)

        val number =
            metaMatch
                ?.groupValues
                ?.getOrNull(1)

        val position =
            metaMatch
                ?.groupValues
                ?.getOrNull(2)
                ?: ""

        val teamName =
            lines.getOrNull(
                metaIndex + 1
            ) ?: ""

        val statsHeaderIndex =
            lines.indexOfFirst {
                it.startsWith("Pos") &&
                        it.contains("GP") &&
                        it.contains("Pts") &&
                        it.contains("SOG")
            }

        val seasonHeaderIndex =
            lines.indexOfFirst {
                it.startsWith("Season") &&
                        it.contains("Team") &&
                        it.contains("Division")
            }

        if (
            statsHeaderIndex < 0 ||
            seasonHeaderIndex <= statsHeaderIndex
        ) {

            return PlayerProfile(
                playerId = playerId,
                name = name,
                number = number,
                position = position,
                teamName = teamName,
                seasons = emptyList(),
                careerTotals = null
            )
        }

        val statRows =
            lines
                .subList(
                    statsHeaderIndex + 1,
                    seasonHeaderIndex
                )
                .map {
                    it.split("\t")
                        .map(String::trim)
                }
                .filter {
                    it.size >= 13
                }

        val seasonInfo =
            mutableListOf<Triple<String, String, String>>()

        var index =
            seasonHeaderIndex + 1

        while (index < lines.size) {

            val line =
                lines[index]

            if (
                line.startsWith(
                    "Admin Login",
                    ignoreCase = true
                )
            ) {
                break
            }

            if (
                line.startsWith(
                    "FPHL ",
                    ignoreCase = true
                ) &&
                index + 1 < lines.size
            ) {

                val detail =
                    lines[index + 1]
                        .split("\t")
                        .map(String::trim)

                val season =
                    line

                val team =
                    detail.getOrElse(0) {
                        ""
                    }

                val division =
                    detail.getOrElse(1) {
                        ""
                    }

                seasonInfo.add(
                    Triple(
                        season,
                        team,
                        division
                    )
                )

                index += 2

            } else {

                index++
            }
        }

        val seasons =
            seasonInfo.mapIndexedNotNull {
                    rowIndex,
                    info ->

                val stats =
                    statRows.getOrNull(
                        rowIndex
                    ) ?: return@mapIndexedNotNull null

                buildSeasonStat(
                    season = info.first,
                    teamName = info.second,
                    division = info.third,
                    stats = stats
                )
            }

        val careerTotals =
            if (
                statRows.size >
                seasonInfo.size
            ) {

                buildSeasonStat(
                    season = "Career",
                    teamName = "",
                    division = "",
                    stats =
                        statRows.last()
                )

            } else {
                null
            }

        return PlayerProfile(
            playerId = playerId,
            name = name,
            number = number,
            position = position,
            teamName = teamName,
            seasons = seasons,
            careerTotals = careerTotals
        )
    }

    private fun buildSeasonStat(
        season: String,
        teamName: String,
        division: String,
        stats: List<String>
    ): PlayerSeasonStat {

        return PlayerSeasonStat(
            season = season,
            teamName = teamName,
            division = division,
            position =
                stats.getOrElse(0) { "" },
            gamesPlayed =
                stats.getOrElse(1) { "" },
            goals =
                stats.getOrElse(2) { "" },
            assists =
                stats.getOrElse(3) { "" },
            points =
                stats.getOrElse(4) { "" },
            pointsPerGame =
                stats.getOrElse(5) { "" },
            penaltyMinutes =
                stats.getOrElse(6) { "" },
            shootingPercent =
                stats.getOrElse(7) { "" },
            powerPlayGoals =
                stats.getOrElse(8) { "" },
            shortHandedGoals =
                stats.getOrElse(9) { "" },
            gameWinningGoals =
                stats.getOrElse(10) { "" },
            plusMinus =
                stats.getOrElse(11) { "" },
            shotsOnGoal =
                stats.getOrElse(12) { "" }
        )
    }

    private fun formatName(
        source: String
    ): String {

        if (!source.contains(",")) {
            return source.trim()
        }

        val parts =
            source.split(
                ",",
                limit = 2
            )

        return "${parts[1].trim()} ${parts[0].trim()}"
    }

    private fun normalizeName(
        source: String
    ): String {

        return source
            .lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .trim()
    }

    private suspend fun renderPlayerLinks(
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

                var started = false

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            if (started) {
                                return
                            }

                            started = true

                            handler.postDelayed(
                                {

                                    view.evaluateJavascript(
                                        """
                                        (function() {

                                            const links =
                                                Array.from(
                                                    document.querySelectorAll(
                                                        'a.person-inline[href*="/player/"]'
                                                    )
                                                );

                                            return links
                                                .map(function(link) {

                                                    const text =
                                                        (
                                                            link.innerText ||
                                                            link.textContent ||
                                                            ''
                                                        ).trim();

                                                    return (
                                                        text +
                                                        '|||' +
                                                        link.href
                                                    );
                                                })
                                                .join('\n');

                                        })()
                                        """.trimIndent()
                                    ) { result ->

                                        finishResult(
                                            webView = webView,
                                            continuation = continuation,
                                            result = result
                                        )
                                    }

                                },
                                5000
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
    private suspend fun renderGoaliePlayerLinks(
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

                var started = false

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            if (started) {
                                return
                            }

                            started = true

                            handler.postDelayed(
                                {

                                    view.evaluateJavascript(
                                        """
                                    (function() {

                                        const goalie =
                                            document.querySelector(
                                                'a[ng-click*="player_type = \'goalies\'"]'
                                            );

                                        if (!goalie) {
                                            return 'not found';
                                        }

                                        goalie.dispatchEvent(
                                            new MouseEvent(
                                                'click',
                                                {
                                                    bubbles: true,
                                                    cancelable: true,
                                                    view: window
                                                }
                                            )
                                        );

                                        return 'clicked';

                                    })()
                                    """.trimIndent()
                                    ) {

                                        handler.postDelayed(
                                            {

                                                view.evaluateJavascript(
                                                    """
                                                (function() {

                                                    const links =
                                                        Array.from(
                                                            document.querySelectorAll(
                                                                'a.person-inline[href*="/player/"]'
                                                            )
                                                        );

                                                    return links
                                                        .map(function(link) {

                                                            const text =
                                                                (
                                                                    link.innerText ||
                                                                    link.textContent ||
                                                                    ''
                                                                ).trim();

                                                            return (
                                                                text +
                                                                '|||' +
                                                                link.href
                                                            );
                                                        })
                                                        .join('\n');

                                                })()
                                                """.trimIndent()
                                                ) { result ->

                                                    finishResult(
                                                        webView = webView,
                                                        continuation = continuation,
                                                        result = result
                                                    )
                                                }

                                            },
                                            2500
                                        )
                                    }

                                },
                                5000
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
    private suspend fun renderTextPage(
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

                var started = false

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {

                            if (started) {
                                return
                            }

                            started = true

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

                                        finishResult(
                                            webView = webView,
                                            continuation = continuation,
                                            result = result
                                        )
                                    }

                                },
                                5000
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

    private fun finishResult(
        webView: WebView,
        continuation:
        kotlinx.coroutines.CancellableContinuation<String>,
        result: String?
    ) {

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
}