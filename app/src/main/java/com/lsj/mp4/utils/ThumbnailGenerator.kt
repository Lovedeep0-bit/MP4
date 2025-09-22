package com.lsj.mp4.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import com.lsj.mp4.data.models.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThumbnailGenerator(private val context: Context) {
    
    private val thumbnailCache = LruCache<String, Bitmap>(50) // Cache 50 thumbnails
    
    suspend fun generateThumbnail(videoFile: VideoFile): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${videoFile.id}_${videoFile.path.hashCode()}"
        
        // Check cache first
        thumbnailCache.get(cacheKey)?.let { return@withContext it }
        
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoFile.uri)
            
            val thumbnail = retriever.getFrameAtTime(
                1000000L, // 1 second
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            retriever.release()
            
            thumbnail?.let {
                // Scale down to save memory
                val scaledThumbnail = Bitmap.createScaledBitmap(it, 160, 120, true)
                thumbnailCache.put(cacheKey, scaledThumbnail)
                scaledThumbnail
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailGenerator", "Error generating thumbnail", e)
            null
        }
    }
    
    fun clearCache() {
        thumbnailCache.evictAll()
    }
    
    fun getCachedThumbnail(videoFile: VideoFile): Bitmap? {
        val cacheKey = "${videoFile.id}_${videoFile.path.hashCode()}"
        return thumbnailCache.get(cacheKey)
    }
}