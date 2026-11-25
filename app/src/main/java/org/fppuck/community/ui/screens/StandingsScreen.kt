package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.fppuck.community.data.FavoriteTeamsStore
import org.fppuck.community.data.LeagueStanding
import org.fppuck.community.data.StandingsRepository
import org.fppuck.community.data.sampleTeams
import androidx.compose.ui.platform.LocalContext




@Composable
fun StandingsScreen(
    modifier: Modifier = Modifier,
    onScreenSelected: (AppScreen) -> Unit,
    onTeamSelected: (String) -> Unit
) {
    val context = LocalContext.current

    val favoriteTeams =
        remember {
            FavoriteTeamsStore.load(
                context
            )
        }

    var standings by remember {
        mutableStateOf<List<LeagueStanding>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        try {

            standings =
                StandingsRepository.fetchStandings(context)

        } catch (exception: Exception) {

            errorMessage =
                exception.message
                    ?: "Unknown standings error"

        } finally {

            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        LeagueTabs(
            currentScreen = AppScreen.STANDINGS,
            onScreenSelected = onScreenSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "STANDINGS",
            color = FphlText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {

            isLoading -> {

                CircularProgressIndicator(
                    color = FphlRed
                )
            }

            errorMessage != null -> {

                Text(
                    text = "Unable to load standings:\n$errorMessage",
                    color = FphlRed
                )
            }

            standings.isEmpty() -> {

                Text(
                    text = "No standings found.",
                    color = FphlMutedText
                )
            }

            else -> {

                StandingsContent(
                    standings = standings,
                    favoriteTeams = favoriteTeams,
                    onTeamSelected = onTeamSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StandingsContent(
    standings: List<LeagueStanding>,
    favoriteTeams: Set<String>,
    onTeamSelected: (String) -> Unit
) {

    val divisions =
        listOf(
            "Atlantic",
            "Great Lakes",
            "Delta",
            "Frontier"
        )

    divisions.forEach { division ->

        val divisionTeams =
            standings.filter {
                it.division == division
            }

        if (divisionTeams.isNotEmpty()) {

            val conference =
                divisionTeams.first().conference

            Text(
                text = conference.uppercase(),
                color = FphlRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$division Division",
                color = FphlText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            DivisionStandingsCard(
                standings = divisionTeams,
                favoriteTeams = favoriteTeams,
                onTeamSelected = onTeamSelected
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
fun DivisionStandingsCard(
    standings: List<LeagueStanding>,
    favoriteTeams: Set<String>,
    onTeamSelected: (String) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FphlCard
        )
    ) {

        Column {

            StandingsHeader()

            standings.forEach { team ->

                StandingRow(
                    team = team,
                    favoriteTeams = favoriteTeams,
                    onTeamSelected = onTeamSelected
                )
            }
        }
    }
}

@Composable
fun StandingsHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "TEAM",
            modifier = Modifier.weight(5f),
            color = FphlMutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        StatHeader("GP")
        StatHeader("W")
        StatHeader("L")
        StatHeader("OT")
        StatHeader("PTS")
    }
}

@Composable
fun StandingRow(
    team: LeagueStanding,
    favoriteTeams: Set<String>,
    onTeamSelected: (String) -> Unit
) {

    val teamInfo =
        sampleTeams.firstOrNull {
            "${it.city} ${it.name}" == team.teamName
        }

    val isFavorite =
        teamInfo?.abbreviation in
                favoriteTeams

    val totalWins =
        team.wins

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFavorite) {
                    FphlRed.copy(
                        alpha = 0.16f
                    )
                } else {
                    Color.Transparent
                }
            )
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            modifier = Modifier.weight(5f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (teamInfo != null) {

                AsyncImage(
                    model =
                        teamInfo.logoUrl,
                    contentDescription =
                        "${team.teamName} logo",
                    contentScale =
                        ContentScale.Fit,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clickable {
                                onTeamSelected(
                                    teamInfo.abbreviation
                                )
                            }
                )

                Spacer(
                    modifier =
                        Modifier.size(8.dp)
                )
            }

            Text(
                text =
                    "${team.rank}. ${team.teamName}",
                color =
                    FphlText,
                fontSize =
                    11.sp,
                fontWeight =
                    if (isFavorite) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
            )
        }

        StandingStat(team.gamesPlayed)
        StandingStat(totalWins)
        StandingStat(team.losses)
        StandingStat(team.overtimeLosses)
        StandingStat(team.points)
    }
}

@Composable
fun RowScope.StatHeader(
    text: String
) {

    Text(
        text = text,
        modifier = Modifier.weight(1f),
        color = FphlMutedText,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RowScope.StandingStat(
    value: Int
) {

    Text(
        text = value.toString(),
        modifier = Modifier.weight(1f),
        color = FphlText,
        fontSize = 11.sp
    )
}