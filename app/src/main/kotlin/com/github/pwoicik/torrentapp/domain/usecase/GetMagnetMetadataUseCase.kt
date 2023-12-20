package com.github.pwoicik.torrentapp.domain.usecase

import arrow.core.Either
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata

typealias GetMagnetMetadataUseCase = SuspendUseCase<
    MagnetInfo,
    Either<GetMagnetMetadataError, MagnetMetadata>,
    >

sealed interface GetMagnetMetadataError {
    // TODO: error handling
}
