package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
import java.time.LocalDate
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import org.fppuck.community.data.FavoriteTeamsStore
import org.fppuck.community.data.LeagueStanding
import org.fppuck.community.data.StandingsRepository
import org.fppuck.community.data.GoalieRepository
import org.fppuck.community.data.TeamGoalieStat
import org.fppuck.community.data.ScoreboardGame
import org.fppuck.community.data.ScoresRepository
import org.fppuck.community.data.sampleTeams
import org.fppuck.community.data.TeamOverviewStats
import org.fppuck.community.data.TeamDetailsCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.fppuck.community.data.ScheduleRepository
import org.fppuck.community.data.ScheduledGame
import org.fppuck.community.data.Team
import org.fppuck.community.data.TeamPlayer
import org.fppuck.community.data.TeamPlayerStat
import org.fppuck.community.data.TeamRepository
import java.time.format.DateTimeFormatter
import java.util.Locale


private enum class TeamDetailTab {
    OVERVIEW,
    ROSTER,
    SCHEDULE,
    STATS
}

@Composable
fun TeamDetailsScreen(
    team: Team,
    modifier: Modifier = Modifier,
    backLabel: String,
    selectedTabName: String,
    onSelectedTabNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onGameSelected: (ScoreboardGame) -> Unit,
    onPlayerSelected: (TeamPlayer) -> Unit
) {

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var favoriteTeams by remember(
        team.abbreviation
    ) {
        mutableStateOf(
            FavoriteTeamsStore.load(
                context
            )
        )
    }
    val isFavorite =
        team.abbreviation in favoriteTeams
    val selectedTab =
        runCatching {
            TeamDetailTab.valueOf(selectedTabName)
        }.getOrDefault(
            TeamDetailTab.OVERVIEW
        )

    /*
     * Restore any recently fetched team data immediately.
     * This survives leaving Team Details and coming back.
     */
    val cachedTeamDetails =
        remember(
            team.abbreviation
        ) {
            TeamDetailsCache.get(
                team.abbreviation
            )
        }

    // -----------------------------
    // OVERVIEW STATE
    // -----------------------------

    var overviewStats by remember(team.abbreviation) {
        mutableStateOf(
            cachedTeamDetails
                ?.overviewStats
        )
    }

    var teamStanding by remember(team.abbreviation) {
        mutableStateOf(
            cachedTeamDetails
                ?.standing
        )
    }

    var overviewLoading by remember(team.abbreviation) {
        mutableStateOf(false)
    }

    var overviewError by remember(team.abbreviation) {
        mutableStateOf<String?>(null)
    }

    // -----------------------------
    // ROSTER STATE
    // -----------------------------

    var roster by remember(team.abbreviation) {
        mutableStateOf(
            cachedTeamDetails
                ?.roster
                ?: emptyList()
        )
    }

    var rosterLoading by remember(team.abbreviation) {
        mutableStateOf(false)
    }

    var rosterError by remember(team.abbreviation) {
        mutableStateOf<String?>(null)
    }

    // -----------------------------
    // PLAYER / GOALIE STATS STATE
    // -----------------------------

    var playerStats by remember(team.abbreviation) {
        mutableStateOf(
            cachedTeamDetails
                ?.playerStats
                ?: emptyList()
        )
    }

    var goalieStats by remember(team.abbreviation) {
        mutableStateOf(
            cachedTeamDetails
                ?.goalieStats
                ?: emptyList()
        )
    }

    var statsLoading by remember(team.abbreviation) {
        mutableStateOf(false)
    }

    var statsError by remember(team.abbreviation) {
        mutableStateOf<String?>(null)
    }

    // -----------------------------
    // SCHEDULE STATE
    // -----------------------------

    var teamSchedule by remember(team.abbreviation) {
        mutableStateOf(
            cachedTeamDetails
                ?.schedule
                ?: emptyList()
        )
    }

    var teamScoreboard by remember(team.abbreviation) {
        mutableStateOf<List<ScoreboardGame>>(emptyList())
    }

    var scheduleLoading by remember(team.abbreviation) {
        mutableStateOf(false)
    }

    var scheduleError by remember(team.abbreviation) {
        mutableStateOf<String?>(null)
    }

    // -----------------------------
    // LOAD OVERVIEW
    // -----------------------------

    LaunchedEffect(
        selectedTab,
        team.abbreviation
    ) {

        if (
            selectedTab == TeamDetailTab.OVERVIEW &&
            overviewStats == null
        ) {

            overviewLoading = true

            try {

                /*
                 * These are independent network/WebView requests.
                 * Start both at the same time instead of waiting
                 * for one to finish before starting the other.
                 */
                val overviewDeferred =
                    async {
                        TeamRepository.fetchTeamOverviewStats(
                            context = context,
                            teamAbbreviation =
                                team.abbreviation
                        )
                    }

                val standingsDeferred =
                    async {
                        StandingsRepository.fetchStandings(
                            context = context
                        )
                    }

                overviewStats =
                    overviewDeferred.await()

                val standings =
                    standingsDeferred.await()

                teamStanding =
                    standings.firstOrNull { standing ->

                        normalizeTeamName(
                            standing.teamName
                        ) ==
                                normalizeTeamName(
                                    "${team.city} ${team.name}"
                                )
                    }

                overviewStats
                    ?.let { stats ->

                        TeamDetailsCache.updateOverview(
                            teamAbbreviation =
                                team.abbreviation,
                            overviewStats =
                                stats,
                            standing =
                                teamStanding
                        )
                    }

                overviewError = null

            } catch (exception: Exception) {

                overviewError =
                    exception.message
                        ?: "Unable to load team overview"

            } finally {

                overviewLoading = false
            }
        }
    }

    // -----------------------------
    // LOAD ROSTER
    // -----------------------------

    LaunchedEffect(
        selectedTab,
        team.abbreviation
    ) {

        if (
            selectedTab == TeamDetailTab.ROSTER &&
            roster.isEmpty() &&
            !rosterLoading
        ) {

            rosterLoading =
                true

            try {

                roster =
                    TeamRepository.fetchRoster(
                        context =
                            context,
                        teamAbbreviation =
                            team.abbreviation
                    )

                TeamDetailsCache.updateRoster(
                    teamAbbreviation =
                        team.abbreviation,
                    roster =
                        roster
                )

                rosterError =
                    null

            } catch (
                exception: Exception
            ) {

                rosterError =
                    exception.message
                        ?: "Unable to load roster"

            } finally {

                rosterLoading =
                    false
            }
        }
    }

    // -----------------------------
    // LOAD STATS
    // -----------------------------

    LaunchedEffect(
        selectedTab,
        team.abbreviation
    ) {

        if (
            selectedTab == TeamDetailTab.STATS &&
            playerStats.isEmpty() &&
            !statsLoading
        ) {

            statsLoading = true

            try {

                /*
                 * Skater and goalie stats come from separate
                 * lookups, so fetch them concurrently.
                 */
                val playerStatsDeferred =
                    async {
                        TeamRepository.fetchPlayerStats(
                            context = context,
                            teamAbbreviation =
                                team.abbreviation
                        )
                    }

                val goalieStatsDeferred =
                    async {
                        GoalieRepository.fetchGoalieStats(
                            context = context,
                            teamAbbreviation =
                                team.abbreviation
                        )
                    }

                playerStats =
                    playerStatsDeferred.await()

                goalieStats =
                    goalieStatsDeferred.await()

                TeamDetailsCache.updateStats(
                    teamAbbreviation =
                        team.abbreviation,
                    playerStats =
                        playerStats,
                    goalieStats =
                        goalieStats
                )

                statsError = null

            } catch (exception: Exception) {

                statsError =
                    exception.message
                        ?: "Unable to load player stats"

            } finally {

                statsLoading = false
            }
        }
    }

    // -----------------------------
    // LOAD SCHEDULE
    // -----------------------------

    LaunchedEffect(
        selectedTab,
        team.abbreviation
    ) {

        if (
            selectedTab == TeamDetailTab.SCHEDULE &&
            teamSchedule.isEmpty() &&
            !scheduleLoading
        ) {

            scheduleLoading = true

            try {

                /*
                 * Show the static schedule first so this tab
                 * does not wait on HockeyShift's WebView.
                 */
                val baseSchedule =
                    ScheduleRepository.fetchSchedule()

                teamSchedule =
                    filterAndSortTeamSchedule(
                        fullSchedule = baseSchedule,
                        team = team
                    )

                TeamDetailsCache.updateSchedule(
                    teamAbbreviation =
                        team.abbreviation,
                    schedule =
                        teamSchedule
                )

                scheduleError = null

            } catch (exception: Exception) {

                scheduleError =
                    exception.message
                        ?: "Unable to load team schedule"

            } finally {

                scheduleLoading = false
            }

            /*
             * Then enrich the visible schedule with local
             * start times and Watch Live links.
             */
            try {

                val enrichedSchedule =
                    ScheduleRepository.fetchSchedule(
                        context
                    )

                teamSchedule =
                    filterAndSortTeamSchedule(
                        fullSchedule =
                            enrichedSchedule,
                        team =
                            team
                    )

                TeamDetailsCache.updateSchedule(
                    teamAbbreviation =
                        team.abbreviation,
                    schedule =
                        teamSchedule
                )

            } catch (
                exception: Exception
            ) {
                /*
                 * Keep the already-visible base schedule.
                 */
            }
        }
    }

    // -----------------------------
    // PREFETCH TEAM TABS
    // -----------------------------

    /*
     * Once the initially visible Overview has finished loading,
     * quietly prepare the other tabs. This avoids competing
     * with the first screen paint, but makes later tab changes
     * much closer to instant.
     *
     * If Team Details was restored directly to another tab,
     * prefetching may begin immediately. The loading guards
     * above prevent duplicate requests.
     */
    LaunchedEffect(
        team.abbreviation,
        selectedTab,
        overviewStats,
        overviewError
    ) {

        if (
            selectedTab == TeamDetailTab.OVERVIEW &&
            overviewStats == null &&
            overviewError == null
        ) {
            return@LaunchedEffect
        }

        /*
         * The static team schedule is cheap, so prepare it
         * before the heavier roster/stat requests.
         */
        if (
            teamSchedule.isEmpty() &&
            !scheduleLoading
        ) {

            scheduleLoading =
                true

            try {

                teamSchedule =
                    filterAndSortTeamSchedule(
                        fullSchedule =
                            ScheduleRepository.fetchSchedule(),
                        team =
                            team
                    )

                TeamDetailsCache.updateSchedule(
                    teamAbbreviation =
                        team.abbreviation,
                    schedule =
                        teamSchedule
                )

                scheduleError =
                    null

            } catch (
                exception: Exception
            ) {

                scheduleError =
                    exception.message
                        ?: "Unable to load team schedule"

            } finally {

                scheduleLoading =
                    false
            }
        }

        /*
         * Roster first: it is commonly opened immediately
         * after Overview.
         */
        if (
            roster.isEmpty() &&
            !rosterLoading
        ) {

            rosterLoading =
                true

            try {

                roster =
                    TeamRepository.fetchRoster(
                        context =
                            context,
                        teamAbbreviation =
                            team.abbreviation
                    )

                TeamDetailsCache.updateRoster(
                    teamAbbreviation =
                        team.abbreviation,
                    roster =
                        roster
                )

                rosterError =
                    null

            } catch (
                exception: Exception
            ) {

                rosterError =
                    exception.message
                        ?: "Unable to load roster"

            } finally {

                rosterLoading =
                    false
            }
        }

        /*
         * Then prepare both stat tables in parallel.
         */
        if (
            playerStats.isEmpty() &&
            !statsLoading
        ) {

            statsLoading =
                true

            try {

                val playerStatsDeferred =
                    async {
                        TeamRepository.fetchPlayerStats(
                            context =
                                context,
                            teamAbbreviation =
                                team.abbreviation
                        )
                    }

                val goalieStatsDeferred =
                    async {
                        GoalieRepository.fetchGoalieStats(
                            context =
                                context,
                            teamAbbreviation =
                                team.abbreviation
                        )
                    }

                playerStats =
                    playerStatsDeferred.await()

                goalieStats =
                    goalieStatsDeferred.await()

                statsError =
                    null

            } catch (
                exception: Exception
            ) {

                statsError =
                    exception.message
                        ?: "Unable to load player stats"

            } finally {

                statsLoading =
                    false
            }
        }

        /*
         * Finally enrich the already-usable static schedule
         * with local times and Watch Live links. This is last
         * because it is not needed to make the tab appear.
         */
        if (teamSchedule.isNotEmpty()) {

            try {

                val enrichedSchedule =
                    ScheduleRepository.fetchSchedule(
                        context
                    )

                teamSchedule =
                    filterAndSortTeamSchedule(
                        fullSchedule =
                            enrichedSchedule,
                        team =
                            team
                    )

                TeamDetailsCache.updateSchedule(
                    teamAbbreviation =
                        team.abbreviation,
                    schedule =
                        teamSchedule
                )

            } catch (
                exception: Exception
            ) {
                /*
                 * Keep the static schedule already loaded.
                 */
            }
        }
    }

    // -----------------------------
    // LIVE SCORES ON TEAM SCHEDULE
    // -----------------------------

    LaunchedEffect(
        selectedTab,
        team.abbreviation
    ) {

        if (
            selectedTab != TeamDetailTab.SCHEDULE
        ) {
            return@LaunchedEffect
        }

        while (true) {

            try {

                teamScoreboard =
                    ScoresRepository
                        .fetchScoreboard(
                            context
                        )

            } catch (
                exception: Exception
            ) {
                /*
                 * Keep the most recent scoreboard data if a
                 * refresh fails.
                 */
            }

            delay(
                30_000
            )
        }
    }

// -----------------------------
// SCREEN
// -----------------------------
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    scrollState
                )
                .padding(
                    horizontal = 16.dp
                )
        ) {
            Spacer(
                modifier = Modifier.height(20.dp)
            )

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
                modifier = Modifier.height(12.dp)
            )

            AsyncImage(
                model = team.logoUrl,
                contentDescription =
                    "${team.city} ${team.name} logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = team.city,
                color = FphlMutedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = team.name,
                color = FphlText,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

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
                        team.abbreviation,
                    color =
                        FphlRed,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        if (isFavorite) {
                            "★"
                        } else {
                            "☆"
                        },
                    color =
                        if (isFavorite) {
                            FphlRed
                        } else {
                            FphlMutedText
                        },
                    fontSize =
                        30.sp,
                    fontWeight =
                        FontWeight.Bold,
                    modifier =
                        Modifier
                            .clickable(enabled = true) {

                                favoriteTeams =
                                    FavoriteTeamsStore.toggle(
                                        context =
                                            context,
                                        currentFavorites =
                                            favoriteTeams,
                                        abbreviation =
                                            team.abbreviation
                                    )
                            }
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TeamDetailTabs(
                selectedTab = selectedTab,
                onTabSelected = {
                    onSelectedTabNameChange(
                        it.name
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            when (selectedTab) {

                TeamDetailTab.OVERVIEW -> {

                    when {

                        overviewLoading -> {

                            CircularProgressIndicator(
                                color = FphlRed
                            )
                        }

                        overviewError != null -> {

                            Text(
                                text =
                                    "Unable to load team overview:\n$overviewError",
                                color = FphlRed
                            )
                        }

                        overviewStats != null -> {

                            TeamOverview(
                                team = team,
                                stats = overviewStats!!,
                                standing = teamStanding
                            )
                        }
                    }
                }

                TeamDetailTab.ROSTER -> {

                    when {

                        rosterLoading ||
                                (
                                        roster.isEmpty() &&
                                                rosterError == null
                                        ) -> {

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                CircularProgressIndicator(
                                    color = FphlRed
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(
                                    text = "Loading roster...",
                                    color = FphlMutedText,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        rosterError != null -> {

                            Text(
                                text =
                                    "Unable to load roster:\n$rosterError",
                                color = FphlRed
                            )
                        }

                        roster.isEmpty() -> {

                            Text(
                                text = "No roster data found.",
                                color = FphlMutedText
                            )
                        }

                        else -> {

                            TeamRoster(
                                players = roster,
                                onPlayerSelected =
                                    onPlayerSelected
                            )
                        }
                    }
                }
                TeamDetailTab.SCHEDULE -> {

                    when {

                        scheduleLoading -> {

                            CircularProgressIndicator(
                                color = FphlRed
                            )
                        }

                        scheduleError != null -> {

                            Text(
                                text =
                                    "Unable to load schedule:\n$scheduleError",
                                color = FphlRed
                            )
                        }

                        teamSchedule.isEmpty() -> {

                            Text(
                                text = "No games found.",
                                color = FphlMutedText
                            )
                        }

                        else -> {

                            TeamSchedule(
                                games = teamSchedule,
                                scores = teamScoreboard,
                                onGameSelected = onGameSelected
                            )
                        }
                    }
                }

                TeamDetailTab.STATS -> {

                    when {

                        statsLoading -> {

                            CircularProgressIndicator(
                                color = FphlRed
                            )
                        }

                        statsError != null -> {

                            Text(
                                text =
                                    "Unable to load stats:\n$statsError",
                                color = FphlRed
                            )
                        }

                        playerStats.isEmpty() -> {

                            Text(
                                text = "No player stats found.",
                                color = FphlMutedText
                            )
                        }

                        else -> {

                            TeamPlayerStats(
                                players = playerStats,
                                onPlayerSelected = onPlayerSelected
                            )

                            if (goalieStats.isNotEmpty()) {

                                Spacer(
                                    modifier =
                                        Modifier.height(24.dp)
                                )

                                TeamGoalieStats(
                                    goalies = goalieStats,
                                    onPlayerSelected = onPlayerSelected
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
        if (scrollState.value > 200) {

            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clickable {

                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    },

                shape = RoundedCornerShape(50.dp),

                colors = CardDefaults.cardColors(
                    containerColor = FphlRed
                )
            ) {

                Text(
                    text = "↑  TOP",
                    color = FphlText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 12.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun TeamDetailTabs(
    selectedTab: TeamDetailTab,
    onTabSelected: (TeamDetailTab) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            )
    ) {

        TeamDetailTabButton(
            title = "Overview",
            tab = TeamDetailTab.OVERVIEW,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        TeamDetailTabButton(
            title = "Roster",
            tab = TeamDetailTab.ROSTER,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        TeamDetailTabButton(
            title = "Schedule",
            tab = TeamDetailTab.SCHEDULE,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        TeamDetailTabButton(
            title = "Stats",
            tab = TeamDetailTab.STATS,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun RowScope.TeamDetailTabButton(
    title: String,
    tab: TeamDetailTab,
    selectedTab: TeamDetailTab,
    onTabSelected: (TeamDetailTab) -> Unit
) {

    val selected =
        tab == selectedTab

    Text(
        text = title,

        color =
            if (selected) {
                FphlRed
            } else {
                FphlMutedText
            },

        fontSize = 14.sp,

        fontWeight =
            if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },

        textAlign =
            TextAlign.Center,

        modifier = Modifier
            .weight(1f)
            .clickable {
                onTabSelected(tab)
            }
            .padding(
                vertical = 10.dp
            )
    )
}

@Composable
private fun TeamOverview(
    team: Team,
    stats: TeamOverviewStats,
    standing: LeagueStanding?
) {

    val wins =
        standing?.wins

    Column {

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
                    Modifier.padding(20.dp)
            ) {

                Text(
                    text = "TEAM OVERVIEW",
                    color = FphlMutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text =
                        "${team.city} ${team.name}",
                    color = FphlText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                standing?.let {

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "${it.division} Division • ${it.conference} Conference",
                        color = FphlMutedText,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                if (standing != null) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        OverviewStat(
                            label = "GP",
                            value =
                                standing.gamesPlayed
                                    .toString()
                        )

                        OverviewStat(
                            label = "W",
                            value =
                                wins.toString()
                        )

                        OverviewStat(
                            label = "L",
                            value =
                                standing.losses
                                    .toString()
                        )

                        OverviewStat(
                            label = "OTL",
                            value =
                                standing.overtimeLosses
                                    .toString()
                        )

                        OverviewStat(
                            label = "PTS",
                            value =
                                standing.points
                                    .toString()
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    OverviewStat(
                        label = "STREAK",
                        value = stats.streak
                    )

                    OverviewStat(
                        label = "GF AVG",
                        value =
                            stats.goalsForAverage
                    )

                    OverviewStat(
                        label = "GOAL +/-",
                        value =
                            stats.goalDifferential
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    OverviewStat(
                        label = "POWER PLAY",
                        value =
                            stats.powerPlayPercent
                    )

                    OverviewStat(
                        label = "PENALTY KILL",
                        value =
                            stats.penaltyKillPercent
                    )
                }
            }
        }
    }
}
@Composable
private fun TeamRoster(
    players: List<TeamPlayer>,
    onPlayerSelected: (TeamPlayer) -> Unit
) {

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
        ) {

            Text(
                text = "#",
                modifier = Modifier.weight(1f),
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "PLAYER",
                modifier = Modifier.weight(5f),
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "POS",
                modifier = Modifier.weight(1f),
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        players.forEach { player ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 4.dp
                    )
                    .clickable {
                        onPlayerSelected(player)
                    },

                shape = RoundedCornerShape(10.dp),

                colors = CardDefaults.cardColors(
                    containerColor = FphlCard
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = player.number ?: "—",
                        modifier = Modifier.weight(1f),
                        color = FphlRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            if (player.rookie) {
                                "${player.name}  R"
                            } else {
                                player.name
                            },

                        modifier = Modifier.weight(5f),
                        color = FphlText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = player.position,
                        modifier = Modifier.weight(1f),
                        color = FphlMutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
private fun TeamPlayerStats(
    players: List<TeamPlayerStat>,
    onPlayerSelected: (TeamPlayer) -> Unit
) {

    Column {

        Text(
            text = "PLAYER STATS",
            color = FphlMutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.padding(
                    bottom = 8.dp
                )
        )

        players.forEach { player ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 4.dp
                    )
                    .clickable {

                        onPlayerSelected(
                            TeamPlayer(
                                number = player.number,
                                name = player.name,
                                position = player.position,
                                rookie = player.rookie
                            )
                        )
                    },
            ) {

                Column(
                    modifier =
                        Modifier.padding(14.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                player.number
                                    ?.let {
                                        "#$it"
                                    }
                                    ?: "—",
                            color = FphlRed,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "  ${player.name}",
                            color = FphlText,
                            fontSize = 15.sp,
                            fontWeight =
                                FontWeight.Bold,
                            modifier =
                                Modifier.weight(1f)
                        )

                        Text(
                            text = player.position,
                            color = FphlMutedText,
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        StatValue(
                            label = "GP",
                            value =
                                player.gamesPlayed
                        )

                        StatValue(
                            label = "G",
                            value =
                                player.goals
                        )

                        StatValue(
                            label = "A",
                            value =
                                player.assists
                        )

                        StatValue(
                            label = "PTS",
                            value =
                                player.points
                        )

                        StatValue(
                            label = "PIM",
                            value =
                                player.penaltyMinutes
                        )

                        StatValue(
                            label = "+/-",
                            value =
                                player.plusMinus
                        )

                        StatValue(
                            label = "SOG",
                            value =
                                player.shotsOnGoal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatValue(
    label: String,
    value: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = label,
            color = FphlMutedText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(2.dp)
        )

        Text(
            text = value,
            color = FphlText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TeamSchedule(
    games: List<ScheduledGame>,
    scores: List<ScoreboardGame>,
    onGameSelected: (ScoreboardGame) -> Unit
) {

    val dateFormatter =
        DateTimeFormatter.ofPattern(
            "EEE, MMM d",
            Locale.US
        )

    games.forEachIndexed { index, game ->

        val scheduledGame =
            scheduledGameToScoreboardGame(
                game
            )

        val liveOrFinalGame =
            scheduledGame
                ?.let { scheduled ->

                    scores.firstOrNull { score ->

                        score.date ==
                                scheduled.date &&
                                score.awayAbbreviation ==
                                scheduled.awayAbbreviation &&
                                score.homeAbbreviation ==
                                scheduled.homeAbbreviation
                    }
                }

        val scoreboardGame =
            liveOrFinalGame
                ?.copy(
                    watchUrl =
                        game.watchUrl
                            ?: liveOrFinalGame.watchUrl
                )
                ?: scheduledGame

        val isLive =
            liveOrFinalGame != null &&
                    liveOrFinalGame.awayScore != null &&
                    liveOrFinalGame.homeScore != null &&
                    !liveOrFinalGame.status.contains(
                        "FINAL",
                        ignoreCase = true
                    )

        val isFinal =
            liveOrFinalGame
                ?.status
                ?.contains(
                    "FINAL",
                    ignoreCase = true
                ) == true ||
                    (
                            liveOrFinalGame == null &&
                                    game.date.isBefore(
                                        LocalDate.now()
                                    )
                            )

        val rawGameStatusText =
            when {

                liveOrFinalGame != null ->
                    liveOrFinalGame
                        .localStatusLabel()

                isFinal ->
                    "FINAL"

                else ->
                    scheduledStatusLabel(
                        game
                    )
            }

        val gameStatusText =
            liveDisplayLabel(
                rawLabel =
                    rawGameStatusText,
                isLive =
                    isLive ||
                            rawGameStatusText.equals(
                                "LIVE",
                                ignoreCase = true
                            )
            )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        scoreboardGame != null
                ) {

                    scoreboardGame?.let(
                        onGameSelected
                    )
                },
            shape =
                RoundedCornerShape(
                    12.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        FphlCard
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                Text(
                    text =
                        game.date
                            .format(
                                dateFormatter
                            )
                            .uppercase(),
                    color =
                        FphlMutedText,
                    fontSize =
                        12.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

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
                            game.awayTeam,
                        color =
                            FphlText,
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.Medium,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    liveOrFinalGame
                        ?.awayScore
                        ?.let { score ->

                            Text(
                                text =
                                    score.toString(),
                                color =
                                    FphlText,
                                fontSize =
                                    18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                }

                Text(
                    text = "at",
                    color =
                        FphlMutedText,
                    fontSize =
                        12.sp,
                    modifier =
                        Modifier.padding(
                            vertical = 4.dp
                        )
                )

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
                            game.homeTeam,
                        color =
                            FphlText,
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.Medium,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    liveOrFinalGame
                        ?.homeScore
                        ?.let { score ->

                            Text(
                                text =
                                    score.toString(),
                                color =
                                    FphlText,
                                fontSize =
                                    18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        gameStatusText,
                    color =
                        when {

                            isLive ->
                                FphlRed

                            isFinal ->
                                FphlMutedText

                            else ->
                                FphlRed
                        },
                    fontSize =
                        12.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        if (
            index <
            games.lastIndex
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )
        }
    }
}

private fun filterAndSortTeamSchedule(
    fullSchedule: List<ScheduledGame>,
    team: Team
): List<ScheduledGame> {

    return fullSchedule
        .filter { game ->

            gameBelongsToTeam(
                game = game,
                team = team
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
}

private fun gameBelongsToTeam(
    game: ScheduledGame,
    team: Team
): Boolean {

    val teamFullName =
        normalizeTeamName(
            "${team.city} ${team.name}"
        )

    val away =
        normalizeTeamName(
            game.awayTeam
        )

    val home =
        normalizeTeamName(
            game.homeTeam
        )

    return away == teamFullName ||
            home == teamFullName
}

private fun normalizeTeamName(
    name: String
): String {

    val normalized =
        name
            .trim()
            .lowercase()

    return when (normalized) {

        "oceanside hc" ->
            "oceanside shock"

        "baton rouge" ->
            "baton rouge kingfish"

        else ->
            normalized
    }
}
@Composable
private fun OverviewStat(
    label: String,
    value: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = label,
            color = FphlMutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text = value,
            color = FphlText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
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
        sampleTeams.firstOrNull { team ->

            normalizeTeamName(
                "${team.city} ${team.name}"
            ) ==
                    normalizeTeamName(
                        game.awayTeam
                    )
        }
            ?: return null

    val homeTeam =
        sampleTeams.firstOrNull { team ->

            normalizeTeamName(
                "${team.city} ${team.name}"
            ) ==
                    normalizeTeamName(
                        game.homeTeam
                    )
        }
            ?: return null

    return ScoreboardGame(
        date = game.date,
        awayAbbreviation =
            awayTeam.abbreviation,
        homeAbbreviation =
            homeTeam.abbreviation,
        awayScore = null,
        homeScore = null,
        status =
            scheduledStatusLabel(
                game
            ),
        watchUrl =
            game.watchUrl
    )
}
@Composable
private fun TeamGoalieStats(
    goalies: List<TeamGoalieStat>,
    onPlayerSelected: (TeamPlayer) -> Unit
) {

    Column {

        Text(
            text = "GOALIE STATS",
            color = FphlMutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.padding(
                    bottom = 8.dp
                )
        )

        goalies.forEach { goalie ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 4.dp
                    )
                    .clickable {

                        onPlayerSelected(
                            TeamPlayer(
                                number = goalie.number,
                                name = goalie.name,
                                position = "G",
                                rookie = goalie.rookie
                            )
                        )
                    },
            ) {

                Column(
                    modifier =
                        Modifier.padding(14.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                goalie.number
                                    ?.let {
                                        "#$it"
                                    }
                                    ?: "—",
                            color = FphlRed,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "  ${goalie.name}" +
                                        if (
                                            goalie.rookie
                                        ) {
                                            "  R"
                                        } else {
                                            ""
                                        },
                            modifier =
                                Modifier.weight(1f),
                            color = FphlText,
                            fontSize = 15.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text = "G",
                            color = FphlMutedText,
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        StatValue(
                            label = "GP",
                            value =
                                goalie.gamesPlayed
                        )

                        StatValue(
                            label = "W",
                            value =
                                goalie.wins
                        )

                        StatValue(
                            label = "L",
                            value =
                                goalie.losses
                        )

                        StatValue(
                            label = "GAA",
                            value =
                                goalie.goalsAgainstAverage
                        )

                        StatValue(
                            label = "SV%",
                            value =
                                goalie.savePercent
                        )

                        StatValue(
                            label = "SV",
                            value =
                                goalie.saves
                        )

                        StatValue(
                            label = "SO",
                            value =
                                goalie.shutouts
                        )
                    }
                }
            }
        }
    }
}