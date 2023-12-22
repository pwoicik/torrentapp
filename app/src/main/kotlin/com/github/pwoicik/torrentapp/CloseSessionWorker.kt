package com.github.pwoicik.torrentapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import arrow.atomic.AtomicInt
import com.github.pwoicik.torrentapp.db.Database
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.SaveResumeDataAlert
import org.libtorrent4j.alerts.SaveResumeDataFailedAlert
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Inject
class CloseSessionWorker(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val session: SessionManager,
    private val db: Database,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        session.pause()
        saveResumeData()
        session.stop()
        return Result.success()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun saveResumeData() =
        coroutineScope {
            val torrents = session.swig()._torrents.filter { it.need_save_resume_data() }
            val counter = AtomicInt(torrents.size)
            val paramsChannel = Channel<AddTorrentParams>()
            session.addListener(object : AlertListener {
                override fun types() =
                    intArrayOf(
                        AlertType.SAVE_RESUME_DATA.swig(),
                        AlertType.SAVE_RESUME_DATA_FAILED.swig(),
                    )

                override fun alert(alert: Alert<*>) {
                    val self = this
                    launch {
                        when (alert) {
                            is SaveResumeDataAlert -> paramsChannel.send(alert.params())
                            is SaveResumeDataFailedAlert -> Unit
                        }
                        if (counter.decrementAndGet() <= 0) {
                            session.removeListener(self)
                            paramsChannel.close()
                        }
                    }
                }
            })
            torrents.forEach {
                it.save_resume_data(TorrentHandle.SAVE_INFO_DICT)
            }
            if (counter.get() == 0) return@coroutineScope
            paramsChannel.consumeEach { params ->
                val bytes = AddTorrentParams.writeResumeData(params).bencode()
                db.torrentQueries.updateResumeData(
                    Base64.encode(bytes),
                    params.infoHashes.best.toHex(),
                )
            }
        }
}
