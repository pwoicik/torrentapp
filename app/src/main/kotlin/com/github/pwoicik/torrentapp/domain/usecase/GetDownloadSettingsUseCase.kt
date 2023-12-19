package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.proto.Settings

typealias GetDownloadSettingsUseCase = SuspendUseCase<Unit, Settings.Download>
