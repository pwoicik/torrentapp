package com.github.pwoicik.torrentapp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.github.pwoicik.torrentapp.ui.main.MagnetInputScreen.Event
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data object MagnetInputScreen : Screen {
    data class State(
        val magnet: TextFieldValue,
        val error: Boolean,
        val event: (Event) -> Unit,
    ) : CircuitUiState {
        operator fun invoke(event: Event) = event(event)
    }

    sealed interface Event : CircuitUiEvent {
        data class MagnetChanged(val value: TextFieldValue) : Event
        data object ConfirmClicked : Event
        data object Cancelled : Event
    }
}

@Composable
fun MagnetInputPresenter(
    navigator: Navigator,
    parseMagnet: ParseMagnetUseCase,
): MagnetInputScreen.State {
    var magnet by remember {
        mutableStateOf(
            TextFieldValue(
                text = "magnet:?xt=urn:btih:28a399dc14f6ff3d37e975b072da4095fe7357e9&dn=archlinux-2023.12.01-x86_64.iso",
                selection = TextRange(0, Int.MAX_VALUE),
            )
        )
    }
    var error by remember { mutableStateOf(false) }
    return MagnetInputScreen.State(
        magnet = magnet,
        error = false,
    ) { ev ->
        when (ev) {
            Event.ConfirmClicked,
            -> {
                parseMagnet(magnet.text).fold(
                    ifLeft = { error = true },
                    ifRight = { navigator.goTo(AddTorrentScreen(it)) },
                )
            }

            is Event.MagnetChanged,
            -> magnet = ev.value

            Event.Cancelled,
            -> navigator.pop()
        }
    }
}

@Composable
fun MagnetInput(
    uiState: MagnetInputScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Download from magnet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            value = uiState.magnet,
            onValueChange = { uiState(Event.MagnetChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { uiState(Event.Cancelled) }) {
                Text("Cancel")
            }
            TextButton(onClick = { uiState(Event.ConfirmClicked) }) {
                Text("Download")
            }
        }
    }
}
