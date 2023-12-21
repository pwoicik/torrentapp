package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.Sha1Hash

typealias PlayPauseTorrentUseCase = SuspendUseCase<PlayPauseTorrentInput, Unit>

data class PlayPauseTorrentInput(
    val action: Action,
    val hash: Sha1Hash,
) {
    enum class Action {
        Play,
        Pause,
    }
}
