package com.github.pwoicik.torrentapp.data.datastore

import android.os.Environment
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import com.github.pwoicik.torrentapp.proto.Settings
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

object SettingsSerializer : OkioSerializer<Settings> {
    override val defaultValue: Settings
        get() = Settings(
            download = Settings.Download(
                savePath = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .absolutePath,
                sequential = false,
                prioritizeFirstLast = false,
            ),
        )

    override suspend fun readFrom(source: BufferedSource): Settings {
        try {
            return Settings.ADAPTER.decode(source)
        } catch (exception: IOException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        t.encode(sink)
    }
}
