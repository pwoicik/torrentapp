package com.github.pwoicik.torrentapp.domain.usecase

import com.github.pwoicik.torrentapp.proto.Settings

typealias SaveDownloadSettingsUseCase = SuspendUseCase<Settings.Download, Unit>
