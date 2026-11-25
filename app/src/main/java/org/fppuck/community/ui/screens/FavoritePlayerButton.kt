package org.fppuck.community.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.fppuck.community.data.FavoritePlayersStore
import org.fppuck.community.data.PlayerProfile

/**
 * A descriptive Material3 [TextButton] that adds or removes the current player
 * from this device's favorites.
 *
 * The saved visual state is initialized once per player from the store, then
 * toggled on every press: pressing saves the player when it is not saved and
 * removes it when it is. Nothing here touches the network; saving and reading
 * is fully offline, exactly as [FavoritePlayersStore] documents.
 *
 * @param profile the player detail screen's profile; its [PlayerProfile.playerId]
 *   is the stable provider identifier used as the store key, and its
 *   [PlayerProfile.name] is the readable display name captured when saved.
 */
@Composable
fun FavoritePlayerButton(
    profile: PlayerProfile,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerId = profile.playerId
    val playerName = profile.name

    // Guard for a blank provider id: there is no stable key to save under, so
    // the button reports that this player cannot be favorited and does not
    // write anything to the store.
    val canFavorite = playerId.isNotBlank() &&
        playerName.isNotBlank()

    var saved by remember(playerId) {
        mutableStateOf(
            canFavorite && FavoritePlayersStore.load(context).containsKey(playerId),
        )
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = {
                if (!canFavorite) {
                    // Nothing to toggle: leave the store untouched.
                    return@TextButton
                }
                if (saved) {
                    FavoritePlayersStore.remove(context, playerId)
                    saved = false
                } else {
                    FavoritePlayersStore.set(context, playerId, playerName)
                    saved = true
                }
            },
            enabled = canFavorite,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (saved) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
        ) {
            Text(
                text = when {
                    !canFavorite -> "Favorite unavailable for this player"
                    saved -> "Remove from My Favorites"
                    else -> "Add to My Favorites"
                },
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }

        if (canFavorite) {
            Text(
                text = if (saved) "Saved" else "Not saved",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
