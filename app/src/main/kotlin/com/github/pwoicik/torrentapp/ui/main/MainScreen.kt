package com.github.pwoicik.torrentapp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.pwoicik.torrentapp.domain.model.MagnetUri
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data object MainScreen : Screen {
    data class State(
        val event: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class MagnetChanged(val value: String) : Event
    }
}

@Composable
fun MainPresenter(screen: MainScreen, navigator: Navigator): MainScreen.State {
    return MainScreen.State {
        when (it) {
            is MainScreen.Event.MagnetChanged -> navigator.goTo(
                AddTorrentScreen(MagnetUri(it.value))
            )
        }
    }
}

@Composable
fun Main(
    uiState: MainScreen.State,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxSize(),
    ) {
        var visible by remember { mutableStateOf(false) }
        if (visible) {
            var magnet by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = "magnet:?xt=urn:btih:28a399dc14f6ff3d37e975b072da4095fe7357e9&dn=archlinux-2023.12.01-x86_64.iso",
                        selection = TextRange(0, Int.MAX_VALUE),
                    ),
                )
            }
            val focusRequester = remember { FocusRequester() }
            AlertDialog(
                onDismissRequest = { visible = false },
                title = { Text("Download from magnet") },
                text = {
                    OutlinedTextField(
                        value = magnet,
                        onValueChange = { magnet = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            visible = false
                            uiState.event(MainScreen.Event.MagnetChanged(magnet.text))
                        },
                    ) {
                        Text("Download")
                    }
                },
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
        FloatingActionButton(
            onClick = { visible = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
        }
    }
}
