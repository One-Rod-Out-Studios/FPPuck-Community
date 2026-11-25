package org.fppuck.community.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume

object ScheduleRepository {

    private const val SCHEDULE_URL =
        "https://www.federalhockey.com/fphl-schedule"

    private const val HOCKEYSHIFT_SCHEDULE_URL =
        "https://www.federalhockey.com/stats?mobile-app=true&theme=false#/182/schedule?season_id=10958"

    private const val CACHE_DURATION_MS =
        15 * 60 * 1000L

    private var cachedSchedule:
            List<ScheduledGame>? = null

    private var cacheTime:
            Long = 0L

    private val dateFormatter =
        DateTimeFormatter.ofPattern(
            "EEEE, MMMM d, uuuu",
            Locale.US
        )

    private val timeRegex =
        Regex(
            """(?i)(\d{1,2}):(\d{2})\s*(AM|PM)\s*([ECMP][DS]T)"""
        )

    /*
     * Existing version.
     *
     * Kept so anything already calling
     * fetchSchedule() will continue working.
     *
     * This gets the complete 476-game schedule,
     * but does not perform the WebView time lookup.
     */
    suspend fun fetchSchedule():
            List<ScheduledGame> {

        return fetchBaseSchedule()
    }

    /*
     * Enhanced version.
     *
     * This keeps the complete static schedule,
     * then merges HockeyShift's actual start times
     * into it by game number.
     */
    suspend fun fetchSchedule(
        context: Context
    ): List<ScheduledGame> {

        val now =
            System.currentTimeMillis()

        cachedSchedule?.let { cached ->

            if (
                now - cacheTime <
                CACHE_DURATION_MS
            ) {

                return cached
            }
        }

        val baseSchedule =
            fetchBaseSchedule()

        val scheduleWithTimes =
            runCatching {

                val renderedRows =
                    renderScheduleRows(
                        context = context
                    )

                val gameTimes =
                    parseGameTimes(
                        renderedRows
                    )
                check(
                    gameTimes.isNotEmpty()
                ) {
                    "HockeyShift rendered no schedule start times."
                }

                baseSchedule.map { game ->

                    val gameTime =
                        gameTimes[
                            game.gameNumber
                        ]

                    if (gameTime == null) {

                        game

                    } else {

                        val sourceZone =
                            zoneForAbbreviation(
                                gameTime.timeZone
                            )

                        game.copy(
                            sourceStartDateTime =
                                sourceZone?.let { zone ->

                                    ZonedDateTime.of(
                                        game.date,
                                        gameTime.time,
                                        zone
                                    )
                                },

                            sourceTimeZoneAbbreviation =
                                gameTime.timeZone,

                            watchUrl =
                                gameTime.watchUrl
                        )                    }
                }

            }.getOrElse {

                return baseSchedule
            }

        cachedSchedule =
            scheduleWithTimes

        cacheTime =
            now

        return scheduleWithTimes
    }

    private suspend fun fetchBaseSchedule():
            List<ScheduledGame> =
        withContext(Dispatchers.IO) {

            val document =
                Jsoup
                    .connect(SCHEDULE_URL)
                    .userAgent(
                        "FPHL Android App"
                    )
                    .timeout(15000)
                    .get()

            /*
             * The official page contains separate
             * team home-schedule tables plus one
             * large ALL GAMES table.
             *
             * The ALL GAMES table has the most rows.
             */
            val allGamesTable =
                document
                    .select("table")
                    .maxByOrNull { table ->

                        table
                            .select("tr")
                            .size
                    }
                    ?: error(
                        "Could not find FPHL schedule table."
                    )

            allGamesTable
                .select("tr")
                .mapNotNull { row ->

                    val cells =
                        row.select("td")

                    if (
                        cells.size < 5
                    ) {

                        return@mapNotNull null
                    }

                    val gameNumber =
                        cells[0]
                            .text()
                            .trim()
                            .toIntOrNull()
                            ?: return@mapNotNull null

                    val date =
                        runCatching {

                            LocalDate.parse(
                                cells[1]
                                    .text()
                                    .trim(),

                                dateFormatter
                            )

                        }.getOrNull()
                            ?: return@mapNotNull null

                    val awayTeam =
                        cells[2]
                            .text()
                            .trim()

                    val homeTeam =
                        cells[4]
                            .text()
                            .trim()

                    ScheduledGame(
                        gameNumber =
                            gameNumber,

                        date =
                            date,

                        awayTeam =
                            awayTeam,

                        homeTeam =
                            homeTeam
                    )
                }
                .distinctBy {
                    it.gameNumber
                }
                .sortedWith(

                    compareBy<ScheduledGame> {
                        it.date
                    }.thenBy {
                        it.gameNumber
                    }
                )
        }

    private data class GameTime(
        val time: LocalTime,
        val timeZone: String,
        val watchUrl: String? = null
    )

