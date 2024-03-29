package com.github.pwoicik.torrentapp.data.usecase

import arrow.core.Either
import arrow.core.right
import com.github.pwoicik.torrentapp.di.ApplicationContext
import com.github.pwoicik.torrentapp.di.DefaultDispatcher
import com.github.pwoicik.torrentapp.di.IoDispatcher
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.MagnetInfo
import com.github.pwoicik.torrentapp.domain.model.MagnetMetadata
import com.github.pwoicik.torrentapp.domain.model.Sha1Hash
import com.github.pwoicik.torrentapp.domain.model.Storage
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataError
import com.github.pwoicik.torrentapp.domain.usecase.GetMagnetMetadataUseCase
import com.github.pwoicik.torrentapp.ui.util.toByteSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.adapters.ImmutableListAdapter
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentInfo

@Inject
class GetMagnetMetadataUseCaseImpl(
    private val session: SessionManager,
    private val context: ApplicationContext,
    private val ioDispatcher: IoDispatcher,
    private val defaultDispatcher: DefaultDispatcher,
) : GetMagnetMetadataUseCase {
    override suspend fun invoke(input: MagnetInfo): Either<GetMagnetMetadataError, MagnetMetadata> {
        val data = withContext(ioDispatcher) {
            session.fetchMagnet(
                input.uri.value,
                Int.MAX_VALUE,
                context.cacheDir,
            )
        }
        val info = TorrentInfo.bdecode(data)
        val hash = info.infoHashes().best.toHex()
        return MagnetMetadata(
            name = info.name()?.takeIf { it.isNotBlank() } ?: hash,
            creator = info.creator()?.takeIf { it.isNotBlank() },
            creationDate = info.creationDate()
                .takeIf { it != 0L }
                ?.let(Instant::fromEpochSeconds),
            numberOfFiles = info.numFiles(),
            totalSize = info.totalSize().let(::ByteSize),
            numberOfPieces = info.numPieces(),
            pieceSize = info.pieceLength().toLong().let(::ByteSize),
            hash = hash.let(::Sha1Hash),
            files = withContext(defaultDispatcher) { info.buildStorage() },
        ).right()
    }
}

private fun TorrentInfo.buildStorage(): ImmutableList<Storage> {
    val files = files()
    return buildStorage(
        List(files.numFiles()) {
            Triple(
                it,
                files.filePath(it).split('/'),
                files.fileSize(it).toByteSize(),
            )
        },
    )
}

private typealias Path = List<String>
private typealias SizedPath = Triple<Int, Path, ByteSize>

fun buildStorage(sizedPaths: List<SizedPath>): ImmutableList<Storage> {
    val (files, nested) = sizedPaths.partition { it.second.size == 1 }
    val storage = ArrayList<Storage>(files.size + nested.size)
    files.mapTo(storage) { (idx, path, size) ->
        Storage.File(
            id = idx,
            name = path.first(),
            size = size,
        )
    }
    if (nested.isEmpty()) return ImmutableListAdapter(storage)
    val nestedFiles = mutableMapOf<List<String>, MutableList<Storage>>()
    nested.groupByTo(
        destination = nestedFiles,
        keySelector = { it.second.dropLast(1) },
        valueTransform = { (idx, fullPath, size) ->
            Storage.File(
                id = idx,
                name = fullPath.last(),
                size = size,
            )
        },
    )
    var maxPathSize = nestedFiles.maxOf { it.key.size }
    while (true) {
        if (maxPathSize == 0) break
        nestedFiles.filter { it.key.size == maxPathSize }.forEach { (fullPath, dirs) ->
            val path = fullPath.dropLast(1)
            nestedFiles.remove(fullPath)
            val dir = Storage.Directory(
                name = fullPath.last(),
                content = ImmutableListAdapter(dirs),
                size = dirs.sumOf { it.size.value }.toByteSize(),
            )
            nestedFiles[path] = nestedFiles[path]
                ?.apply {
                    add(dir)
                    sortWith(StorageComparator)
                }
                ?: mutableListOf(dir)
        }
        --maxPathSize
    }
    nestedFiles.values.forEach(storage::addAll)
    storage.sortWith(StorageComparator)
    return ImmutableListAdapter(storage)
}

private object StorageComparator : Comparator<Storage> {
    override fun compare(o1: Storage, o2: Storage): Int {
        return if (o1 is Storage.Directory && o2 is Storage.Directory) {
            o1.name.compareTo(o2.name)
        } else if (o1 is Storage.Directory) {
            -1
        } else if (o2 is Storage.Directory) {
            1
        } else {
            (o1 as Storage.File).name.compareTo((o2 as Storage.File).name, ignoreCase = true)
        }
    }
}
