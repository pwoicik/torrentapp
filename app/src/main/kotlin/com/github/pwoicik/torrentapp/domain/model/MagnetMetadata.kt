package com.github.pwoicik.torrentapp.domain.model

import androidx.compose.runtime.Stable
import com.github.pwoicik.torrentapp.ui.util.toByteSize
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant

data class MagnetMetadata(
    val name: String,
    val creator: String?,
    val creationDate: Instant?,
    val numberOfFiles: Int,
    val totalSize: ByteSize,
    val numberOfPieces: Int,
    val pieceSize: ByteSize,
    val hash: Sha1Hash,
    val files: ImmutableList<Storage>,
)

sealed interface Storage {
    val name: String

    data class Directory(
        override val name: String,
        val content: ImmutableList<Storage>,
    ) : Storage {
        val size: ByteSize by lazy { calculateSize() }
    }

    @Stable
    interface File : Storage {
        val id: Int
        override val name: String
        val size: ByteSize
        var selected: Boolean
    }
}

private fun Storage.Directory.calculateSize(): ByteSize {
    val stack = ArrayDeque<Storage>().apply { addAll(content) }
    tailrec fun go(acc: Long, stack: MutableList<Storage>): Long {
        if (stack.isEmpty()) return acc
        return when (val head = stack.removeLast()) {
            is Storage.File -> go(acc + head.size.value, stack)
            is Storage.Directory -> go(acc, stack.apply { addAll(head.content) })
        }
    }
    return go(0, stack).toByteSize()
}
