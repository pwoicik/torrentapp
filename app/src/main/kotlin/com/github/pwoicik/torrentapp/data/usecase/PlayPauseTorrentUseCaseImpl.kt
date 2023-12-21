package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.domain.usecase.PlayPauseTorrentInput
import com.github.pwoicik.torrentapp.domain.usecase.PlayPauseTorrentUseCase
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.Sha1Hash

@Inject
class PlayPauseTorrentUseCaseImpl(
    private val session: SessionManager,
    private val db: Database,
) : PlayPauseTorrentUseCase {
    override suspend fun invoke(input: PlayPauseTorrentInput) {
        val handle = session.find(Sha1Hash.parseHex(input.hash.value)) ?: return
        when (input.action) {
            PlayPauseTorrentInput.Action.Play -> handle.resume()
            PlayPauseTorrentInput.Action.Pause -> handle.pause()
        }
        db.torrentQueries.updateState(
            when (input.action) {
                PlayPauseTorrentInput.Action.Play -> false
                PlayPauseTorrentInput.Action.Pause -> true
            },
            input.hash.value,
        )
    }
}
