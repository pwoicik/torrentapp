package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata

typealias SaveMagnetUseCase = SuspendUseCase<SaveMagnetInput, Unit>

data class SaveMagnetInput(
    val info: MagnetInfo,
    val metadata: MagnetMetadata? = null,
    val startImmediately: Boolean,
    val sequential: Boolean,
    val prioritizeFirstAndLast: Boolean,
    val savePath: String,
)
