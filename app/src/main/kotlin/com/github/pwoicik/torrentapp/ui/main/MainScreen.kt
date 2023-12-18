package com.github.pwoicik.torrentapp.ui.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pwoicik.torrentapp.ApplicationConstants
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
import com.github.pwoicik.torrentapp.domain.usecase.invoke
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.github.pwoicik.torrentapp.ui.main.MainScreen.Event
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data object MainScreen : Screen {
    data class State(
        val showMagnetInput: Boolean,
        val magnetError: Boolean,
        val event: (Event) -> Unit,
    ) : CircuitUiState {
        operator fun invoke(event: Event) = event(event)
    }

    sealed interface Event : CircuitUiEvent {
        data object AddMagnetClicked : Event
        data object MagnetChanged : Event
        data class MagnetConfirmed(val value: String) : Event
        data object MagnetInputDismissed : Event
    }
}

@Composable
fun MainPresenter(
    navigator: Navigator,
    parseMagnet: ParseMagnetUseCase,
    getTorrents: GetTorrentsUseCase,
): MainScreen.State {
    val torrents by getTorrents().collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(torrents) {
        // TODO: display torrents
        Log.d("test", torrents.toString())
    }
    var showMagnetInput by remember { mutableStateOf(false) }
    var magnetError by remember { mutableStateOf(false) }
    return MainScreen.State(
        showMagnetInput = showMagnetInput,
        magnetError = magnetError,
    ) { ev ->
        when (ev) {
            Event.AddMagnetClicked -> showMagnetInput = true

            Event.MagnetChanged -> magnetError = false

            is Event.MagnetConfirmed -> {
                parseMagnet(ev.value).fold(
                    ifLeft = { magnetError = true },
                    ifRight = {
                        showMagnetInput = false
                        navigator.goTo(AddTorrentScreen(it))
                    },
                )
            }

            Event.MagnetInputDismissed -> showMagnetInput = false
        }
    }
}

@Composable
fun Main(
    uiState: MainScreen.State,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(text = "Torrent App") },
                actions = {
                    StatsButton()
                    IconButton(
                        onClick = {
                            context.sendBroadcast(
                                Intent()
                                    .setPackage(context.packageName)
                                    .setAction(ApplicationConstants.ACTION_FINISH),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerOff,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            MagnetFab(
                onClick = { uiState(Event.AddMagnetClicked) },
                showMagnetInput = uiState.showMagnetInput,
                magnetError = uiState.magnetError,
                onDismissMagnetInput = { uiState(Event.MagnetInputDismissed) },
                onMagnetChange = { uiState(Event.MagnetChanged) },
                onMagnetConfirm = { uiState(Event.MagnetConfirmed(it)) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {

        }
    }
}

@Composable
private fun StatsButton(
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    IconButton(
        onClick = { visible = true },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Outlined.MonitorHeart,
            contentDescription = null,
        )
    }
    if (visible) {
        BasicAlertDialog(onDismissRequest = { visible = false }) {
            CircuitContent(
                screen = SessionStatsScreen,
                onNavEvent = {
                    if (it is NavEvent.Pop) {
                        visible = false
                    }
                },
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        MaterialTheme.shapes.extraLarge,
                    )
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun MagnetFab(
    onClick: () -> Unit,
    showMagnetInput: Boolean,
    magnetError: Boolean,
    onDismissMagnetInput: () -> Unit,
    onMagnetChange: (String) -> Unit,
    onMagnetConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
    }
    if (showMagnetInput) {
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
            onDismissRequest = onDismissMagnetInput,
            title = { Text("Download from magnet") },
            text = {
                OutlinedTextField(
                    value = magnet,
                    onValueChange = {
                        magnet = it
                        onMagnetChange(it.text)
                    },
                    isError = magnetError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onMagnetConfirm(magnet.text) },
                ) {
                    Text("Download")
                }
            },
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
