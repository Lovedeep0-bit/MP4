package com.lsj.mp4.utils

object VideoPlayerConfig {
    // Buffer durations in milliseconds
    const val MIN_BUFFER_DURATION = 15000 // 15 seconds
    const val MAX_BUFFER_DURATION = 50000 // 50 seconds
    const val MIN_PLAYBACK_START_BUFFER = 2500 // 2.5 seconds
    const val MIN_PLAYBACK_RESUME_BUFFER = 5000 // 5 seconds
    
    // Thumbnail generation settings
    const val THUMBNAIL_WIDTH = 320
    const val THUMBNAIL_HEIGHT = 180
    const val THUMBNAIL_QUALITY = 80
    
    // Player settings
    const val DEFAULT_VIDEO_QUALITY = "auto"
    const val MAX_VIDEO_QUALITY = "1080p"
    
    // Gesture settings
    const val SEEK_SENSITIVITY = 5f
    const val BRIGHTNESS_SENSITIVITY = 1000f
    const val VOLUME_SENSITIVITY = 1000f
}
