package com.github.pwoicik.torrentapp.domain.usecase

import arrow.core.Either
import com.github.pwoicik.torrentapp.domain.model.Magnet
import com.github.pwoicik.torrentapp.domain.model.MagnetUri

typealias ParseMagnetUseCase = UseCase<MagnetUri, Either<ParseMagnetError, Magnet>>

sealed interface ParseMagnetError {
    data object MagnetInvalid : ParseMagnetError
}
