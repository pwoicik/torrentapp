package com.github.pwoicik.torrentapp

import com.github.pwoicik.torrentapp.data.usecase.buildStorage
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.Storage
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildStorageTest {
    @Test
    fun `buildStorage produces correct file tree`() {
        assertEquals(output, buildStorage(input))
    }

    private val input = listOf(
        Triple(0, listOf("file1"), ByteSize(1)),
        Triple(1, listOf("dir1", "file2"), ByteSize(2)),
        Triple(2, listOf("dir1", "file3"), ByteSize(3)),
        Triple(3, listOf("dir1", "dir2", "file4"), ByteSize(4)),
        Triple(4, listOf("dir3", "dir4", "file5"), ByteSize(5)),
        Triple(5, listOf("dir3", "dir4", "dir5", "dir6", "file6"), ByteSize(6)),
        Triple(6, listOf("dir7", "dir8", "dir9", "file7"), ByteSize(7)),
    )

    private val output = listOf(
        Storage.Directory(
            "dir1",
            ByteSize(9),
            persistentListOf(
                Storage.Directory(
                    "dir2",
                    ByteSize(4),
                    persistentListOf(
                        Storage.File(3, "file4", ByteSize(4)),
                    ),
                ),
                Storage.File(1, "file2", ByteSize(2)),
                Storage.File(2, "file3", ByteSize(3)),
            ),
        ),
        Storage.Directory(
            "dir3",
            ByteSize(11),
            persistentListOf(
                Storage.Directory(
                    "dir4",
                    ByteSize(11),
                    persistentListOf(
                        Storage.Directory(
                            "dir5",
                            ByteSize(6),
                            persistentListOf(
                                Storage.Directory(
                                    "dir6",
                                    ByteSize(6),
                                    persistentListOf(
                                        Storage.File(5, "file6", ByteSize(6)),
                                    ),
                                ),
                            ),
                        ),
                        Storage.File(4, "file5", ByteSize(5)),
                    ),
                ),
            ),
        ),
        Storage.Directory(
            "dir7",
            ByteSize(7),
            persistentListOf(
                Storage.Directory(
                    "dir8",
                    ByteSize(7),
                    persistentListOf(
                        Storage.Directory(
                            "dir9",
                            ByteSize(7),
                            persistentListOf(Storage.File(6, "file7", ByteSize(7))),
                        ),
                    ),
                ),
            ),
        ),
        Storage.File(0, "file1", ByteSize(1)),
    )
}
