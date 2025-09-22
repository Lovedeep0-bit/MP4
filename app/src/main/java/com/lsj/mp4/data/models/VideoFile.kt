package com.lsj.mp4.data.models

import android.net.Uri

data class VideoFile(
    val id: Long,
    val title: String,
    val path: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String,
    val thumbnailPath: String? = null,
    val parentFolder: String,
    val resolution: String? = null,
    val lastPlayPosition: Long = 0L,
    val watchProgress: Float = 0f, // 0.0 to 1.0 (0% to 100%)
    val isCompleted: Boolean = false,
    val lastWatched: Long = 0L
) {
    val progressPercentage: Int
        get() = (watchProgress * 100).toInt()
    
    val isWatched: Boolean
        get() = watchProgress > 0.1f // Consider watched if more than 10% viewed
    
    val isFullyWatched: Boolean
        get() = watchProgress >= 0.95f // Consider fully watched if 95% or more viewed
}

data class FolderItem(
    val name: String,
    val path: String,
    val videoCount: Int,
    val videos: List<VideoFile> = emptyList()
)
