package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.data.datastore.SettingsDataStore
import com.github.pwoicik.torrentapp.domain.usecase.GetDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.proto.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class GetDownloadSettingsUseCaseImpl(
    private val store: SettingsDataStore,
) : GetDownloadSettingsUseCase {
    override fun invoke(input: Unit): Flow<Settings.Download> {
        return store.data.map { it.download }
    }
}
