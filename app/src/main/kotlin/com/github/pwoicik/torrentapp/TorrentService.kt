package com.github.pwoicik.torrentapp

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.pwoicik.torrentapp.di.inject
import com.github.pwoicik.torrentapp.ui.util.formatSpeed
import com.github.pwoicik.torrentapp.ui.util.toByteSize
import com.github.pwoicik.torrentapp.util.registerReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TorrentService : LifecycleService() {
    private val sessionManager by inject { sessionManager }
    private lateinit var finishReceiver: BroadcastReceiver
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        registerFinishReceiver()
        acquireWakeLock()
        NotificationManagerCompat.from(this)
            .createNotificationChannel(
                NotificationChannelCompat.Builder(
                    ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                )
                    .setName("Download service")
                    .setSound(null, null)
                    .setVibrationEnabled(false)
                    .build()
            )
        ServiceCompat.startForeground(
            this,
            1,
            makeNotification().build(),
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
        sessionManager.start()
        lifecycleScope.launch {
            val manager = NotificationManagerCompat.from(this@TorrentService)
            val stats = sessionManager.stats()
            while (isActive) {
                if (ContextCompat.checkSelfPermission(
                        this@TorrentService,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    manager.notify(
                        1,
                        makeNotification()
                            .setContentText(
                                "↓ %s | ↑ %s".format(
                                    stats.downloadRate().toByteSize().formatSpeed(),
                                    stats.uploadRate().toByteSize().formatSpeed(),
                                ),
                            )
                            .build(),
                    )
                }
                delay(1.seconds)
            }
        }
    }

    private fun acquireWakeLock() {
        wakeLock = getSystemService<PowerManager>()!!.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            ID,
        ).apply { acquire(Long.MAX_VALUE) }
    }

    private fun makeNotification() = NotificationCompat.Builder(this, ID)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setLocalOnly(true)
        .setContentTitle("Service running…")
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .addAction(makeShutdownAction())

    private fun makeShutdownAction() = NotificationCompat.Action.Builder(
        -1,
        "Shutdown",
        PendingIntent.getBroadcast(
            this,
            1,
            Intent()
                .setPackage(packageName)
                .setAction(ApplicationConstants.ACTION_FINISH),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
        .setShowsUserInterface(false)
        .setContextual(false)
        .build()

    private fun registerFinishReceiver() {
        finishReceiver = registerReceiver(ApplicationConstants.ACTION_FINISH) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.stop()
        wakeLock.release()
        unregisterReceiver(finishReceiver)
    }

    companion object {
        val ID = TorrentService::class.qualifiedName!!
    }
}
