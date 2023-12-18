package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata

typealias SaveMagnetUseCase = SuspendUseCase<SaveMagnetInput, Unit>

sealed interface SaveMagnetInput {
    @JvmInline
    value class Info(val value: MagnetInfo) : SaveMagnetInput

    @JvmInline
    value class Metadata(val value: MagnetMetadata) : SaveMagnetInput
}
