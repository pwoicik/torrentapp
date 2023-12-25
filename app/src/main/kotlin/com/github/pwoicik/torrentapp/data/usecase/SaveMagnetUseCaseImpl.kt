package com.github.pwoicik.torrentapp.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.di.IoDispatcher
import com.github.pwoicik.torrentapp.domain.model.Storage
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetError
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetInput
import com.github.pwoicik.torrentapp.domain.usecase.SaveMagnetUseCase
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.ErrorCode
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.swig.error_code
import java.nio.file.LinkOption
import kotlin.io.encoding.Base64
import kotlin.io.path.Path
import kotlin.io.path.exists

@Inject
class SaveMagnetUseCaseImpl(
    private val db: Database,
    private val session: SessionManager,
    private val ioDispatcher: IoDispatcher,
) : SaveMagnetUseCase {
    override suspend fun invoke(input: SaveMagnetInput): Either<SaveMagnetError, Unit> {
        if (Path(input.savePath, input.info.name).exists(LinkOption.NOFOLLOW_LINKS)) {
            return SaveMagnetError.FileAlreadyExists.left()
        }

        val params = AddTorrentParams.parseMagnetUri(input.info.uri.value).apply {
            savePath = input.savePath
            flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
            input.metadata?.files
                ?.let { filePriorities(it.toPriorities()) }
        }
        db.torrentQueries.insert(
            hash = input.info.hash.value,
            name = input.info.name,
            paused = !input.startImmediately,
            sequential = input.sequential,
            savePath = input.savePath,
            resumeData = Base64.Default.encode(AddTorrentParams.writeResumeDataBuf(params)),
        )
        val handle = withContext(ioDispatcher) {
            SessionHandle(session.swig()).addTorrent(params, ErrorCode(error_code()))
        }
        if (input.startImmediately) {
            handle.resume()
        }

        return Unit.right()
    }
}

private fun List<Storage>.toPriorities(): Array<Priority> {
    val stack = ArrayDeque(this)
    val files = mutableListOf<Storage.File>()
    tailrec fun go() {
        if (stack.isEmpty()) return
        when (val node = stack.removeLast()) {
            is Storage.Directory -> stack.addAll(node.content)
            is Storage.File -> files.add(node)
        }
        go()
    }
    go()
    val output = arrayOfNulls<Priority>(files.size)
    files.forEach {
        output[it.id] = if (it.selected) {
            Priority.DEFAULT
        } else {
            Priority.IGNORE
        }
    }
    @Suppress("UNCHECKED_CAST")
    return output as Array<Priority>
}
