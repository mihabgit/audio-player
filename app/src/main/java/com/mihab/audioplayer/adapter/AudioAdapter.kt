package com.mihab.audioplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mihab.audioplayer.R
import com.mihab.audioplayer.model.Audio

class AudioAdapter(private val audios: List<Audio>) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AudioAdapter.AudioViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.audio, parent, false)
        return AudioViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AudioAdapter.AudioViewHolder, position: Int) {
        val audio = audios[position]

        val durationInMinutesSeconds = "${
            (audio.duration.toInt() / (60 * 1000)).toString()
                .padStart(2, '0')
        }:${
            (audio.duration.toInt() % 60).toString()
                .padStart(2, '0')
        }"


        holder.audioName.text = audio.name
        holder.audioDuration.text = durationInMinutesSeconds
        holder.audioArtist.text = audio.artist
        holder.audioCover.setImageURI(audio.cover)
        holder.itemView.tag = position

    }

    override fun getItemCount(): Int {
        return audios.size
    }

    class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val audioName: TextView = itemView.findViewById(R.id.tvSongName)
        val audioDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val audioArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val audioCover: ImageView = itemView.findViewById(R.id.ivAudioArt)
    }


}



















