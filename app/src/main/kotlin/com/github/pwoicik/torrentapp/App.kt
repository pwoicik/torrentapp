package com.github.pwoicik.torrentapp

import android.app.Application
import com.github.pwoicik.torrentapp.di.AppComponent
import com.github.pwoicik.torrentapp.di.create

class App : Application() {
    val appComponent = AppComponent::class.create(this)
}
