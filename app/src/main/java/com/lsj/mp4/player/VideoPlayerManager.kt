package com.lsj.mp4.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.lsj.mp4.data.models.VideoFile

class VideoPlayerManager(private val context: Context) {
    
    private var exoPlayer: ExoPlayer? = null
    private var currentVideoFile: VideoFile? = null
    
    fun initializePlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            1000,  // Min buffer: 1 second
                            5000,  // Max buffer: 5 seconds  
                            500,   // Buffer for playback: 0.5 seconds
                            1000   // Buffer for playback after rebuffer: 1 second
                        )
                        .build()
                )
                .build()
        }
        return exoPlayer!!
    }
    
    fun prepareVideo(videoFile: VideoFile, playWhenReady: Boolean = true) {
        currentVideoFile = videoFile
        val mediaItem = MediaItem.fromUri(videoFile.uri)
        
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            this.playWhenReady = playWhenReady
            // Don't seek here - let VideoPlayerActivity handle it properly
        }
    }
    
    fun release() {
        // Save current position before releasing
        currentVideoFile?.let { video ->
            exoPlayer?.currentPosition?.let { position ->
                savePlayPosition(video.id, position)
            }
        }
        
        exoPlayer?.release()
        exoPlayer = null
        currentVideoFile = null
    }
    
    private fun savePlayPosition(videoId: Long, position: Long) {
        val prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("position_$videoId", position).apply()
    }
    
    fun getPlayer(): ExoPlayer? = exoPlayer
    fun getCurrentVideo(): VideoFile? = currentVideoFile
    
    fun pause() {
        exoPlayer?.pause()
    }
    
    fun play() {
        exoPlayer?.play()
    }
    
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }
    
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
}
