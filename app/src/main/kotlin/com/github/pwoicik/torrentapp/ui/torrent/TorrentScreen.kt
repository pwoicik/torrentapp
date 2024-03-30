package com.github.pwoicik.torrentapp.ui.torrent

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.ui.torrent.TorrentScreen.State
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data class TorrentScreen(
    val hash: Sha1Hash,
) : Screen {
    data class State(
        val name: String,
    ) : CircuitUiState
}

@Composable
fun TorrentPresenter(screen: TorrentScreen, navigator: Navigator): State {
    return State(
        name = "",
    )
}

@Composable
fun TorrentContent(uiState: State, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
    }
}
