package com.lsj.mp4.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lsj.mp4.data.models.FolderItem
import com.lsj.mp4.data.models.VideoFile
import com.lsj.mp4.utils.MediaScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {
    private val mediaScanner = MediaScanner(context)
    private val sharedPrefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
    
    private val _videos = MutableLiveData<List<VideoFile>>()
    val videos: LiveData<List<VideoFile>> = _videos
    
    private val _folders = MutableLiveData<List<FolderItem>>()
    val folders: LiveData<List<FolderItem>> = _folders
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _scanProgress = MutableLiveData<String>()
    val scanProgress: LiveData<String> = _scanProgress
    
    suspend fun refreshVideoLibrary() {
        _isLoading.postValue(true)
        _scanProgress.postValue("Scanning video files...")
        
        try {
            val scannedVideos = mediaScanner.scanVideos()
            Log.d("VideoRepository", "Scanned ${scannedVideos.size} videos")
            
            val enhancedVideos = scannedVideos.map { video ->
                val progress = getWatchProgress(video.id)
                val isCompleted = getCompletionStatus(video.id)
                val lastWatched = getLastWatchedTime(video.id)
                
                Log.d("VideoRepository", "Video ${video.title}: progress=$progress, completed=$isCompleted")
                
                video.copy(
                    lastPlayPosition = getLastPlayPosition(video.id),
                    watchProgress = progress,
                    isCompleted = isCompleted,
                    lastWatched = lastWatched
                )
            }
            
            _videos.postValue(enhancedVideos)
            _scanProgress.postValue("Organizing folders...")
            
            val foldersData = mediaScanner.scanFolders(enhancedVideos)
            _folders.postValue(foldersData)
            
            _scanProgress.postValue("Scan complete - ${enhancedVideos.size} videos found")
            Log.d("VideoRepository", "Found ${foldersData.size} folders")
            
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error scanning videos", e)
            _scanProgress.postValue("Error: ${e.message}")
        } finally {
            _isLoading.postValue(false)
            Log.d("VideoRepository", "Loading completed, setting isLoading to false")
        }
    }
    
    fun saveLastPlayPosition(videoId: Long, position: Long) {
        sharedPrefs.edit().putLong("position_$videoId", position).apply()
    }
    
    fun getLastPlayPosition(videoId: Long): Long {
        return sharedPrefs.getLong("position_$videoId", 0L)
    }
    
    fun searchVideos(query: String): List<VideoFile> {
        return _videos.value?.filter { 
            it.title.contains(query, ignoreCase = true) ||
            it.parentFolder.contains(query, ignoreCase = true)
        } ?: emptyList()
    }
    
    fun getRecentVideos(): List<VideoFile> {
        val recentVideoIds = getRecentVideoIds()
        return _videos.value?.filter { it.id in recentVideoIds } ?: emptyList()
    }
    
    private fun getRecentVideoIds(): List<Long> {
        return sharedPrefs.all.entries
            .filter { it.key.startsWith("position_") && it.value as Long > 0 }
            .sortedByDescending { sharedPrefs.getLong("${it.key}_last_played", 0L) }
            .take(10)
            .map { it.key.substringAfter("position_").toLong() }
    }
    
    fun saveLastPlayedTime(videoId: Long) {
        sharedPrefs.edit().putLong("position_${videoId}_last_played", System.currentTimeMillis()).apply()
    }
    
    fun saveWatchProgress(videoId: Long, progress: Float, duration: Long) {
        val isCompleted = progress >= 0.95f
        sharedPrefs.edit().apply {
            putFloat("progress_$videoId", progress)
            putBoolean("completed_$videoId", isCompleted)
            putLong("last_watched_$videoId", System.currentTimeMillis())
            if (isCompleted) {
                putLong("completed_at_$videoId", System.currentTimeMillis())
            }
            apply()
        }
        Log.d("VideoRepository", "Saved progress for video $videoId: ${(progress * 100).toInt()}%")
    }
    
    fun getWatchProgress(videoId: Long): Float {
        return sharedPrefs.getFloat("progress_$videoId", 0f)
    }
    
    fun getCompletionStatus(videoId: Long): Boolean {
        return sharedPrefs.getBoolean("completed_$videoId", false)
    }
    
    fun getLastWatchedTime(videoId: Long): Long {
        return sharedPrefs.getLong("last_watched_$videoId", 0L)
    }
    
    fun getCompletedVideos(): List<VideoFile> {
        return _videos.value?.filter { it.isCompleted } ?: emptyList()
    }
    
    fun getInProgressVideos(): List<VideoFile> {
        return _videos.value?.filter { it.isWatched && !it.isCompleted } ?: emptyList()
    }
    
    fun clearProgress(videoId: Long) {
        sharedPrefs.edit().apply {
            remove("progress_$videoId")
            remove("completed_$videoId")
            remove("last_watched_$videoId")
            remove("completed_at_$videoId")
            apply()
        }
    }
    
    suspend fun updateVideosAndFolders(updatedVideos: List<VideoFile>) {
        _videos.postValue(updatedVideos)
        val foldersData = mediaScanner.scanFolders(updatedVideos)
        _folders.postValue(foldersData)
    }
    
}
