package com.lsj.mp4

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.lsj.mp4.data.models.FolderItem
import com.lsj.mp4.data.models.VideoFile
import com.lsj.mp4.data.repository.VideoRepository
import com.lsj.mp4.ui.adapters.FoldersAdapter
import com.lsj.mp4.ui.adapters.VideosAdapter
import com.lsj.mp4.ui.player.VideoPlayerActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var videoRepository: VideoRepository
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var foldersAdapter: FoldersAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var headerTitle: TextView
    
    private var isShowingFolder = false
    private var currentFolder: FolderItem? = null
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupRepository()
        setupRecyclerView()
        setupBackButton()
        requestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh progress data when returning from video player
        refreshProgressData()
        
        // If showing a folder, refresh the folder view with updated progress
        if (isShowingFolder && currentFolder != null) {
            refreshCurrentFolderView()
        }
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyText = findViewById(R.id.emptyText)
        headerTitle = findViewById(R.id.headerTitle)
    }
    
    private fun setupRepository() {
        videoRepository = VideoRepository(this)
        
        videoRepository.folders.observe(this) { folders ->
            foldersAdapter.updateFolders(folders)
            updateEmptyState()
        }
        
        videoRepository.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    private fun setupRecyclerView() {
        foldersAdapter = FoldersAdapter { folder ->
            openFolderView(folder)
        }
        
        // Use grid layout with larger cards; span count 2 on phones
        val span = 2
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, span)
        recyclerView.adapter = foldersAdapter
        // Ensure adapter inflates grid variants (thumbnails on top, text at bottom)
        foldersAdapter.setGridView(true)
    }
    
    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isShowingFolder) {
                    // Go back to main folder view
                    showMainView()
                } else {
                    // Close the app
                    finish()
                }
            }
        })
    }
    
    
    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            refreshVideoLibrary()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 100 && grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshVideoLibrary()
        }
    }
    
    private fun refreshVideoLibrary() {
        lifecycleScope.launch {
            videoRepository.refreshVideoLibrary()
            
            // Add some test progress data after scanning
            addTestProgressData()
        }
    }
    
    private fun addTestProgressData() {
        lifecycleScope.launch {
            // Wait a bit for videos to be loaded
            kotlinx.coroutines.delay(1000)
            
            val videos = videoRepository.videos.value
            if (videos != null && videos.isNotEmpty()) {
                android.util.Log.d("MainActivity", "Adding test progress to ${videos.size} videos")
                
                // Mark first video as completed
                videoRepository.saveWatchProgress(videos[0].id, 1.0f, videos[0].duration)
                
                // Mark second video as partially watched (50%)
                if (videos.size > 1) {
                    videoRepository.saveWatchProgress(videos[1].id, 0.5f, videos[1].duration)
                }
                
                // Mark third video as partially watched (25%)
                if (videos.size > 2) {
                    videoRepository.saveWatchProgress(videos[2].id, 0.25f, videos[2].duration)
                }
                
                // Refresh the UI without triggering loading state
                refreshVideosWithProgress()
            }
        }
    }
    
    private fun refreshProgressData() {
        lifecycleScope.launch {
            val currentVideos = videoRepository.videos.value ?: return@launch
            val updatedVideos = currentVideos.map { video ->
                val progress = videoRepository.getWatchProgress(video.id)
                val isCompleted = videoRepository.getCompletionStatus(video.id)
                val lastWatched = videoRepository.getLastWatchedTime(video.id)
                
                android.util.Log.d("MainActivity", "Refreshing progress for ${video.title}: progress=$progress, completed=$isCompleted")
                
                video.copy(
                    watchProgress = progress,
                    isCompleted = isCompleted,
                    lastWatched = lastWatched
                )
            }
            
            // Update videos and folders without triggering loading state
            videoRepository.updateVideosAndFolders(updatedVideos)
        }
    }
    
    private fun refreshCurrentFolderView() {
        currentFolder?.let { folder ->
            val updatedFolderVideos = folder.videos.map { video ->
                val progress = videoRepository.getWatchProgress(video.id)
                val isCompleted = videoRepository.getCompletionStatus(video.id)
                val lastWatched = videoRepository.getLastWatchedTime(video.id)
                
                video.copy(
                    watchProgress = progress,
                    isCompleted = isCompleted,
                    lastWatched = lastWatched
                )
            }
            
            // Update the current folder with fresh progress data
            currentFolder = folder.copy(videos = updatedFolderVideos)
            
            // Update the adapter with fresh data
            videosAdapter?.updateVideos(updatedFolderVideos)
            
            android.util.Log.d("MainActivity", "Refreshed folder view with updated progress data")
        }
    }
    
    private fun refreshVideosWithProgress() {
        lifecycleScope.launch {
            val currentVideos = videoRepository.videos.value ?: return@launch
            val updatedVideos = currentVideos.map { video ->
                val progress = videoRepository.getWatchProgress(video.id)
                val isCompleted = videoRepository.getCompletionStatus(video.id)
                val lastWatched = videoRepository.getLastWatchedTime(video.id)
                
                android.util.Log.d("MainActivity", "Updating video ${video.title}: progress=$progress, completed=$isCompleted")
                
                video.copy(
                    watchProgress = progress,
                    isCompleted = isCompleted,
                    lastWatched = lastWatched
                )
            }
            
            // Update videos and folders without triggering loading state
            videoRepository.updateVideosAndFolders(updatedVideos)
        }
    }
    
    private fun updateEmptyState() {
        val hasContent = foldersAdapter.itemCount > 0
        emptyText.visibility = if (hasContent) android.view.View.GONE else android.view.View.VISIBLE
    }
    
    private fun openVideoPlayer(video: VideoFile) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra("video_uri", video.uri.toString())
            putExtra("video_title", video.title)
            putExtra("video_id", video.id)
            
            // Pass last position for resume playback (unless completed)
            val lastPosition = if (video.isCompleted) {
                0L // Start from beginning if completed
            } else {
                videoRepository.getLastPlayPosition(video.id)
            }
            putExtra("last_position", lastPosition)
            
            android.util.Log.d("MainActivity", "Opening video ${video.title}: completed=${video.isCompleted}, lastPosition=${lastPosition}ms")
        }
        startActivity(intent)
    }
    
    private fun openFolderView(folder: FolderItem) {
        // If it's a single video, play it directly
        if (folder.videoCount == 1) {
            openVideoPlayer(folder.videos.first())
            return
        }
        
        // Show videos in that folder with updated progress data
        val folderVideos = folder.videos.map { video ->
            val progress = videoRepository.getWatchProgress(video.id)
            val isCompleted = videoRepository.getCompletionStatus(video.id)
            val lastWatched = videoRepository.getLastWatchedTime(video.id)
            
            video.copy(
                watchProgress = progress,
                isCompleted = isCompleted,
                lastWatched = lastWatched
            )
        }
        
        videosAdapter = VideosAdapter { video ->
            openVideoPlayer(video)
        }
        videosAdapter.updateVideos(folderVideos)
        recyclerView.adapter = videosAdapter
        isShowingFolder = true
        currentFolder = folder
        headerTitle.text = folder.name
        headerTitle.visibility = android.view.View.VISIBLE
        updateEmptyState()
    }
    
    private fun showMainView() {
        recyclerView.adapter = foldersAdapter
        isShowingFolder = false
        currentFolder = null
        headerTitle.text = ""
        headerTitle.visibility = android.view.View.GONE
        updateEmptyState()
    }
}