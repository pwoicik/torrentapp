package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.SavedTorrent
import kotlinx.coroutines.flow.Flow

typealias GetTorrentsUseCase = UseCase<Unit, Flow<List<SavedTorrent>>>
