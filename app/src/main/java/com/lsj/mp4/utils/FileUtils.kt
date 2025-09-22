package com.lsj.mp4.utils

import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
    
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp * 1000) // Convert from seconds to milliseconds
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
    
    fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
        val extension = getFileExtension(fileName)
        return videoExtensions.contains(extension)
    }
}
