package org.fppuck.community.ui

import org.fppuck.community.ui.theme.FphlText
import org.fppuck.community.ui.theme.FphlBackground
import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fppuck.community.data.ScoreboardGame
import org.fppuck.community.data.sampleTeams
import org.fppuck.community.ui.screens.AppScreen
import org.fppuck.community.ui.screens.GameDetailsScreen
import org.fppuck.community.ui.screens.HomeScreen
import org.fppuck.community.ui.screens.FppuckSidebar
import org.fppuck.community.ui.screens.FppuckSidebarPage
import org.fppuck.community.ui.screens.FppuckMenuBar
import org.fppuck.community.ui.theme.FppuckAppearance
import org.fppuck.community.ui.screens.PlayerDetailsScreen
import org.fppuck.community.ui.screens.ScheduleScreen
import org.fppuck.community.ui.screens.StandingsScreen
import org.fppuck.community.ui.screens.TeamDetailsScreen
import org.fppuck.community.ui.screens.TeamsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fppuck.community.data.ScoresRepository


private data class NavigationSnapshot(
    val screenName: String,
    val activeSectionName: String,
    val selectedTeamAbbreviation: String?,
    val selectedTeamTabName: String,
    val selectedPlayerName: String?,
    val selectedPlayerTeamAbbreviation: String?,
    val selectedGame: ScoreboardGame?
)

