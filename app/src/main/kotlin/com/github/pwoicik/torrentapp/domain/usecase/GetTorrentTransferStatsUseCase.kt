package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.model.TorrentTransferStats
import kotlinx.coroutines.flow.Flow

typealias GetTorrentTransferStatsUseCase = UseCase<Sha1Hash, Flow<TorrentTransferStats>>
