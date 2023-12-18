package com.github.pwoicik.torrentapp.domain.model

data class SessionInfo(
    val listenPort: Int = 0,
    val dhtNodes: Long = 0,
    val downloadRate: ByteSize = ByteSize(0),
    val totalDownload: ByteSize = ByteSize(0),
    val uploadRate: ByteSize = ByteSize(0),
    val totalUpload: ByteSize = ByteSize(0),
)
