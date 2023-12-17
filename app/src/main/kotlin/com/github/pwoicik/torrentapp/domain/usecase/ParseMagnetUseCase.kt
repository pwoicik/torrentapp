package com.github.pwoicik.torrentapp.domain.usecase

import arrow.core.Either
import com.github.pwoicik.torrentapp.domain.model.Magnet

typealias ParseMagnetUseCase = UseCase<String, Either<ParseMagnetError, Magnet>>

sealed interface ParseMagnetError {
    data object MagnetInvalid : ParseMagnetError
}
