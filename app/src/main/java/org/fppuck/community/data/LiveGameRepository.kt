package org.fppuck.community.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import kotlin.coroutines.resume

object LiveGameRepository {

    private const val CACHE_DURATION_MS =
        8_000L

    private data class CachedLiveState(
        val state: LiveGameState,
        val cachedAtMillis: Long
    )

    private val cache =
        mutableMapOf<String, CachedLiveState>()

    private val fetchMutex =
        Mutex()

    suspend fun fetchLiveState(
        context: Context,
        gameId: String,
        forceRefresh: Boolean = false
    ): LiveGameState? {

        val now =
            System.currentTimeMillis()

        if (!forceRefresh) {

            cache[gameId]
                ?.let { cached ->

                    if (
                        now - cached.cachedAtMillis <
                        CACHE_DURATION_MS
                    ) {
                        return cached.state
                    }
                }
        }

        return fetchMutex.withLock {

            val lockedNow =
                System.currentTimeMillis()

            if (!forceRefresh) {

                cache[gameId]
                    ?.let { cached ->

                        if (
                            lockedNow -
                            cached.cachedAtMillis <
                            CACHE_DURATION_MS
                        ) {
                            return@withLock cached.state
                        }
                    }
            }

            val url =
                "https://www.federalhockey.com/stats" +
                        "?mobile-app=true&theme=false" +
                        "#/182/game/$gameId/play-by-play"

            val renderedText =
                renderLivePage(
                    context = context,
                    url = url
                )

            val parsed = parseLiveState(renderedText)
            if (parsed != null) {

                cache[gameId] =
                    CachedLiveState(
                        state = parsed,
                        cachedAtMillis =
                            System.currentTimeMillis()
                    )
            }

            parsed
                ?: cache[gameId]
                    ?.state
        }
    }

    private fun parseLiveState(
        rawText: String
    ): LiveGameState? {

        val lines =
            rawText.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        if (lines.isEmpty()) {
            return null
        }

        val awayIndex =
            lines.indexOfFirst {
                it.equals(
                    "Away",
                    ignoreCase = true
                )
            }

        val homeIndex =
            lines.indexOfFirst {
                it.equals(
                    "Home",
                    ignoreCase = true
                )
            }

        if (
            awayIndex < 0 ||
            homeIndex <= awayIndex
        ) {
            return null
        }

        val statusRegex =
            Regex(
                """^(\d{1,2}:\d{2})\s*\|\s*(.+)$""",
                RegexOption.IGNORE_CASE
            )

        val statusIndex =
            (
                    homeIndex + 1
                            ..
                            minOf(
                                homeIndex + 12,
                                lines.lastIndex
                            )
                    )
                .firstOrNull { index ->
                    statusRegex.matches(
                        lines[index]
                    ) ||
                            lines[index].equals(
                                "FINAL",
                                ignoreCase = true
                            )
                }

        val scoreSectionEnd =
            statusIndex
                ?: minOf(
                    homeIndex + 8,
                    lines.size
                )

        val awayScore =
            lines
                .subList(
                    awayIndex + 1,
                    homeIndex
                )
                .mapNotNull {
                    it.toIntOrNull()
                }
                .lastOrNull()

        val homeScore =
            lines
                .subList(
                    homeIndex + 1,
                    scoreSectionEnd
                )
                .mapNotNull {
                    it.toIntOrNull()
                }
                .lastOrNull()

        val statusLine =
            statusIndex
                ?.let {
                    lines[it]
                }

        val statusMatch =
            statusLine
                ?.let {
                    statusRegex.matchEntire(
                        it
                    )
                }

        val headerClock =
            statusMatch
                ?.groupValues
                ?.getOrNull(1)

        val period =
            when {

                statusMatch != null ->
                    statusMatch.groupValues[2]
                        .trim()

                statusLine?.equals(
                    "FINAL",
                    ignoreCase = true
                ) == true ->
                    "FINAL"

                else ->
                    null
            }

        /*
         * Critical HockeyShift detail:
         *
         * Play-by-Play event times are ELAPSED time in the period,
         * while the hockey scoreboard clock counts DOWN.
         *
         * Example:
         *   PBP event time 11:54
         *   => rink clock remaining 08:06
         *
         * The feed itself is newest-first. Therefore the FIRST
         * real event after the Play / Team / Time header is the
         * freshest event in the current period.
         */
        val rawLastPlay =
            parseLastPlay(
                lines
            )

        val convertedLastPlay =
            rawLastPlay
                ?.let { play ->

                    val displayTime =
                        elapsedToRemainingClock(
                            elapsed = play.time,
                            period = period
                        )
                            ?: play.time

                    play.copy(
                        time = displayTime
                    )
                }

        val liveClock =
            convertedLastPlay
                ?.time
                ?.takeIf {
                    it.matches(
                        Regex(
                            """^\d{1,2}:\d{2}$"""
                        )
                    )
                }
                ?: headerClock

        return LiveGameState(
            awayScore = awayScore,
            homeScore = homeScore,
            clock = liveClock,
            period = period,
            lastPlay = convertedLastPlay
        )
    }

