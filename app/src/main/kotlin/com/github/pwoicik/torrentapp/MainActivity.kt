package com.github.pwoicik.torrentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.github.pwoicik.torrentapp.di.inject
import com.github.pwoicik.torrentapp.ui.main.MainScreen
import com.github.pwoicik.torrentapp.ui.theme.TorrentAppTheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration

class MainActivity : ComponentActivity() {
    private val circuit by inject { circuit }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            TorrentAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val backstack = rememberSaveableBackStack { push(MainScreen) }
                    val circuitNavigator = rememberCircuitNavigator(backstack)
                    val navigator = rememberAndroidScreenAwareNavigator(circuitNavigator, this)
                    CircuitCompositionLocals(circuit) {
                        ContentWithOverlays {
                            NavigableCircuitContent(
                                navigator = navigator,
                                backstack = backstack,
                                decoration = GestureNavigationDecoration { navigator.pop() }
                            )
                        }
                    }
                }
            }
        }
    }
}
