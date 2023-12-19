package com.github.pwoicik.torrentapp.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A side effect of composition that runs when it leaves the composition.
 * [effect] is launched inside [GlobalScope], so it must only perform short tasks.
 */
@DelicateCoroutinesApi
@Composable
fun OnDisposedEffect(effect: suspend () -> Unit) {
    DisposableEffect(Unit) {
        onDispose {
            GlobalScope.launch { effect() }
        }
    }
}
