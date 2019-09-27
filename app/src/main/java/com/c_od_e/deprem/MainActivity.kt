package com.c_od_e.deprem

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.c_od_e.deprem.DepremService.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        btnStart.setOnClickListener {
            notifyService(Action.START)
        }
        btnStop.setOnClickListener {
            notifyService(Action.STOP)
        }
    }

    private fun notifyService(action: Action) {
        if (serviceAlreadyStopped() && action == Action.STOP) return
        Intent(this, DepremService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
                return
            } else {
                startService(it)
            }
        }
    }

    private fun serviceAlreadyStopped() = AppData.serviceState == State.STOPPED.name
}
