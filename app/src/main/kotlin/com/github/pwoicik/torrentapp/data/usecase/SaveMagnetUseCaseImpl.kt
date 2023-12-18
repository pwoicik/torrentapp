package com.github.pwoicik.torrentapp.data.usecase

import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.db.Torrent
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetInput
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import me.tatarka.inject.annotations.Inject

@Inject
class SaveMagnetUseCaseImpl(
    private val db: Database,
) : SaveMagnetUseCase {
    override suspend fun invoke(input: SaveMagnetInput) {
        when (input) {
            is SaveMagnetInput.Info -> db.torrentQueries.insert(
                Torrent(input.value.hash.value, input.value.name, false)
            )

            is SaveMagnetInput.Metadata,
            -> db.torrentQueries.insert(
                Torrent(input.value.hash.value, input.value.name, false)
            )
        }
    }
}