    private fun elapsedToRemainingClock(
        elapsed: String?,
        period: String?
    ): String? {

        val raw =
            elapsed
                ?: return null

        /*
         * Only regulation periods are guaranteed to be 20:00.
         * Do not guess at overtime format.
         */
        val regulationPeriod =
            period
                ?.trim()
                ?.uppercase()
                ?.let {
                    it == "1ST" ||
                            it == "2ND" ||
                            it == "3RD"
                }
                ?: false

        if (!regulationPeriod) {
            return null
        }

        val parts =
            raw.split(
                ":"
            )

        if (parts.size != 2) {
            return null
        }

        val minutes =
            parts[0]
                .toIntOrNull()
                ?: return null

        val seconds =
            parts[1]
                .toIntOrNull()
                ?: return null

        val elapsedSeconds =
            minutes * 60 +
                    seconds

        val periodSeconds =
            20 * 60

        if (
            elapsedSeconds < 0 ||
            elapsedSeconds >
            periodSeconds
        ) {
            return null
        }

        val remaining =
            periodSeconds -
                    elapsedSeconds

        return String.format(
            java.util.Locale.US,
            "%d:%02d",
            remaining / 60,
            remaining % 60
        )
    }

    private fun parseLastPlay(
        lines: List<String>
    ): LivePlay? {

        val playHeaderIndex =
            lines.indexOfFirst { line ->

                val normalized =
                    line
                        .replace(
                            "\t",
                            " "
                        )
                        .replace(
                            Regex("""\s+"""),
                            " "
                        )
                        .trim()
                        .uppercase()

                normalized.contains(
                    "PLAY"
                ) &&
                        normalized.contains(
                            "TEAM"
                        ) &&
                        normalized.contains(
                            "TIME"
                        )
            }

        if (playHeaderIndex < 0) {
            return null
        }

        val teamTimeRegex =
            Regex(
                """^([A-Z]{2,5})\s+(\d{1,2}:\d{2})$"""
            )

        val stopLabels =
            setOf(
                "GAME STATS",
                "SCORING",
                "SHOTS",
                "TEAM STATS",
                "OFFICIALS",
                "TEAM STAFF"
            )

        var index =
            playHeaderIndex + 1

        /*
         * HockeyShift LIVE is newest-first.
         *
         * The previous versions kept walking the feed looking for
         * a "latest" event. That was backwards and eventually
         * landed on early-period / previous-period events.
         *
         * Return the FIRST real hockey event after the header.
         */
        while (
            index + 1 <
            lines.size
        ) {

            val type =
                lines[index]
                    .trim()

            val typeUpper =
                type.uppercase()

            if (
                typeUpper in stopLabels ||
                typeUpper.startsWith(
                    "ADMIN LOGIN"
                ) ||
                typeUpper.startsWith(
                    "COPYRIGHT"
                )
            ) {
                return null
            }

            val teamTime =
                lines[index + 1]
                    .replace(
                        "\t",
                        " "
                    )
                    .replace(
                        Regex("""\s+"""),
                        " "
                    )
                    .trim()
                    .uppercase()

            val match =
                teamTimeRegex
                    .matchEntire(
                        teamTime
                    )

            if (match == null) {

                index++
                continue
            }

            val team =
                match.groupValues[1]

            val time =
                match.groupValues[2]

            val descriptionLines =
                mutableListOf<String>()

            var descriptionIndex =
                index + 2

            while (
                descriptionIndex <
                lines.size
            ) {

                val candidate =
                    lines[descriptionIndex]
                        .trim()

                val candidateUpper =
                    candidate.uppercase()

                /*
                 * Period-boundary markers are separate HockeyShift
                 * feed items. Do not swallow them into a shot /
                 * penalty description.
                 */
                if (
                    candidateUpper.startsWith(
                        "START OF "
                    ) ||
                    candidateUpper.startsWith(
                        "END OF "
                    ) ||
                    candidateUpper in stopLabels ||
                    candidateUpper.startsWith(
                        "ADMIN LOGIN"
                    ) ||
                    candidateUpper.startsWith(
                        "COPYRIGHT"
                    )
                ) {
                    break
                }

                val followingLine =
                    if (
                        descriptionIndex + 1 <
                        lines.size
                    ) {
                        lines[descriptionIndex + 1]
                            .replace(
                                "\t",
                                " "
                            )
                            .replace(
                                Regex("""\s+"""),
                                " "
                            )
                            .trim()
                            .uppercase()
                    } else {
                        ""
                    }

                if (
                    teamTimeRegex.matches(
                        followingLine
                    )
                ) {
                    break
                }

                if (
                    candidate.isNotBlank() &&
                    !candidate.equals(
                        "expand_more",
                        ignoreCase = true
                    )
                ) {
                    descriptionLines.add(
                        candidate
                    )
                }

                descriptionIndex++
            }

            var description =
                descriptionLines
                    .joinToString(
                        " "
                    )
                    .trim()

            description =
                description
                    .replace(
                        Regex(
                            """(?i)(\d+\s+mins)(Power Play)"""
                        ),
                        "$1 • $2"
                    )
                    .replace(
                        Regex(
                            """\s+"""
                        ),
                        " "
                    )
                    .trim()

            val administrativeEvent =
                typeUpper == "GOALIE" ||
                        typeUpper.contains(
                            "STARTING GOALIE"
                        ) ||
                        description.startsWith(
                            "Starting Goalie",
                            ignoreCase = true
                        )

            if (!administrativeEvent) {

                return LivePlay(
                    type = type,
                    teamAbbreviation = team,
                    time = time,
                    description = description
                )
            }

            index =
                if (
                    descriptionIndex >
                    index
                ) {
                    descriptionIndex
                } else {
                    index + 2
                }
        }

        return null
    }

