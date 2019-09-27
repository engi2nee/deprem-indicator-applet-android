package com.c_od_e.deprem

import com.chibatching.kotpref.KotprefModel

object AppData : KotprefModel() {
    var lastIndex by intPref(default = 0)
    var serviceState by stringPref(DepremService.State.STOPPED.name)
}
