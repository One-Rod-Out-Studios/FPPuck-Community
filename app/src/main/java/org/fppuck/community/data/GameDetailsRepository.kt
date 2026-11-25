package org.fppuck.community.data

import org.fppuck.community.data.GameOfficial
import org.fppuck.community.data.GameStar
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GameDetailsRepository {

    private fun parseStars(
        lines: List<String>
    ): List<GameStar> {

        val start =
            lines.indexOfFirst {
                it.equals(
                    "3 STARS",
                    ignoreCase = true
                )
            }

        val end =
            lines.indexOfFirst {
                it.equals(
                    "SCORING SUMMARY",
                    ignoreCase = true
                )
            }

        if (
            start < 0 ||
            end <= start
        ) {
            return emptyList()
        }

        val section =
            lines.subList(
                start + 1,
                end
            )

        val stars =
            mutableListOf<GameStar>()

        var index = 0

        while (index < section.size) {

            val rank =
                section[index]

            if (
                rank.equals(
                    "1ST STAR",
                    ignoreCase = true
                ) ||
                rank.equals(
                    "2ND STAR",
                    ignoreCase = true
                ) ||
                rank.equals(
                    "3RD STAR",
                    ignoreCase = true
                )
            ) {

                val playerName =
                    section.getOrNull(
                        index + 1
                    ) ?: ""

                val statLine =
                    section.getOrNull(
                        index + 2
                    ) ?: ""

                val teamName =
                    section.getOrNull(
                        index + 3
                    ) ?: ""

                stars.add(
                    GameStar(
                        rank = rank,
                        playerName = playerName,
                        statLine = statLine,
                        teamName = teamName
                    )
                )

                index += 4

            } else {

                index++
            }
        }

        return stars
    }

    private fun parseOfficials(
        lines: List<String>
    ): List<GameOfficial> {

        val start =
            lines.indexOfFirst {
                it.equals(
                    "OFFICIALS",
                    ignoreCase = true
                )
            }

        val end =
            lines.indexOfFirst {
                it.equals(
                    "TEAM STAFF",
                    ignoreCase = true
                )
            }

        if (
            start < 0 ||
            end <= start
        ) {
            return emptyList()
        }

        return lines
            .subList(
                start + 1,
                end
            )
            .mapNotNull { line ->

                val columns =
                    line.split("\t")
                        .map {
                            it.trim()
                        }
                        .filter {
                            it.isNotEmpty()
                        }

                if (
                    columns.size < 2 ||
                    columns[0].equals(
                        "Type",
                        ignoreCase = true
                    )
                ) {
                    null

                } else {

                    GameOfficial(
                        type = columns[0],
                        name =
                            columns
                                .drop(1)
                                .joinToString(" ")
                    )
                }
            }
    }

    private fun parseNamedTime(
        lines: List<String>,
        label: String
    ): String? {

        val index =
            lines.indexOfFirst {
                it.startsWith(
                    label,
                    ignoreCase = true
                )
            }

        if (index < 0) {
            return null
        }

        val columns =
            lines[index]
                .split("\t")
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        return columns
            .getOrNull(1)
    }
    private const val SCHEDULE_URL =
        "https://www.federalhockey.com/stats" +
                "?mobile-app=true&theme=false" +
                "#/182/schedule?season_id=10958"

    private val gameIdCache =
        mutableMapOf<String, String>()

    private data class CachedGameDetails(
        val details: GameDetailsData,
        val cachedAtMillis: Long
    )

    /*
     * Game Details is one of the heavier HockeyShift pages.
     * Keep a short cache for live games and a much longer one
     * for completed games so backing out and reopening a game
     * does not immediately rebuild another WebView.
     */
    private const val LIVE_CACHE_DURATION_MS =
        12_000L

    private const val FINAL_CACHE_DURATION_MS =
        10 * 60 * 1000L

    private val detailsCache =
        mutableMapOf<String, CachedGameDetails>()

    private val detailsFetchMutex =
        Mutex()

    suspend fun fetchGameDetails(
        context: Context,
        game: ScoreboardGame,
        forceRefresh: Boolean = false
    ): GameDetailsData {

        val cacheKey =
            "${game.date}|" +
                    "${game.awayAbbreviation}|" +
                    game.homeAbbreviation

        val isFinal =
            game.status.contains(
                "FINAL",
                ignoreCase = true
            ) ||
                    game.date.isBefore(
                        LocalDate.now()
                    )

        val cacheDuration =
            if (isFinal) {
                FINAL_CACHE_DURATION_MS
            } else {
                LIVE_CACHE_DURATION_MS
            }

        val now =
            System.currentTimeMillis()

        if (forceRefresh) {
            gameIdCache.remove(
                cacheKey
            )
        }

        if (!forceRefresh) {

            detailsCache[cacheKey]
                ?.let { cached ->

                    if (
                        now - cached.cachedAtMillis <
                        cacheDuration
                    ) {
                        return cached.details
                    }
                }
        }

        /*
         * Prevent duplicate Game Details WebViews if the same
         * game is requested twice at nearly the same time.
         */
        return detailsFetchMutex.withLock {

            val lockedNow =
                System.currentTimeMillis()

            if (!forceRefresh) {

                detailsCache[cacheKey]
                    ?.let { cached ->

                        if (
                            lockedNow -
                            cached.cachedAtMillis <
                            cacheDuration
                        ) {
                            return@withLock cached.details
                        }
                    }
            }

            val gameId =
                resolveGameId(
                    context = context,
                    game = game
                )

            val url =
                "https://www.federalhockey.com/stats" +
                        "?mobile-app=true&theme=false" +
                        "#/182/game/$gameId/boxscore"

            val text =
                renderText(
                    context = context,
                    url = url,
                    delayMillis = 6000
                )

            if (
                !pageMatchesRequestedGame(
                    text = text,
                    game = game
                )
            ) {

                gameIdCache.remove(
                    cacheKey
                )

                error(
                    "HockeyShift returned the wrong game page for " +
                            "${game.awayAbbreviation} @ " +
                            "${game.homeAbbreviation}."
                )
            }

            val parsed =
                withContext(
                    Dispatchers.Default
                ) {
                    parseGameDetails(
                        gameId = gameId,
                        text = text
                    )
                }

            detailsCache[cacheKey] =
                CachedGameDetails(
                    details = parsed,
                    cachedAtMillis =
                        System.currentTimeMillis()
                )

            parsed
        }
    }

    private suspend fun resolveGameId(
        context: Context,
        game: ScoreboardGame
    ): String {

        val cacheKey =
            "${game.date}|" +
                    "${game.awayAbbreviation}|" +
                    game.homeAbbreviation

        gameIdCache[cacheKey]
            ?.let {
                return it
            }

        /*
         * First resolve the requested matchup to its official
         * FPHL game number using the static schedule.
         *
         * This is much safer than searching a giant HockeyShift
         * DOM blob for both team abbreviations. HockeyShift uses
         * frozen/split tables, so one DOM container can contain
         * several neighboring games.
         */
        val scheduledGame =
            ScheduleRepository
                .fetchSchedule()
                .firstOrNull { scheduled ->

                    scheduled.date ==
                            game.date &&
                            scheduledTeamMatches(
                                rawTeamName =
                                    scheduled.awayTeam,
                                abbreviation =
                                    game.awayAbbreviation
                            ) &&
                            scheduledTeamMatches(
                                rawTeamName =
                                    scheduled.homeTeam,
                                abbreviation =
                                    game.homeAbbreviation
                            )
                }


        val rendered =
            renderGameLinks(
                context
            )

        val matches =
            rendered.lines()
                .mapNotNull { line ->

                    val parts =
                        line.split(
                            "|||",
                            limit = 2
                        )

                    if (parts.size < 2) {
                        return@mapNotNull null
                    }

                    val gameNumber =
                        parts[0]
                            .trim()
                            .toIntOrNull()
                            ?: return@mapNotNull null

                    val gameId =
                        parts[1]
                            .trim()
                            .takeIf {
                                it.all(
                                    Char::isDigit
                                )
                            }
                            ?: return@mapNotNull null

                    if (
                        gameNumber ==
                        scheduledGame?.gameNumber
                    ) {
                        gameId
                    } else {
                        null
                    }
                }
                .distinct()

        val gameId =
            chooseHockeyShiftGameId(
                matches,
                rendered,
                game
            ) ?: error("No hockey shift game is available for the requested selection.")

        gameIdCache[cacheKey] =
            gameId

        return gameId
    }

    private fun scheduledTeamMatches(
        rawTeamName: String,
        abbreviation: String
    ): Boolean {

        val team =
            sampleTeams
                .firstOrNull {
                    it.abbreviation.equals(
                        abbreviation,
                        ignoreCase = true
                    )
                }
                ?: return false

        val expected =
            normalizeTeamText(
                "${team.city} ${team.name}"
            )

        val actual =
            normalizeTeamText(
                when (
                    rawTeamName.trim()
                        .lowercase()
                ) {

                    "oceanside hc" ->
                        "Oceanside Shock"

                    "baton rouge" ->
                        "Baton Rouge Kingfish"

                    else ->
                        rawTeamName
                }
            )

        return actual ==
                expected
    }

    private fun pageMatchesRequestedGame(
        text: String,
        game: ScoreboardGame
    ): Boolean {

        val awayTeam =
            sampleTeams
                .firstOrNull {
                    it.abbreviation.equals(
                        game.awayAbbreviation,
                        ignoreCase = true
                    )
                }
                ?: return false

        val homeTeam =
            sampleTeams
                .firstOrNull {
                    it.abbreviation.equals(
                        game.homeAbbreviation,
                        ignoreCase = true
                    )
                }
                ?: return false

        val expectedAway =
            normalizeTeamText(
                "${awayTeam.city} ${awayTeam.name}"
            )

        val expectedHome =
            normalizeTeamText(
                "${homeTeam.city} ${homeTeam.name}"
            )

        val lines =
            text.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
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
            return false
        }

        val awayBlock =
            lines
                .subList(
                    awayIndex + 1,
                    homeIndex
                )
                .joinToString(
                    " "
                )

        val homeEnd =
            minOf(
                homeIndex + 8,
                lines.size
            )

        val homeBlock =
            lines
                .subList(
                    homeIndex + 1,
                    homeEnd
                )
                .joinToString(
                    " "
                )

        return normalizeTeamText(
            awayBlock
        ).contains(
            expectedAway
        ) &&
                normalizeTeamText(
                    homeBlock
                ).contains(
                    expectedHome
                )
    }

    private fun normalizeTeamText(
        value: String
    ): String {

        return value
            .lowercase()
            .replace(
                Regex(
                    """[^a-z0-9]+"""
                ),
                " "
            )
            .trim()
    }

    private suspend fun renderGameLinks(
        context: Context
    ): String =
        withContext(
            Dispatchers.Main
        ) {

            suspendCancellableCoroutine { continuation ->

                val webView =
                    WebView(
                        context
                    )

                val handler =
                    Handler(
                        Looper.getMainLooper()
                    )

                var started =
                    false

                var finished =
                    false

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

                            const elementText =
                                function(element) {

                                    return (
                                        element.innerText ||
                                        element.textContent ||
                                        ''
                                    )
                                    .replace(/\s+/g, ' ')
                                    .trim();
                                };

                            const tables =
                                Array.from(
                                    document.querySelectorAll(
                                        'table'
                                    )
                                );

                            /*
                             * Frozen HockeyShift tables keep the game
                             * number and action/game links in separate
                             * synchronized tables. Pair those tables by
                             * row index instead of climbing through huge
                             * parent containers.
                             */
                            let numberRows = [];

                            tables.forEach(
                                function(table) {

                                    const rows =
                                        Array.from(
                                            table.querySelectorAll(
                                                'tbody tr'
                                            )
                                        );

                                    const matches =
                                        rows.filter(
                                            function(row) {

                                                const firstCell =
                                                    row.querySelector(
                                                        'td'
                                                    );

                                                return (
                                                    firstCell &&
                                                    /^\d+$/.test(
                                                        elementText(
                                                            firstCell
                                                        )
                                                    )
                                                );
                                            }
                                        );

                                    if (
                                        matches.length >
                                        numberRows.length
                                    ) {
                                        numberRows =
                                            matches;
                                    }
                                }
                            );

                            let gameLinkRows = [];
                            let bestLinkCount = 0;
                            let bestDistance =
                                Number.MAX_SAFE_INTEGER;

                            tables.forEach(
                                function(table) {

                                    const rows =
                                        Array.from(
                                            table.querySelectorAll(
                                                'tbody tr'
                                            )
                                        );

                                    const linkCount =
                                        rows.reduce(
                                            function(total, row) {

                                                return total +
                                                    (
                                                        row.querySelector(
                                                            'a[href*="/182/game/"]'
                                                        )
                                                            ? 1
                                                            : 0
                                                    );
                                            },
                                            0
                                        );

                                    const distance =
                                        Math.abs(
                                            rows.length -
                                            numberRows.length
                                        );

                                    if (
                                        linkCount >
                                        bestLinkCount ||
                                        (
                                            linkCount ===
                                            bestLinkCount &&
                                            linkCount > 0 &&
                                            distance <
                                            bestDistance
                                        )
                                    ) {

                                        bestLinkCount =
                                            linkCount;

                                        bestDistance =
                                            distance;

                                        gameLinkRows =
                                            rows;
                                    }
                                }
                            );

                            const count =
                                Math.min(
                                    numberRows.length,
                                    gameLinkRows.length
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

                                if (
                                    !/^\d+$/.test(
                                        gameNumber
                                    )
                                ) {
                                    continue;
                                }

                                const link =
                                    gameLinkRows[i]
                                        .querySelector(
                                            'a[href*="/182/game/"]'
                                        );

                                if (!link) {
                                    continue;
                                }

                                const href =
                                    link.href ||
                                    link.getAttribute(
                                        'href'
                                    ) ||
                                    '';

                                const match =
                                    href.match(
                                        /\/game\/(\d+)/
                                    );

                                if (!match) {
                                    continue;
                                }

                                results.push(
                                    gameNumber +
                                    '|||' +
                                    match[1]
                                );
                            }

                            /*
                             * If HockeyShift still has more schedule
                             * rows hidden, ask it to load them while
                             * our polling loop continues.
                             */
                            const loadMore =
                                Array.from(
                                    document.querySelectorAll(
                                        'button, a, [role="button"]'
                                    )
                                )
                                .find(
                                    function(element) {

                                        return elementText(
                                            element
                                        )
                                        .toUpperCase() ===
                                        'LOAD MORE';
                                    }
                                );

                            if (loadMore) {
                                loadMore.click();
                            }

                            var items = document.querySelectorAll("ul.score-ticker-games li.item");
                            var prevLabel = "";
                            for (var i = 0; i < items.length; i++) {
                                var item = items[i];
                                var match = elementText(item).match(/^([A-Za-z]{3})[ \t]+([A-Za-z]{3})[ \t]+(\d{1,2})(?=[^A-Za-z0-9]|$)/);
                                if (match) {
                                    prevLabel = match[1] + " " + match[2] + " " + parseInt(match[3], 10);
                                }
                                if (prevLabel === "") {
                                    continue;
                                }
                                var dateLabel = prevLabel;
                                var game = item.querySelector(".game[data-game-id]");
                                if (!game) {
                                    continue;
                                }
                                var anchor = game.querySelector("a[href*='/boxscore']");
                                if (!anchor) {
                                    continue;
                                }
                                var id = anchor.getAttribute("href").match(/\/182\/game\/(\d+)\/boxscore/);
                                if (!id) {
                                    continue;
                                }
                                var gameText = elementText(game).replace(/^([A-Za-z]{3})[ \t]+([A-Za-z]{3})[ \t]+(\d{1,2})(?=[^A-Za-z0-9]|$)[ \t]*/, "");
                                results.push("TICKER|||" + id[1] + "|||" + dateLabel + "|||" + gameText);
                            }
                            return results.join(
                                '\n'
                            );
                        })()
                        """.trimIndent()
                    ) { result ->

                        val rendered =
                            runCatching {

                                if (
                                    result == null ||
                                    result == "null"
                                ) {
                                    ""
                                } else {

                                    JSONTokener(
                                        result
                                    )
                                        .nextValue()
                                        .toString()
                                }

                            }.getOrDefault(
                                ""
                            )

                        val mappedRows =
                            rendered.lines()
                                .count {
                                    it.contains(
                                        "|||"
                                    )
                                }

                        if (
                            (
                                    mappedRows >= 4 &&
                                            attempt >= 6
                                    ) ||
                            attempt >= 20
                        ) {

                            finish(
                                rendered
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
                    SCHEDULE_URL
                )
            }
        }

    private fun parseGameDetails(
        gameId: String,
        text: String
    ): GameDetailsData {

        val lines =
            text.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        val dateTimeVenue =
            lines.firstOrNull {
                Regex(
                    """^[A-Z][a-z]{2} \d{1,2}, \d{4} at .+ - .+$"""
                ).containsMatchIn(it)
            }

        return GameDetailsData(
            gameId = gameId,

            dateTimeVenue =
                dateTimeVenue,

            periodScores =
                parsePeriodTable(
                    lines = lines,
                    startLabel = "SCORING",
                    endLabel = "SHOTS"
                ),

            shots =
                parseShotsOnGoal(text.lines()),

            scoringEvents =
                parseScoringEvents(lines),

            penalties =
                parsePenalties(lines),

            stars =
                parseStars(lines),

            officials =
                parseOfficials(lines),

            startTime =
                parseNamedTime(
                    lines = lines,
                    label = "Start Time"
                ),

            endTime =
                parseNamedTime(
                    lines = lines,
                    label = "End Time"
                ),

            attendance =
                parseAttendance(lines)
        )
    }

    private fun parsePeriodTable(
        lines: List<String>,
        startLabel: String,
        endLabel: String
    ): List<GamePeriodBreakdown> {

        val start =
            lines.indexOfFirst {
                it.equals(
                    startLabel,
                    ignoreCase = true
                )
            }

        val end =
            lines.indexOfFirst {
                it.equals(
                    endLabel,
                    ignoreCase = true
                )
            }

        if (
            start < 0 ||
            end <= start
        ) {
            return emptyList()
        }

        return lines
            .subList(
                start + 1,
                end
            )
            .mapNotNull { line ->

                val columns =
                    line.split("\t")
                        .map {
                            it.trim()
                        }
                        .filter {
                            it.isNotEmpty()
                        }

                if (columns.size < 5) {
                    return@mapNotNull null
                }

                val first =
                    columns[1].toIntOrNull()
                        ?: return@mapNotNull null

                val second =
                    columns[2].toIntOrNull()
                        ?: return@mapNotNull null

                val third =
                    columns[3].toIntOrNull()
                        ?: return@mapNotNull null

                val total =
                    columns[4].toIntOrNull()
                        ?: return@mapNotNull null

                GamePeriodBreakdown(
                    teamName = columns[0],
                    first = first,
                    second = second,
                    third = third,
                    total = total
                )
            }
    }

    private fun parseScoringEvents(
        lines: List<String>
    ): List<GameScoringEvent> {

        val start =
            lines.indexOfFirst {
                it.equals(
                    "SCORING SUMMARY",
                    ignoreCase = true
                )
            }

        val end =
            lines.indexOfFirst {
                it.equals(
                    "PENALTY SUMMARY",
                    ignoreCase = true
                )
            }

        if (
            start < 0 ||
            end <= start
        ) {
            return emptyList()
        }

        val events =
            mutableListOf<GameScoringEvent>()

        var index =
            start + 1

        while (index < end) {

            val firstLine =
                lines[index]
                    .split("\t")
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotEmpty()
                    }

            if (
                firstLine.size >= 3 &&
                isPeriod(firstLine[0]) &&
                isClockTime(firstLine[1]) &&
                index + 1 < end
            ) {

                val secondLine =
                    lines[index + 1]
                        .split("\t")
                        .map {
                            it.trim()
                        }
                        .filter {
                            it.isNotEmpty()
                        }

                if (secondLine.size >= 3) {

                    events.add(
                        GameScoringEvent(
                            period =
                                firstLine[0],

                            time =
                                firstLine[1],

                            strength =
                                firstLine
                                    .drop(2)
                                    .joinToString(" "),

                            teamName =
                                secondLine[0],

                            description =
                                secondLine[1],

                            scoreAfterGoal =
                                secondLine[2]
                        )
                    )

                    index += 2
                    continue
                }
            }

            index++
        }

        return events
    }

    private fun parsePenalties(
        lines: List<String>
    ): List<GamePenalty> {

        val start =
            lines.indexOfFirst {
                it.equals(
                    "PENALTY SUMMARY",
                    ignoreCase = true
                )
            }

        val end =
            lines.indexOfFirst {
                it.equals(
                    "OFFICIALS",
                    ignoreCase = true
                )
            }

        if (
            start < 0 ||
            end <= start
        ) {
            return emptyList()
        }

        val penalties =
            mutableListOf<GamePenalty>()

        var index =
            start + 1

        while (index < end) {

            val firstLine =
                lines[index]
                    .split("\t")
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotEmpty()
                    }

            if (
                firstLine.size >= 3 &&
                isPeriod(firstLine[0]) &&
                isClockTime(firstLine[1])
            ) {

                val period =
                    firstLine[0]

                val time =
                    firstLine[1]

                val player =
                    firstLine
                        .drop(2)
                        .joinToString(" ")

                val detailTokens =
                    mutableListOf<String>()

                var nextIndex =
                    index + 1

                while (nextIndex < end) {

                    val possibleNextPenalty =
                        lines[nextIndex]
                            .split("\t")
                            .map {
                                it.trim()
                            }
                            .filter {
                                it.isNotEmpty()
                            }

                    if (
                        possibleNextPenalty.size >= 3 &&
                        isPeriod(
                            possibleNextPenalty[0]
                        ) &&
                        isClockTime(
                            possibleNextPenalty[1]
                        )
                    ) {
                        break
                    }

                    detailTokens.addAll(
                        possibleNextPenalty
                    )

                    nextIndex++
                }

                val teamName =
                    detailTokens
                        .firstOrNull()
                        ?: ""

                val length =
                    detailTokens
                        .lastOrNull {
                            isClockTime(it)
                        }
                        ?: ""

                val infraction =
                    detailTokens
                        .drop(1)
                        .filter {
                            it != length
                        }
                        .joinToString(" ")

                penalties.add(
                    GamePenalty(
                        period = period,
                        time = time,
                        player = player,
                        teamName = teamName,
                        infraction = infraction,
                        length = length
                    )
                )

                index =
                    nextIndex

                continue
            }

            index++
        }

        return penalties
    }

    private fun parseAttendance(
        lines: List<String>
    ): String? {

        val index =
            lines.indexOfFirst {
                it.equals(
                    "ATTENDANCE",
                    ignoreCase = true
                )
            }

        if (index < 0) {
            return null
        }

        return lines
            .drop(index + 1)
            .firstOrNull {
                it.all(Char::isDigit)
            }
    }

    private fun isPeriod(
        value: String
    ): Boolean {

        return Regex(
            """^(\d+(st|nd|rd|th)|OT|SO)$""",
            RegexOption.IGNORE_CASE
        ).matches(value)
    }

    private fun isClockTime(
        value: String
    ): Boolean {

        return Regex(
            """^\d{1,2}:\d{2}$"""
        ).matches(value)
    }

    private suspend fun renderText(
        context: Context,
        url: String,
        delayMillis: Long
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

                val maxAttempts =
                    maxOf(
                        4,
                        (
                                delayMillis /
                                        250L
                                ).toInt()
                    )

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

                        val upper =
                            text.uppercase(
                                Locale.US
                            )

                        val ready =
                            (
                                    upper.contains(
                                        "SCORING"
                                    ) &&
                                            upper.contains(
                                                "PENALTY SUMMARY"
                                            )
                                    ) ||
                                    (
                                            upper.contains(
                                                "SCORING"
                                            ) &&
                                                    upper.contains(
                                                        "OFFICIALS"
                                                    )
                                            )

                        if (
                            ready ||
                            attempt >= maxAttempts
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