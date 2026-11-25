package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlCard
import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fppuck.community.data.FavoriteTeamsStore
import org.fppuck.community.data.Team
import org.fppuck.community.data.sampleTeams
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage


@Composable
fun TeamsScreen(
    modifier: Modifier = Modifier,
    onTeamSelected: (Team) -> Unit
) {

    val context =
        LocalContext.current

    var favoriteTeams by remember {
        mutableStateOf(
            FavoriteTeamsStore.load(context)
        )
    }
    val sortedTeams =
        sampleTeams.filter { team ->
            team.abbreviation in favoriteTeams
        } +
                sampleTeams.filterNot { team ->
                    team.abbreviation in favoriteTeams
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

        Text(
            text = "TEAMS",
            color = FphlText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        sortedTeams.forEachIndexed { index, team ->

            TeamCard(
                team = team,
                isFavorite =
                    team.abbreviation in favoriteTeams,
                canFavorite = true,
                onFavoriteClick = {

                    favoriteTeams =
                        FavoriteTeamsStore.toggle(
                            context = context,
                            currentFavorites = favoriteTeams,
                            abbreviation = team.abbreviation
                        )
                },
                onClick = {
                    onTeamSelected(team)
                }
            )

            if (index < sortedTeams.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TeamCard(
    team: Team,
    isFavorite: Boolean,
    canFavorite: Boolean = true,
    onFavoriteClick: () -> Unit,
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
            containerColor = FphlCard
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = team.logoUrl,
                contentDescription = "${team.city} ${team.name} logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = team.city,
                    color = FphlMutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = team.name,
                    color = FphlText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .clickable(enabled = canFavorite) {
                            onFavoriteClick()
                        }
                        .padding(8.dp)
            )
        }
    }
}