package com.github.pwoicik.torrentapp

import com.github.pwoicik.torrentapp.data.usecase.buildStorage
import com.github.pwoicik.torrentapp.data.usecase.buildStorage2
import com.github.pwoicik.torrentapp.domain.model.ByteSize
import com.github.pwoicik.torrentapp.domain.model.Storage
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BuildStorageTest {
    @Test
    fun `buildStorage produces correct file tree`() {
        assertEquals(params.second, buildStorage(params.first).toSet())
    }

    @Test
    fun `buildStorage2 produces correct file tree`() {
        assertEquals(params.second, buildStorage2(params.first).toSet())
    }

    private val params = Pair(
        listOf(
            Pair(listOf("file1"), ByteSize(1)),
            Pair(listOf("dir1", "file2"), ByteSize(2)),
            Pair(listOf("dir1", "file3"), ByteSize(3)),
            Pair(listOf("dir1", "dir2", "file4"), ByteSize(4)),
            Pair(listOf("dir3", "dir4", "file5"), ByteSize(5)),
            Pair(listOf("dir3", "dir4", "dir5", "dir6", "file6"), ByteSize(6)),
            Pair(listOf("dir7", "dir8", "dir9", "file7"), ByteSize(7)),
        ),
        setOf(
            Storage.File("file1", ByteSize(1)),
            Storage.Directory(
                "dir1",
                persistentListOf(
                    Storage.File("file2", ByteSize(2)),
                    Storage.File("file3", ByteSize(3)),
                    Storage.Directory(
                        "dir2",
                        persistentListOf(
                            Storage.File("file4", ByteSize(4)),
                        ),
                    ),
                ),
            ),
            Storage.Directory(
                "dir3",
                persistentListOf(
                    Storage.Directory(
                        "dir4",
                        persistentListOf(
                            Storage.File("file5", ByteSize(5)),
                            Storage.Directory(
                                "dir5",
                                persistentListOf(
                                    Storage.Directory(
                                        "dir6",
                                        persistentListOf(
                                            Storage.File("file6", ByteSize(6)),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            Storage.Directory(
                "dir7",
                persistentListOf(
                    Storage.Directory(
                        "dir8",
                        persistentListOf(
                            Storage.Directory(
                                "dir9",
                                persistentListOf(Storage.File("file7", ByteSize(7))),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )
}
