package com.github.pwoicik.torrentapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.github.pwoicik.torrentapp.db.Database
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.BDecodeNode
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.SessionManager
import org.libtorrent4j.swig.error_code
import org.libtorrent4j.swig.libtorrent
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Inject
class RestoreSessionWorker(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val session: SessionManager,
    private val db: Database,
) : CoroutineWorker(appContext, params) {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun doWork(): Result {
        session.start()
        val handle = SessionHandle(session.swig())
        db.torrentQueries.getResumeData().awaitAsList()
            .asSequence()
            .mapNotNull { it.resumeData?.let(Base64::decode) }
            .map(BDecodeNode::bdecode)
            .mapNotNull(::readResumeData)
            .forEach(handle::asyncAddTorrent)
        return Result.success()
    }

    private fun readResumeData(node: BDecodeNode): AddTorrentParams? {
        val errorCode = error_code()
        val params = libtorrent.read_resume_data(node.swig(), errorCode)
        if (errorCode.to_bool() || params == null) return null
        return AddTorrentParams(params)
    }
}
