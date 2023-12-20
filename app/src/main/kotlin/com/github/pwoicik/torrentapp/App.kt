package com.github.pwoicik.torrentapp

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkerParameters
import com.github.pwoicik.torrentapp.di.AppComponent
import com.github.pwoicik.torrentapp.di.create

class App : Application(), Configuration.Provider {
    val appComponent = AppComponent::class.create(this)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(WorkerFactory())
            .build()

    private inner class WorkerFactory : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ) = Class.forName(workerClassName).kotlin
            .let(appComponent.workers::get)!!
            .invoke(appContext, workerParameters)
    }
}
