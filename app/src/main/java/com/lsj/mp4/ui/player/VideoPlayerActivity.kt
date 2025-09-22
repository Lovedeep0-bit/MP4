package com.lsj.mp4.ui.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import com.lsj.mp4.R
import com.lsj.mp4.data.models.VideoFile
import com.lsj.mp4.data.repository.VideoRepository
import com.lsj.mp4.player.VideoPlayerManager

enum class AspectRatio(val displayName: String, val resizeMode: Int) {
    ORIGINAL("Original", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FIT_SCREEN("Fit Screen", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var playerView: PlayerView
    private lateinit var videoPlayerManager: VideoPlayerManager
    private lateinit var videoRepository: VideoRepository
    private var videoFile: VideoFile? = null
    
    private lateinit var playPauseButton: ImageButton
    private lateinit var skipBackwardButton: ImageButton
    private lateinit var skipForwardButton: ImageButton
    private lateinit var tracksButton: ImageButton
    private lateinit var aspectRatioButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var titleText: TextView
    
    private var isControlsVisible = true
    private var showRemainingTime = false
    private var tracksDialog: AlertDialog? = null
    private var currentAspectRatio = AspectRatio.ORIGINAL
    private val hideControlsRunnable = Runnable { hideControls() }
    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = Runnable { updateProgress() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen setup
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        setContentView(R.layout.activity_video_player)
        
        initializeViews()
        setupPlayer()
        setupControls()
        
        // Load video from intent
        loadVideoFromIntent()
    }
    
    private fun initializeViews() {
        playerView = findViewById(R.id.playerView)
        playPauseButton = findViewById(R.id.playPauseButton)
        skipBackwardButton = findViewById(R.id.skipBackwardButton)
        skipForwardButton = findViewById(R.id.skipForwardButton)
        tracksButton = findViewById(R.id.tracksButton)
        aspectRatioButton = findViewById(R.id.aspectRatioButton)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)
        titleText = findViewById(R.id.titleText)
        
        // Hide default controls
        playerView.useController = false
    }
    
    private fun setupPlayer() {
        videoPlayerManager = VideoPlayerManager(this)
        videoRepository = VideoRepository(this)
        val player = videoPlayerManager.initializePlayer()
        
        playerView.player = player
        
        // Player event listener
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayPauseButton()
                
                if (playbackState == Player.STATE_READY) {
                    updateTimeDisplay()
                    seekBar.max = (player.duration / 1000).toInt()
                }

                if (playbackState == Player.STATE_ENDED) {
                    // Mark video as completed and save progress
                    videoFile?.let { video ->
                        videoRepository.saveWatchProgress(video.id, 1.0f, player.duration)
                        videoRepository.saveLastPlayPosition(video.id, 0L) // Reset position for completed videos
                    }
                    // Auto-close when video finishes
                    finish()
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton()
                
                if (isPlaying) {
                    startSeekBarUpdate()
                    startProgressTracking()
                    scheduleControlsHide()
                } else {
                    handler.removeCallbacks(hideControlsRunnable)
                    handler.removeCallbacks(progressUpdateRunnable)
                }
            }
        })
    }
    
    private fun setupControls() {
        playPauseButton.setOnClickListener {
            val player = videoPlayerManager.getPlayer()
            if (player?.isPlaying == true) {
                player.pause()
            } else {
                player?.play()
            }
        }
        
        skipBackwardButton.setOnClickListener {
            val player = videoPlayerManager.getPlayer()
            player?.let { p ->
                val currentPosition = p.currentPosition
                val newPosition = maxOf(0, currentPosition - 10000) // Skip back 10 seconds
                p.seekTo(newPosition)
                android.util.Log.d("VideoPlayerActivity", "Skipped backward to: ${newPosition}ms")
            }
        }
        
        skipForwardButton.setOnClickListener {
            val player = videoPlayerManager.getPlayer()
            player?.let { p ->
                val currentPosition = p.currentPosition
                val duration = p.duration
                val newPosition = minOf(duration, currentPosition + 10000) // Skip forward 10 seconds
                p.seekTo(newPosition)
                android.util.Log.d("VideoPlayerActivity", "Skipped forward to: ${newPosition}ms")
            }
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val player = videoPlayerManager.getPlayer()
                    val seekPosition = progress * 1000L
                    
                    // Only seek if player is ready and position is valid
                    if (player != null && player.playbackState == Player.STATE_READY && seekPosition < player.duration) {
                        player.seekTo(seekPosition)
                        android.util.Log.d("VideoPlayerActivity", "Seeking to: ${seekPosition}ms")
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
                isUserSeeking = true
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                scheduleControlsHide()
            }
        })
        
        playerView.setOnClickListener {
            if (isControlsVisible) {
                hideControls()
                } else {
                showControls()
                scheduleControlsHide()
            }
        }
        
        totalTimeText.setOnClickListener {
            showRemainingTime = !showRemainingTime
            updateTimeDisplay()
        }
        
        tracksButton.setOnClickListener {
            showTracksDialog()
        }
        
        aspectRatioButton.setOnClickListener {
            toggleAspectRatio()
        }
    }
    
    private fun loadVideoFromIntent() {
        val videoUri = intent.getStringExtra("video_uri")?.let { Uri.parse(it) }
        val videoTitle = intent.getStringExtra("video_title") ?: "Video"
        val videoId = intent.getLongExtra("video_id", -1L)
        val lastPosition = intent.getLongExtra("last_position", 0L)
        
        android.util.Log.d("VideoPlayerActivity", "Received video data: title=$videoTitle, id=$videoId, lastPosition=${lastPosition}ms")
        
        if (videoUri != null && videoId != -1L) {
            videoFile = VideoFile(
                id = videoId,
                title = videoTitle,
                path = "",
                uri = videoUri,
                duration = 0L,
                size = 0L,
                dateAdded = 0L,
                mimeType = "",
                parentFolder = "",
                lastPlayPosition = lastPosition
            )
            
            titleText.text = removeFileExtension(videoTitle)
            videoPlayerManager.prepareVideo(videoFile!!)
            
            // Seek to last position after video is prepared and buffered
            if (lastPosition > 0) {
                val player = videoPlayerManager.getPlayer()
                player?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            // Wait a bit more for buffering, then seek
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (player.playbackState == Player.STATE_READY) {
                                    player.seekTo(lastPosition)
                                    android.util.Log.d("VideoPlayerActivity", "Resuming from position: ${lastPosition}ms")
                                }
                                player.removeListener(this) // Remove listener after seeking
                            }, 100) // Small delay to ensure proper buffering
                        }
                    }
                })
            }
            
            // Save play time
            videoRepository.saveLastPlayedTime(videoId)
        }
    }
    
    private fun updatePlayPauseButton() {
        val player = videoPlayerManager.getPlayer()
        playPauseButton.setImageResource(
            if (player?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    private var isUserSeeking = false
    
    private fun startSeekBarUpdate() {
        val updateSeekBar = object : Runnable {
            override fun run() {
                val player = videoPlayerManager.getPlayer()
                if (player != null && player.isPlaying && !isUserSeeking) {
                    val currentPosition = (player.currentPosition / 1000).toInt()
                    seekBar.progress = currentPosition
                    currentTimeText.text = formatTime(player.currentPosition)
                    updateTimeDisplay()
                    
                    handler.postDelayed(this, 1000)
                } else if (player != null && !isUserSeeking) {
                    // Update time even when paused
                    currentTimeText.text = formatTime(player.currentPosition)
                    updateTimeDisplay()
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(updateSeekBar)
    }
    
    private fun startProgressTracking() {
        handler.post(progressUpdateRunnable)
    }
    
    private fun updateProgress() {
        val player = videoPlayerManager.getPlayer()
        videoFile?.let { video ->
            if (player != null && player.duration > 0) {
                val currentPosition = player.currentPosition
                val duration = player.duration
            val progress = currentPosition.toFloat() / duration.toFloat()
            
            // Save progress and position every 5 seconds
            videoRepository.saveWatchProgress(video.id, progress, duration)
            videoRepository.saveLastPlayPosition(video.id, currentPosition)
                
                // Schedule next update
                handler.postDelayed(progressUpdateRunnable, 5000)
            }
        }
    }
    
    private fun showControls() {
        findViewById<View>(R.id.controlsLayout).visibility = View.VISIBLE
        isControlsVisible = true
    }
    
    private fun hideControls() {
        findViewById<View>(R.id.controlsLayout).visibility = View.GONE
        isControlsVisible = false
    }
    
    private fun scheduleControlsHide() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000)
    }
    
    private fun formatTime(timeMs: Long): String {
        val seconds = timeMs / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
            } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    private fun removeFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(0, lastDotIndex)
        } else {
            fileName
        }
    }
    
    private fun updateTimeDisplay() {
        val player = videoPlayerManager.getPlayer()
        if (player != null && player.duration > 0) {
            if (showRemainingTime) {
                val remainingTime = player.duration - player.currentPosition
                totalTimeText.text = "-${formatTime(remainingTime)}"
            } else {
                totalTimeText.text = formatTime(player.duration)
            }
        }
    }
    
    private fun showTracksDialog() {
        val player = videoPlayerManager.getPlayer() ?: return
        
        // Close existing dialog if open
        tracksDialog?.dismiss()
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tracks, null)
        val audioTracksList = dialogView.findViewById<ListView>(R.id.audioTracksList)
        val subtitleTracksList = dialogView.findViewById<ListView>(R.id.subtitleTracksList)
        
        // Create adapters
        val audioAdapter = TrackAdapter(this, mutableListOf())
        val subtitleAdapter = TrackAdapter(this, mutableListOf())
        
        audioTracksList.adapter = audioAdapter
        subtitleTracksList.adapter = subtitleAdapter
        
        // Function to update track lists
        fun updateTrackLists() {
            val tracks = player.currentTracks
            
            // Update audio tracks
            val audioTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            val audioTrackItems = mutableListOf<TrackItem>()
            
            val isAudioDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
            audioTrackItems.add(TrackItem("Disable track", isAudioDisabled, null) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                // Add small delay to allow ExoPlayer to process the change
                handler.postDelayed({ updateTrackLists() }, 100)
            })
            
            audioTracks.forEach { group ->
                group.mediaTrackGroup.let { trackGroup ->
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(i)
                        val trackName = getTrackDisplayName(format, "Audio Track ${i + 1}")
                        val isSelected = group.isTrackSelected(i) && !isAudioDisabled
                        audioTrackItems.add(TrackItem(trackName, isSelected, format) {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setOverrideForType(
                                    androidx.media3.common.TrackSelectionOverride(trackGroup, i)
                                )
                                .build()
                            // Add small delay to allow ExoPlayer to process the change
                            handler.postDelayed({ updateTrackLists() }, 100)
                        })
                    }
                }
            }
            
            // Update subtitle tracks
            val subtitleTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            val subtitleTrackItems = mutableListOf<TrackItem>()
            
            val isSubtitleDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            subtitleTrackItems.add(TrackItem("Disable track", isSubtitleDisabled, null) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                // Add small delay to allow ExoPlayer to process the change
                handler.postDelayed({ updateTrackLists() }, 100)
            })
            
            subtitleTracks.forEach { group ->
                group.mediaTrackGroup.let { trackGroup ->
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(i)
                        val trackName = getTrackDisplayName(format, "Subtitle Track ${i + 1}")
                        val isSelected = group.isTrackSelected(i) && !isSubtitleDisabled
                        subtitleTrackItems.add(TrackItem(trackName, isSelected, format) {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(
                                    androidx.media3.common.TrackSelectionOverride(trackGroup, i)
                                )
                                .build()
                            // Add small delay to allow ExoPlayer to process the change
                            handler.postDelayed({ updateTrackLists() }, 100)
                        })
                    }
                }
            }
            
            // Update adapters
            audioAdapter.clear()
            audioAdapter.addAll(audioTrackItems)
            audioAdapter.notifyDataSetChanged()
            
            subtitleAdapter.clear()
            subtitleAdapter.addAll(subtitleTrackItems)
            subtitleAdapter.notifyDataSetChanged()
        }
        
        // Initial update
        updateTrackLists()
        
        // Add track change listener
        val trackChangeListener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // Update dialog when tracks change
                if (tracksDialog?.isShowing == true) {
                    updateTrackLists()
                }
            }
        }
        player.addListener(trackChangeListener)
        
        // Create and show dialog
        tracksDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Close") { _, _ ->
                player.removeListener(trackChangeListener)
                tracksDialog = null
            }
            .setOnDismissListener {
                player.removeListener(trackChangeListener)
                tracksDialog = null
            }
            .show()
    }
    
    private fun toggleAspectRatio() {
        val aspectRatios = AspectRatio.values()
        val currentIndex = currentAspectRatio.ordinal
        val nextIndex = (currentIndex + 1) % aspectRatios.size
        currentAspectRatio = aspectRatios[nextIndex]
        applyAspectRatio(currentAspectRatio)
    }
    
    private fun applyAspectRatio(aspectRatio: AspectRatio) {
        when (aspectRatio) {
            AspectRatio.ORIGINAL -> {
                playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                // Reset scaling for original aspect ratio
                playerView.scaleX = 1.0f
                playerView.scaleY = 1.0f
            }
            AspectRatio.FIT_SCREEN -> {
                playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                // Reset scaling for fit screen
                playerView.scaleX = 1.0f
                playerView.scaleY = 1.0f
            }
        }
    }
    
    private fun getTrackDisplayName(format: Format, defaultName: String): String {
        val label = format.label
        val language = format.language
        val codec = format.codecs
        
        return when {
            !label.isNullOrEmpty() -> {
                val languageInfo = if (!language.isNullOrEmpty() && language != "und") {
                    " - [${language.uppercase()}]"
                } else ""
                "$label$languageInfo"
            }
            !language.isNullOrEmpty() && language != "und" -> {
                val codecInfo = if (!codec.isNullOrEmpty()) " - [$codec]" else ""
                "${language.uppercase()}$codecInfo"
            }
            else -> defaultName
        }
    }
    
    private data class TrackItem(
        val name: String,
        val isSelected: Boolean,
        val format: Format?,
        val action: () -> Unit
    )
    
    private class TrackAdapter(
        private val context: android.content.Context,
        private val items: MutableList<TrackItem>
    ) : ArrayAdapter<TrackItem>(context, R.layout.item_track, items) {
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_track, parent, false)
            val item = items[position]
            
            val trackItemLayout = view.findViewById<LinearLayout>(R.id.trackItemLayout)
            val trackName = view.findViewById<TextView>(R.id.trackName)
            
            // Set background based on selection state
            trackItemLayout.setBackgroundResource(
                if (item.isSelected) R.drawable.track_selected_background 
                else R.drawable.track_default_background
            )
            
            trackName.text = item.name
            
            view.setOnClickListener { item.action() }
            
            return view
        }
    }
    
    override fun onPause() {
        super.onPause()
        videoPlayerManager.getPlayer()?.pause()
        
        // Save progress when pausing
        val player = videoPlayerManager.getPlayer()
        videoFile?.let { video ->
            if (player != null && player.duration > 0) {
                val currentPosition = player.currentPosition
                val duration = player.duration
                val progress = currentPosition.toFloat() / duration.toFloat()
                videoRepository.saveWatchProgress(video.id, progress, duration)
                videoRepository.saveLastPlayPosition(video.id, currentPosition)
                android.util.Log.d("VideoPlayerActivity", "Saved progress on pause: ${(progress * 100).toInt()}% at position ${currentPosition}ms")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        
        // Save final progress before destroying
        val player = videoPlayerManager.getPlayer()
        videoFile?.let { video ->
            if (player != null && player.duration > 0) {
                val currentPosition = player.currentPosition
                val duration = player.duration
                val progress = currentPosition.toFloat() / duration.toFloat()
                videoRepository.saveWatchProgress(video.id, progress, duration)
                videoRepository.saveLastPlayPosition(video.id, currentPosition)
                android.util.Log.d("VideoPlayerActivity", "Saved final progress: ${(progress * 100).toInt()}% at position ${currentPosition}ms")
            }
        }
        
        videoPlayerManager.release()
    }
}