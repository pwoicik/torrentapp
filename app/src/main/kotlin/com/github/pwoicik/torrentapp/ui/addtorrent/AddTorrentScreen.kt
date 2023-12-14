package com.github.pwoicik.torrentapp.ui.addtorrent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize
import org.libtorrent4j.AddTorrentParams

@Parcelize
data class AddTorrentScreen(
    val magnet: String,
) : Screen {
    data class State(
        val torrent: AddTorrentParams?,
        val event: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event {

    }
}

@Composable
fun AddTorrentPresenter(
    screen: AddTorrentScreen,
    navigator: Navigator,
): AddTorrentScreen.State {
    var torrentParams by remember { mutableStateOf<AddTorrentParams?>(null) }
    LaunchedEffect(Unit) {
        torrentParams = try {
            AddTorrentParams.parseMagnetUri(screen.magnet)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    return AddTorrentScreen.State(
        torrent = torrentParams
    ) {}
}

@Composable
fun AddTorrent(
    uiState: AddTorrentScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .fillMaxSize(),
    ) {
        uiState.torrent?.let {
            it::class.java.declaredFields
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.name)
            Text(
                text = "Version",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.version.toString())
            Text(
                text = "Comment",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.comment.toString())
            Text(
                text = "Hash",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.infoHashes.best.toString())
        }
    }
}
