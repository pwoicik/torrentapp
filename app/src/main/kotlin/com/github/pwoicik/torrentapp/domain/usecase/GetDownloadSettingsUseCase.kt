package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.proto.Settings
import kotlinx.coroutines.flow.Flow

typealias GetDownloadSettingsUseCase = UseCase<Unit, Flow<Settings.Download>>
