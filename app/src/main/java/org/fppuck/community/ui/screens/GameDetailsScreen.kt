package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
import org.fppuck.community.data.ScoresRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.fppuck.community.data.GameDetailsData
import org.fppuck.community.data.GameDetailsRepository
import org.fppuck.community.data.GamePenalty
import org.fppuck.community.data.GamePeriodBreakdown
import org.fppuck.community.data.GameScoringEvent
import org.fppuck.community.data.LiveGameRepository
import org.fppuck.community.data.LiveGameState
import org.fppuck.community.data.ScoreboardGame
import org.fppuck.community.data.SporfieRepository
import org.fppuck.community.data.sampleTeams
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun GameDetailsScreen(
    game: ScoreboardGame,
    modifier: Modifier = Modifier,
    backLabel: String,
    onTeamSelected: (String) -> Unit,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    var manualRefreshRunning by remember(game) {
        mutableStateOf(false)
    }

    val awayTeam =
        sampleTeams.firstOrNull {
            it.abbreviation ==
                    game.awayAbbreviation
        }

    val homeTeam =
        sampleTeams.firstOrNull {
            it.abbreviation ==
                    game.homeAbbreviation
        }

    var details by remember(game) {
        mutableStateOf<GameDetailsData?>(null)
    }

    var loading by remember(game) {
        mutableStateOf(false)
    }

    var error by remember(game) {
        mutableStateOf<String?>(null)
    }
    var refreshedGame by remember(game) {
        mutableStateOf(game)
    }

    var liveState by remember(game) {
        mutableStateOf<LiveGameState?>(null)
    }

    var directWatchUrl by remember(game) {
        mutableStateOf<String?>(null)
    }

    var resolvingWatchUrl by remember(game) {
        mutableStateOf(false)
    }

    var watchUrlResolutionFailed by remember(game) {
        mutableStateOf(false)
    }
    val futureScheduledGame =
        refreshedGame.awayScore == null &&
                refreshedGame.homeScore == null &&
                !refreshedGame.status.contains(
                    "FINAL",
                    ignoreCase = true
                ) &&
                !refreshedGame.status.equals(
                    "LIVE",
                    ignoreCase = true
                ) &&
                !refreshedGame.date.isBefore(
                    LocalDate.now()
                )

    val scheduledStartLabel =
        if (futureScheduledGame) {

            refreshedGame.localStatusLabel()
                .takeIf {
                    it.contains(":")
                }

        } else {
            null
        }

    LaunchedEffect(
        refreshedGame.watchUrl,
        refreshedGame.date,
        refreshedGame.awayAbbreviation,
        refreshedGame.homeAbbreviation
    ) {

        val rawWatchUrl =
            refreshedGame.watchUrl

        if (
            rawWatchUrl.isNullOrBlank()
        ) {

            directWatchUrl =
                null

            resolvingWatchUrl =
                false

            watchUrlResolutionFailed =
                false

        } else if (
            rawWatchUrl.contains(
                "/event/",
                ignoreCase = true
            )
        ) {

            directWatchUrl =
                rawWatchUrl

            resolvingWatchUrl =
                false

            watchUrlResolutionFailed =
                false

        } else {

            resolvingWatchUrl =
                true

            watchUrlResolutionFailed =
                false

            directWatchUrl =
                SporfieRepository
                    .resolveEventUrl(
                        context = context,
                        game = refreshedGame,
                        watchUrl = rawWatchUrl
                    )

            resolvingWatchUrl =
                false

            watchUrlResolutionFailed =
                directWatchUrl == null
        }
    }

    LaunchedEffect(game) {

        refreshedGame =
            game

        /*
         * Only today's game needs continuous scoreboard
         * updates. Past games are final and future games
         * haven't started yet.
         */
        if (
            game.date != LocalDate.now()
        ) {
            return@LaunchedEffect
        }

        while (true) {

            try {

                val latestGame =
                    ScoresRepository
                        .fetchScoreboard(
                            context
                        )
                        .firstOrNull {
                            it.date ==
                                    game.date &&
                                    it.awayAbbreviation ==
                                    game.awayAbbreviation &&
                                    it.homeAbbreviation ==
                                    game.homeAbbreviation
                        }

                latestGame
                    ?.let { latest ->

                        refreshedGame =
                            latest.copy(
                                watchUrl =
                                    game.watchUrl
                                        ?: latest.watchUrl
                            )
                    }

            } catch (
                exception: Exception
            ) {
                /*
                 * Keep displaying the last known data
                 * if one refresh fails.
                 */
            }

            if (
                refreshedGame.status.contains(
                    "FINAL",
                    ignoreCase = true
                )
            ) {
                break
            }

            delay(
                30_000
            )
        }
    }

    LaunchedEffect(
        game.date,
        game.awayAbbreviation,
        game.homeAbbreviation
    ) {

        /*
         * Game Details must refresh independently of the
         * scoreboard.
         *
         * Previously this fetch was keyed to refreshedGame.
         * If a penalty, shot total, or other boxscore detail
         * changed without the score/status changing, Compose
         * had no reason to rerun the details request.
         *
         * For today's active game, poll the boxscore every
         * 30 seconds. The repository's short live cache and
         * mutex still prevent wasteful duplicate WebViews.
         */
        while (true) {

            val currentGame =
                refreshedGame

            val currentlyScheduled =
                currentGame.awayScore == null &&
                        currentGame.homeScore == null &&
                        !currentGame.status.contains(
                            "FINAL",
                            ignoreCase = true
                        ) &&
                        !currentGame.status.equals(
                            "LIVE",
                            ignoreCase = true
                        ) &&
                        !currentGame.date.isBefore(
                            LocalDate.now()
                        )

            if (!currentlyScheduled) {

                loading =
                    details == null

                try {

                    details =
                        GameDetailsRepository
                            .fetchGameDetails(
                                context =
                                    context,

                                game =
                                    currentGame
                            )

                    error =
                        null

                } catch (
                    exception: Exception
                ) {

                    error =
                        exception.message
                            ?: "Unable to load game details"

                } finally {

                    loading =
                        false
                }
            }

            val finalOrPast =
                refreshedGame.status.contains(
                    "FINAL",
                    ignoreCase = true
                ) ||
                        refreshedGame.date.isBefore(
                            LocalDate.now()
                        )

            if (finalOrPast) {
                break
            }

            /*
             * Future scheduled games do not need continuous
             * boxscore polling. Once puck drop has passed,
             * the screen can be reopened/refreshed normally.
             */
            // A scheduled screen for today keeps polling the scoreboard every 30s,
            // so re-check refreshedGame before any box-score fetch.
            if (currentlyScheduled && refreshedGame.date != LocalDate.now()) {
                break
            }

            delay(
                30_000
            )
        }
    }

    LaunchedEffect(
        details?.gameId,
        refreshedGame.date
    ) {

        val gameId =
            details?.gameId
                ?: return@LaunchedEffect

        if (
            refreshedGame.date !=
            LocalDate.now()
        ) {
            return@LaunchedEffect
        }

        while (true) {

            try {

                liveState =
                    LiveGameRepository
                        .fetchLiveState(
                            context = context,
                            gameId = gameId
                        )

            } catch (
                exception: Exception
            ) {
                /*
                 * Keep the last good live state if a
                 * play-by-play refresh fails.
                 */
            }

            if (
                refreshedGame.status.contains(
                    "FINAL",
                    ignoreCase = true
                ) ||
                liveState
                    ?.period
                    ?.contains(
                        "FINAL",
                        ignoreCase = true
                    ) == true
            ) {
                break
            }

            delay(
                15_000
            )
        }
    }

    val awayScore =
        liveState
            ?.awayScore
            ?: refreshedGame.awayScore
            ?: details
                ?.periodScores
                ?.getOrNull(0)
                ?.total

    val homeScore =
        liveState
            ?.homeScore
            ?: refreshedGame.homeScore
            ?: details
                ?.periodScores
                ?.getOrNull(1)
                ?.total

    val isLiveGame =
        refreshedGame.date ==
                LocalDate.now() &&
                awayScore != null &&
                homeScore != null &&
                !refreshedGame.status.contains(
                    "FINAL",
                    ignoreCase = true
                )

    /*
     * When HockeyShift gives us a live period/clock, make it
     * explicit that this is GAME TIME rather than local time.
     *
     * Examples:
     *   LIVE • 1ST 12:48
     *   LIVE • 2ND
     *   LIVE
     */
    val liveGameTimeLabel =
        if (
            isLiveGame ||
            refreshedGame.status.equals(
                "LIVE",
                ignoreCase = true
            )
        ) {

            val liveStatus =
                (
                        liveState
                            ?.statusLabel()
                            ?: refreshedGame
                                .localStatusLabel()
                        )
                    .uppercase()

            when {
                liveStatus == "LIVE" ->
                    "LIVE"

                liveStatus.startsWith(
                    "LIVE"
                ) ->
                    liveStatus

                else ->
                    "LIVE • $liveStatus"
            }

        } else {
            null
        }

    val dateFormatter =
        DateTimeFormatter.ofPattern(
            "EEEE, MMMM d, uuuu",
            Locale.US
        )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Text(
            text = "←  $backLabel",
            color = FphlMutedText,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable {
                    onBack()
                }
                .padding(
                    vertical = 8.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                buildString {

                    append(
                        game.date
                            .format(dateFormatter)
                            .uppercase()
                    )

                    scheduledStartLabel
                        ?.let { startTime ->

                            append("  •  ")
                            append(
                                startTime.uppercase()
                            )
                        }
                },
            color = FphlMutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    if (futureScheduledGame) {

                        "SCHEDULED"

                    } else if (
                        refreshedGame.status.contains(
                            "FINAL",
                            ignoreCase = true
                        ) ||
                        (
                                game.date.isBefore(
                                    LocalDate.now()
                                ) &&
                                        details != null
                                )
                    ) {

                        "FINAL"

                    } else if (
                        liveGameTimeLabel != null
                    ) {

                        liveGameTimeLabel

                    } else {

                        refreshedGame.status.uppercase()
                    },

                modifier =
                    Modifier.weight(1f),

                color =
                    FphlRed,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold
            )

            TextButton(
                enabled =
                    !manualRefreshRunning,
                onClick = {

                    coroutineScope.launch {

                        manualRefreshRunning =
                            true

                        try {

                            val latestGame =
                                ScoresRepository
                                    .fetchScoreboard(
                                        context =
                                            context,
                                        forceRefresh =
                                            true
                                    )
                                    .firstOrNull {
                                        it.date ==
                                                game.date &&
                                                it.awayAbbreviation ==
                                                game.awayAbbreviation &&
                                                it.homeAbbreviation ==
                                                game.homeAbbreviation
                                    }

                            latestGame
                                ?.let { latest ->

                                    refreshedGame =
                                        latest.copy(
                                            watchUrl =
                                                game.watchUrl
                                                    ?: latest.watchUrl
                                        )
                                }

                            val refreshTarget =
                                latestGame
                                    ?: refreshedGame

                            if (
                                refreshTarget.awayScore != null ||
                                refreshTarget.homeScore != null ||
                                refreshTarget.status.contains(
                                    "FINAL",
                                    ignoreCase = true
                                )
                            ) {

                                val refreshedDetails =
                                    GameDetailsRepository
                                        .fetchGameDetails(
                                            context =
                                                context,
                                            game =
                                                refreshTarget,
                                            forceRefresh =
                                                true
                                        )

                                details =
                                    refreshedDetails

                                if (
                                    !refreshTarget.status.contains(
                                        "FINAL",
                                        ignoreCase = true
                                    )
                                ) {

                                    liveState =
                                        LiveGameRepository
                                            .fetchLiveState(
                                                context =
                                                    context,
                                                gameId =
                                                    refreshedDetails.gameId,
                                                forceRefresh =
                                                    true
                                            )
                                }
                            }

                            error =
                                null

                        } catch (
                            exception: Exception
                        ) {

                            error =
                                exception.message
                                    ?: "Unable to refresh game"

                        } finally {

                            manualRefreshRunning =
                                false
                        }
                    }
                }
            ) {

                Text(
                    text =
                        if (
                            manualRefreshRunning
                        ) {
                            "REFRESHING..."
                        } else {
                            "REFRESH"
                        },
                    color =
                        if (
                            manualRefreshRunning
                        ) {
                            FphlMutedText
                        } else {
                            FphlRed
                        },
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
        liveGameTimeLabel
            ?.takeIf {
                it != "LIVE"
            }
            ?.let {

                Text(
                    text =
                        if (
                            liveState
                                ?.lastPlay != null
                        ) {
                            "AS OF LAST RECORDED PLAY"
                        } else {
                            "LIVE STATUS"
                        },
                    color =
                        FphlMutedText,
                    fontSize =
                        10.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
            ) {

                AsyncImage(
                    model =
                        awayTeam?.logoUrl,
                    contentDescription =
                        game.awayAbbreviation,
                    contentScale =
                        ContentScale.Fit,
                    modifier =
                        Modifier
                            .size(96.dp)
                            .clickable(
                                enabled = awayTeam != null
                            ) {
                                awayTeam?.let {
                                    onTeamSelected(
                                        it.abbreviation
                                    )
                                }
                            }
                )

                Text(
                    text =
                        awayTeam?.name
                            ?: game.awayAbbreviation,
                    color = FphlText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        awayScore
                            ?.toString()
                            ?: "—",
                    color = FphlText,
                    fontSize = 38.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }

            Text(
                text = "AT",
                color = FphlMutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
            ) {

                AsyncImage(
                    model =
                        homeTeam?.logoUrl,
                    contentDescription =
                        game.homeAbbreviation,
                    contentScale =
                        ContentScale.Fit,
                    modifier =
                        Modifier
                            .size(96.dp)
                            .clickable(
                                enabled = homeTeam != null
                            ) {
                                homeTeam?.let {
                                    onTeamSelected(
                                        it.abbreviation
                                    )
                                }
                            }
                )

                Text(
                    text =
                        homeTeam?.name
                            ?: game.homeAbbreviation,
                    color = FphlText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        homeScore
                            ?.toString()
                            ?: "—",
                    color = FphlText,
                    fontSize = 38.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }
        }

        liveState
            ?.lastPlay
            ?.let { lastPlay ->

                val lastPlayTeam =
                    sampleTeams
                        .firstOrNull {
                            it.abbreviation.equals(
                                lastPlay.teamAbbreviation,
                                ignoreCase = true
                            )
                        }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                FphlCard
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    "LAST PLAY",
                                color =
                                    FphlMutedText,
                                fontSize =
                                    11.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            lastPlay.time
                                ?.let { time ->

                                    Text(
                                        text =
                                            time,
                                        color =
                                            FphlMutedText,
                                        fontSize =
                                            12.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            lastPlayTeam
                                ?.let { team ->

                                    AsyncImage(
                                        model =
                                            team.logoUrl,
                                        contentDescription =
                                            team.abbreviation,
                                        contentScale =
                                            ContentScale.Fit,
                                        modifier =
                                            Modifier.size(
                                                30.dp
                                            )
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.size(
                                                10.dp
                                            )
                                    )
                                }

                            Column(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(
                                    text =
                                        lastPlay.type
                                            .uppercase(),
                                    color =
                                        FphlRed,
                                    fontSize =
                                        15.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )

                                lastPlay.teamAbbreviation
                                    ?.let { team ->

                                        Text(
                                            text =
                                                team.uppercase(),
                                            color =
                                                FphlMutedText,
                                            fontSize =
                                                11.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                            }
                        }

                        if (
                            lastPlay.description
                                .isNotBlank()
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    lastPlay.description,
                                color =
                                    FphlText,
                                fontSize =
                                    14.sp
                            )
                        }
                    }
                }
            }

        if (
            !refreshedGame.watchUrl.isNullOrBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    directWatchUrl
                        ?.let { eventUrl ->

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        eventUrl
                                    )
                                )

                            context.startActivity(
                                intent
                            )
                        }
                },
                enabled =
                    directWatchUrl != null,
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            FphlRed,
                        contentColor =
                            FphlText
                    )
            ) {

                Text(
                    text =
                        when {

                            resolvingWatchUrl ->
                                "FINDING LIVE FEED..."

                            watchUrlResolutionFailed ->
                                "LIVE FEED LINK UNAVAILABLE"

                            else ->
                                "WATCH LIVE"
                        },
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        if (futureScheduledGame) {

            InfoCard(
                text =
                    scheduledStartLabel
                        ?.let {
                            "Scheduled puck drop: $it"
                        }
                        ?: "Game has not started."
            )

        } else if (loading) {

            CircularProgressIndicator(
                color = FphlRed
            )

        } else if (error != null) {

            InfoCard(
                text =
                    "Unable to load detailed boxscore.\n$error"
            )

        } else {

            details?.let { gameDetails ->

                gameDetails.dateTimeVenue
                    ?.let { dateTimeVenue ->

                        InfoCard(
                            text =
                                if (isLiveGame) {

                                    venueOnly(
                                        dateTimeVenue
                                    )

                                } else {

                                    localizeDateTimeVenue(
                                        dateTimeVenue
                                    )
                                }
                        )

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )
                    }
                PeriodTable(
                    title = "SCORING",
                    rows =
                        gameDetails.periodScores
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                InfoCard(
                    text = run {
                        val shotsRows = gameDetails.shots
                        val awayKnownNames = listOfNotNull(
                            awayTeam?.name,
                            awayTeam?.city,
                            game.awayAbbreviation,
                            awayTeam?.let { team ->
                                team.city + " " + team.name
                            }
                        ).map { it.lowercase().trim() }
                        val homeKnownNames = listOfNotNull(
                            homeTeam?.name,
                            homeTeam?.city,
                            game.homeAbbreviation,
                            homeTeam?.let { team ->
                                team.city + " " + team.name
                            }
                        ).map { it.lowercase().trim() }
                        val awayRow = shotsRows.firstOrNull { row ->
                            val label = row.teamName.lowercase().trim()
                            awayKnownNames.any { known ->
                                label == known ||
                                    label.replace(" ", "") ==
                                        known.replace(" ", "")
                            }
                        }
                        val homeRow = shotsRows.firstOrNull { row ->
                            val label = row.teamName.lowercase().trim()
                            homeKnownNames.any { known ->
                                label == known ||
                                    label.replace(" ", "") ==
                                        known.replace(" ", "")
                            }
                        }
                        val awayTotal = awayRow?.total
                        val homeTotal = homeRow?.total
                        val awayLabel =
                            awayTeam?.name ?: game.awayAbbreviation
                        val homeLabel =
                            homeTeam?.name ?: game.homeAbbreviation
                        val awayText =
                            awayTotal?.toString() ?: "Unavailable"
                        val homeText =
                            homeTotal?.toString() ?: "Unavailable"
                        "Shots on Goal: $awayLabel $awayText - $homeText $homeLabel"
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                if (gameDetails.shots.isNotEmpty()) {
                    PeriodTable(
                        title = "SHOTS",
                        rows =
                            gameDetails.shots
                    )
                }
                if (gameDetails.stars.isNotEmpty()) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    ThreeStarsCard(
                        stars = gameDetails.stars
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                ScoringSummary(
                    events =
                        gameDetails.scoringEvents
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                PenaltySummary(
                    penalties =
                        gameDetails.penalties
                )
                if (gameDetails.officials.isNotEmpty()) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    OfficialsCard(
                        officials =
                            gameDetails.officials
                    )
                }

                if (
                    gameDetails.startTime != null ||
                    gameDetails.endTime != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    InfoCard(
                        text =
                            buildString {

                                gameDetails.startTime
                                    ?.let { startTime ->

                                        append(
                                            "Start: ${
                                                localizeGameClockTime(
                                                    rawTime = startTime,
                                                    dateTimeVenue =
                                                        gameDetails.dateTimeVenue,
                                                    fallbackDate =
                                                        game.date
                                                )
                                            }"
                                        )
                                    }

                                if (
                                    gameDetails.startTime != null &&
                                    gameDetails.endTime != null
                                ) {
                                    append("\n")
                                }

                                gameDetails.endTime
                                    ?.let { endTime ->

                                        append(
                                            "End: ${
                                                localizeGameClockTime(
                                                    rawTime = endTime,
                                                    dateTimeVenue =
                                                        gameDetails.dateTimeVenue,
                                                    fallbackDate =
                                                        game.date
                                                )
                                            }"
                                        )
                                    }
                            }
                    )                }

                gameDetails.attendance
                    ?.let { attendance ->

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )

                        InfoCard(
                            text =
                                "Attendance: $attendance"
                        )
                    }
            }
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )
    }
}
private fun sourceZoneFromAbbreviation(
    abbreviation: String
): ZoneId? {

    return when (
        abbreviation.uppercase()
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

private val hockeyShiftDateFormatter =
    DateTimeFormatter.ofPattern(
        "MMM d, uuuu",
        Locale.US
    )

private val hockeyShiftTimeFormatter =
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern(
            "h:mma"
        )
        .toFormatter(
            Locale.US
        )

private val localCompletedTimeFormatter =
    DateTimeFormatter.ofPattern(
        "h:mm a z",
        Locale.US
    )

private fun venueOnly(
    raw: String
): String {

    return raw
        .substringAfter(
            " - ",
            raw
        )
        .trim()
}

private fun localizeDateTimeVenue(
    raw: String
): String {

    val match =
        Regex(
            """^([A-Za-z]{3} \d{1,2}, \d{4}) at (\d{1,2}:\d{2}\s*(?:am|pm)) ([A-Z]{3})\s*-\s*(.+)$""",
            RegexOption.IGNORE_CASE
        ).find(
            raw.trim()
        )
            ?: return raw

    return try {

        val sourceDate =
            LocalDate.parse(
                match.groupValues[1],
                hockeyShiftDateFormatter
            )

        val sourceTime =
            LocalTime.parse(
                match.groupValues[2]
                    .replace(" ", ""),
                hockeyShiftTimeFormatter
            )

        val sourceZone =
            sourceZoneFromAbbreviation(
                match.groupValues[3]
            )
                ?: return raw

        val localDateTime =
            ZonedDateTime.of(
                sourceDate,
                sourceTime,
                sourceZone
            )
                .withZoneSameInstant(
                    ZoneId.systemDefault()
                )

        val localDate =
            localDateTime.format(
                hockeyShiftDateFormatter
            )

        val localTime =
            localDateTime.format(
                localCompletedTimeFormatter
            )

        val venue =
            match.groupValues[4]

        "$localDate at $localTime - $venue"

    } catch (
        exception: Exception
    ) {

        raw
    }
}

private fun localizeGameClockTime(
    rawTime: String,
    dateTimeVenue: String?,
    fallbackDate: LocalDate
): String {

    if (dateTimeVenue == null) {
        return rawTime
    }

    val zoneMatch =
        Regex(
            """\b(EST|EDT|CST|CDT|MST|MDT|PST|PDT)\b""",
            RegexOption.IGNORE_CASE
        ).find(
            dateTimeVenue
        )
            ?: return rawTime

    val sourceZone =
        sourceZoneFromAbbreviation(
            zoneMatch.value
        )
            ?: return rawTime

    val dateMatch =
        Regex(
            """([A-Za-z]{3} \d{1,2}, \d{4})"""
        ).find(
            dateTimeVenue
        )

    return try {

        val sourceDate =
            dateMatch
                ?.groupValues
                ?.getOrNull(1)
                ?.let {
                    LocalDate.parse(
                        it,
                        hockeyShiftDateFormatter
                    )
                }
                ?: fallbackDate

        val sourceTime =
            LocalTime.parse(
                rawTime
                    .replace(" ", ""),
                hockeyShiftTimeFormatter
            )

        ZonedDateTime.of(
            sourceDate,
            sourceTime,
            sourceZone
        )
            .withZoneSameInstant(
                ZoneId.systemDefault()
            )
            .format(
                localCompletedTimeFormatter
            )

    } catch (
        exception: Exception
    ) {

        rawTime
    }
}
@Composable
private fun InfoCard(
    text: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FphlCard
            )
    ) {

        Text(
            text = text,
            color = FphlText,
            fontSize = 14.sp,
            modifier =
                Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PeriodTable(
    title: String,
    rows: List<GamePeriodBreakdown>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FphlCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "TEAM",
                    modifier =
                        Modifier.weight(4f),
                    color = FphlMutedText,
                    fontSize = 10.sp
                )

                PeriodHeader("1ST")
                PeriodHeader("2ND")
                PeriodHeader("3RD")
                PeriodHeader("T")
            }

            rows.forEach { row ->

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            row.teamName,
                        modifier =
                            Modifier.weight(4f),
                        color = FphlText,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    PeriodValue(
                        row.first
                    )

                    PeriodValue(
                        row.second
                    )

                    PeriodValue(
                        row.third
                    )

                    PeriodValue(
                        row.total
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.PeriodHeader(
    text: String
) {

    Text(
        text = text,
        modifier =
            Modifier.weight(1f),
        color = FphlMutedText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RowScope.PeriodValue(
    value: Int
) {

    Text(
        text =
            value.toString(),
        modifier =
            Modifier.weight(1f),
        color = FphlText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ScoringSummary(
    events: List<GameScoringEvent>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FphlCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text =
                    "SCORING SUMMARY",
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (events.isEmpty()) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "No scoring events available.",
                    color =
                        FphlMutedText
                )
            }

            events.forEach { event ->

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    text =
                        "${event.period}  ${event.time}  ${event.strength}",
                    color = FphlRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        event.teamName,
                    color = FphlText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        event.description,
                    color = FphlText,
                    fontSize = 13.sp
                )

                Text(
                    text =
                        event.scoreAfterGoal,
                    color = FphlMutedText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PenaltySummary(
    penalties: List<GamePenalty>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FphlCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text =
                    "PENALTY SUMMARY",
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (penalties.isEmpty()) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "No penalties available.",
                    color =
                        FphlMutedText
                )
            }

            penalties.forEach { penalty ->

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    text =
                        "${penalty.period}  ${penalty.time}  ${penalty.length}",
                    color = FphlRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        penalty.player,
                    color = FphlText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        penalty.teamName,
                    color = FphlMutedText,
                    fontSize = 12.sp
                )

                Text(
                    text =
                        penalty.infraction,
                    color = FphlText,
                    fontSize = 13.sp
                )
            }
        }
    }
}
@Composable
private fun ThreeStarsCard(
    stars: List<org.fppuck.community.data.GameStar>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FphlCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = "3 STARS",
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.Bold
            )

            stars.forEach { star ->

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    text = star.rank,
                    color = FphlRed,
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = star.playerName,
                    color = FphlText,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = star.statLine,
                    color = FphlText,
                    fontSize = 13.sp
                )

                Text(
                    text = star.teamName,
                    color = FphlMutedText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun OfficialsCard(
    officials: List<org.fppuck.community.data.GameOfficial>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = FphlCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = "OFFICIALS",
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.Bold
            )

            officials.forEach { official ->

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "${official.type}: ${official.name}",
                    color = FphlText,
                    fontSize = 13.sp
                )
            }
        }
    }
}