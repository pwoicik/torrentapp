package com.github.pwoicik.torrentapp.data.usecase

import arrow.core.Either
import arrow.core.right
import com.github.pwoicik.torrentapp.di.ApplicationContext
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.Magnet
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataError
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentInfo
import java.time.Instant

@Inject
class GetMagnetMetadataUseCaseImpl(
    private val session: SessionManager,
    private val context: ApplicationContext,
) : GetMagnetMetadataUseCase {
    override suspend fun invoke(input: Magnet): Either<GetMagnetMetadataError, MagnetMetadata> {
        val info = withContext(Dispatchers.IO) {
            session.fetchMagnet(
                input.uri.uri,
                Int.MAX_VALUE,
                context.cacheDir,
            ).let(TorrentInfo::bdecode)
        }
        val hash = info.infoHashes().best.toHex()
        return MagnetMetadata(
            name = info.name()?.takeIf { it.isNotBlank() } ?: hash,
            creator = info.creator()?.takeIf { it.isNotBlank() },
            creationDate = info.creationDate()
                .takeIf { it != 0L }
                ?.let(Instant::ofEpochSecond),
            numberOfFiles = info.numFiles(),
            totalSize = info.totalSize().let(::ByteSize),
            numberOfPieces = info.numPieces(),
            pieceSize = info.pieceLength().toLong().let(::ByteSize),
            hash = hash.let(::Sha1Hash),
        ).right()
    }
}
