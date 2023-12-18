package com.github.pwoicik.torrentapp.domain.usecase

import kotlinx.coroutines.flow.Flow

typealias GetTorrentsUseCase = UseCase<Unit, Flow<Any>>
