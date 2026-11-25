package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext

@Composable
fun ScheduleScreen(
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

    var games by remember {
        mutableStateOf<List<ScheduledGame>>(emptyList())
    }

    var scoreboardGames by remember {
        mutableStateOf<List<ScoreboardGame>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        /*
         * Show the lightweight static schedule first.
         * This avoids making the user wait for HockeyShift's
         * rendered page just to see the Schedule tab.
         */
        try {

            games =
                upcomingGames(
                    ScheduleRepository.fetchSchedule()
                )

            errorMessage =
                null

        } catch (
            exception: Exception
        ) {

            errorMessage =
                exception.message
                    ?: "Unknown schedule error"

        } finally {

            isLoading =
                false
        }

        /*
         * Enrich the already-visible schedule in the background
         * with local puck-drop times and Watch Live links.
         */
        try {

            val enrichedSchedule =
                ScheduleRepository.fetchSchedule(
                    context
                )

            games =
                upcomingGames(
                    enrichedSchedule
                )

        } catch (
            exception: Exception
        ) {
            /*
             * Keep the static schedule that is already visible.
             */
        }
    }

    /*
     * Keep today's scores/statuses current while the Schedule
     * screen is open. This lets the Schedule tab become a
     * live scoreboard too instead of staying purely static.
     */
    LaunchedEffect(Unit) {

        while (true) {

            try {

                scoreboardGames =
                    ScoresRepository
                        .fetchScoreboard(
                            context
                        )

            } catch (
                exception: Exception
            ) {
                /*
                 * Keep the last successful scoreboard snapshot.
                 */
            }

            delay(
                30_000
            )
        }
    }

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
            currentScreen = AppScreen.SCHEDULE,
            onScreenSelected = onScreenSelected
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "SCHEDULE",
                modifier =
                    Modifier.weight(1f),
                color = FphlText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

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
            modifier = Modifier.height(16.dp)
        )

        when {

            isLoading -> {

                CircularProgressIndicator(
                    color = FphlRed
                )
            }

            errorMessage != null -> {

                Text(
                    text =
                        "Unable to load schedule:\n$errorMessage",
                    color = FphlRed,
                    fontSize = 14.sp
                )
            }

            games.isEmpty() -> {

                Text(
                    text = "No upcoming games found.",
                    color = FphlMutedText
                )
            }

            else -> {

                ScheduleList(
                    games = games,
                    scoreboardGames = scoreboardGames,
                    favoriteTeams = favoriteTeams,
                    onTeamSelected = onTeamSelected,
                    onGameSelected = onGameSelected
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun ScheduleList(
    games: List<ScheduledGame>,
    scoreboardGames: List<ScoreboardGame>,
    favoriteTeams: Set<String>,
    onTeamSelected: (String) -> Unit,
    onGameSelected: (ScoreboardGame) -> Unit
) {

    val displayFormatter =
        DateTimeFormatter.ofPattern(
            "EEE, MMM d",
            Locale.US
        )

    val groupedGames =
        games.groupBy {
            it.date
        }

    groupedGames.forEach { (date, gamesForDate) ->

        val sortedGamesForDate =
            gamesForDate.filter { game ->

                val awayTeam =
                    findTeamByScheduleName(
                        game.awayTeam
                    )

                val homeTeam =
                    findTeamByScheduleName(
                        game.homeTeam
                    )

                awayTeam?.abbreviation in favoriteTeams ||
                        homeTeam?.abbreviation in favoriteTeams
            } +
                    gamesForDate.filterNot { game ->

                        val awayTeam =
                            findTeamByScheduleName(
                                game.awayTeam
                            )

                        val homeTeam =
                            findTeamByScheduleName(
                                game.homeTeam
                            )

                        awayTeam?.abbreviation in favoriteTeams ||
                                homeTeam?.abbreviation in favoriteTeams
                    }

        Text(
            text = date
                .format(displayFormatter)
                .uppercase(),
            color = FphlMutedText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        sortedGamesForDate.forEach { game ->

            val scheduledGame =
                scheduledGameToScoreboardGame(
                    game
                )

            val liveOrFinalGame =
                scheduledGame
                    ?.let { scheduled ->

                        scoreboardGames.firstOrNull { score ->

                            score.date ==
                                    scheduled.date &&
                                    score.awayAbbreviation ==
                                    scheduled.awayAbbreviation &&
                                    score.homeAbbreviation ==
                                    scheduled.homeAbbreviation
                        }
                    }

            val displayedGame =
                liveOrFinalGame
                    ?.copy(
                        watchUrl =
                            game.watchUrl
                                ?: liveOrFinalGame.watchUrl
                    )
                    ?: scheduledGame

            val isFavoriteGame =
                scheduledGame?.let { scheduled ->

                    scheduled.awayAbbreviation in
                            favoriteTeams ||
                            scheduled.homeAbbreviation in
                            favoriteTeams
                } ?: false

            ScheduleGameCard(
                game = game,
                scoreboardGame =
                    liveOrFinalGame,
                isFavorite =
                    isFavoriteGame,
                onTeamSelected =
                    onTeamSelected,
                onClick = {
                    displayedGame?.let {
                        onGameSelected(it)
                    }
                }
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )
    }
}

private fun upcomingGames(
    schedule: List<ScheduledGame>
): List<ScheduledGame> {

    return schedule
        .filter {
            !it.date.isBefore(
                LocalDate.now()
            )
        }
        .sortedWith(
            compareBy<ScheduledGame> {
                it.date
            }.thenBy {
                it.sourceStartDateTime
                    ?.toInstant()
                    ?.toEpochMilli()
                    ?: Long.MAX_VALUE
            }.thenBy {
                it.gameNumber
            }
        )
        .take(40)
}

@Composable
fun ScheduleGameCard(
    game: ScheduledGame,
    scoreboardGame: ScoreboardGame?,
    isFavorite: Boolean,
    onTeamSelected: (String) -> Unit,
    onClick: () -> Unit
) {

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

            val isLive =
                scoreboardGame?.awayScore != null &&
                        scoreboardGame.homeScore != null &&
                        !scoreboardGame.status.contains(
                            "FINAL",
                            ignoreCase = true
                        )

            val scheduledStatus =
                scheduledStatusLabel(
                    game
                )

            val displayIsLive =
                isLive ||
                        scheduledStatus == "LIVE"

            val statusLabel =
                liveDisplayLabel(
                    rawLabel =
                        scoreboardGame
                            ?.localStatusLabel()
                            ?: scheduledStatus,
                    isLive =
                        displayIsLive
                )

            Text(
                text =
                    statusLabel,
                color =
                    if (displayIsLive) {
                        FphlRed
                    } else {
                        FphlMutedText
                    },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            ScheduleTeamRow(
                teamName =
                    game.awayTeam,
                score =
                    scoreboardGame
                        ?.awayScore,

                onClick = {
                    findTeamByScheduleName(
                        game.awayTeam
                    )?.let { team ->

                        onTeamSelected(
                            team.abbreviation
                        )
                    }
                }
            )

            Text(
                text = "at",
                color = FphlMutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 60.dp,
                    top = 4.dp,
                    bottom = 4.dp
                )
            )

            ScheduleTeamRow(
                teamName =
                    game.homeTeam,
                score =
                    scoreboardGame
                        ?.homeScore,

                onClick = {
                    findTeamByScheduleName(
                        game.homeTeam
                    )?.let { team ->

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
fun ScheduleTeamRow(
    teamName: String,
    score: Int?,
    onClick: () -> Unit
) {

    val team =
        findTeamByScheduleName(
            teamName
        )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 4.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        if (team != null) {

            AsyncImage(
                model = team.logoUrl,
                contentDescription =
                    "$teamName logo",
                contentScale =
                    ContentScale.Fit,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clickable(
                            enabled = team != null
                        ) {
                            onClick()
                        }
            )

            Spacer(
                modifier =
                    Modifier.size(12.dp)
            )
        }

        Text(
            text =
                teamName,
            modifier =
                Modifier.weight(1f),
            color =
                FphlText,
            fontSize =
                16.sp,
            fontWeight =
                FontWeight.Medium
        )

        score?.let {

            Text(
                text =
                    it.toString(),
                color =
                    FphlText,
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
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

private fun scheduledGameToScoreboardGame(
    game: ScheduledGame
): ScoreboardGame? {

    val awayTeam =
        findTeamByScheduleName(
            game.awayTeam
        )
            ?: return null

    val homeTeam =
        findTeamByScheduleName(
            game.homeTeam
        )
            ?: return null

    return ScoreboardGame(
        date =
            game.date,

        awayAbbreviation =
            awayTeam.abbreviation,

        homeAbbreviation =
            homeTeam.abbreviation,

        awayScore =
            null,

        homeScore =
            null,

        status =
            scheduledStatusLabel(
                game
            ),

        watchUrl =
            game.watchUrl
    )
}
private fun findTeamByScheduleName(
    teamName: String
): Team? {

    val normalizedName =
        teamName
            .trim()
            .lowercase()

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