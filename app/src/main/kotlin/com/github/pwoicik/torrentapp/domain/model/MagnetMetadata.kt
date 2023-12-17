package com.github.pwoicik.torrentapp.domain.model

import java.time.Instant

data class MagnetMetadata(
    val name: String,
    val creator: String?,
    val creationDate: Instant?,
    val numberOfFiles: Int,
    val totalSize: ByteSize,
    val numberOfPieces: Int,
    val pieceSize: ByteSize,
    val hash: Sha1Hash,
)
