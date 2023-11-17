package com.mihab.audioplayer.model

import android.net.Uri

data class Audio(
    val id: Long,
    val name: String,
    val duration: String,
    val artist: String,
    val cover: Uri
)
