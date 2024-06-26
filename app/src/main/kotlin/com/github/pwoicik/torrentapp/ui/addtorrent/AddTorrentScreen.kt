package com.github.pwoicik.torrentapp.ui.addtorrent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import arrow.core.getOrElse
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata
import com.github.pwoicik.torrentapp.domain.usecase.GetDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetError
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetInput
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import com.github.pwoicik.torrentapp.domain.usecase.invoke
import com.github.pwoicik.torrentapp.proto.Settings
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen.Event
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen.State
import com.github.pwoicik.torrentapp.ui.util.formatSize
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddTorrentScreen(
    val magnet: MagnetInfo,
) : Screen {
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Loaded(
            val metadata: MagnetMetadata?,
            val startImmediately: Boolean,
            val sequentialDownload: Boolean,
            val prioritizeFirstAndLast: Boolean,
            val savePath: String,
            val contentLayout: ContentLayout,
            val error: SaveMagnetError? = null,
            val event: (Event) -> Unit,
        ) : State {
            operator fun invoke(event: Event) = event(event)
        }
    }

    sealed interface Event : CircuitUiEvent {
        data object NavigationBackClicked : Event
        data object StartImmediatelyClicked : Event
        data object SequentialDownloadClicked : Event
        data object PrioritizeFirstAndLastClicked : Event
        data class ContentLayoutSelected(val value: ContentLayout) : Event
        data object DownloadClicked : Event
        data object ErrorSeen : Event
    }
}

enum class ContentLayout {
    Original,
    Subfolder,
    Flat,
}

@Composable
fun AddTorrentPresenter(
    screen: AddTorrentScreen,
    navigator: Navigator,
    getMagnetMetadata: GetMagnetMetadataUseCase,
    saveMagnet: SaveMagnetUseCase,
    getDownloadSettings: GetDownloadSettingsUseCase,
): State {
    val scope = rememberCoroutineScope()
    val metadata by produceState<MagnetMetadata?>(initialValue = null) {
        value = getMagnetMetadata(screen.magnet).getOrElse { TODO() }
    }
    val settings = produceState<Settings.Download?>(initialValue = null) {
        value = getDownloadSettings().first()
    }.value ?: return State.Loading
    var startImmediately by remember { mutableStateOf(true) }
    var sequentialDownload by remember { mutableStateOf(settings.sequential) }
    var prioritizeFirstAndLast by remember { mutableStateOf(settings.prioritizeFirstLast) }
    var contentLayout by remember { mutableStateOf(ContentLayout.Original) }
    var saveMagnetError by remember { mutableStateOf<SaveMagnetError?>(null) }
    return State.Loaded(
        metadata = metadata,
        startImmediately = startImmediately,
        sequentialDownload = sequentialDownload,
        prioritizeFirstAndLast = prioritizeFirstAndLast,
        savePath = settings.savePath,
        contentLayout = contentLayout,
        error = saveMagnetError,
    ) { ev ->
        when (ev) {
            Event.StartImmediatelyClicked,
            -> startImmediately = !startImmediately

            Event.SequentialDownloadClicked,
            -> sequentialDownload = !sequentialDownload

            Event.PrioritizeFirstAndLastClicked,
            -> prioritizeFirstAndLast = !prioritizeFirstAndLast

            Event.NavigationBackClicked,
            -> navigator.pop()

            is Event.ContentLayoutSelected,
            -> contentLayout = ev.value

            Event.DownloadClicked,
            -> scope.launch {
                saveMagnet(
                    SaveMagnetInput(
                        info = screen.magnet,
                        metadata = metadata,
                        startImmediately = startImmediately,
                        sequential = sequentialDownload,
                        prioritizeFirstAndLast = prioritizeFirstAndLast,
                        savePath = settings.savePath,
                    ),
                ).fold(
                    ifLeft = { saveMagnetError = it },
                    ifRight = { navigator.pop() },
                )
            }

            Event.ErrorSeen,
            -> saveMagnetError = null
        }
    }
}

private val ContentPadding = PaddingValues(18.dp, 12.dp)

