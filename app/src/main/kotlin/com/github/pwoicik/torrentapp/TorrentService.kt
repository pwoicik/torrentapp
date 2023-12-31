package com.github.pwoicik.torrentapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.pwoicik.torrentapp.data.TorrentEngine
import com.github.pwoicik.torrentapp.domain.usecase.GetSessionInfoUseCase
import com.github.pwoicik.torrentapp.domain.usecase.invoke
import com.github.pwoicik.torrentapp.ui.util.formatSpeed
import com.github.pwoicik.torrentapp.util.registerReceiver
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject

@Inject
class TorrentService(
    private val engine: TorrentEngine,
    private val getSessionInfoUseCase: GetSessionInfoUseCase,
) : LifecycleService() {
    private lateinit var finishReceiver: BroadcastReceiver
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        registerFinishReceiver()
        acquireWakeLock()
        startForeground()
        startSession()
    }

    override fun onDestroy() {
        wakeLock.release()
        unregisterReceiver(finishReceiver)
        runBlocking { engine.stop() }
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        wakeLock = getSystemService<PowerManager>()!!.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            ID,
        ).apply { acquire(Long.MAX_VALUE) }
    }

    private fun registerFinishReceiver() {
        finishReceiver = registerReceiver(ApplicationConstants.ACTION_FINISH) {
            stopSelf()
        }
    }

    private fun startForeground() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(
                ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            ).setName("Download service").setSound(null, null).setVibrationEnabled(false)
                .build(),
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
    }

    private fun startSession() {
        val manager = NotificationManagerCompat.from(this)
        lifecycleScope.launch {
            engine.start()
            getSessionInfoUseCase().collect {
                if (Build.VERSION.SDK_INT < 33 ||
                    checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED
                ) {
                    manager.notify(
                        1,
                        makeNotification().setContentText(
                            "↓ %s | ↑ %s".format(
                                it.downloadRate.formatSpeed(),
                                it.uploadRate.formatSpeed(),
                            ),
                        ).build(),
                    )
                }
            }
        }
    }

    private fun makeNotification() =
        NotificationCompat.Builder(this, ID).setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true).setOnlyAlertOnce(true).setLocalOnly(true)
            .setContentTitle("Service running…").setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            ).addAction(makeShutdownAction())

    private fun makeShutdownAction() =
        NotificationCompat.Action.Builder(
            -1,
            "Shutdown",
            PendingIntent.getBroadcast(
                this,
                1,
                Intent().setPackage(packageName).setAction(ApplicationConstants.ACTION_FINISH),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        ).setShowsUserInterface(false).setContextual(false).build()

    companion object {
        val ID = TorrentService::class.qualifiedName!!
    }
}
