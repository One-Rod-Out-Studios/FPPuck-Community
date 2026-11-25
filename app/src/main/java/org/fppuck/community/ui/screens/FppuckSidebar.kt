package org.fppuck.community.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.fppuck.community.R
import org.fppuck.community.data.FavoritePlayersStore
import org.fppuck.community.data.FavoriteTeamsStore
import org.fppuck.community.data.sampleTeams
import org.fppuck.community.ui.theme.FppuckAppearance

@Composable
fun FppuckSidebar(selected: AppScreen, onSelect: (AppScreen) -> Unit) {
    val width = (LocalConfiguration.current.screenWidthDp * .68f).coerceIn(224f, 320f).dp
    ModalDrawerSheet(
        modifier = Modifier.width(width).fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 20.dp)
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("FPPuck Community", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 16.dp),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            SidebarRow("Appearance", "Light, Dark, System", AppScreen.APPEARANCE, selected, onSelect)
            SidebarRow("Manage Favorites", "Favorite teams and players", AppScreen.FAVORITES, selected, onSelect)
            SidebarRow("Glossary", "Hockey stats and abbreviations", AppScreen.MORE, selected, onSelect)
            SidebarRow("Help", "Report an issue", AppScreen.SUPPORT, selected, onSelect)
            SidebarRow("Patch Notes", "Development changelog", AppScreen.PATCH_NOTES, selected, onSelect)
            SidebarRow("Privacy Policy", "Data and privacy", AppScreen.PRIVACY, selected, onSelect)
        }
        HorizontalDivider()
        SidebarRow("About FPPuck", "Version and credits", AppScreen.ABOUT, selected, onSelect)
    }
}

@Composable
private fun SidebarRow(
    title: String,
    subtitle: String,
    screen: AppScreen? = null,
    selected: AppScreen? = null,
    onSelect: (AppScreen) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val active = screen != null && selected == screen
    Surface(color = if (active) colors.secondaryContainer else colors.surface) {
        val action = if (screen == null) Modifier else Modifier
            .testTag("sidebar-${screen.name}")
            .clickable(role = Role.Button) { onSelect(screen) }
        Row(
            modifier = Modifier.fillMaxWidth().then(action).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (screen == null) colors.onSurfaceVariant else colors.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (screen != null) Text("›", Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge, color = colors.tertiary)
        }
    }
}

@Composable
fun FppuckMenuBar(title: String, onOpen: () -> Unit, onBack: (() -> Unit)?) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpen) {
                Icon(painterResource(R.drawable.ic_settings_gear), contentDescription = "Open settings")
            }
            Text(title, Modifier.weight(1f).padding(horizontal = 8.dp).testTag("menu-title"),
                style = MaterialTheme.typography.titleLarge)
            onBack?.let { TextButton(onClick = it) { Text("Back") } }
        }
    }
}

@Composable
fun FppuckSidebarPage(
    destination: AppScreen,
    modifier: Modifier = Modifier,
    appearance: FppuckAppearance,
    onAppearanceChange: (FppuckAppearance) -> Unit,
    onBrowseTeams: () -> Unit
) {
    if (destination == AppScreen.MORE) {
        MoreScreen(modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (destination) {
            AppScreen.APPEARANCE -> FppuckAppearance.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .selectable(selected = mode == appearance, role = Role.RadioButton,
                            onClick = { onAppearanceChange(mode) })
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(selected = mode == appearance, onClick = null)
                    Text(mode.label)
                }
            }
            AppScreen.FAVORITES -> FppuckFavoritesPage(onBrowseTeams)
            AppScreen.PATCH_NOTES -> FppuckPublicationCopy("fppuck-patch-notes.txt")
            AppScreen.SUPPORT -> FppuckSupportPage()
            AppScreen.ABOUT -> FppuckAboutPage()
            AppScreen.PRIVACY -> FppuckPrivacyPolicy()
            else -> error("Expected a sidebar destination")
        }
    }
}

@Composable
private fun FppuckFavoritesPage(onBrowseTeams: () -> Unit) {
    val context = LocalContext.current
    var teamFavorites by remember { mutableStateOf(FavoriteTeamsStore.load(context)) }
    var playerFavorites by remember { mutableStateOf(FavoritePlayersStore.load(context)) }

    Text(
        "Favorites are saved on this device. No account is needed.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text("Favorite teams (${teamFavorites.size})", style = MaterialTheme.typography.titleSmall)
    if (teamFavorites.isEmpty()) {
        Text("No teams saved yet. Add teams from team screens.", style = MaterialTheme.typography.bodyMedium)
    } else {
        teamFavorites.sorted().forEach { abbreviation: String ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(sampleTeams.firstOrNull { it.abbreviation == abbreviation }?.name ?: abbreviation,
                    modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    teamFavorites = FavoriteTeamsStore.toggle(context, teamFavorites, abbreviation)
                }) {
                    Text("Remove team")
                }
            }
        }
    }

    Text("Favorite players (${playerFavorites.size})", style = MaterialTheme.typography.titleSmall)
    if (playerFavorites.isEmpty()) {
        Text("No players saved yet. Add players from player detail screens.", style = MaterialTheme.typography.bodyMedium)
    } else {
        playerFavorites.entries.sortedBy { entry: Map.Entry<String, String> -> entry.value }.forEach { entry: Map.Entry<String, String> ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(entry.value, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    FavoritePlayersStore.remove(context, entry.key)
                    playerFavorites = FavoritePlayersStore.load(context)
                }) {
                    Text("Remove player")
                }
            }
        }
    }

    OutlinedButton(onClick = onBrowseTeams) { Text("Browse teams and players") }
}

@Composable
private fun FppuckSupportPage() {
    Text("Report issues through the repository where you obtained this source. Leave personal data out of issue reports.")
}

@Composable
private fun FppuckAboutPage() {
    val context = LocalContext.current
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    Text("Installed version: $versionName", style = MaterialTheme.typography.bodyMedium)
    FppuckPublicationCopy("fppuck-about.txt")
}
