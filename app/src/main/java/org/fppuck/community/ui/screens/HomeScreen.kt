package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
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
import org.fppuck.community.data.FavoriteTeamsStore
import org.fppuck.community.data.ScheduleRepository
import org.fppuck.community.data.ScheduledGame
import org.fppuck.community.data.ScoreboardGame
import org.fppuck.community.data.ScoresRepository
import org.fppuck.community.data.Team
import org.fppuck.community.data.sampleTeams
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.clickable


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onScreenSelected: (AppScreen) -> Unit,
    onTeamSelected: (String) -> Unit,
    onGameSelected: (ScoreboardGame) -> Unit
) {

    val context = LocalContext.current
    val coroutineScope =
        rememberCoroutineScope()

    var manualRefreshRunning by remember {
        mutableStateOf(false)
    }

    val favoriteTeams =
        remember {
            FavoriteTeamsStore.load(context)
        }

    var seasonSchedule by remember {
        mutableStateOf<List<ScheduledGame>>(emptyList())
    }

    var scoreboardGames by remember {
        mutableStateOf<List<ScoreboardGame>>(emptyList())
    }

    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }

    var scheduleLoading by remember {
        mutableStateOf(true)
    }

    var scoreboardLoading by remember {
        mutableStateOf(true)
    }

    var scheduleError by remember {
        mutableStateOf<String?>(null)
    }

    var scoreboardError by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Show the lightweight static schedule immediately.
     * Then enrich it in the background with HockeyShift
     * start times and Watch Live links.
     */
    LaunchedEffect(Unit) {

        try {

            seasonSchedule =
                ScheduleRepository.fetchSchedule()

            scheduleError =
                null

        } catch (
            exception: Exception
        ) {

            scheduleError =
                exception.message
                    ?: "Unable to load schedule"

        } finally {

            scheduleLoading =
                false
        }

        try {

            val enrichedSchedule =
                ScheduleRepository.fetchSchedule(
                    context
                )

            seasonSchedule =
                enrichedSchedule

        } catch (
            exception: Exception
        ) {
            /*
             * Keep the static schedule already on screen.
             */
        }
    }

    /*
     * Refresh the HockeyShift scoreboard every 30 seconds.
     */
    LaunchedEffect(Unit) {

        var firstLoad = true

        while (true) {

            try {

                scoreboardGames =
                    ScoresRepository.fetchScoreboard(context)

                scoreboardError = null

            } catch (exception: Exception) {

                scoreboardError =
                    exception.message
                        ?: "Unable to load scores"

            } finally {

                if (firstLoad) {
                    scoreboardLoading = false
                    firstLoad = false
                }
            }

            delay(30_000)
        }
    }

    /*
     * Merge the full schedule with the live scoreboard.
     *
     * If HockeyShift has a score/status for a game,
     * that version replaces the plain scheduled version.
     */
    val allGames =
        mergeScheduleAndScores(
            schedule = seasonSchedule,
            scores = scoreboardGames
        )

    val gamesForSelectedDate =
        allGames
            .filter {
                it.date == selectedDate
            }
            .let { gamesForDate ->

                val favoriteGames =
                    gamesForDate.filter { game ->

                        game.awayAbbreviation in favoriteTeams ||
                                game.homeAbbreviation in favoriteTeams
                    }

                val otherGames =
                    gamesForDate.filterNot { game ->

                        game.awayAbbreviation in favoriteTeams ||
                                game.homeAbbreviation in favoriteTeams
                    }

                favoriteGames + otherGames
            }

    /*
     * Do not block the entire Home screen while the live
     * scoreboard WebView is loading. The static schedule is
     * enough to render immediately; scores appear as soon as
     * the background refresh completes.
     */
    val isLoading =
        scheduleLoading &&
                seasonSchedule.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "FPHL",
            color = FphlText,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "FEDERAL PROSPECTS HOCKEY LEAGUE",
            color = FphlMutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        LeagueTabs(
            currentScreen = AppScreen.HOME,
            onScreenSelected = onScreenSelected
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        ScoreDateSelector(
            selectedDate = selectedDate,
            onPreviousDate = {
                selectedDate =
                    selectedDate.minusDays(1)
            },
            onNextDate = {
                selectedDate =
                    selectedDate.plusDays(1)
            },
            onToday = {
                selectedDate =
                    LocalDate.now()
            }
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.End
        ) {

            TextButton(
                enabled =
                    !manualRefreshRunning,
                onClick = {

                    coroutineScope.launch {

                        manualRefreshRunning =
                            true

                        try {

                            scoreboardGames =
                                ScoresRepository
                                    .fetchScoreboard(
                                        context =
                                            context,
                                        forceRefresh =
                                            true
                                    )

                            scoreboardError =
                                null

                        } catch (
                            exception: Exception
                        ) {

                            scoreboardError =
                                exception.message
                                    ?: "Unable to refresh scores"

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

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        when {

            isLoading -> {

                CircularProgressIndicator(
                    color = FphlRed
                )
            }

            gamesForSelectedDate.isEmpty() &&
                    scheduleError != null &&
                    scoreboardError != null -> {

                Text(
                    text =
                        "Unable to load FPHL data.\n" +
                                "${scheduleError ?: ""}\n" +
                                "${scoreboardError ?: ""}",
                    color = FphlRed,
                    fontSize = 14.sp
                )
            }

            gamesForSelectedDate.isEmpty() -> {

                Text(
                    text = "No games scheduled.",
                    color = FphlMutedText,
                    fontSize = 14.sp
                )
            }

            else -> {

                gamesForSelectedDate
                    .forEachIndexed { index, game ->

                        ScoreboardGameCard(
                            game = game,

                            isFavorite =
                                game.awayAbbreviation in
                                        favoriteTeams ||
                                        game.homeAbbreviation in
                                        favoriteTeams,

                            onTeamSelected =
                                onTeamSelected,

                            onClick = {
                                onGameSelected(game)
                            }
                        )

                        if (
                            index <
                            gamesForSelectedDate.lastIndex
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )
                        }
                    }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun ScoreDateSelector(
    selectedDate: LocalDate,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onToday: () -> Unit
) {

    val today =
        LocalDate.now()

    val formatter =
        DateTimeFormatter.ofPattern(
            "EEE, MMM d",
            Locale.US
        )

    val dateText =
        if (selectedDate == today) {

            "TODAY • ${
                selectedDate
                    .format(formatter)
                    .uppercase()
            }"

        } else {

            selectedDate
                .format(formatter)
                .uppercase()
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        TextButton(
            onClick = onPreviousDate
        ) {

            Text(
                text = "‹",
                color = FphlText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = onToday
        ) {

            Text(
                text = dateText,
                color =
                    if (selectedDate == today) {
                        FphlRed
                    } else {
                        FphlText
                    },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = onNextDate
        ) {

            Text(
                text = "›",
                color = FphlText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ScoreboardGameCard(
    game: ScoreboardGame,
    isFavorite: Boolean,
    onTeamSelected: (String) -> Unit,
    onClick: () -> Unit
) {

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

    val isLive =
        game.awayScore != null &&
                !game.status.contains(
                    "FINAL",
                    ignoreCase = true
                )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isFavorite) {
                    FphlRed.copy(
                        alpha = 0.16f
                    )
                } else {
                    FphlCard
                }
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            val statusLabel =
                liveDisplayLabel(
                    rawLabel =
                        game.localStatusLabel(),
                    isLive =
                        isLive ||
                                game.status.equals(
                                    "LIVE",
                                    ignoreCase = true
                                )
                )

            Text(
                text =
                    statusLabel,
                color =
                    if (
                        isLive ||
                        statusLabel == "LIVE"
                    ) {
                        FphlRed
                    } else {
                        FphlMutedText
                    },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            ScoreTeamRow(
                logoUrl =
                    awayTeam?.logoUrl,

                teamName =
                    if (awayTeam != null) {
                        "${awayTeam.city} ${awayTeam.name}"
                    } else {
                        game.awayAbbreviation
                    },

                score =
                    game.awayScore,

                onClick =
                    awayTeam?.let { team ->
                        {
                            onTeamSelected(
                                team.abbreviation
                            )
                        }
                    }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            ScoreTeamRow(
                logoUrl =
                    homeTeam?.logoUrl,

                teamName =
                    if (homeTeam != null) {
                        "${homeTeam.city} ${homeTeam.name}"
                    } else {
                        game.homeAbbreviation
                    },

                score =
                    game.homeScore,

                onClick =
                    homeTeam?.let { team ->
                        {
                            onTeamSelected(
                                team.abbreviation
                            )
                        }
                    }
            )
        }
    }
}

@Composable
fun ScoreTeamRow(
    logoUrl: String?,
    teamName: String,
    score: Int?,
    onClick: (() -> Unit)?
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = onClick != null
            ) {
                onClick?.invoke()
            }
            .padding(
                vertical = 4.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        if (logoUrl != null) {

            AsyncImage(
                model = logoUrl,
                contentDescription =
                    "$teamName logo",
                contentScale =
                    ContentScale.Fit,
                modifier =
                    Modifier.size(48.dp)
            )

            Spacer(
                modifier =
                    Modifier.size(12.dp)
            )
        }

        Text(
            text = teamName,
            modifier =
                Modifier.weight(1f),
            color = FphlText,
            fontSize = 16.sp,
            fontWeight =
                FontWeight.Medium
        )

        if (score != null) {

            Text(
                text =
                    score.toString(),
                color = FphlText,
                fontSize = 22.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
private fun mergeScheduleAndScores(
    schedule: List<ScheduledGame>,
    scores: List<ScoreboardGame>
): List<ScoreboardGame> {

    val scoreLookup =
        scores.associateBy {
            gameKey(
                date = it.date,
                away = it.awayAbbreviation,
                home = it.homeAbbreviation
            )
        }

    val scheduleKeys =
        mutableSetOf<String>()

    /*
     * Actual scheduled start instant for sorting.
     *
     * Using the instant instead of the source clock time
     * means 7:00 PM EDT correctly sorts before
     * 7:05 PM CDT, etc.
     */
    val startTimeLookup =
        mutableMapOf<String, Long>()

    val gameNumberLookup =
        mutableMapOf<String, Int>()

    val merged =
        schedule.mapNotNull { scheduledGame ->

            val awayTeam =
                findTeamByScheduleName(
                    scheduledGame.awayTeam
                )
                    ?: return@mapNotNull null

            val homeTeam =
                findTeamByScheduleName(
                    scheduledGame.homeTeam
                )
                    ?: return@mapNotNull null

            val key =
                gameKey(
                    date = scheduledGame.date,
                    away = awayTeam.abbreviation,
                    home = homeTeam.abbreviation
                )

            scheduleKeys.add(key)

            startTimeLookup[key] =
                scheduledGame
                    .sourceStartDateTime
                    ?.toInstant()
                    ?.toEpochMilli()
                    ?: Long.MAX_VALUE

            gameNumberLookup[key] =
                scheduledGame.gameNumber

            scoreLookup[key]
                ?.copy(
                    watchUrl =
                        scheduledGame.watchUrl
                )
                ?: ScoreboardGame(
                    date =
                        scheduledGame.date,

                    awayAbbreviation =
                        awayTeam.abbreviation,

                    homeAbbreviation =
                        homeTeam.abbreviation,

                    status =
                        scheduledStatusLabel(
                            scheduledGame
                        ),

                    watchUrl =
                        scheduledGame.watchUrl
                )
        }
            .toMutableList()

    /*
     * Keep any scoreboard games that somehow
     * weren't found in the full schedule.
     */
    scores.forEach { score ->

        val key =
            gameKey(
                date = score.date,
                away = score.awayAbbreviation,
                home = score.homeAbbreviation
            )

        if (key !in scheduleKeys) {
            merged.add(score)
        }
    }

    return merged.sortedWith(

        compareBy<ScoreboardGame> {
            it.date

        }.thenBy { game ->

            val key =
                gameKey(
                    date = game.date,
                    away = game.awayAbbreviation,
                    home = game.homeAbbreviation
                )

            startTimeLookup[key]
                ?: Long.MAX_VALUE

        }.thenBy { game ->

            val key =
                gameKey(
                    date = game.date,
                    away = game.awayAbbreviation,
                    home = game.homeAbbreviation
                )

            gameNumberLookup[key]
                ?: Int.MAX_VALUE
        }
    )
}
private fun liveDisplayLabel(
    rawLabel: String,
    isLive: Boolean
): String {

    val label =
        rawLabel.trim().uppercase()

    if (!isLive) {
        return label
    }

    return when {
        label == "LIVE" ->
            "LIVE"

        label.startsWith(
            "LIVE"
        ) ->
            label

        else ->
            "LIVE • $label"
    }
}

private fun scheduledStatusLabel(
    game: ScheduledGame
): String {

    val startInstant =
        game.sourceStartDateTime
            ?.toInstant()

    val hasReachedPuckDrop =
        game.date == LocalDate.now() &&
                startInstant != null &&
                !startInstant.isAfter(
                    java.time.Instant.now()
                )

    return if (hasReachedPuckDrop) {
        "LIVE"
    } else {
        game.localStartTimeLabel()
            ?: "SCHEDULED"
    }
}

private fun gameKey(
    date: LocalDate,
    away: String,
    home: String
): String {

    return "$date|$away|$home"
}

private fun findTeamByScheduleName(
    teamName: String
): Team? {

    val normalizedName =
        teamName
            .trim()
            .lowercase()

    /*
     * The public schedule occasionally uses names
     * different from the official team directory.
     */
    val aliases =
        mapOf(
            "oceanside hc" to
                    "oceanside shock",

            "baton rouge" to
                    "baton rouge kingfish"
        )

    val resolvedName =
        aliases[normalizedName]
            ?: normalizedName

    return sampleTeams.firstOrNull { team ->

        val fullName =
            "${team.city} ${team.name}"
                .trim()
                .lowercase()

        fullName == resolvedName
    }
}