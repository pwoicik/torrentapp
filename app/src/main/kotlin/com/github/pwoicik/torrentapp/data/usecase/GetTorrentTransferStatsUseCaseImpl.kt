package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.model.TorrentState
import com.github.pwoicik.torrentapp.domain.model.TorrentTransferStats
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentTransferStatsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentStatus
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@Inject
class GetTorrentTransferStatsUseCaseImpl(
    private val session: SessionManager,
) : GetTorrentTransferStatsUseCase {
    override fun invoke(input: Sha1Hash): Flow<TorrentTransferStats> =
        flow {
            val sha = org.libtorrent4j.Sha1Hash.parseHex(input.value)
            var handle: TorrentHandle? = null
            while (coroutineContext.isActive) {
                if (handle == null) {
                    handle = session.find(sha)
                }
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
        }.conflate()

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
}
