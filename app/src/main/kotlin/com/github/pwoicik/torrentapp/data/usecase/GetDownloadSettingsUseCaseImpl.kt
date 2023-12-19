package com.github.pwoicik.torrentapp.data.usecase

import androidx.datastore.core.DataStore
import com.github.pwoicik.torrentapp.domain.usecase.GetDownloadSettingsUseCase
import com.github.pwoicik.torrentapp.proto.Settings
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class GetDownloadSettingsUseCaseImpl(
    private val store: DataStore<Settings>,
) : GetDownloadSettingsUseCase {
    override suspend fun invoke(input: Unit): Settings.Download {
        return store.data.first().download
    }
}
