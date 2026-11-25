package org.fppuck.community.ui.screens

import org.fppuck.community.ui.theme.FphlMutedText
import org.fppuck.community.ui.theme.FphlRed
import org.fppuck.community.ui.theme.FphlText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun LeagueTabs(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        LeagueTab(
            title = "Scores",
            screen = AppScreen.HOME,
            currentScreen = currentScreen,
            onScreenSelected = onScreenSelected
        )

        LeagueTab(
            title = "Schedule",
            screen = AppScreen.SCHEDULE,
            currentScreen = currentScreen,
            onScreenSelected = onScreenSelected
        )

        LeagueTab(
            title = "Standings",
            screen = AppScreen.STANDINGS,
            currentScreen = currentScreen,
            onScreenSelected = onScreenSelected
        )
    }
}

@Composable
private fun LeagueTab(
    title: String,
    screen: AppScreen,
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {

    val selected = screen == currentScreen

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable {
                onScreenSelected(screen)
            }
            .padding(horizontal = 8.dp)
    ) {

        Text(
            text = title,
            color = if (selected) FphlText else FphlMutedText,
            fontSize = 14.sp,
            fontWeight =
                if (selected) FontWeight.Bold
                else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(3.dp)
                .background(
                    if (selected) FphlRed
                    else Color.Transparent
                )
        )
    }
}