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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun MoreScreen(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Spacer(
            modifier = Modifier.height(6.dp)
        )
        Text(
            text = "Hockey Glossary",
            color = FphlRed,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        GlossarySection(
            title = "SKATER STATS",
            entries = listOf(
                "GP" to "Games Played",
                "G" to "Goals",
                "A" to "Assists",
                "PTS" to "Points",
                "PTS/GP" to "Points Per Game",
                "PIM" to "Penalty Minutes",
                "+/-" to "Plus / Minus",
                "SOG" to "Shots on Goal",
                "S%" to "Shooting Percentage",
                "PPG" to "Power Play Goals",
                "SHG" to "Short-Handed Goals",
                "GWG" to "Game-Winning Goals"
            )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        GlossarySection(
            title = "GOALIE STATS",
            entries = listOf(
                "GP" to "Games Played",
                "W" to "Wins",
                "L" to "Losses",
                "T" to "Ties",
                "OTL" to "Overtime Losses",
                "SA" to "Shots Against",
                "GA" to "Goals Against",
                "GAA" to "Goals Against Average",
                "SV" to "Saves",
                "SV%" to "Save Percentage",
                "SO" to "Shutouts",
                "MP" to "Minutes Played"
            )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        GlossarySection(
            title = "STANDINGS",
            entries = listOf(
                "GP" to "Games Played",
                "W" to "Wins",
                "L" to "Losses",
                "OTL" to "Overtime Losses",
                "PTS" to "Standings Points",
                "RW" to "Regulation Wins",
                "OTW" to "Overtime Wins",
                "SOW" to "Shootout Wins"
            )
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        GlossarySection(
            title = "TEAM STATS",
            entries = listOf(
                "GF AVG" to "Goals For Average",
                "GOAL +/-" to "Goal Differential",
                "PP%" to "Power Play Percentage",
                "PK%" to "Penalty Kill Percentage"
            )
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun GlossarySection(
    title: String,
    entries: List<Pair<String, String>>
) {

    Text(
        text = title,
        color = FphlMutedText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FphlCard
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            entries.forEachIndexed { index, entry ->

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = entry.first,
                        color = FphlRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f)
                    )

                    Text(
                        text = entry.second,
                        color = FphlText,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(3f)
                    )
                }

                if (index < entries.lastIndex) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }
            }
        }
    }
}