@Composable
fun AddTorrentContent(
    screen: AddTorrentScreen,
    uiState: State,
    modifier: Modifier = Modifier,
) {
    if (uiState !is State.Loaded) return
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add torrent") },
                navigationIcon = {
                    IconButton(onClick = { uiState(Event.NavigationBackClicked) }) {
                        Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uiState(Event.DownloadClicked) },
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.height(4.dp)) {
                if (uiState.metadata == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            LabeledValue(
                label = "Name",
                value = screen.magnet.name,
                isSelectable = true,
            )
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
                selected = uiState.savePath,
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

    LaunchedEffect(uiState.error) {
        when (val err = uiState.error) {
            null -> {
                snackbarHostState.currentSnackbarData?.dismiss()
            }

            SaveMagnetError.FileAlreadyExists -> {
                snackbarHostState.showSnackbar(
                    message = "File already exists!",
                )
                uiState(Event.ErrorSeen)
            }

            is SaveMagnetError.UnknownError -> {
                snackbarHostState.showSnackbar(
                    message = err.message,
                )
                uiState(Event.ErrorSeen)
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
            .padding(start = 6.dp, end = 18.dp)
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
private fun DirectoryPicker(selected: String) {
    var value by remember(selected) {
        mutableStateOf(
            TextFieldValue(
                text = selected,
                selection = TextRange(Int.MAX_VALUE),
            ),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(ContentPadding),
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
        modifier = modifier.padding(ContentPadding),
    ) {
        Text(
            text = "Content layout",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun TorrentInfo(info: MagnetMetadata, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row {
            info.creator?.let {
                LabeledValue(
                    label = "Created by",
                    value = it,
                    modifier = Modifier.weight(1f),
                )
            }

            info.creationDate?.let {
                LabeledValue(
                    label = "Created on",
                    value = LocalDateTime.Format {
                        dayOfMonth(Padding.ZERO)
                        char('.')
                        monthNumber(Padding.ZERO)
                        char('.')
                        year()
                        char(' ')
                        hour(Padding.ZERO)
                        minute(Padding.ZERO)
                    }.format(it.toLocalDateTime(TimeZone.currentSystemDefault())),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        FilesInfo(
            info = info,
            modifier = Modifier.padding(top = 6.dp),
        )
        LabeledValue(
            label = "Pieces",
            value = "%d x %s".format(
                info.numberOfPieces,
                info.pieceSize.formatSize(),
            ),
        )
        LabeledValue(
            label = "Hash",
            value = info.hash.value,
            isSelectable = true,
        )
    }
}

@Composable
private fun FilesInfo(info: MagnetMetadata, modifier: Modifier = Modifier) {
    val overlayHost = LocalOverlayHost.current
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(0.5.dp),
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .clickable(role = Role.Button) {
                    coroutineScope.launch {
                        overlayHost.show(FileStructureOverlay(info))
                    }
                }
                .padding(ContentPadding),
        ) {
            Text(
                text = "Files",
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
            )
        }
        Row {
            LabeledValue(
                label = "Selected",
                value = buildAnnotatedString {
                    append(info.numberOfFiles.toString())
                    withStyle(
                        MaterialTheme.typography.labelMedium.toSpanStyle()
                            .copy(MaterialTheme.colorScheme.onSurfaceVariant),
                    ) {
                        append("  out of  ")
                    }
                    append(info.numberOfFiles.toString())
                },
                modifier = Modifier.weight(1f),
            )
            LabeledValue(
                label = "Size",
                value = info.totalSize.formatSize(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isSelectable: Boolean = false,
) {
    Column(
        modifier = modifier.padding(ContentPadding),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isSelectable) {
            SelectionContainer {
                Text(value)
            }
        } else {
            Text(value)
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: AnnotatedString,
    modifier: Modifier = Modifier,
    isSelectable: Boolean = false,
) {
    Column(
        modifier = modifier.padding(ContentPadding),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isSelectable) {
            SelectionContainer {
                Text(value)
            }
        } else {
            Text(value)
        }
    }
}
