package com.github.pwoicik.torrentapp.domain.usecase

import arrow.core.Either
import com.github.pwoicik.torrentapp.domain.model.Magnet
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata

typealias GetMagnetMetadataUseCase = SuspendUseCase<Magnet, Either<GetMagnetMetadataError, MagnetMetadata>>

sealed interface GetMagnetMetadataError {

}
