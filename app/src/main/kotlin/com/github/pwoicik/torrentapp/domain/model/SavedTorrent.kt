package com.github.pwoicik.torrentapp.domain.model

data class SavedTorrent(
    val hash: Sha1Hash,
    val name: String,
    val startPaused: Boolean,
)
