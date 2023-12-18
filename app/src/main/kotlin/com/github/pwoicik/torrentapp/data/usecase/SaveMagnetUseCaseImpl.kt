package com.github.pwoicik.torrentapp.data.usecase

import android.os.Environment
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.db.Torrent
import com.github.pwoicik.torrentapp.di.ApplicationContext
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
    private val context: ApplicationContext,
) : SaveMagnetUseCase {
    override suspend fun invoke(input: SaveMagnetInput) {
        db.torrentQueries.insert(
            Torrent(
                hash = input.info.hash.value,
                name = input.info.name,
                paused = true,
                sequential = false,
            )
        )

        session.swig().add_torrent(
            AddTorrentParams.parseMagnetUri(input.info.uri.value).apply {
                savePath = context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    .absolutePath
            }.swig(),
            error_code(),
        )
    }
}
