package com.github.pwoicik.torrentapp.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pwoicik.torrentapp.proto.Settings
import com.github.pwoicik.torrentapp.ui.settings.SettingsScreen.Event
import com.github.pwoicik.torrentapp.ui.settings.SettingsScreen.State
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object SettingsScreen : Screen {
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Loaded(
            val saveLocation: String,
            val sequential: Boolean,
            val prioritizeFirstLast: Boolean,
            val event: (Event) -> Unit,
        ) : State {
            operator fun invoke(event: Event) = event(event)
        }
    }

    sealed interface Event : CircuitUiEvent {
        data class SaveLocationChanged(val value: Uri) : Event
        data object SequentialChanged : Event
        data object PrioritizeFirstLastChanged : Event
        data object NavBack : Event
    }
}

@Composable
fun SettingsPresenter(
    navigator: Navigator,
    store: DataStore<Settings>,
): State {
    val settings = store.data.collectAsStateWithLifecycle(initialValue = null).value?.download
        ?: return State.Loading
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var saveLocation by remember { mutableStateOf(settings.savePath) }
    var sequential by remember { mutableStateOf(settings.sequential) }
    var prioritizeFirstLast by remember { mutableStateOf(settings.prioritizeFirstLast) }
    return State.Loaded(
        saveLocation = saveLocation,
        sequential = sequential,
        prioritizeFirstLast = prioritizeFirstLast,
    ) event@{ ev ->
        when (ev) {
            Event.NavBack -> coroutineScope.launch {
                store.updateData {
                    it.copy(
                        download = it.download.copy(
                            savePath = saveLocation,
                            sequential = sequential,
                            prioritizeFirstLast = prioritizeFirstLast,
                        ),
                    )
                }
                navigator.pop()
            }

            is Event.SaveLocationChanged -> {
                // TODO: test on every android version
                val id = DocumentsContract.getTreeDocumentId(ev.value)
                saveLocation = Environment.getExternalStorageDirectory()
                    .resolve(id.substringAfter(':'))
                    .takeIf { it.exists() }
                    ?.absolutePath
                    ?: return@event
                context.contentResolver.takePersistableUriPermission(
                    ev.value,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        .or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                )
            }

            Event.PrioritizeFirstLastChanged,
            -> prioritizeFirstLast = !prioritizeFirstLast

            Event.SequentialChanged,
            -> sequential = !sequential
        }
    }
}

@Composable
fun SettingsContent(
    uiState: State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        if (uiState !is State.Loaded) return
        BackHandler { uiState(Event.NavBack) }
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            navigationIcon = {
                IconButton(onClick = { uiState(Event.NavBack) }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = null,
                    )
                }
            },
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
        ) {
            SaveLocationSetting(
                value = uiState.saveLocation,
                onValueChange = { uiState(Event.SaveLocationChanged(it)) },
            )

            SwitchSetting(
                title = "Sequential download",
                description = "Download pieces in sequential order, if disabled rarest pieces are prioritized",
                checked = uiState.sequential,
                onCheckedChange = { uiState(Event.SequentialChanged) },
            )

            SwitchSetting(
                title = "First and last pieces first",
                description = "Prioritize downloading pieces at the start and end of files",
                checked = uiState.prioritizeFirstLast,
                onCheckedChange = { uiState(Event.PrioritizeFirstLastChanged) },
            )
        }
    }
}

@Composable
private fun SaveLocationSetting(
    value: String,
    onValueChange: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            SettingDescription(
                title = "Download location",
                description = "Default location for downloaded torrents",
                modifier = Modifier.weight(1f),
            )
        }
        val interactionSource = remember { MutableInteractionSource() }
        OutlinedTextField(
            value = value,
            onValueChange = {},
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                Icon(imageVector = Icons.Rounded.Folder, contentDescription = null)
            },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
        )
        val pickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) f@{ onValueChange(it ?: return@f) }
        LaunchedEffect(Unit) {
            interactionSource.interactions.collect {
                if (it is PressInteraction.Release) {
                    pickerLauncher.launch(null)
                }
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onCheckedChange() }
            .padding(24.dp),
    ) {
        SettingDescription(
            title = title,
            description = description,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
        )
    }
}

@Composable
private fun SettingDescription(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelLarge.copy(
                lineBreak = LineBreak.Paragraph,
            ),
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
