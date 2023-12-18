package com.github.pwoicik.torrentapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

inline fun Context.registerReceiver(
    intentFilter: IntentFilter,
    flags: Int = ContextCompat.RECEIVER_NOT_EXPORTED,
    crossinline callback: () -> Unit,
): BroadcastReceiver {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            callback()
        }
    }
    ContextCompat.registerReceiver(
        this,
        receiver,
        intentFilter,
        flags,
    )
    return receiver
}

inline fun Context.registerReceiver(
    action: String,
    priority: Int = 0,
    crossinline callback: () -> Unit,
) = registerReceiver(
    intentFilter = IntentFilter(action).apply { this.priority = priority },
    callback = callback,
)
