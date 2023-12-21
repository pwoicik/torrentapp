package com.github.pwoicik.torrentapp.data

import com.github.pwoicik.torrentapp.di.AppScope
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.SessionInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionStats
import org.libtorrent4j.swig.session
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@Inject
@AppScope
class SessionInfoRepository(
    session: SessionManager,
) {
    private val info = flow {
        while (coroutineContext.isActive) {
            val swig: session? = session.swig()
            val stats: SessionStats = session.stats()
            emit(
                SessionInfo(
                    listenPort = swig?.listen_port() ?: 0,
                    dhtNodes = stats.dhtNodes(),
                    downloadRate = ByteSize(stats.downloadRate()),
                    totalDownload = ByteSize(stats.totalDownload()),
                    uploadRate = ByteSize(stats.uploadRate()),
                    totalUpload = ByteSize(stats.totalUpload()),
                ),
            )
            delay(1.seconds)
        }
    }.shareIn(
        scope = @OptIn(DelicateCoroutinesApi::class) GlobalScope,
        started = SharingStarted.Lazily,
        replay = 1,
    )

    fun getInfo(): Flow<SessionInfo> = info
}
