package com.github.pwoicik.torrentapp.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pwoicik.torrentapp.domain.usecase.GetDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.SaveDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.invoke
import com.github.pwoicik.torrentapp.ui.settings.SettingsScreen.Event
import com.github.pwoicik.torrentapp.ui.settings.SettingsScreen.State
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
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
    }
}

@Composable
fun SettingsPresenter(
    getDownloadSettings: GetDownloadSettingsUseCase,
    saveDownloadSettings: SaveDownloadSettingsUseCase,
): State {
    val settings = getDownloadSettings().collectAsStateWithLifecycle(initialValue = null).value
        ?: return State.Loading
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return State.Loaded(
        saveLocation = settings.savePath,
        sequential = settings.sequential,
        prioritizeFirstLast = settings.prioritizeFirstLast,
    ) event@{ ev ->
        when (ev) {
            is Event.SaveLocationChanged -> coroutineScope.launch {
                // TODO: test on every android version
                val id = DocumentsContract.getTreeDocumentId(ev.value)
                saveDownloadSettings(
                    settings.copy(
                        savePath = Environment.getExternalStorageDirectory()
                            .resolve(id.substringAfter(':'))
                            .takeIf { it.exists() }
                            ?.absolutePath
                            ?: return@launch,
                    ),
                )
                context.contentResolver.takePersistableUriPermission(
                    ev.value,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        .or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                )
            }

            Event.PrioritizeFirstLastChanged -> coroutineScope.launch {
                saveDownloadSettings(
                    settings.copy(prioritizeFirstLast = !settings.prioritizeFirstLast),
                )
            }

            Event.SequentialChanged -> coroutineScope.launch {
                saveDownloadSettings(
                    settings.copy(sequential = !settings.sequential),
                )
            }
        }
    }
}

@Composable
fun SettingsContent(uiState: State, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        if (uiState !is State.Loaded) return
        val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            navigationIcon = {
                IconButton(onClick = { backDispatcher?.onBackPressed() }) {
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

            @Suppress("MaxLineLength")
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
            ActivityResultContracts.OpenDocumentTree(),
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
