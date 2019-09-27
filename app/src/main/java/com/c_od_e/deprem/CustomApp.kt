package com.c_od_e.deprem

import android.app.Application
import com.chibatching.kotpref.Kotpref

class CustomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Kotpref.init(applicationContext)
    }
}