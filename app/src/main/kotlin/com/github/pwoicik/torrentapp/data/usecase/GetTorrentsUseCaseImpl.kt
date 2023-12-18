package com.github.pwoicik.torrentapp.data.usecase

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.domain.usecase.GetTorrentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetTorrentsUseCaseImpl(
    private val db: Database,
) : GetTorrentsUseCase {
    override fun invoke(input: Unit): Flow<Any> {
        return db.torrentQueries.selectAll().asFlow().mapToList(Dispatchers.IO)
    }
}