    private suspend fun renderLivePage(
        context: Context,
        url: String
    ): String =
        withContext(
            Dispatchers.Main
        ) {

            suspendCancellableCoroutine { continuation ->

                val webView =
                    WebView(context)

                val handler =
                    Handler(
                        Looper.getMainLooper()
                    )

                var started =
                    false

                var finished =
                    false

                var lastLength =
                    -1

                var stableCaptures =
                    0

                fun finish(
                    value: String
                ) {

                    if (finished) {
                        return
                    }

                    finished =
                        true

                    handler
                        .removeCallbacksAndMessages(
                            null
                        )

                    webView.stopLoading()
                    webView.destroy()

                    if (
                        continuation.isActive
                    ) {
                        continuation.resume(
                            value
                        )
                    }
                }

                fun capture(
                    attempt: Int
                ) {

                    webView.evaluateJavascript(
                        """
                        (function() {

                            if (!document.body) {
                                return "";
                            }

                            return document.body.innerText || "";
                        })()
                        """.trimIndent()
                    ) { result ->

                        val text =
                            runCatching {

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

                            }.getOrDefault("")

                        if (
                            text.length ==
                            lastLength &&
                            text.isNotBlank()
                        ) {
                            stableCaptures++
                        } else {
                            stableCaptures = 0
                        }

                        lastLength =
                            text.length

                        val normalized =
                            text
                                .replace(
                                    "\t",
                                    " "
                                )
                                .uppercase()

                        val hasLiveHeader =
                            normalized.contains(
                                "AWAY"
                            ) &&
                                    normalized.contains(
                                        "HOME"
                                    )

                        val hasPlayTable =
                            normalized.contains(
                                "PLAY"
                            ) &&
                                    normalized.contains(
                                        "TEAM"
                                    ) &&
                                    normalized.contains(
                                        "TIME"
                                    )

                        val ready =
                            hasLiveHeader &&
                                    hasPlayTable &&
                                    attempt >= 4 &&
                                    stableCaptures >= 1

                        if (
                            ready ||
                            attempt >= 20
                        ) {

                            finish(
                                text
                            )

                        } else {

                            handler.postDelayed(
                                {
                                    capture(
                                        attempt + 1
                                    )
                                },
                                250
                            )
                        }
                    }
                }

                webView.settings
                    .javaScriptEnabled =
                    true

                webView.settings
                    .domStorageEnabled =
                    true

                webView.settings
                    .cacheMode =
                    WebSettings.LOAD_NO_CACHE

                webView.clearCache(
                    true
                )

                webView.webViewClient =
                    object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView,
                            pageUrl: String
                        ) {

                            if (started) {
                                return
                            }

                            started =
                                true

                            handler.postDelayed(
                                {
                                    capture(
                                        0
                                    )
                                },
                                250
                            )
                        }
                    }

                continuation
                    .invokeOnCancellation {

                        handler
                            .removeCallbacksAndMessages(
                                null
                            )

                        webView.destroy()
                    }

                webView.loadUrl(
                    url
                )
            }
        }
}
