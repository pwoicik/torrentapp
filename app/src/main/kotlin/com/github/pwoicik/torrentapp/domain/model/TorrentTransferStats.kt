package com.github.pwoicik.torrentapp.domain.model

data class TorrentTransferStats(
    val progress: Float = 0f,
    val downloadSpeed: ByteSize = ByteSize(),
    val downloaded: ByteSize = ByteSize(),
    val remaining: ByteSize = ByteSize(),
    val uploadSpeed: ByteSize = ByteSize(),
    val uploaded: ByteSize = ByteSize(),
)
