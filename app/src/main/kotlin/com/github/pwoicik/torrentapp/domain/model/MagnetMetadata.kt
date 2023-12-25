package com.github.pwoicik.torrentapp.domain.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val size: ByteSize

    @Stable
    data class Directory(
        override val name: String,
        override val size: ByteSize,
        val content: ImmutableList<Storage>,
    ) : Storage {
        var selected by mutableStateOf(SelectionState.Selected)
    }

    @Stable
    data class File(
        val id: Int,
        override val name: String,
        override val size: ByteSize,
    ) : Storage {
        var selected by mutableStateOf(true)
    }
}

enum class SelectionState {
    Selected,
    NotSelected,
    PartiallySelected,
}
