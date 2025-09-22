package com.lsj.mp4.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lsj.mp4.R
import com.lsj.mp4.data.models.VideoFile
import java.io.File

class VideosAdapter(private val onVideoClick: (VideoFile) -> Unit) : 
    RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {
    
    private var videos = listOf<VideoFile>()
    private var isGridView = false
    
    fun updateVideos(newVideos: List<VideoFile>) {
        videos = newVideos
        notifyDataSetChanged()
    }
    
    fun setGridView(gridView: Boolean) {
        isGridView = gridView
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val layoutId = if (isGridView) R.layout.item_video_grid else R.layout.item_video_list
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }
    
    override fun getItemCount() = videos.size
    
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.videoTitle)
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val metaText: TextView? = itemView.findViewById(R.id.videoMeta)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val completionIcon: ImageView = itemView.findViewById(R.id.completionIcon)
        
        fun bind(video: VideoFile) {
            // Show title without file extension
            val cleanTitle = try {
                val fileName = File(video.path).nameWithoutExtension
                if (fileName.isNotBlank()) fileName else video.title.substringBeforeLast('.', video.title)
            } catch (_: Exception) {
                video.title.substringBeforeLast('.', video.title)
            }
            titleText.text = cleanTitle
            // Meta line under thumbnail (e.g., duration formatted HH:MM:SS)
            metaText?.text = formatDuration(video.duration)
            
            // Show progress indicators
            updateProgressIndicators(video)
            
            // Load thumbnail
            Glide.with(itemView.context)
                .load(video.uri)
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .centerCrop()
                .into(thumbnailImage)
            
            itemView.setOnClickListener {
                onVideoClick(video)
            }
        }
        
        private fun updateProgressIndicators(video: VideoFile) {
            android.util.Log.d("VideosAdapter", "Updating progress for ${video.title}: progress=${video.watchProgress}, isWatched=${video.isWatched}, isCompleted=${video.isCompleted}")
            
            // Always reset visibility first
            progressBar.visibility = View.GONE
            completionIcon.visibility = View.GONE
            
            if (video.isCompleted) {
                // Show completion checkmark
                android.util.Log.d("VideosAdapter", "Showing completion checkmark for ${video.title}")
                completionIcon.visibility = View.VISIBLE
            } else if (video.isWatched) {
                // Show progress bar
                android.util.Log.d("VideosAdapter", "Showing progress bar for ${video.title}: ${video.progressPercentage}%")
                progressBar.visibility = View.VISIBLE
                progressBar.progress = video.progressPercentage
            } else {
                // Hide both indicators for unwatched videos
                android.util.Log.d("VideosAdapter", "Hiding indicators for unwatched video: ${video.title}")
            }
        }
        
        private fun formatDuration(durationMs: Long): String {
            val seconds = durationMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            } else {
                String.format("%02d:%02d", minutes, seconds % 60)
            }
        }
        
        // No file size display
    }
}
