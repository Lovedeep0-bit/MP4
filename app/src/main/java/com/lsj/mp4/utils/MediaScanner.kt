package com.lsj.mp4.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lsj.mp4.data.models.FolderItem
import com.lsj.mp4.data.models.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScanner(private val context: Context) {
    
    companion object {
        private val VIDEO_EXTENSIONS = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
        private val VIDEO_MIME_TYPES = arrayOf(
            "video/mp4", "video/avi", "video/x-msvideo", "video/quicktime",
            "video/x-ms-wmv", "video/x-flv", "video/webm", "video/3gpp"
        )
    }
    
    suspend fun scanVideos(): List<VideoFile> = withContext(Dispatchers.IO) {
        android.util.Log.d("MediaScanner", "Starting comprehensive video scan")
        
        // First try to scan Movies folder specifically
        val moviesVideos = scanMoviesFolder()
        android.util.Log.d("MediaScanner", "Found ${moviesVideos.size} videos in Movies folder")
        
        // Then scan all other videos
        val allVideos = scanAllVideos()
        android.util.Log.d("MediaScanner", "Found ${allVideos.size} total videos")
        
        // Combine and deduplicate
        val combinedVideos = (moviesVideos + allVideos).distinctBy { it.path }
        android.util.Log.d("MediaScanner", "Final count: ${combinedVideos.size} unique videos")
        
        combinedVideos
    }
    
    private suspend fun scanMoviesFolder(): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RESOLUTION
        )
        
        // Focus on Movies folder and all video types
        val selection = "${MediaStore.Video.Media.DATA} LIKE ? AND ${MediaStore.Video.Media.MIME_TYPE} IN (${VIDEO_MIME_TYPES.joinToString(",") { "?" }})"
        val selectionArgs = arrayOf("%/Movies/%") + VIDEO_MIME_TYPES
        
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val path = cursor.getString(dataColumn) ?: continue
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val mimeType = cursor.getString(mimeColumn) ?: "video/*"
                val resolution = cursor.getString(resolutionColumn)
                
                // Validate file exists and has valid extension
                val file = File(path)
                if (file.exists() && file.extension.lowercase() in VIDEO_EXTENSIONS) {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    
                    videos.add(
                        VideoFile(
                            id = id,
                            title = name,
                            path = path,
                            uri = uri,
                            duration = duration,
                            size = size,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                            parentFolder = file.parent ?: "Unknown",
                            resolution = resolution
                        )
                    )
                }
            }
        }
        
        videos
    }
    
    private suspend fun scanAllVideos(): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RESOLUTION
        )
        
        // Scan all videos without MIME type restriction to catch more files
        val selection = "${MediaStore.Video.Media.DATA} IS NOT NULL"
        
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val path = cursor.getString(dataColumn) ?: continue
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val mimeType = cursor.getString(mimeColumn) ?: "video/*"
                val resolution = cursor.getString(resolutionColumn)
                
                // Validate file exists and has valid extension
                val file = File(path)
                if (file.exists() && file.extension.lowercase() in VIDEO_EXTENSIONS) {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    
                    videos.add(
                        VideoFile(
                            id = id,
                            title = name,
                            path = path,
                            uri = uri,
                            duration = duration,
                            size = size,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                            parentFolder = file.parent ?: "Unknown",
                            resolution = resolution
                        )
                    )
                }
            }
        }
        
        videos
    }
    
    suspend fun scanFolders(videos: List<VideoFile>): List<FolderItem> = withContext(Dispatchers.Default) {
        android.util.Log.d("MediaScanner", "Processing ${videos.size} videos into folders")
        
        // Group videos by their parent folder, but handle special cases
        val folderGroups = videos.groupBy { video ->
            val parentPath = video.parentFolder
            val parentName = File(parentPath).name
            
            // If videos are in a root media folder (like Movies), treat each video individually
            // unless they're in a subfolder
            if (isRootMediaFolder(parentName) && !isInSubfolder(video.path, parentPath)) {
                video.path // Use full path as unique identifier for individual videos
            } else {
                parentPath // Use parent folder for actual subfolders
            }
        }
        
        val folders = mutableListOf<FolderItem>()

        folderGroups.forEach { (groupKey, folderVideos) ->
            val parentPath = folderVideos.first().parentFolder
            val parentName = File(parentPath).name
            
            android.util.Log.d("MediaScanner", "Group: $groupKey, Videos: ${folderVideos.size}")

            // Create a folder only when the directory actually contains multiple videos
            // AND it's not a root media folder
            if (folderVideos.size > 1 && !isRootMediaFolder(parentName)) {
                android.util.Log.d("MediaScanner", "Creating folder for $parentName with ${folderVideos.size} videos")
                folders.add(
                    FolderItem(
                        name = parentName,
                        path = parentPath,
                        videoCount = folderVideos.size,
                        videos = folderVideos.sortedBy { it.title }
                    )
                )
            } else {
                // Single videos or videos in root folders should remain individual
                android.util.Log.d("MediaScanner", "Creating individual item for video: ${folderVideos.first().title}")
                folderVideos.forEach { video ->
                    folders.add(
                        FolderItem(
                            name = video.title,
                            path = video.path,
                            videoCount = 1,
                            videos = listOf(video)
                        )
                    )
                }
            }
        }

        android.util.Log.d("MediaScanner", "Final result: ${folders.size} items (${folders.count { it.videoCount > 1 }} folders, ${folders.count { it.videoCount == 1 }} individual videos)")
        folders.sortedBy { it.name }
    }
    
    private fun isRootMediaFolder(folderName: String): Boolean {
        val rootFolders = listOf("Movies", "Videos", "Download", "DCIM", "Camera")
        return rootFolders.any { folderName.contains(it, ignoreCase = true) }
    }
    
    private fun isInSubfolder(videoPath: String, parentPath: String): Boolean {
        // Check if the video is in a subfolder of the parent path
        val relativePath = videoPath.removePrefix(parentPath).trimStart('/')
        return relativePath.contains('/')
    }
}
