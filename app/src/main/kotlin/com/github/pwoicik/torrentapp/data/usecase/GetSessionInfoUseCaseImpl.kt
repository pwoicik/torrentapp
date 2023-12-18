package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.SessionInfo
import com.github.pwoicik.torrentapp.domain.usecase.GetSessionInfoUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@Inject
class GetSessionInfoUseCaseImpl(
    private val session: SessionManager,
) : GetSessionInfoUseCase {
    override fun invoke(input: Unit) = flow {
        val swig = session.swig()
        val stats = session.stats()
        while (coroutineContext.isActive) {
            emit(
                SessionInfo(
                    listenPort = swig.listen_port(),
                    dhtNodes = stats.dhtNodes(),
                    downloadRate = ByteSize(stats.downloadRate()),
                    totalDownload = ByteSize(stats.totalDownload()),
                    uploadRate = ByteSize(stats.uploadRate()),
                    totalUpload = ByteSize(stats.totalUpload()),
                ),
            )
            delay(1.seconds)
        }
    }.conflate()
}