    private fun parseGameTimes(
        renderedRows: String
    ): Map<Int, GameTime> {

        val result =
            mutableMapOf<Int, GameTime>()

        renderedRows
            .lines()
            .forEach { line ->

                val parts =
                    line.split(
                        "|||",
                        limit = 3
                    )

                if (parts.size < 2) {
                    return@forEach
                }

                val gameNumber =
                    parts[0]
                        .trim()
                        .toIntOrNull()
                        ?: return@forEach

                val timeText =
                    parts[1]
                        .trim()
                        .uppercase(
                            Locale.US
                        )

                val match =
                    timeRegex.find(
                        timeText
                    )
                        ?: return@forEach

                var hour =
                    match
                        .groupValues[1]
                        .toInt()

                val minute =
                    match
                        .groupValues[2]
                        .toInt()

                val amPm =
                    match
                        .groupValues[3]
                        .uppercase(
                            Locale.US
                        )

                val timeZone =
                    match
                        .groupValues[4]
                        .uppercase(
                            Locale.US
                        )

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

                val watchUrl =
                    parts
                        .getOrNull(2)
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }

                result[
                    gameNumber
                ] =
                    GameTime(
                        time =
                            LocalTime.of(
                                hour,
                                minute
                            ),

                        timeZone =
                            timeZone,

                        watchUrl =
                            watchUrl
                    )
            }

