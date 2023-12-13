package com.github.pwoicik.torrentapp.di

import android.content.Context
import com.github.pwoicik.torrentapp.App

fun <T> Context.inject(select: AppComponent.() -> T) =
    lazy(LazyThreadSafetyMode.NONE) { appComponent.select() }

private val Context.appComponent get() = (applicationContext as App).appComponent
