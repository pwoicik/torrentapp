package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.SessionInfo
import kotlinx.coroutines.flow.Flow

typealias GetSessionInfoUseCase = UseCase<Unit, Flow<SessionInfo>>
