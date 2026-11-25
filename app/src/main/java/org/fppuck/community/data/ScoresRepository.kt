package org.fppuck.community.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ScoresRepository {

    private const val DEBUG_TAG =
        "FPHL_SCORES"

    private const val SCORES_URL =
        "https://www.federalhockey.com/stats?mobile-app=true&theme=false#/182/schedule?season_id=10958"

    /*
     * Several screens use the same HockeyShift scoreboard.
     * Keep a very short-lived shared cache so moving between
     * Home, Schedule, Team Schedule, and Game Details does not
     * immediately launch another WebView for identical data.
     *
     * The normal 30-second refresh loops still get fresh data,
     * because this cache expires well before their next poll.
     */
    private const val CACHE_DURATION_MS =
        12_000L

    private val fetchMutex =
        Mutex()

    @Volatile
    private var cachedScoreboard:
            List<ScoreboardGame> =
        emptyList()

    @Volatile
    private var cachedAtMillis:
            Long =
        0L

    suspend fun fetchScoreboard(
        context: Context,
        forceRefresh: Boolean = false
    ): List<ScoreboardGame> {

        val now =
            System.currentTimeMillis()

        val cached =
            cachedScoreboard

        if (
            !forceRefresh &&
            cached.isNotEmpty() &&
            now - cachedAtMillis <
            CACHE_DURATION_MS
        ) {
            return cached
        }

        /*
         * If two screens ask for scores at nearly the same
         * moment, only one of them performs the WebView load.
         * The other waits here and then receives the same
         * freshly cached result.
         */
        return fetchMutex.withLock {

            val lockedNow =
                System.currentTimeMillis()

            val lockedCached =
                cachedScoreboard

            if (
                !forceRefresh &&
                lockedCached.isNotEmpty() &&
                lockedNow - cachedAtMillis <
                CACHE_DURATION_MS
            ) {
                return@withLock lockedCached
            }

            val pageText =
                renderPage(context)

            val parsed =
                withContext(
                    Dispatchers.Default
                ) {
                    parseScoreboard(
                        pageText
                    )
                }

            logDebugSnapshot(
                pageText = pageText,
                parsed = parsed
            )

            /*
             * Do not replace a good cache with an empty parse
             * caused by a transient HockeyShift/WebView issue.
             */
            if (parsed.isNotEmpty()) {

                cachedScoreboard =
                    parsed

                cachedAtMillis =
                    System.currentTimeMillis()
            }

            if (parsed.isNotEmpty()) {
                parsed
            } else {
                cachedScoreboard
            }
        }
    }

    private fun logDebugSnapshot(
        pageText: String,
        parsed: List<ScoreboardGame>
    ) {

        Log.d(
            DEBUG_TAG,
            "----- SCOREBOARD FETCH -----"
        )

        Log.d(
            DEBUG_TAG,
            "Raw chars=${pageText.length}, parsed games=${parsed.size}"
        )

        parsed.forEach { game ->

            Log.d(
                DEBUG_TAG,
                "PARSED ${game.date} " +
                        "${game.awayAbbreviation}@${game.homeAbbreviation} " +
                        "score=${game.awayScore}-${game.homeScore} " +
                        "status='${game.status}' " +
                        "local='${game.localStatusLabel()}'"
            )
        }

        val lines =
            pageText.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        val matchIndexes =
            lines.indices.filter { index ->

                val line =
                    lines[index]

                line.contains(
                    "MSM",
                    ignoreCase = true
                ) ||
                        line.contains(
                            "MRM",
                            ignoreCase = true
                        ) ||
                        line.contains(
                            "Mid-South",
                            ignoreCase = true
                        ) ||
                        line.contains(
                            "Monroe",
                            ignoreCase = true
                        )
            }

        if (matchIndexes.isEmpty()) {

            Log.d(
                DEBUG_TAG,
                "RAW MSM/MRM: no matching text found in rendered page"
            )

        } else {

            val indexesToLog =
                matchIndexes
                    .flatMap { index ->

                        (
                                (index - 6)
                                    .coerceAtLeast(0)
                                        ..
                                        (index + 10)
                                            .coerceAtMost(
                                                lines.lastIndex
                                            )
                                ).toList()
                    }
                    .distinct()
                    .sorted()

            Log.d(
                DEBUG_TAG,
                "----- RAW MSM/MRM REGION -----"
            )

            indexesToLog.forEach { index ->

                Log.d(
                    DEBUG_TAG,
                    "RAW[$index] ${lines[index]}"
                )
            }
        }

        Log.d(
            DEBUG_TAG,
            "----- END SCOREBOARD FETCH -----"
        )
    }

    private suspend fun renderPage(
        context: Context
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
                var finished = false

                /*
                 * HockeyShift renders the schedule progressively.
                 * Seeing one matchup is NOT enough to mean the
                 * scoreboard is finished rendering.
                 */
                var lastKnownTeamCount = 0
                var stableTeamCountCaptures = 0

                fun finish(
                    value: String
                ) {

                    if (finished) {
                        return
                    }

                    finished = true

                    handler.removeCallbacksAndMessages(
                        null
                    )

                    webView.stopLoading()
                    webView.destroy()

                    if (continuation.isActive) {
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

                        val knownTeamCount =
                            sampleTeams
                                .count { team ->

                                    Regex(
                                        """(?m)(^|\s)${Regex.escape(team.abbreviation)}(\s|$)"""
                                    ).containsMatchIn(
                                        text
                                    )
                                }

                        if (
                            knownTeamCount >= 2 &&
                            knownTeamCount ==
                            lastKnownTeamCount
                        ) {

                            stableTeamCountCaptures++

                        } else {

                            stableTeamCountCaptures =
                                0
                        }

                        lastKnownTeamCount =
                            knownTeamCount

                        /*
                         * Do not capture the first matchup that
                         * Angular happens to render.
                         *
                         * Wait at least ~1.5 seconds AND require
                         * the set/count of rendered teams to have
                         * stopped growing across several polls.
                         * This prevents later rows (such as
                         * Mid-South / Monroe) from being omitted.
                         */
                        val ready =
                            knownTeamCount >= 2 &&
                                    attempt >= 6 &&
                                    stableTeamCountCaptures >= 2

                        if (
                            ready ||
                            attempt >= 16
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

                            /*
                             * HockeyShift is an Angular page. Poll until
                             * the rendered scoreboard has stabilized,
                             * rather than accepting the first matchup
                             * that appears.
                             */
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
                    SCORES_URL
                )
            }
        }

    private fun parseScoreboard(
        text: String
    ): List<ScoreboardGame> {

        val lines =
            text.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        val results =
            mutableListOf<ScoreboardGame>()

        val knownTeams =
            sampleTeams
                .map {
                    it.abbreviation
                }
                .toSet()

        val dateFormatter =
            DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMM d uuuu")
                .toFormatter(Locale.US)

        val dayNames =
            setOf(
                "MON",
                "TUE",
                "WED",
                "THU",
                "FRI",
                "SAT",
                "SUN"
            )

        var currentDate: LocalDate? = null
        var index = 0

        while (index < lines.size) {

            /*
             * Stop when we leave the scoreboard strip
             * and reach the main stats navigation.
             */
            if (
                lines[index] == "FPHL" &&
                lines.getOrNull(index + 1)
                    ?.equals(
                        "menu",
                        ignoreCase = true
                    ) == true
            ) {
                break
            }

            /*
             * Date headers appear as:
             *
             * SAT
             * OCT
             * 10
             */
            if (
                lines[index] in dayNames &&
                index + 2 < lines.size
            ) {

                val month =
                    lines[index + 1]

                val day =
                    lines[index + 2]

                val year =
                    LocalDate.now().year

                currentDate =
                    runCatching {

                        LocalDate.parse(
                            "$month $day $year",
                            dateFormatter
                        )

                    }.getOrNull()

                index += 3
                continue
            }

            val date =
                currentDate

            if (
                date != null &&
                lines[index] in knownTeams
            ) {

                val away =
                    lines[index]

                /*
                 * Completed/live format:
                 *
                 * MNL
                 * 2
                 * IND
                 * 4
                 * FINAL
                 */
                val awayScore =
                    lines.getOrNull(index + 1)
                        ?.toIntOrNull()

                if (awayScore != null) {

                    val home =
                        lines.getOrNull(index + 2)

                    val homeScore =
                        lines.getOrNull(index + 3)
                            ?.toIntOrNull()

                    val statusLine =
                        lines.getOrNull(index + 4)

                    val nextLine =
                        lines.getOrNull(index + 5)

                    val periodOnly =
                        statusLine
                            ?.matches(
                                Regex(
                                    "(?i)^(1ST|2ND|3RD|OT|SO)$"
                                )
                            ) == true

                    val clockOnly =
                        nextLine
                            ?.matches(
                                Regex(
                                    "^\\d{1,2}:\\d{2}$"
                                )
                            ) == true

                    val status =
                        when {

                            periodOnly &&
                                    clockOnly ->

                                "$statusLine $nextLine"

                            else ->
                                statusLine
                        }

                    if (
                        home != null &&
                        home in knownTeams &&
                        homeScore != null &&
                        status != null
                    ) {

                        results.add(
                            ScoreboardGame(
                                date = date,
                                awayAbbreviation = away,
                                homeAbbreviation = home,
                                awayScore = awayScore,
                                homeScore = homeScore,
                                status = status
                            )
                        )

                        index +=
                            if (
                                periodOnly &&
                                clockOnly
                            ) {
                                6
                            } else {
                                5
                            }

                        continue
                    }
                }

                /*
                 * Scheduled format:
                 *
                 * WTW
                 * BBB
                 * 7:00PM EDT
                 */
                val home =
                    lines.getOrNull(index + 1)

                val time =
                    lines.getOrNull(index + 2)

                if (
                    home != null &&
                    home in knownTeams &&
                    time != null &&
                    time.contains(":")
                ) {

                    results.add(
                        ScoreboardGame(
                            date = date,
                            awayAbbreviation = away,
                            homeAbbreviation = home,
                            status = time
                        )
                    )

                    index += 3
                    continue
                }
            }

            index++
        }

        return results
    }
}