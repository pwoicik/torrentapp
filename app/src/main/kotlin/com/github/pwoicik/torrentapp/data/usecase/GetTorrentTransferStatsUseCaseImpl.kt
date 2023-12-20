package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.model.TorrentTransferStats
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentTransferStatsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@Inject
class GetTorrentTransferStatsUseCaseImpl(
    private val session: SessionManager,
) : GetTorrentTransferStatsUseCase {
    override fun invoke(input: Sha1Hash): Flow<TorrentTransferStats> =
        flow {
            val sha = org.libtorrent4j.Sha1Hash.parseHex(input.value)
            var handle = session.find(sha)
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
}
