package com.github.pwoicik.torrentapp.ui.main

import android.content.Intent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pwoicik.torrentapp.ApplicationConstants
import com.github.pwoicik.torrentapp.domain.model.Torrent
import com.github.pwoicik.torrentapp.domain.model.TorrentState
import com.github.pwoicik.torrentapp.domain.model.TorrentTransferStats
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.invoke
import com.github.pwoicik.torrentapp.ui.main.MainScreen.Event
import com.github.pwoicik.torrentapp.ui.settings.SettingsScreen
import com.github.pwoicik.torrentapp.ui.util.formatSpeed
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object MainScreen : Screen {
    data class State(
        val torrents: List<Torrent>?,
        val event: (Event) -> Unit,
    ) : CircuitUiState {
        operator fun invoke(event: Event) = event(event)
    }

    sealed interface Event : CircuitUiEvent {
        data class ChildNav(val value: NavEvent.GoTo) : Event
    }
}

@Composable
fun MainPresenter(navigator: Navigator, getTorrents: GetTorrentsUseCase): MainScreen.State {
    val torrents by getTorrents().collectAsStateWithLifecycle(initialValue = null)
    return MainScreen.State(
        torrents = torrents,
    ) {
        when (it) {
            is Event.ChildNav -> navigator.onNavEvent(it.value)
        }
    }
}

@Composable
fun MainContent(uiState: MainScreen.State, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Scaffold(
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
                    IconButton(
                        onClick = { uiState(Event.ChildNav(NavEvent.GoTo(SettingsScreen))) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        floatingActionButton = { MagnetFab(onNavEvent = { uiState(Event.ChildNav(it)) }) },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(uiState.torrents.orEmpty()) {
                Torrent(torrent = it)
            }
        }
    }
}

@Composable
private fun Torrent(torrent: Torrent, modifier: Modifier = Modifier) {
    val stats by torrent.transferStats.collectAsStateWithLifecycle(TorrentTransferStats())
    val coroutineScope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(12.dp, 12.dp, 18.dp, 12.dp),
    ) {
        PlayPauseButton(
            paused = stats.state.isPaused,
            onClick = {
                coroutineScope.launch {
                    if (stats.state.isPaused) {
                        torrent.resume()
                    } else {
                        torrent.pause()
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Column {
            Text(torrent.name)
            LinearProgressIndicator(
                progress = { stats.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row {
                    Text(
                        text = when (stats.state) {
                            TorrentState.CheckingFiles -> "Checking files"
                            TorrentState.Downloading -> "Downloading"
                            TorrentState.DownloadingMetadata -> "Downloading metadata"
                            TorrentState.Seeding -> "Seeding"
                            TorrentState.CheckingResumeData -> "Checking"
                            TorrentState.Finished -> "Finished"
                            TorrentState.Paused -> "Paused"
                            TorrentState.Stopped -> "Stopped"
                        },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "↓ %s  ↑ %s".format(
                            stats.downloadSpeed.formatSpeed(),
                            stats.uploadSpeed.formatSpeed(),
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "%.2f%%".format(stats.progress * 100),
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Static),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    paused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(
        targetState = paused,
        label = "PlayPauseIcon",
    )
    val radius by transition.animateInt(label = "Radius") {
        if (it) 30 else 50
    }
    val backgroundColor by transition.animateColor(label = "BackgroundColor") {
        if (it) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    }
    val color by transition.animateColor(label = "IconColor") {
        if (it) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    }
    Icon(
        imageVector = if (paused) {
            Icons.Filled.PlayArrow
        } else {
            Icons.Filled.Pause
        },
        tint = color,
        contentDescription = null,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(radius)
            }
            .drawBehind { drawRect(backgroundColor) }
            .clickable(role = Role.Button) { onClick() }
            .padding(7.5.dp)
            .size(24.dp),
    )
}

@Composable
private fun StatsButton(modifier: Modifier = Modifier) {
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
private fun MagnetFab(onNavEvent: (NavEvent.GoTo) -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    FloatingActionButton(
        onClick = { visible = true },
        modifier = modifier,
    ) {
        Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
    }
    if (visible) {
        BasicAlertDialog(onDismissRequest = { visible = false }) {
            CircuitContent(
                screen = MagnetInputScreen,
                onNavEvent = {
                    when (it) {
                        is NavEvent.GoTo,
                        -> {
                            visible = false
                            onNavEvent(it)
                        }

                        is NavEvent.Pop,
                        -> visible = false

                        else -> Unit
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
