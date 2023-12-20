package com.github.pwoicik.torrentapp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pwoicik.torrentapp.domain.model.SessionInfo
import com.github.pwoicik.torrentapp.domain.usecase.GetSessionInfoUseCase
import com.github.pwoicik.torrentapp.domain.usecase.invoke
import com.github.pwoicik.torrentapp.ui.main.SessionStatsScreen.Event
import com.github.pwoicik.torrentapp.ui.util.formatSize
import com.github.pwoicik.torrentapp.ui.util.formatSpeed
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data object SessionStatsScreen : Screen {
    data class State(
        val info: SessionInfo,
        val event: (Event) -> Unit,
    ) : CircuitUiState {
        operator fun invoke(event: Event) = event(event)
    }

    sealed interface Event : CircuitUiEvent {
        data object CloseButtonClicked : Event
    }
}

@Composable
fun SessionStatsPresenter(
    navigator: Navigator,
    getSessionInfo: GetSessionInfoUseCase,
): SessionStatsScreen.State {
    val info by getSessionInfo().collectAsStateWithLifecycle(initialValue = SessionInfo())
    return SessionStatsScreen.State(
        info = info,
    ) {
        when (it) {
            Event.CloseButtonClicked -> navigator.pop()
        }
    }
}

@Composable
fun SessionStats(uiState: SessionStatsScreen.State, modifier: Modifier = Modifier) {
    val stats = uiState.info
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Session stats",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabeledValue(label = "Listening port", value = stats.listenPort.toString())
            LabeledValue(label = "DHT nodes", value = stats.dhtNodes.toString())
            Row {
                LabeledValue(
                    label = "Download rate",
                    value = stats.downloadRate.formatSpeed(),
                    modifier = Modifier.weight(1f),
                )
                LabeledValue(
                    label = "Downloaded",
                    value = stats.totalDownload.formatSize(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row {
                LabeledValue(
                    label = "Upload rate",
                    value = stats.uploadRate.formatSpeed(),
                    modifier = Modifier.weight(1f),
                )
                LabeledValue(
                    label = "Uploaded",
                    value = stats.totalUpload.formatSize(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        TextButton(
            onClick = { uiState(Event.CloseButtonClicked) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Close")
        }
    }
}

@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
