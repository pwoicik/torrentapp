package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.Torrent
import kotlinx.coroutines.flow.Flow

typealias GetTorrentsUseCase = UseCase<Unit, Flow<List<Torrent>>>