        return result
    }    private fun zoneForAbbreviation(
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

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun renderScheduleRows(
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
                        continuation.resume(value)
                    }
                }

                fun captureRows() {

                    webView.evaluateJavascript(
                        """
                    (function() {

                        const timeRegex =
                            /\d{1,2}:\d{2}\s*(AM|PM)\s*(E[DS]T|C[DS]T|M[DS]T|P[DS]T)/i;

                        const elementText =
                            function(element) {

                                return (
                                    element.innerText ||
                                    element.textContent ||
                                    ''
                                )
                                    .replace(
                                        /\s+/g,
                                        ' '
                                    )
                                    .trim();
                            };

                        const isWatchLink =
                            function(element) {

                                const text =
                                    elementText(element)
                                        .toUpperCase();

                                const title =
                                    (
                                        element.getAttribute(
                                            'title'
                                        ) ||
                                        ''
                                    )
                                        .toUpperCase();

                                const ariaLabel =
                                    (
                                        element.getAttribute(
                                            'aria-label'
                                        ) ||
                                        ''
                                    )
                                        .toUpperCase();

                                return (
                                    text.includes(
                                        'WATCH LIVE'
                                    ) ||
                                    (
                                        text.includes(
                                            'WATCH'
                                        ) &&
                                        text.includes(
                                            'LIVE'
                                        )
                                    ) ||
                                    title.includes(
                                        'WATCH LIVE'
                                    ) ||
                                    ariaLabel.includes(
                                        'WATCH LIVE'
                                    )
                                );
                            };

                        const findWatchLink =
                            function(root) {

                                if (!root) {
                                    return null;
                                }

                                const candidates =
                                    Array.from(
                                        root.querySelectorAll(
                                            'a[href], a[ng-href], a[data-href]'
                                        )
                                    );

                                return (
                                    candidates.find(
                                        isWatchLink
                                    ) ||
                                    null
                                );
                            };

                        const tables =
                            Array.from(
                                document.querySelectorAll(
                                    'table'
                                )
                            );

                        let numberRows = [];
                        let timeRows = [];

                        tables.forEach(
                            function(table) {

                                const rows =
                                    Array.from(
                                        table.querySelectorAll(
                                            'tbody tr'
                                        )
                                    );

                                const numberMatches =
                                    rows.filter(
                                        function(row) {

                                            const firstCell =
                                                row.querySelector(
                                                    'td'
                                                );

                                            if (!firstCell) {
                                                return false;
                                            }

                                            const text =
                                                elementText(
                                                    firstCell
                                                );

                                            return /^\d+$/.test(
                                                text
                                            );
                                        }
                                    );

                                const timeMatches =
                                    rows.filter(
                                        function(row) {

                                            return timeRegex.test(
                                                elementText(
                                                    row
                                                )
                                            );
                                        }
                                    );

                                if (
                                    numberMatches.length >
                                    numberRows.length
                                ) {

                                    numberRows =
                                        numberMatches;
                                }

                                if (
                                    timeMatches.length >
                                    timeRows.length
                                ) {

                                    timeRows =
                                        timeMatches;
                                }
                            }
                        );

                        const count =
                            Math.min(
                                numberRows.length,
                                timeRows.length
                            );

                        /*
                         * HockeyShift renders the wide schedule as several
                         * synchronized/frozen tables. The game number, time,
                         * and Actions columns can therefore live in different
                         * <table> elements.
                         *
                         * Pick the table whose rows contain the most
                         * "Watch Live" links and keep its full row list so
                         * row index i still matches game/time row i.
                         */
                        let actionRows = [];
                        let bestWatchCount = 0;
                        let bestRowDistance =
                            Number.MAX_SAFE_INTEGER;

                        tables.forEach(
                            function(table) {

                                const rows =
                                    Array.from(
                                        table.querySelectorAll(
                                            'tbody tr'
                                        )
                                    );

                                const watchCount =
                                    rows.reduce(
                                        function(total, row) {

                                            return total +
                                                (
                                                    findWatchLink(
                                                        row
                                                    )
                                                        ? 1
                                                        : 0
                                                );
                                        },
                                        0
                                    );

                                const rowDistance =
                                    Math.abs(
                                        rows.length -
                                        count
                                    );

                                if (
                                    watchCount >
                                    bestWatchCount ||
                                    (
                                        watchCount ===
                                        bestWatchCount &&
                                        watchCount > 0 &&
                                        rowDistance <
                                        bestRowDistance
                                    )
                                ) {

                                    bestWatchCount =
                                        watchCount;

                                    bestRowDistance =
                                        rowDistance;

                                    actionRows =
                                        rows;
                                }
                            }
                        );

                        /*
                         * Final fallback: collect Watch Live links from all
                         * schedule tables in display order. If HockeyShift
                         * gives every game one link, ordinal position also
                         * lines up with game number/time rows.
                         */
                        const allWatchLinks =
                            tables.flatMap(
                                function(table) {

                                    return Array.from(
                                        table.querySelectorAll(
                                            'a[href], a[ng-href], a[data-href]'
                                        )
                                    )
                                        .filter(
                                            isWatchLink
                                        );
                                }
                            );

                        const results = [];

                        for (
                            let i = 0;
                            i < count;
                            i++
                        ) {

                            const firstCell =
                                numberRows[i]
                                    .querySelector(
                                        'td'
                                    );

                            if (!firstCell) {
                                continue;
                            }

                            const gameNumber =
                                elementText(
                                    firstCell
                                );

                            const timeText =
                                elementText(
                                    timeRows[i]
                                );

                            const match =
                                timeText.match(
                                    timeRegex
                                );

                            if (
                                !/^\d+$/.test(
                                    gameNumber
                                ) ||
                                !match
                            ) {
                                continue;
                            }

                            let watchLink =
                                actionRows[i]
                                    ? findWatchLink(
                                        actionRows[i]
                                    )
                                    : null;

                            if (!watchLink) {

                                watchLink =
                                    findWatchLink(
                                        timeRows[i]
                                    );
                            }

                            if (
                                !watchLink &&
                                allWatchLinks.length ===
                                count
                            ) {

                                watchLink =
                                    allWatchLinks[i];
                            }

                            let watchUrl = '';

                            if (watchLink) {

                                const href =
                                    watchLink.href ||
                                    watchLink.getAttribute(
                                        'href'
                                    ) ||
                                    watchLink.getAttribute(
                                        'ng-href'
                                    ) ||
                                    watchLink.getAttribute(
                                        'data-href'
                                    ) ||
                                    '';

                                if (href) {

                                    try {

                                        watchUrl =
                                            new URL(
                                                href,
                                                window.location.href
                                            ).href;

                                    } catch (error) {

                                        watchUrl =
                                            href;
                                    }
                                }
                            }

                            results.push(
                                gameNumber +
                                '|||' +
                                match[0] +
                                '|||' +
                                watchUrl
                            );
                        }

                        return results.join(
                            '\n'
                        );

                    })()
                    """.trimIndent()
                    ) { rawResult ->

                        finish(
                            decodeJavascriptString(
                                rawResult
                            )
                        )
                    }
                }

                fun expandSchedule(
                    attempt: Int
                ) {

                    if (attempt >= 14) {

                        captureRows()
                        return
                    }

                    webView.evaluateJavascript(
                        """
                    (function() {

                        const candidates =
                            Array.from(
                                document.querySelectorAll(
                                    'button, a, [ng-click]'
                                )
                            );

                        const loadMore =
                            candidates.find(
                                function(element) {

                                    const text =
                                        (
                                            element.innerText ||
                                            element.textContent ||
                                            ''
                                        )
                                        .replace(
                                            /\s+/g,
                                            ' '
                                        )
                                        .trim()
                                        .toUpperCase();

                                    return (
                                        text === 'LOAD MORE' &&
                                        element.offsetParent !== null &&
                                        !element.disabled &&
                                        element.getAttribute(
                                            'aria-disabled'
                                        ) !== 'true'
                                    );
                                }
                            );

                        if (!loadMore) {
                            return 'done';
                        }

                        loadMore.click();

                        return 'clicked';

                    })()
                    """.trimIndent()
                    ) { result ->

                        val clicked =
                            result.contains(
                                "clicked",
                                ignoreCase = true
                            )

                        if (clicked) {

                            handler.postDelayed(
                                {
                                    expandSchedule(
                                        attempt + 1
                                    )
                                },
                                400
                            )

                        } else {

                            handler.postDelayed(
                                {
                                    captureRows()
                                },
                                400
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

                            handler.postDelayed(
                                {
                                    expandSchedule(
                                        0
                                    )
                                },
                                2000
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
                    HOCKEYSHIFT_SCHEDULE_URL
                )
            }
        }
    private fun decodeJavascriptString(
        rawResult: String
    ): String {

        if (
            rawResult == "null" ||
            rawResult.isBlank()
        ) {

            return ""
        }

        return runCatching {

            JSONObject(
                """{"value":$rawResult}"""
            )
                .getString(
                    "value"
                )

        }.getOrDefault("")
    }
}