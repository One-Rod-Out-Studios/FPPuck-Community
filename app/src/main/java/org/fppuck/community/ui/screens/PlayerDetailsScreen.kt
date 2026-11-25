package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fppuck.community.data.PlayerProfile
import org.fppuck.community.data.PlayerRepository
import org.fppuck.community.data.PlayerSeasonStat


@Composable
fun PlayerDetailsScreen(
    playerName: String,
    teamAbbreviation: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    var profile by remember(
        playerName,
        teamAbbreviation
    ) {
        mutableStateOf<PlayerProfile?>(null)
    }

    var loading by remember(
        playerName,
        teamAbbreviation
    ) {
        mutableStateOf(true)
    }

    var error by remember(
        playerName,
        teamAbbreviation
    ) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(
        playerName,
        teamAbbreviation
    ) {

        loading = true

        try {

            profile =
                PlayerRepository
                    .fetchPlayerProfile(
                        context = context,
                        teamAbbreviation =
                            teamAbbreviation,
                        playerName =
                            playerName
                    )

            error = null

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to load player."

        } finally {

            loading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Text(
            text = "←  Team",
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
                Modifier.height(16.dp)
        )

        when {

            loading -> {

                CircularProgressIndicator(
                    color = FphlRed
                )
            }

            error != null -> {

                Text(
                    text =
                        "Unable to load player:\n$error",
                    color = FphlRed
                )
            }

            profile != null -> {

                PlayerProfileContent(
                    profile = profile!!,
                    onTeamSelected = onBack
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(32.dp)
        )
    }
}

@Composable
private fun PlayerProfileContent(
    profile: PlayerProfile,
    onTeamSelected: () -> Unit
) {

    Text(
        text = profile.name,
        color = FphlText,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold
    )

    Spacer(
        modifier =
            Modifier.height(6.dp)
    )

    Text(
        text =
            buildString {

                profile.number
                    ?.let {
                        append("#$it  ")
                    }

                append(
                    profile.position
                )
            },
        color = FphlRed,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = profile.teamName,
        color = FphlMutedText,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable {
                onTeamSelected()
            }
            .padding(
                vertical = 6.dp
            )
    )

    FavoritePlayerButton(profile)

    profile.seasons
        .firstOrNull()
        ?.let { current ->

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text = "CURRENT SEASON",
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            PlayerStatCard(
                stats = current
            )
        }

    profile.careerTotals
        ?.let { totals ->

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text = "CAREER TOTALS",
                color = FphlMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            PlayerStatCard(
                stats = totals
            )
        }

    if (profile.seasons.isNotEmpty()) {

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = "SEASON HISTORY",
            color = FphlMutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        profile.seasons.forEach { season ->

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            PlayerStatCard(
                stats = season
            )
        }
    }
}

@Composable
private fun PlayerStatCard(
    stats: PlayerSeasonStat
) {

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

            Text(
                text = stats.season,
                color = FphlText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (
                stats.teamName.isNotBlank()
            ) {

                Text(
                    text =
                        buildString {

                            append(
                                stats.teamName
                            )

                            if (
                                stats.division
                                    .isNotBlank()
                            ) {

                                append(
                                    " • ${stats.division}"
                                )
                            }
                        },
                    color = FphlMutedText,
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                PlayerStatValue(
                    "GP",
                    stats.gamesPlayed
                )

                PlayerStatValue(
                    "G",
                    stats.goals
                )

                PlayerStatValue(
                    "A",
                    stats.assists
                )

                PlayerStatValue(
                    "PTS",
                    stats.points
                )

                PlayerStatValue(
                    "PIM",
                    stats.penaltyMinutes
                )

                PlayerStatValue(
                    "+/-",
                    stats.plusMinus
                )

                PlayerStatValue(
                    "SOG",
                    stats.shotsOnGoal
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                PlayerStatValue(
                    "PTS/GP",
                    stats.pointsPerGame
                )

                PlayerStatValue(
                    "S%",
                    stats.shootingPercent
                )

                PlayerStatValue(
                    "PPG",
                    stats.powerPlayGoals
                )

                PlayerStatValue(
                    "SHG",
                    stats.shortHandedGoals
                )

                PlayerStatValue(
                    "GWG",
                    stats.gameWinningGoals
                )
            }
        }
    }
}
@Composable
private fun PlayerStatValue(
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

        Text(
            text = value,
            color = FphlText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}