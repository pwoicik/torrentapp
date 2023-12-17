package com.github.pwoicik.torrentapp.domain.model

data class Magnet(
    val uri: MagnetUri,
    val name: String,
    val hash: Sha1Hash,
)
