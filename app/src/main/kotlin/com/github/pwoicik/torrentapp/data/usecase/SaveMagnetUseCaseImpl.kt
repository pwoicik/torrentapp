package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.db.Torrent
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetInput
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.SessionManager
import org.libtorrent4j.swig.error_code

@Inject
class SaveMagnetUseCaseImpl(
    private val db: Database,
    private val session: SessionManager,
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

        if (!input.startImmediately) return
        session.swig().add_torrent(
            AddTorrentParams.parseMagnetUri(input.info.uri.value).apply {
                savePath = input.savePath
            }.swig(),
            error_code(),
        )
    }
}
