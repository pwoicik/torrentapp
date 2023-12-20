package com.github.pwoicik.torrentapp.di

import android.app.Activity
import android.app.Service
import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.dataStoreFile
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.pwoicik.torrentapp.MainActivity
import com.github.pwoicik.torrentapp.TorrentService
import com.github.pwoicik.torrentapp.data.datastore.SettingsDataStore
import com.github.pwoicik.torrentapp.data.datastore.SettingsSerializer
import com.github.pwoicik.torrentapp.data.usecase.GetDownloadSettingsUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.GetMagnetMetadataUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.GetSessionInfoUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.GetTorrentTransferStatsUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.GetTorrentsUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.ParseMagnetUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.SaveDownloadSettingsUseCaseImpl
import com.github.pwoicik.torrentapp.data.usecase.SaveMagnetUseCaseImpl
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.domain.usecase.GetDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetSessionInfoUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentTransferStatsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
import com.github.pwoicik.torrentapp.domain.usecase.SaveDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentContent
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentPresenter
import com.github.pwoicik.torrentapp.ui.addtorrent.AddTorrentScreen
import com.github.pwoicik.torrentapp.ui.main.MagnetInput
import com.github.pwoicik.torrentapp.ui.main.MagnetInputPresenter
import com.github.pwoicik.torrentapp.ui.main.MagnetInputScreen
import com.github.pwoicik.torrentapp.ui.main.MainContent
import com.github.pwoicik.torrentapp.ui.main.MainPresenter
import com.github.pwoicik.torrentapp.ui.main.MainScreen
import com.github.pwoicik.torrentapp.ui.main.SessionStats
import com.github.pwoicik.torrentapp.ui.main.SessionStatsPresenter
import com.github.pwoicik.torrentapp.ui.main.SessionStatsScreen
import com.github.pwoicik.torrentapp.ui.settings.SettingsContent
import com.github.pwoicik.torrentapp.ui.settings.SettingsPresenter
import com.github.pwoicik.torrentapp.ui.settings.SettingsScreen
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import kotlinx.coroutines.Dispatchers
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import okio.FileSystem
import okio.Path.Companion.toPath
import org.libtorrent4j.SessionManager
import kotlin.reflect.KClass

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

    val GetDownloadSettingsUseCaseImpl.bind: GetDownloadSettingsUseCase
        @Provides get() = this

    val SaveDownloadSettingsUseCaseImpl.bind: SaveDownloadSettingsUseCase
        @Provides get() = this

    val GetTorrentTransferStatsUseCaseImpl.bind: GetTorrentTransferStatsUseCase
        @Provides get() = this
}

interface UiComponent {
    @AppScope
    @Provides
    fun circuit(presenterFactories: Set<Presenter.Factory>, uiFactories: Set<Ui.Factory>) =
        Circuit.Builder()
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
            -> presenterOf { MainPresenter(navigator, getTorrents()) }

            is SessionStatsScreen,
            -> presenterOf { SessionStatsPresenter(navigator, getSessionInfo()) }

            is MagnetInputScreen,
            -> presenterOf { MagnetInputPresenter(navigator, parseMagnet()) }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun mainUiFactory(getTorrentTransferStats: () -> GetTorrentTransferStatsUseCase) =
        Ui.Factory { screen, _ ->
            when (screen) {
                is MainScreen,
                -> ui<MainScreen.State> { state, modifier ->
                    MainContent(state, getTorrentTransferStats(), modifier)
                }

                is SessionStatsScreen,
                -> ui<SessionStatsScreen.State> { state, modifier -> SessionStats(state, modifier) }

                is MagnetInputScreen,
                -> ui<MagnetInputScreen.State> { state, modifier -> MagnetInput(state, modifier) }

                else -> null
            }
        }

    @[Provides IntoSet]
    fun addTorrentPresenterFactory(
        getMagnetMetadata: () -> GetMagnetMetadataUseCase,
        saveMagnet: () -> SaveMagnetUseCase,
        getDownloadSettings: () -> GetDownloadSettingsUseCase,
    ) = Presenter.Factory { screen, navigator, _ ->
        when (screen) {
            is AddTorrentScreen,
            -> presenterOf {
                AddTorrentPresenter(
                    screen,
                    navigator,
                    getMagnetMetadata(),
                    saveMagnet(),
                    getDownloadSettings(),
                )
            }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun addTorrentUiFactory() =
        Ui.Factory { screen, _ ->
            when (screen) {
                is AddTorrentScreen,
                -> ui<AddTorrentScreen.State> { state, modifier ->
                    AddTorrentContent(
                        screen,
                        state,
                        modifier,
                    )
                }

                else -> null
            }
        }

    @[Provides IntoSet]
    fun settingsPresenterFactory(
        getDownloadSettings: () -> GetDownloadSettingsUseCase,
        saveDownloadSettings: () -> SaveDownloadSettingsUseCase,
    ) = Presenter.Factory { screen, _, _ ->
        when (screen) {
            is SettingsScreen,
            -> presenterOf { SettingsPresenter(getDownloadSettings(), saveDownloadSettings()) }

            else -> null
        }
    }

    @[Provides IntoSet]
    fun settingsUiFactory() =
        Ui.Factory { screen, _ ->
            when (screen) {
                is SettingsScreen,
                -> ui<SettingsScreen.State> { state, modifier -> SettingsContent(state, modifier) }

                else -> null
            }
        }
}

interface DataComponent {
    @AppScope
    @Provides
    fun database(context: ApplicationContext) =
        Database(
            AndroidSqliteDriver(
                schema = Database.Schema.synchronous(),
                context = context,
                name = "torrentapp.db",
                useNoBackupDirectory = true,
            ),
        )

    @AppScope
    @Provides
    fun datastore(context: ApplicationContext): SettingsDataStore =
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = SettingsSerializer,
            ) { context.dataStoreFile("settings.pb").absolutePath.toPath() },
        )

    @AppScope
    @Provides
    fun torrentSession() = SessionManager(false)

    val sessionManager: SessionManager
}

private typealias ActivityBinding = Pair<KClass<out Activity>, () -> Activity>
private typealias ServiceBinding = Pair<KClass<out Service>, () -> Service>

interface AndroidComponent {
    @[Provides IntoMap]
    fun mainActivity(provider: () -> MainActivity): ActivityBinding =
        MainActivity::class to provider

    @[Provides IntoMap]
    fun torrentService(provider: () -> TorrentService): ServiceBinding =
        TorrentService::class to provider

    val activities: Map<KClass<out Activity>, () -> Activity>
    val services: Map<KClass<out Service>, () -> Service>
    val workers: Map<KClass<out ListenableWorker>, (Context, WorkerParameters) -> ListenableWorker>
        get() = emptyMap()
}

@AppScope
@Component
abstract class AppComponent(
    @get:Provides protected val context: ApplicationContext,
) : UiComponent, DataComponent, UseCaseComponent, AndroidComponent {
    @Provides
    protected fun mainDispatcher(): MainDispatcher = Dispatchers.Main

    @Suppress("InjectDispatcher")
    @Provides
    protected fun defaultDispatcher(): DefaultDispatcher = Dispatchers.Default

    @Suppress("InjectDispatcher")
    @Provides
    protected fun ioDispatcher(): IoDispatcher = Dispatchers.IO
}