@Composable
fun FphlApp(appearance: FppuckAppearance, onAppearanceChange: (FppuckAppearance) -> Unit) {

    val drawer = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    /*
     * The actual Home screen is composed immediately underneath
     * this overlay, so its loading starts while the title screen
     * is visible.
     */
    var showStartup by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        delay(
            1_400
        )

        showStartup =
            false
    }

    var currentScreenName by rememberSaveable {
        mutableStateOf(AppScreen.HOME.name)
    }

    var activeSectionName by rememberSaveable {
        mutableStateOf(AppScreen.HOME.name)
    }

    var selectedTeamAbbreviation by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedTeamTabName by rememberSaveable {
        mutableStateOf("OVERVIEW")
    }

    var selectedPlayerName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedPlayerTeamAbbreviation by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedGame by remember {
        mutableStateOf<ScoreboardGame?>(null)
    }

    val backStack =
        remember {
            mutableStateListOf<NavigationSnapshot>()
        }

    val currentScreen =
        AppScreen.entries.firstOrNull {
            it.name == currentScreenName
        } ?: AppScreen.HOME

    val activeSection =
        AppScreen.entries.firstOrNull {
            it.name == activeSectionName
        } ?: AppScreen.HOME

    val selectedTeam =
        sampleTeams.firstOrNull {
            it.abbreviation ==
                    selectedTeamAbbreviation
        }

    fun currentSnapshot(): NavigationSnapshot =
        NavigationSnapshot(
            screenName =
                currentScreenName,

            activeSectionName =
                activeSectionName,

            selectedTeamAbbreviation =
                selectedTeamAbbreviation,

            selectedTeamTabName =
                selectedTeamTabName,

            selectedPlayerName =
                selectedPlayerName,

            selectedPlayerTeamAbbreviation =
                selectedPlayerTeamAbbreviation,

            selectedGame =
                selectedGame
        )

    fun pushCurrentState() {
        backStack.add(
            currentSnapshot()
        )
    }

    fun goBack() {

        if (backStack.isEmpty()) {
            return
        }

        val previous =
            backStack.removeAt(
                backStack.lastIndex
            )

        currentScreenName =
            previous.screenName

        activeSectionName =
            previous.activeSectionName

        selectedTeamAbbreviation =
            previous.selectedTeamAbbreviation

        selectedTeamTabName =
            previous.selectedTeamTabName

        selectedPlayerName =
            previous.selectedPlayerName

        selectedPlayerTeamAbbreviation =
            previous.selectedPlayerTeamAbbreviation

        selectedGame =
            previous.selectedGame
    }

    fun openTopLevel(
        screen: AppScreen
    ) {

        if (currentScreen == screen) {
            return
        }

        pushCurrentState()

        currentScreenName =
            screen.name

        activeSectionName =
            screen.name
    }

    fun openTeam(
        abbreviation: String
    ) {

        pushCurrentState()

        selectedTeamAbbreviation =
            abbreviation

        selectedTeamTabName =
            "OVERVIEW"

        currentScreenName =
            AppScreen.TEAM_DETAILS.name
    }

    fun openGame(
        game: ScoreboardGame
    ) {

        pushCurrentState()

        selectedGame =
            game

        currentScreenName =
            AppScreen.GAME_DETAILS.name
    }

    fun openPlayer(
        playerName: String,
        teamAbbreviation: String
    ) {

        pushCurrentState()

        selectedPlayerName =
            playerName

        selectedPlayerTeamAbbreviation =
            teamAbbreviation

        currentScreenName =
            AppScreen.PLAYER_DETAILS.name
    }

    fun backLabel(): String {

        val previous =
            backStack.lastOrNull()
                ?: return "Back"

        val previousScreen =
            runCatching {
                AppScreen.valueOf(
                    previous.screenName
                )
            }.getOrNull()
                ?: return "Back"

        return when (previousScreen) {

            AppScreen.HOME ->
                "Scores"

            AppScreen.SCHEDULE ->
                "Schedule"

            AppScreen.TEAMS ->
                "Teams"

            AppScreen.STANDINGS ->
                "Standings"

            AppScreen.MORE ->
                "Glossary"

            AppScreen.TEAM_DETAILS -> {

                sampleTeams
                    .firstOrNull {
                        it.abbreviation ==
                                previous.selectedTeamAbbreviation
                    }
                    ?.name
                    ?: "Team"
            }

            AppScreen.PLAYER_DETAILS ->
                previous.selectedPlayerName
                    ?: "Player"

            AppScreen.GAME_DETAILS ->
                "Game"

            else -> previousScreen.sidebarLabel ?: "Back"
        }
    }
    BackHandler(
        enabled =
            backStack.isNotEmpty() && !drawer.isOpen
    ) {
        goBack()
    }
    BackHandler(enabled = drawer.isOpen) {
        drawerScope.launch { drawer.close() }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = !showStartup,
            drawerContent = {
                FppuckSidebar(currentScreen) { destination ->
                    if (destination != currentScreen) {
                        pushCurrentState()
                        currentScreenName = destination.name
                    }
                    drawerScope.launch { drawer.close() }
                }
            }
        ) {
        Scaffold(
            containerColor =
                FphlBackground,

            topBar = {
                FppuckMenuBar(
                    title = currentScreen.sidebarLabel ?: "FPPuck",
                    onOpen = { drawerScope.launch { drawer.open() } },
                    onBack = if (currentScreen.sidebarLabel != null && backStack.isNotEmpty()) ::goBack else null
                )
            },
            bottomBar = {

                if (currentScreen.sidebarLabel == null) BottomNavigation(
                    selectedScreen =
                        activeSection,

                    onScreenSelected = {
                        openTopLevel(it)
                    }
                )
            }
        ) { innerPadding ->

            when (currentScreen) {

                AppScreen.HOME -> {

                    HomeScreen(
                        modifier =
                            Modifier.padding(
                                innerPadding
                            ),

                        onScreenSelected = {
                            openTopLevel(it)
                        },

                        onTeamSelected = { abbreviation ->

                            openTeam(
                                abbreviation
                            )
                        },

                        onGameSelected = { game ->

                            openGame(
                                game
                            )
                        }
                    )
                }

                AppScreen.SCHEDULE -> {

                    ScheduleScreen(
                        modifier =
                            Modifier.padding(
                                innerPadding
                            ),

                        onScreenSelected = {
                            openTopLevel(it)
                        },

                        onTeamSelected = { abbreviation ->

                            openTeam(
                                abbreviation
                            )
                        },

                        onGameSelected = { game ->

                            openGame(
                                game
                            )
                        }
                    )
                }

                AppScreen.TEAMS -> {

                    TeamsScreen(
                        modifier =
                            Modifier.padding(
                                innerPadding
                            ),

                        onTeamSelected = { team ->

                            openTeam(
                                team.abbreviation
                            )
                        }
                    )
                }

                AppScreen.TEAM_DETAILS -> {

                    selectedTeam?.let { team ->

                        TeamDetailsScreen(
                            team =
                                team,

                            modifier =
                                Modifier.padding(
                                    innerPadding
                                ),

                            backLabel =
                                backLabel(),

                            selectedTabName =
                                selectedTeamTabName,

                            onSelectedTabNameChange = {
                                selectedTeamTabName =
                                    it
                            },

                            onBack = {
                                goBack()
                            },

                            onGameSelected = { game ->

                                openGame(
                                    game
                                )
                            },

                            onPlayerSelected = { player ->

                                openPlayer(
                                    playerName =
                                        player.name,

                                    teamAbbreviation =
                                        team.abbreviation
                                )
                            }
                        )
                    }
                }

                AppScreen.PLAYER_DETAILS -> {

                    val playerName =
                        selectedPlayerName

                    val teamAbbreviation =
                        selectedPlayerTeamAbbreviation

                    if (
                        playerName != null &&
                        teamAbbreviation != null
                    ) {

                        PlayerDetailsScreen(
                            playerName =
                                playerName,

                            teamAbbreviation =
                                teamAbbreviation,

                            modifier =
                                Modifier.padding(
                                    innerPadding
                                ),

                            onBack = {
                                goBack()
                            }
                        )
                    }
                }

                AppScreen.GAME_DETAILS -> {

                    selectedGame?.let { game ->

                        GameDetailsScreen(
                            game =
                                game,

                            modifier =
                                Modifier.padding(
                                    innerPadding
                                ),

                            backLabel =
                                backLabel(),

                            onTeamSelected = { abbreviation ->

                                openTeam(
                                    abbreviation
                                )
                            },

                            onBack = {
                                goBack()
                            }
                        )
                    }
                }

                AppScreen.STANDINGS -> {

                    StandingsScreen(
                        modifier =
                            Modifier.padding(
                                innerPadding
                            ),

                        onScreenSelected = {
                            openTopLevel(it)
                        },

                        onTeamSelected = { abbreviation ->

                            openTeam(
                                abbreviation
                            )
                        }
                    )
                }

                else -> FppuckSidebarPage(
                    destination = currentScreen,
                    modifier = Modifier.padding(innerPadding),
                    appearance = appearance,
                    onAppearanceChange = onAppearanceChange,
                    onBrowseTeams = { openTopLevel(AppScreen.TEAMS) }
                )
            }
        }
        }

        AnimatedVisibility(
            visible =
                showStartup,
            enter =
                EnterTransition.None,
            exit =
                fadeOut()
        ) {
            StartupOverlay()
        }
    }
}

