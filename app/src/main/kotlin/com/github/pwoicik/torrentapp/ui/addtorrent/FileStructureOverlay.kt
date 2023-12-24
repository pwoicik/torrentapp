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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata
import com.github.pwoicik.torrentapp.domain.model.Storage
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import kotlinx.collections.immutable.ImmutableList

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
            FileStructure(storage = Storage.Directory("", info.files))
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
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { }
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) { currentNode ->
        FileStructure(
            storage = currentNode.content,
            onGoUp = { backPressDispatcher?.onBackPressed() },
            onOpenDirectory = {
                navigatingUp = false
                backstack.add(it)
            },
        )
    }
}

@Composable
private fun FileStructure(
    storage: ImmutableList<Storage>,
    onGoUp: () -> Unit,
    onOpenDirectory: (Storage.Directory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button) { onGoUp() }
                    .padding(12.dp),
            ) {
                Icon(imageVector = Icons.Default.KeyboardDoubleArrowUp, contentDescription = null)
                Text("(Go up)")
            }
        }
        items(storage) {
            when (it) {
                is Storage.File -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = it.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is Storage.Directory -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onOpenDirectory(it) }
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = it.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
