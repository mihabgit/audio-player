package com.mihab.audioplayer

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.MediaController.MediaPlayerControl
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mihab.audioplayer.adapter.AudioAdapter
import com.mihab.audioplayer.model.Audio
import com.mihab.audioplayer.service.AudioBinder
import com.mihab.audioplayer.service.AudioService

class MainActivity : AppCompatActivity(), MediaPlayerControl {

    private val audios: ArrayList<Audio> = arrayListOf()
    private val PERMISSIONS_REQUEST_READ_MEDIA_AUDIO = 1
    private lateinit var recyclerView: RecyclerView

    private var audioService: AudioService? = null
    private var playIntent: Intent? = null
    private var audioBound = false

    private lateinit var controller: AudioController

    private var paused: Boolean = false
    private var playbackPaused: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

    }

    override fun onStart() {
        super.onStart()

        if (playIntent == null) {
            playIntent = Intent(this, AudioService::class.java)
            bindService(playIntent!!, audioConnection, BIND_AUTO_CREATE)
            startService(playIntent)
        }

    }

    private fun checkPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            ) {
                Toast.makeText(
                    this,
                    "This permission is need for showing Audio list",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                        PERMISSIONS_REQUEST_READ_MEDIA_AUDIO
                    )
                }
            }
        } else {
            displayAudios()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_MEDIA_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    displayAudios()
                } else {
                    // have to handle permission denied
                }
                return
            }
        }
    }

    private fun displayAudios() {
        recyclerView = findViewById(R.id.rvAudioList)
        recyclerView.setHasFixedSize(true)

        getAudioList()

        audios.sortWith { a, b -> a.name.compareTo(b.name) }

        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager

        val adapter = AudioAdapter(audios)
        recyclerView.adapter = adapter

        recyclerView.doOnLayout {
            setController()
        }

    }

    private fun getAudioList() {
        val audioResolver = contentResolver
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val selection =
            "${MediaStore.Audio.Media.DATA} LIKE ? AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("%/Music/%", "10000")
        val audioCursor = audioResolver.query(audioUri, null, selection, selectionArgs, null)

        if ((audioCursor != null) && audioCursor.moveToFirst()) {
            val titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val durationColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

            do {
                val thisId = audioCursor.getLong(idColumn)
                val thisTitle = audioCursor.getString(titleColumn)
                val thisDuration = audioCursor.getString(durationColumn)
                val thisArtist = audioCursor.getString(artistColumn)
                val thisAlbumId = audioCursor.getString(albumIdColumn)

                val albumArtUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtContentUri =
                    ContentUris.withAppendedId(albumArtUri, thisAlbumId.toLong())
                audios.add(Audio(thisId, thisTitle, thisDuration, thisArtist, albumArtContentUri))

            } while (audioCursor.moveToNext())

            audioCursor.close()
        } else {
            Log.d("MainActivity", "The audio list is empty!")
        }
    }

    private val audioConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioBinder

            audioService = binder.getService
            audioService?.setList(audios)

            audioBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioBound = false
        }
    }

    fun audioPicked(view: View) {
        audioService?.setAudio(view.tag.toString().toInt())
        audioService?.playAudio()
    }

    override fun onDestroy() {
        stopService(playIntent)
        audioService = null

        super.onDestroy()
    }

    private fun setController() {
        controller = AudioController(this)

        controller.setPrevNextListeners({ playNext() }) { playPrev() }

        controller.setMediaPlayer(this)
        controller.setAnchorView(findViewById(R.id.rvAudioList))
        controller.isEnabled = true
        controller.show()

    }

    private fun playNext() {
        audioService?.playNext()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller.show(0)
    }

    private fun playPrev() {
        audioService?.playPrev()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller.show(0)
    }

    override fun start() {
        audioService?.go()
    }

    override fun pause() {
        playbackPaused = true
        audioService?.pausePlayer()
    }

    override fun getDuration(): Int {
        if (audioService != null && audioBound && audioService!!.isPlaying())
            return audioService!!.getDuration()
        else return 0
    }

    override fun getCurrentPosition(): Int {
        if (audioService != null && audioBound && audioService!!.isPlaying()) {
            return audioService!!.getPosition()
        } else {
            return 0
        }
    }

    override fun seekTo(pos: Int) {
        audioService?.seek(pos)
    }

    override fun isPlaying(): Boolean {
        if (audioService != null && audioBound)
            return audioService!!.isPlaying()
        return false
    }

    override fun getBufferPercentage(): Int {
        val duration = audioService?.getDuration()
        if (duration != null) {
            if (duration > 0) {
                return (audioService!!.getPosition() * 100) / (duration)

            }
        }
        return 0
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getAudioSessionId(): Int {
        return 1
    }


}















