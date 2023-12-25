package com.github.pwoicik.torrentapp.domain.usecase

import arrow.core.Either
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata

typealias SaveMagnetUseCase = SuspendUseCase<SaveMagnetInput, Either<SaveMagnetError, Unit>>

data class SaveMagnetInput(
    val info: MagnetInfo,
    val metadata: MagnetMetadata? = null,
    val startImmediately: Boolean,
    val sequential: Boolean,
    val prioritizeFirstAndLast: Boolean,
    val savePath: String,
)

sealed interface SaveMagnetError {
    data object FileAlreadyExists : SaveMagnetError
    data class UnknownError(val message: String) : SaveMagnetError
}
