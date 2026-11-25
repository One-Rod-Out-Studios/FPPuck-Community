package org.fppuck.community.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PublicationCopyState {
    data object Loading : PublicationCopyState
    data class Loaded(val text: String) : PublicationCopyState
    data object Missing : PublicationCopyState
}

private suspend fun loadAssetText(context: android.content.Context, assetName: String): String? =
    withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        } catch (_: java.io.IOException) {
            null
        }
    }

/**
 * Renders the plain-text publication copy for About and Patch Notes pages.
 *
 * Renders the bundled text as separate paragraphs.
 */
@Composable
internal fun FppuckPublicationCopy(
    assetName: String,
) {
    val context = LocalContext.current
    val state by produceState<PublicationCopyState>(
        initialValue = PublicationCopyState.Loading,
        assetName,
    ) {
        value = loadAssetText(context, assetName)?.let { PublicationCopyState.Loaded(it) }
            ?: PublicationCopyState.Missing
    }

    when (val current = state) {
        PublicationCopyState.Loading -> {
            Text(
                text = "Loading…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        PublicationCopyState.Missing -> {
            Text(
                text = "Publication copy is not available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        is PublicationCopyState.Loaded -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Publication copy" },
            ) {
                current.text
                    .replace("\r\n", "\n")
                    .split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
            }
        }
    }
}
