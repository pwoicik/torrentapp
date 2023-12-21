package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.db.Torrent
import com.github.pwoicik.torrentapp.di.IoDispatcher
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetInput
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.ErrorCode
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.swig.error_code

@Inject
class SaveMagnetUseCaseImpl(
    private val db: Database,
    private val session: SessionManager,
    private val ioDispatcher: IoDispatcher,
) : SaveMagnetUseCase {
    override suspend fun invoke(input: SaveMagnetInput) {
        db.torrentQueries.insert(
            Torrent(
                hash = input.info.hash.value,
                name = input.info.name,
                paused = !input.startImmediately,
                sequential = input.sequential,
                savePath = input.savePath,
            ),
        )

        val handle = withContext(ioDispatcher) {
            SessionHandle(session.swig()).addTorrent(
                AddTorrentParams.parseMagnetUri(input.info.uri.value).apply {
                    savePath = input.savePath
                    flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
                },
                ErrorCode(error_code()),
            )
        }
        if (input.startImmediately) {
            handle.resume()
        }
    }
}
