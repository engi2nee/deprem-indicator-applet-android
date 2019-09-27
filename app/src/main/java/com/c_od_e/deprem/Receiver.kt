package com.c_od_e.deprem

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && AppData.serviceState == DepremService.State.STARTED.name) {
            Intent(context, DepremService::class.java).also {
                it.action = DepremService.Action.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(it)
                    return
                }
                context.startService(it)
            }
        }
    }
}