@Composable
private fun StartupOverlay() {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    FphlBackground
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            112.dp
                        )
                        .background(
                            color =
                                FphlCard,
                            shape =
                                RoundedCornerShape(
                                    28.dp
                                )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "FP",
                    color =
                        FphlText,
                    fontSize =
                        48.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .padding(
                                bottom =
                                    18.dp
                            )
                            .width(
                                54.dp
                            )
                            .height(
                                5.dp
                            )
                            .background(
                                color =
                                    FphlRed,
                                shape =
                                    RoundedCornerShape(
                                        50.dp
                                    )
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            Text(
                text =
                    "FPPuck",
                color =
                    FphlText,
                fontSize =
                    36.sp,
                fontWeight =
                    FontWeight.ExtraBold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "THE UNOFFICIAL APP FOR FPHL HOCKEY",
                color =
                    FphlMutedText,
                fontSize =
                    12.sp,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )

            CircularProgressIndicator(
                modifier =
                    Modifier.size(
                        24.dp
                    ),
                color =
                    FphlRed,
                strokeWidth =
                    2.dp
            )
        }
    }
}

@Composable
fun BottomNavigation(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    FphlCard
                )
                .navigationBarsPadding()
                .padding(
                    vertical = 14.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {

        BottomNavItem(
            title =
                "Home",

            screen =
                AppScreen.HOME,

            currentScreen =
                selectedScreen,

            onScreenSelected =
                onScreenSelected
        )

        BottomNavItem(
            title =
                "Schedule",

            screen =
                AppScreen.SCHEDULE,

            currentScreen =
                selectedScreen,

            onScreenSelected =
                onScreenSelected
        )

        BottomNavItem(
            title =
                "Teams",

            screen =
                AppScreen.TEAMS,

            currentScreen =
                selectedScreen,

            onScreenSelected =
                onScreenSelected
        )

        BottomNavItem(
            title =
                "Standings",

            screen =
                AppScreen.STANDINGS,

            currentScreen =
                selectedScreen,

            onScreenSelected =
                onScreenSelected
        )

    }
}

@Composable
fun BottomNavItem(
    title: String,
    screen: AppScreen,
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {

    val selected =
        screen == currentScreen

    Text(
        text =
            title,

        color =
            if (selected) {
                FphlRed
            } else {
                FphlMutedText
            },

        fontSize =
            12.sp,

        fontWeight =
            if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },

        modifier =
            Modifier
                .clickable {
                    onScreenSelected(
                        screen
                    )
                }
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                )
    )
}
