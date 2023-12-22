package com.github.pwoicik.torrentapp.data

import android.util.Log
import app.cash.sqldelight.async.coroutines.awaitAsList
import arrow.atomic.AtomicInt
import com.github.pwoicik.torrentapp.TorrentService
import com.github.pwoicik.torrentapp.db.Database
import com.github.pwoicik.torrentapp.di.AppScope
import com.github.pwoicik.torrentapp.di.IoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.AlertListener
import org.libtorrent4j.BDecodeNode
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.SaveResumeDataAlert
import org.libtorrent4j.alerts.SaveResumeDataFailedAlert
import org.libtorrent4j.swig.error_code
import org.libtorrent4j.swig.libtorrent
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@AppScope
@Inject
class TorrentEngine(
    private val db: Database,
    ioDispatcher: IoDispatcher,
) {
    private val lifecycleScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    val session = SessionManager(false)

    init {
        session.addListener(object : AlertListener {
            override fun types() = null

            override fun alert(alert: Alert<*>) {
                if ("(Peer)|(Block)".toRegex() in alert::class.simpleName!!) return
                Log.d(
                    TorrentService.ID,
                    "%s: %s".format(
                        alert::class.simpleName,
                        alert.message(),
                    ),
                )
            }
        })
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun start() =
        lifecycleScope.launch {
            session.start()
            val handle = SessionHandle(session.swig())
            db.torrentQueries.getResumeData().awaitAsList()
                .asSequence()
                .mapNotNull { it.resumeData?.let(Base64::decode) }
                .map(BDecodeNode::bdecode)
                .mapNotNull(::readResumeData)
                .forEach(handle::asyncAddTorrent)
        }.join()

    suspend fun stop() =
        lifecycleScope.launch {
            session.pause()
            saveResumeData()
            session.stop()
            lifecycleScope.cancel()
        }.join()

    private fun readResumeData(node: BDecodeNode): AddTorrentParams? {
        val errorCode = error_code()
        val params = libtorrent.read_resume_data(node.swig(), errorCode)
        if (errorCode.to_bool() || params == null) return null
        return AddTorrentParams(params)
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
