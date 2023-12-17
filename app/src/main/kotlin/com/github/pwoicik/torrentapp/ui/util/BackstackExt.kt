package com.github.pwoicik.torrentapp.ui.util

import com.slack.circuit.backstack.BackStack

val <T : BackStack.Record> BackStack<T>.current get() = topRecord!!.screen
