package com.github.pwoicik.torrentapp.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.pwoicik.torrentapp.domain.model.Magnet
import com.github.pwoicik.torrentapp.domain.model.MagnetUri
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.model.UriMustBeValid
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetError
import com.github.pwoicik.torrentapp.domain.usecase.ParseMagnetUseCase
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams

@Inject
class ParseMagnetUseCaseImpl : ParseMagnetUseCase {
    override fun invoke(input: String): Either<ParseMagnetError, Magnet> {
        val params = try {
            AddTorrentParams.parseMagnetUri(input)
        } catch (e: IllegalArgumentException) {
            return ParseMagnetError.MagnetInvalid.left()
        }
        val hash = params.infoHashes.best.toHex()
        return Magnet(
            uri = @OptIn(UriMustBeValid::class) MagnetUri(input),
            name = params.name?.takeIf { it.isNotBlank() } ?: hash,
            hash = Sha1Hash(hash),
        ).right()
    }
}
