package com.github.pwoicik.torrentapp.data.usecase

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.di.IoDispatcher
import com.github.pwoicik.torrentapp.domain.model.SavedTorrent
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@Inject
class GetTorrentsUseCaseImpl(
    private val db: Database,
    private val ioDispatcher: IoDispatcher,
) : GetTorrentsUseCase {
    override fun invoke(input: Unit) =
        db.torrentQueries.getTorrentBasicInfos().asFlow()
            .map {
                withContext(ioDispatcher) {
                    it.awaitAsList().map {
                        SavedTorrent(
                            hash = Sha1Hash(it.hash),
                            name = it.name,
                            startPaused = it.paused,
                        )
                    }
                }
            }
}
