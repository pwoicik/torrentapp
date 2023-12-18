package com.github.pwoicik.torrentapp.domain.usecase

import arrow.core.Either
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo

typealias ParseMagnetUseCase = UseCase<String, Either<ParseMagnetError, MagnetInfo>>

sealed interface ParseMagnetError {
    data object MagnetInvalid : ParseMagnetError
}
