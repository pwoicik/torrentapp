package com.github.pwoicik.torrentapp.data.usecase

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.di.IoDispatcher
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.model.Torrent
import com.github.pwoicik.torrentapp.domain.model.TorrentState
import com.github.pwoicik.torrentapp.domain.model.TorrentTransferStats
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentStatus
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@Inject
class GetTorrentsUseCaseImpl(
    private val db: Database,
    private val session: SessionManager,
    private val ioDispatcher: IoDispatcher,
) : GetTorrentsUseCase {
    override fun invoke(input: Unit): Flow<List<Torrent>> =
        db.torrentQueries.getTorrentBasicInfos().asFlow()
            .map {
                withContext(ioDispatcher) {
                    it.awaitAsList().map {
                        TorrentImpl(
                            hash = Sha1Hash(it.hash),
                            name = it.name,
                            session = session,
                            db = db,
                        )
                    }
                }
            }
}

private class TorrentImpl(
    override val hash: Sha1Hash,
    override val name: String,
    private val session: SessionManager,
    private val db: Database,
) : Torrent {
    private val handle by nullableLazy {
        session.find(org.libtorrent4j.Sha1Hash.parseHex(hash.value))
    }

    override val transferStats: Flow<TorrentTransferStats> = flow {
        while (coroutineContext.isActive) {
            val counters = try {
                handle?.status(TorrentHandle.QUERY_ACCURATE_DOWNLOAD_COUNTERS)
            } catch (_: RuntimeException) {
                null
            }
            val stats = if (counters == null) {
                TorrentTransferStats()
            } else {
                TorrentTransferStats(
                    state = when {
                        !session.isRunning
                        -> TorrentState.Stopped

                        session.isPaused
                        -> TorrentState.Paused

                        counters.flags().and_(TorrentFlags.PAUSED).non_zero()
                        -> TorrentState.Paused

                        else -> counters.state().toTorrentState()
                    },
                    progress = counters.progress(),
                    downloadSpeed = ByteSize(counters.downloadPayloadRate().toLong()),
                    downloaded = ByteSize(counters.totalWantedDone()),
                    remaining = ByteSize(counters.totalWanted() - counters.totalWantedDone()),
                    uploadSpeed = ByteSize(counters.uploadPayloadRate().toLong()),
                    uploaded = ByteSize(counters.allTimeUpload()),
                )
            }
            emit(stats)
            delay(1.seconds)
        }
    }

    override suspend fun pause() {
        handle?.pause()
        db.torrentQueries.updateState(
            paused = true,
            hash = hash.value,
        )
    }

    override suspend fun resume() {
        handle?.resume()
        db.torrentQueries.updateState(
            paused = false,
            hash = hash.value,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TorrentImpl) return false
        if (this === other) return true
        return hash == other.hash && name == other.name
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

/**
 * Returns an implementation of [Lazy] that runs the [initializer] every time
 * the [Lazy.value] is requested but null. If it returns a non-null value, it is saved and any
 * subsequent calls to [getValue] do not invoke [initializer].
 */
private fun <T : Any> nullableLazy(initializer: () -> T?) = NullableLazy(initializer)

private class NullableLazy<T : Any>(initializer: () -> T?) : Lazy<T?> {
    private var initializer: (() -> T?)? = initializer

    @Volatile
    private var _value: T? = null

    override val value: T?
        get() {
            val v1 = _value
            if (v1 !== null) {
                return v1
            }
            return synchronized(this) {
                val v2 = _value
                if (v2 !== null) {
                    v2
                } else {
                    val newValue = initializer?.invoke()
                        ?.also { _value = it }
                    initializer = null
                    newValue
                }
            }
        }

    override fun isInitialized() = _value !== null
}

private fun TorrentStatus.State.toTorrentState(): TorrentState =
    when (this) {
        TorrentStatus.State.CHECKING_FILES -> TorrentState.CheckingFiles
        TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DownloadingMetadata
        TorrentStatus.State.DOWNLOADING -> TorrentState.Downloading
        TorrentStatus.State.FINISHED -> TorrentState.Finished
        TorrentStatus.State.SEEDING -> TorrentState.Seeding
        TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CheckingResumeData
        TorrentStatus.State.UNKNOWN -> TorrentState.Paused
    }
