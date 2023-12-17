package com.github.pwoicik.torrentapp.di

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.pwoicik.torrentapp.MainActivityDelegate
import com.github.pwoicik.torrentapp.data.usecase.GetMagnetMetadataUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.ParseMagnetUseCaseImpl
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
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
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import org.libtorrent4j.SessionManager

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

interface UseCaseComponent {
    val ParseMagnetUseCaseImpl.bind: ParseMagnetUseCase
        @Provides get() = this

    val GetMagnetMetadataUseCaseImpl.bind: GetMagnetMetadataUseCase
        @Provides get() = this
}

interface UiComponent {
    @AppScope
    @Provides
    fun circuit(
        presenterFactories: Set<Presenter.Factory>,
        uiFactories: Set<Ui.Factory>,
    ) = Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
        .build()

    @[Provides IntoSet]
    fun mainPresenterFactory(
        parseMagnet: () -> ParseMagnetUseCase,
    ) = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is MainScreen -> presenterOf { MainPresenter(parseMagnet(), navigator) }
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
    fun addTorrentPresenterFactory(
        getMagnetMetadata: () -> GetMagnetMetadataUseCase,
    ) = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is AddTorrentScreen,
            -> presenterOf { AddTorrentPresenter(getMagnetMetadata(), screen, navigator) }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun addTorrentUiFactory() = Ui.Factory { screen, _ ->
        when (screen) {
            is AddTorrentScreen,
            -> ui<AddTorrentScreen.State> { state, modifier -> AddTorrent(screen, state, modifier) }

            else -> null
        }
    }
}

interface NetComponent {
    @AppScope
    @Provides
    fun torrentSession() = SessionManager(false)

    val sessionManager: SessionManager
}

interface DbComponent {
    @AppScope
    @Provides
    fun driver(context: ApplicationContext): SqlDriver = AndroidSqliteDriver(
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
    @get:Provides protected val context: ApplicationContext,
) : UiComponent, DbComponent, NetComponent, UseCaseComponent {
    abstract val mainActivityDelegate: MainActivityDelegate
}
