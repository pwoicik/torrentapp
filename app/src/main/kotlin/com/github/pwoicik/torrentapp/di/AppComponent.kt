package com.github.pwoicik.torrentapp.di

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.pwoicik.torrentapp.MainActivityDelegate
import com.github.pwoicik.torrentapp.data.usecase.GetMagnetMetadataUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.GetSessionInfoUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.GetTorrentsUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.ParseMagnetUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.SaveMagnetUseCaseImpl
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetSessionInfoUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrent
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentPresenter
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.github.pwoicik.torrentapp.ui.main.Main
import com.github.pwoicik.torrentapp.ui.main.MainPresenter
import com.github.pwoicik.torrentapp.ui.main.MainScreen
import com.github.pwoicik.torrentapp.ui.main.SessionStats
import com.github.pwoicik.torrentapp.ui.main.SessionStatsPresenter
import com.github.pwoicik.torrentapp.ui.main.SessionStatsScreen
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

    val SaveMagnetUseCaseImpl.bind: SaveMagnetUseCase
        @Provides get() = this

    val GetTorrentsUseCaseImpl.bind: GetTorrentsUseCase
        @Provides get() = this

    val GetSessionInfoUseCaseImpl.bind: GetSessionInfoUseCase
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
        getTorrents: () -> GetTorrentsUseCase,
        getSessionInfo: () -> GetSessionInfoUseCase,
    ) = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is MainScreen,
            -> presenterOf { MainPresenter(navigator, parseMagnet(), getTorrents()) }

            is SessionStatsScreen,
            -> presenterOf { SessionStatsPresenter(navigator, getSessionInfo()) }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun mainUiFactory(
        session: () -> SessionManager,
    ) = Ui.Factory { screen, _ ->
        when (screen) {
            is MainScreen,
            -> ui<MainScreen.State> { state, modifier -> Main(state, modifier) }

            is SessionStatsScreen,
            -> ui<SessionStatsScreen.State> { state, modifier -> SessionStats(state, modifier) }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun addTorrentPresenterFactory(
        getMagnetMetadata: () -> GetMagnetMetadataUseCase,
        saveMagnet: () -> SaveMagnetUseCase,
    ) = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is AddTorrentScreen,
            -> presenterOf {
                AddTorrentPresenter(
                    screen,
                    navigator,
                    getMagnetMetadata(),
                    saveMagnet()
                )
            }

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
    fun database(context: ApplicationContext) = Database(
        AndroidSqliteDriver(
            schema = Database.Schema.synchronous(),
            context = context,
            name = "torrentapp.db",
            useNoBackupDirectory = true,
        ),
    )
}

@AppScope
@Component
abstract class AppComponent(
    @get:Provides protected val context: ApplicationContext,
) : UiComponent, DbComponent, NetComponent, UseCaseComponent {
    abstract val mainActivityDelegate: MainActivityDelegate
}
