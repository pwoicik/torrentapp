package com.github.pwoicik.torrentapp.ui.addtorrent

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import arrow.core.getOrElse
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetInput
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen.Event
import com.github.pwoicik.torrentapp.ui.util.formatSize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Parcelize
data class AddTorrentScreen(
    val magnet: MagnetInfo,
) : Screen {
    data class State(
        val metadata: MagnetMetadata?,
        val startImmediately: Boolean,
        val sequentialDownload: Boolean,
        val prioritizeFirstAndLast: Boolean,
        val downloadLocation: String,
        val contentLayout: ContentLayout,
        val event: (Event) -> Unit,
    ) : CircuitUiState {
        operator fun invoke(event: Event) = event(event)
    }

    sealed interface Event : CircuitUiEvent {
        data object NavigationBackClicked : Event
        data object StartImmediatelyClicked : Event
        data object SequentialDownloadClicked : Event
        data object PrioritizeFirstAndLastClicked : Event
        data class ContentLayoutSelected(val value: ContentLayout) : Event
        data object DownloadClicked : Event
    }
}

enum class ContentLayout {
    Original,
    Subfolder,
    Flat,
    ;
}

@Composable
fun AddTorrentPresenter(
    screen: AddTorrentScreen,
    navigator: Navigator,
    getMagnetMetadata: GetMagnetMetadataUseCase,
    saveMagnet: SaveMagnetUseCase,
): AddTorrentScreen.State {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metadata by produceState<MagnetMetadata?>(initialValue = null) {
        value = getMagnetMetadata(screen.magnet).getOrElse { TODO() }
    }
    var startImmediately by remember { mutableStateOf(true) }
    var sequentialDownload by remember { mutableStateOf(false) }
    var prioritizeFirstAndLast by remember { mutableStateOf(false) }
    var contentLayout by remember { mutableStateOf(ContentLayout.Original) }
    return AddTorrentScreen.State(
        metadata = metadata,
        startImmediately = startImmediately,
        sequentialDownload = sequentialDownload,
        prioritizeFirstAndLast = prioritizeFirstAndLast,
        downloadLocation = context
            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
            .absolutePath,
        contentLayout = contentLayout,
    ) {
        when (it) {
            Event.StartImmediatelyClicked,
            -> startImmediately = !startImmediately

            Event.SequentialDownloadClicked,
            -> sequentialDownload = !sequentialDownload

            Event.PrioritizeFirstAndLastClicked,
            -> prioritizeFirstAndLast = !prioritizeFirstAndLast

            Event.NavigationBackClicked,
            -> navigator.pop()

            is Event.ContentLayoutSelected,
            -> contentLayout = it.value

            Event.DownloadClicked,
            -> scope.launch {
                saveMagnet(
                    metadata?.let(SaveMagnetInput::Metadata)
                        ?: SaveMagnetInput.Info(screen.magnet)
                )
            }
        }
    }
}

@Composable
fun AddTorrent(
    screen: AddTorrentScreen,
    uiState: AddTorrentScreen.State,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        FloatingActionButton(
            onClick = { uiState(Event.DownloadClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null)
        }
        // TODO: fix insets
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            TopAppBar(
                title = { Text("Add torrent") },
                navigationIcon = {
                    IconButton(onClick = { uiState(Event.NavigationBackClicked) }) {
                        Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = null)
                    }
                },
            )
            Box(Modifier.height(4.dp)) {
                if (uiState.metadata == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            screen.magnet.let {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(it.name)
                }
            }
            Checkbox(
                checked = uiState.startImmediately,
                onCheckedChange = { uiState(Event.StartImmediatelyClicked) },
                label = "Start downloading immediately",
            )
            Checkbox(
                checked = uiState.sequentialDownload,
                onCheckedChange = { uiState(Event.SequentialDownloadClicked) },
                label = "Sequential download",
            )
            Checkbox(
                checked = uiState.prioritizeFirstAndLast,
                onCheckedChange = { uiState(Event.PrioritizeFirstAndLastClicked) },
                label = "Download first and last pieces first",
            )
            DirectoryPicker(
                selected = uiState.downloadLocation,
            )
            ContentLayoutPicker(
                selected = uiState.contentLayout,
                onSelect = { uiState(Event.ContentLayoutSelected(it)) },
            )
            AnimatedVisibility(visible = uiState.metadata != null) {
                TorrentInfo(info = uiState.metadata!!)
            }
        }
    }
}

@Composable
private fun Checkbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange() },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DirectoryPicker(
    selected: String,
) {
    var value by remember(selected) {
        mutableStateOf(
            TextFieldValue(
                text = selected,
                selection = TextRange(Int.MAX_VALUE),
            )
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        readOnly = true,
        label = { Text("Save to") },
        trailingIcon = {
            Icon(imageVector = Icons.Default.Folder, contentDescription = null)
        },
        interactionSource = interactionSource,
        modifier = Modifier.padding(12.dp),
    )
    LaunchedEffect(Unit) {
        interactionSource.interactions.collect {
            if (it !is PressInteraction.Release) {
                return@collect
            }
            // TODO
        }
    }
}

@Composable
private fun ContentLayoutPicker(
    selected: ContentLayout,
    onSelect: (ContentLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(12.dp),
    ) {
        Text(
            text = "Content layout",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.alignByBaseline(),
        )
        Box(Modifier.alignByBaseline()) {
            var expanded by remember { mutableStateOf(false) }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                ContentLayout.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(it.name) },
                        onClick = {
                            expanded = false
                            onSelect(it)
                        },
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { expanded = true }
                    .padding(8.dp, 3.dp, 6.dp, 3.dp),
            ) {
                Text(
                    text = selected.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .alignByBaseline()
                        .animateContentSize(),
                )
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(3.dp)
                        ),
                )
            }
        }
    }
}

@Composable
private fun TorrentInfo(
    info: MagnetMetadata,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row {
            info.creator?.let { creator ->
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = "Created by",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(creator)
                }
            }
            info.creationDate?.let { instant ->
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = "Created on",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    // TODO: formatting
                    Text(
                        text = LocalDateTime.ofInstant(
                            instant,
                            ZoneId.systemDefault(),
                        ).format(DateTimeFormatter.ISO_DATE_TIME),
                    )
                }
            }
        }
        Row {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = "Files",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(info.numberOfFiles.toString())
            }
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = "Size",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(info.totalSize.formatSize())
            }
        }
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = "Pieces",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "%d x %s".format(
                    info.numberOfPieces,
                    info.pieceSize.formatSize(),
                )
            )
        }
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = "Hash",
                style = MaterialTheme.typography.labelMedium,
            )
            SelectionContainer {
                Text(info.hash.value)
            }
        }
    }
}