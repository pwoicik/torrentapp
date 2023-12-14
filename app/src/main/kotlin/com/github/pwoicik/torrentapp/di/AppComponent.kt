package com.github.pwoicik.torrentapp.di

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrent
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentPresenter
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.github.pwoicik.torrentapp.ui.main.Main
import com.github.pwoicik.torrentapp.ui.main.MainPresenter
import com.github.pwoicik.torrentapp.ui.main.MainScreen
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

interface UiComponent {
    @[Provides IntoSet]
    fun mainPresenterFactory() = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is MainScreen -> presenterOf { MainPresenter(screen, navigator) }
            else -> null
        }
    }

    @[Provides IntoSet]
    fun mainUiFactory() = Ui.Factory { screen, _ ->
        when (screen) {
            is MainScreen,
            -> ui<MainScreen.State> { state, modifier -> Main(state, modifier) }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun addTorrentPresenterFactory() = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is AddTorrentScreen -> presenterOf { AddTorrentPresenter(screen, navigator) }
            else -> null
        }
    }

    @[Provides IntoSet]
    fun addTorrentUiFactory() = Ui.Factory { screen, _ ->
        when (screen) {
            is AddTorrentScreen,
            -> ui<AddTorrentScreen.State> { state, modifier -> AddTorrent(state, modifier) }

            else -> null
        }
    }
}

interface NetComponent {
    @AppScope
    @Provides
    fun abc() = CoroutineScope(Dispatchers.IO).embeddedServer(Netty) {}
}

interface DbComponent {
    @AppScope
    @Provides
    fun driver(context: Context): SqlDriver = AndroidSqliteDriver(
        schema = Database.Schema.synchronous(),
        context = context,
        name = "torrentapp.db",
    )

    @AppScope
    @Provides
    fun database(driver: SqlDriver) = Database(driver)
}

@AppScope
@Component
abstract class AppComponent(
    @get:Provides protected val appContext: Context,
) : UiComponent, DbComponent {
    @AppScope
    @Provides
    protected fun circuit(
        presenterFactories: Set<Presenter.Factory>,
        uiFactories: Set<Ui.Factory>,
    ) = Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
        .build()

    abstract val circuit: Circuit
}
