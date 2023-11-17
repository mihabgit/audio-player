package com.mihab.audioplayer.service

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import com.mihab.audioplayer.model.Audio

class AudioService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audios: ArrayList<Audio>
    private var audioPosition = 0

    private val audioBinder: IBinder = AudioBinder(this)

    override fun onCreate() {
        super.onCreate()
        audioPosition = 0
        mediaPlayer = MediaPlayer()

        initAudioPlayer()
        val randmon = kotlin.random.Random.Default
    }

    private fun initAudioPlayer() {
        mediaPlayer.setWakeMode(
            applicationContext,
            PowerManager.PARTIAL_WAKE_LOCK
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
    }

    fun setList(theAudios: ArrayList<Audio>) {
        audios = theAudios
    }

    override fun onBind(intent: Intent?): IBinder {
        return audioBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mediaPlayer.stop()
        mediaPlayer.release()
        return false
    }

    fun playAudio() {

        mediaPlayer.reset()
        val playAudio = audios[audioPosition]
        val currentAudioId: Long = playAudio.id

        val trackUri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentAudioId)

        try {
            mediaPlayer.setDataSource(applicationContext, trackUri)
        } catch (ex: Exception) {
            Log.d("AudioService", "Error setting data source", ex)
        }
        mediaPlayer.prepare()

    }

    fun setAudio(audioIndex: Int) {
        audioPosition = audioIndex
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mediaPlayer.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {

    }

    fun getPosition(): Int {
        return mediaPlayer.currentPosition
    }

    fun getDuration(): Int {
        return mediaPlayer.duration
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun pausePlayer() {
        mediaPlayer.pause()
    }

    fun seek(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun go() {
        mediaPlayer.start()
    }

    fun playNext() {
        audioPosition++
        if (audioPosition >= audios.size) audioPosition = 0
        playAudio()
    }

    fun playPrev() {
        audioPosition--
        if (audioPosition < 0) audioPosition = audios.size - 1
        playAudio()
    }

}