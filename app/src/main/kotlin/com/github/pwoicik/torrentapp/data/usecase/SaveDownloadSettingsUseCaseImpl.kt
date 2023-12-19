package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.data.datastore.SettingsDataStore
import com.github.pwoicik.torrentapp.domain.usecase.SaveDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.proto.Settings
import me.tatarka.inject.annotations.Inject

@Inject
class SaveDownloadSettingsUseCaseImpl(
    private val store: SettingsDataStore,
) : SaveDownloadSettingsUseCase {
    override suspend fun invoke(input: Settings.Download) {
        store.updateData { it.copy(download = input) }
    }
}
