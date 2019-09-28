package com.c_od_e.deprem

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.*


class DepremService : Service() {
    private val NOTIFICATION_ID: Int = 9826
    private val CHANNEL_ID = "DEPREM-INDICATOR-APPLET"
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false
    private lateinit var builder: Notification.Builder
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (intent.action) {
                Action.START.name -> startService()
                Action.STOP.name -> stopService()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        initNotification()
        val notification = createStickyNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initNotification() {
        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            CHANNEL_ID
        ) else Notification.Builder(this)
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun startService() {
        if (isServiceRunning) return
        isServiceRunning = true
        AppData.serviceState = State.STARTED.name
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DepremService::lock").apply {
                    acquire()
                }
            }
        startFetchingProcess()
    }

    private fun stopService() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e("Service Error", "${e.message}")
        }
        isServiceRunning = false
        AppData.serviceState = State.STOPPED.name
    }

    private fun startFetchingProcess() {
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceRunning) {
                launch(Dispatchers.IO) {
                    fetchDepremList()
                }
                delay(1 * 60 * 1000)
            }
        }
    }

    private fun fetchDepremList() {
        try {
            Fuel.get(Constants.url)
                .response { _, _, result ->
                    val (bytes, error) = result
                    if (bytes != null) {
                        val html = String(bytes)
                        val eqRows = html.split("\r\n").filter { it.contains(Constants.keywordEN) || it.contains(Constants.keywordTR) }
                        if (AppData.lastIndex == 0) { //First time checking
                            AppData.lastIndex = eqRows.lastIndex
                        } else if (AppData.lastIndex < eqRows.lastIndex) {
                            AppData.lastIndex = eqRows.lastIndex
                            notifyNewDeprem(getTitle(pareLatestEq(eqRows)))
                        }
                    } else {
                        Log.d("Response Error:", "${error?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.d("Error:", "${e.message}")
        }
    }

    private fun pareLatestEq(eqRows: List<String>): List<String> {
        return eqRows.first().split(" ")
    }

    private fun getTitle(data: List<String>): String {
        return applicationContext.resources.getString(
            R.string.new_deprem,
            data[6],
            data[0], data[1]
        )
    }

    private fun notifyNewDeprem(title: String) {
        builder.setContentTitle(title)
        builder.setContentText("But hey!, You are  alive if you're reading this!")
        val notification = builder.notification
        builder.setPriority(Notification.PRIORITY_MAX)
        notificationManager.notify(R.string.app_name, notification)
    }

    private fun createStickyNotification(): Notification {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DEPREM-INDICATOR-APPLET",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "DEPREM-INDICATOR-APPLET"
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(400, 400, 100, 400, 200, 300, 400, 400, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        return builder
            .setContentTitle("Deprem Indicator Applet")
            .setContentText("Service is Running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }


    enum class Action {
        START,
        STOP
    }

    enum class State {
        STARTED,
        STOPPED,
    }
}
