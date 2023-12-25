package com.github.pwoicik.torrentapp.ui.addtorrent

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata
import com.github.pwoicik.torrentapp.domain.model.SelectionState
import com.github.pwoicik.torrentapp.domain.model.Storage
import com.github.pwoicik.torrentapp.ui.util.formatSize
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.adapters.ImmutableListAdapter

class FileStructureOverlay(
    private val info: MagnetMetadata,
) : Overlay<Unit> {
    @Composable
    override fun Content(navigator: OverlayNavigator<Unit>) {
        val visible = remember { MutableTransitionState(false) }
        BackHandler { visible.targetState = false }
        LaunchedEffect(Unit) { visible.targetState = true }
        LaunchedEffect(visible.isIdle) {
            if (visible.isIdle && !visible.currentState) {
                navigator.finish(Unit)
            }
        }
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            FileStructure(storage = Storage.Directory("(root)", ByteSize(), info.files))
        }
    }
}

@Composable
fun FileStructure(storage: Storage.Directory, modifier: Modifier = Modifier) {
    var navigatingUp by remember { mutableStateOf(false) }
    val backstack = remember { mutableStateListOf(storage) }
    val backPressDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backstack.size == 1) {
                    remove()
                    backPressDispatcher?.onBackPressed()
                } else {
                    navigatingUp = true
                    backstack.removeLast()
                }
            }
        }
        backPressDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { }
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        Breadcrumbs(
            backstack = ImmutableListAdapter(backstack),
            onClick = { backstack.removeRange(it + 1, backstack.size) },
        )
        AnimatedContent(
            targetState = backstack.last(),
            transitionSpec = {
                if (navigatingUp) {
                    slideInHorizontally { -it } togetherWith
                        slideOutHorizontally { it } + fadeOut() + scaleOut(targetScale = 0.9f)
                } else {
                    slideInHorizontally { it } togetherWith
                        fadeOut() + scaleOut(targetScale = 0.9f)
                }
            },
            contentAlignment = Alignment.TopStart,
            label = "CurrentDirectory",
            modifier = Modifier.weight(1f),
        ) { currentNode ->
            FileStructure(
                storage = currentNode.content,
                onGoUp = { backPressDispatcher?.onBackPressed() },
                onOpenDirectory = {
                    navigatingUp = false
                    backstack.add(it)
                },
                onSelectClick = {
                    when (it) {
                        is Storage.File -> {
                            it.selected = !it.selected
                        }

                        is Storage.Directory -> {
                            it.select(it.selected != SelectionState.Selected)
                        }
                    }
                    backstack.refreshSelected()
                },
            )
        }
    }
}

@Composable
private fun Breadcrumbs(
    backstack: ImmutableList<Storage.Directory>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        itemsIndexed(backstack) { i, directory ->
            Text(
                text = directory.name,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = if (i == backstack.lastIndex) {
                    MaterialTheme.colorScheme.primary.copy(0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(0.7f)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onClick(i) }
                    .padding(vertical = 4.dp, horizontal = 6.dp),
            )
            if (i != backstack.lastIndex) {
                Text(
                    text = "▶",
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .offset(y = (-1.5).dp),
                )
            }
        }
    }
    LaunchedEffect(backstack.size) {
        listState.scrollToItem(backstack.lastIndex)
    }
}

@Composable
private fun FileStructure(
    storage: ImmutableList<Storage>,
    onGoUp: () -> Unit,
    onOpenDirectory: (Storage.Directory) -> Unit,
    onSelectClick: (Storage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        item(0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button) { onGoUp() }
                    .padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardDoubleArrowUp,
                    contentDescription = null,
                )
                Text("(Go up)")
            }
        }
        items(
            items = storage,
            key = { it.name },
        ) {
            when (it) {
                is Storage.File -> File(
                    file = it,
                    onClick = { onSelectClick(it) },
                )

                is Storage.Directory -> Directory(
                    directory = it,
                    onClick = { onOpenDirectory(it) },
                    onSelectClick = { onSelectClick(it) },
                )
            }
        }
    }
}

@Composable
private fun File(
    file: Storage.File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Checkbox) { onClick() }
            .padding(12.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary.copy(0.7f),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = file.size.formatSize(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            Checkbox(
                checked = file.selected,
                onCheckedChange = { onClick() },
            )
        }
    }
}

@Composable
private fun Directory(
    directory: Storage.Directory,
    onClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) { onClick() }
            .padding(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary.copy(0.7f),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = directory.name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = directory.size.formatSize(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            TriStateCheckbox(
                state = when (directory.selected) {
                    SelectionState.Selected -> ToggleableState.On
                    SelectionState.NotSelected -> ToggleableState.Off
                    SelectionState.PartiallySelected -> ToggleableState.Indeterminate
                },
                onClick = {
                    onSelectClick()
                },
            )
        }
    }
}

private fun List<Storage.Directory>.refreshSelected() =
    asReversed().forEach { it.refreshSelected() }

private fun Storage.Directory.refreshSelected() {
    var partiallySelected = false
    var selectedCount = 0
    for (child in content) {
        when (child.selectionState()) {
            SelectionState.Selected -> {
                ++selectedCount
            }

            SelectionState.NotSelected -> {
                if (selectedCount > 0) break
            }

            SelectionState.PartiallySelected -> {
                partiallySelected = true
                break
            }
        }
    }
    selected = when {
        partiallySelected
        -> SelectionState.PartiallySelected

        selectedCount == content.size
        -> SelectionState.Selected

        selectedCount == 0 -> SelectionState.NotSelected

        else -> SelectionState.PartiallySelected
    }
}

private fun Storage.selectionState(): SelectionState =
    when (this) {
        is Storage.Directory -> selected
        is Storage.File -> if (selected) {
            SelectionState.Selected
        } else {
            SelectionState.NotSelected
        }
    }

private fun Storage.Directory.select(selected: Boolean) {
    val stack = ArrayDeque<Storage.Directory>()
    stack.add(this)

    val newState = if (selected) {
        SelectionState.Selected
    } else {
        SelectionState.NotSelected
    }

    tailrec fun go() {
        if (stack.isEmpty()) return
        val dir = stack.removeLast()
        dir.selected = newState
        dir.content.forEach {
            when (it) {
                is Storage.File -> it.selected = selected
                is Storage.Directory -> stack.add(it)
            }
        }
        go()
    }
    go()
}
