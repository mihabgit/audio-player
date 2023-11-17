package com.mihab.audioplayer.service

import android.os.Binder

class AudioBinder(private val service: AudioService) : Binder() {

    val getService: AudioService
        get() = service

}