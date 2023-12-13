package com.github.pwoicik.torrentapp.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data object MainScreen : Screen {
    data class State(
        val event: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent
}

@Composable
fun MainPresenter(screen: MainScreen): MainScreen.State {
    return MainScreen.State {
    }
}

@Composable
fun Main(
    state: MainScreen.State,
    modifier: Modifier = Modifier,
) {
}
