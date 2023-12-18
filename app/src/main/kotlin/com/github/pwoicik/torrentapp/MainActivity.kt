package com.github.pwoicik.torrentapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.pwoicik.torrentapp.di.inject
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.github.pwoicik.torrentapp.ui.main.MainScreen
import com.github.pwoicik.torrentapp.ui.theme.TorrentAppTheme
import com.github.pwoicik.torrentapp.ui.util.current
import com.github.pwoicik.torrentapp.util.registerReceiver
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration
import me.tatarka.inject.annotations.Inject

class MainActivity : ComponentActivity() {
    private val delegate by inject { mainActivityDelegate }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, TorrentService::class.java))
        registerFinishReceiver()
        delegate.onIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { delegate.Content() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let(delegate::onIntent)
    }

    private fun registerFinishReceiver() {
        val receiver = registerReceiver(ApplicationConstants.ACTION_FINISH) {
            finishAndRemoveTask()
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                unregisterReceiver(receiver)
            }
        })
    }
}

@Inject
class MainActivityDelegate(
    private val circuit: Circuit,
    private val parseMagnet: ParseMagnetUseCase,
) {
    private val magnet = mutableStateOf<MagnetInfo?>(null)

    @Composable
    fun Content() {
        TorrentAppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val backstack = rememberSaveableBackStack { push(MainScreen) }
                val circuitNavigator = rememberCircuitNavigator(backstack)
                val navigator =
                    rememberAndroidScreenAwareNavigator(circuitNavigator, LocalContext.current)
                CircuitCompositionLocals(circuit) {
                    ContentWithOverlays {
                        NavigableCircuitContent(
                            navigator = navigator,
                            backstack = backstack,
                            decoration = GestureNavigationDecoration { navigator.pop() }
                        )
                    }
                }
                LaunchedEffect(magnet.value) f@{
                    val mag = magnet.value ?: return@f
                    (backstack.current as? AddTorrentScreen)?.let {
                        if (it.magnet.uri == mag.uri) {
                            return@f
                        }
                    }
                    magnet.value = null
                    navigator.goTo(AddTorrentScreen(mag))
                }
            }
        }
    }

    fun onIntent(intent: Intent) {
        when {
            intent.scheme == "magnet" -> {
                magnet.value = parseMagnet(intent.toUri(0)).getOrNull()
            }
        }
    }
}
