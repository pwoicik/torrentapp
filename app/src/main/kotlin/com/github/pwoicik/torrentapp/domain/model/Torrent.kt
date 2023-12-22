package com.github.pwoicik.torrentapp.domain.model

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

@Stable
interface Torrent {
    val hash: Sha1Hash

    val name: String

    val transferStats: Flow<TorrentTransferStats>

    suspend fun pause()

    suspend fun resume()
}
