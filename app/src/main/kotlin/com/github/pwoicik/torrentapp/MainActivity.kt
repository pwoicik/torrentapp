package com.github.pwoicik.torrentapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.pwoicik.torrentapp.di.inject
import com.github.pwoicik.torrentapp.domain.model.MagnetUri
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
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

    private var magnet by mutableStateOf<MagnetUri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerFinishReceiver()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        magnet = magnetFromIntent(intent)

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
                    LaunchedEffect(magnet) {
                        magnet?.let {
                            magnet = null
                            navigator.goTo(AddTorrentScreen(it))
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        magnet = intent?.let(::magnetFromIntent)
    }

    private fun magnetFromIntent(intent: Intent): MagnetUri? {
        if (intent.scheme == "magnet") {
            return intent.toUri(0).let(::MagnetUri)
        }
        return null
    }

    private fun registerFinishReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                finishAffinity()
            }
        }
        val filter = IntentFilter(ApplicationConstants.ACTION_FINISH)
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                unregisterReceiver(receiver)
            }
        })
    }
}
