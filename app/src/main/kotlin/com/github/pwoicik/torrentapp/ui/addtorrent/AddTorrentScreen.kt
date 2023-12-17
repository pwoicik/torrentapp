package com.github.pwoicik.torrentapp.ui.addtorrent

import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.BDecodeNode
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.swig.libtorrent
import org.libtorrent4j.swig.torrent_flags_t

@Parcelize
data class AddTorrentScreen(
    val magnet: String,
) : Screen {
    data class State(
        val torrent: AddTorrentParams?,
        val event: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event {

    }
}

@Composable
fun AddTorrentPresenter(
    screen: AddTorrentScreen,
    navigator: Navigator,
    session: SessionManager,
): AddTorrentScreen.State {
    val torrentParams = remember {
        try {
            AddTorrentParams.parseMagnetUri(screen.magnet)!!
        } catch (_: IllegalArgumentException) {
            TODO()
        }
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (torrentParams.name.isNullOrEmpty()) {
                torrentParams.name = torrentParams.infoHashes.best.toHex()
            }
            torrentParams.flags = torrent_flags_t().apply {
                and_(TorrentFlags.AUTO_MANAGED.inv())
                or_(TorrentFlags.UPLOAD_MODE)
                or_(TorrentFlags.STOP_WHEN_READY)
            }
            torrentParams.savePath = context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                .absolutePath
            val bencode = session.fetchMagnet(screen.magnet, Int.MAX_VALUE, context.cacheDir)
            Log.d("test", libtorrent.print_entry(BDecodeNode.bdecode(bencode).swig()))
        }
    }
    return AddTorrentScreen.State(
        torrent = torrentParams,
    ) {}
}

@Composable
fun AddTorrent(
    uiState: AddTorrentScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .fillMaxSize(),
    ) {
        uiState.torrent?.let {
            it::class.java.declaredFields
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.name)
            Text(
                text = "Version",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.version.toString())
            Text(
                text = "Comment",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.comment.toString())
            Text(
                text = "Hash",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(it.infoHashes.best.toString())
        }
    }
}
