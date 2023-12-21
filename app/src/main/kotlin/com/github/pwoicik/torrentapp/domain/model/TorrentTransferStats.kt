package com.github.pwoicik.torrentapp.domain.model

data class TorrentTransferStats(
    val state: TorrentState = TorrentState.Paused,
    val progress: Float = 0f,
    val downloadSpeed: ByteSize = ByteSize(),
    val downloaded: ByteSize = ByteSize(),
    val remaining: ByteSize = ByteSize(),
    val uploadSpeed: ByteSize = ByteSize(),
    val uploaded: ByteSize = ByteSize(),
)

enum class TorrentState {
    CheckingFiles,
    Downloading,
    DownloadingMetadata,
    Seeding,
    CheckingResumeData,
    Finished,
    Paused,
    Stopped,
    ;

    val isPaused get() = this == Paused || this == Stopped
}
