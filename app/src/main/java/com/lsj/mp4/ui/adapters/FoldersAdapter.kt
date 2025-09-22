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
import com.lsj.mp4.data.models.FolderItem
import com.lsj.mp4.data.models.VideoFile
import java.io.File

class FoldersAdapter(private val onFolderClick: (FolderItem) -> Unit) : 
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var folders = listOf<FolderItem>()
    private var isGridView = false

    private companion object {
        const val TYPE_FOLDER = 0
        const val TYPE_VIDEO = 1
    }
    
    fun updateFolders(newFolders: List<FolderItem>) {
        folders = newFolders
        notifyDataSetChanged()
    }
    
    fun setGridView(gridView: Boolean) {
        isGridView = gridView
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        val item = folders[position]
        return if (item.videoCount == 1) TYPE_VIDEO else TYPE_FOLDER
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_VIDEO) {
            val layoutId = if (isGridView) R.layout.item_video_grid else R.layout.item_video_list
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            VideoViewHolder(view)
        } else {
            val layoutId = if (isGridView) R.layout.item_folder_grid else R.layout.item_folder_list
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            FolderViewHolder(view)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = folders[position]
        if (holder is VideoViewHolder) {
            holder.bind(item.videos.first())
            holder.itemView.setOnClickListener { onFolderClick(item) }
        } else if (holder is FolderViewHolder) {
            holder.bind(item)
        }
    }
    
    override fun getItemCount() = folders.size
    
    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderPreview1: ImageView? = itemView.findViewById(R.id.folderPreview1)
        private val folderPreview2: ImageView? = itemView.findViewById(R.id.folderPreview2)
        private val folderPreview3: ImageView? = itemView.findViewById(R.id.folderPreview3)
        private val folderPreview4: ImageView? = itemView.findViewById(R.id.folderPreview4)
        private val folderName: TextView = itemView.findViewById(R.id.folderName)
        private val videoCount: TextView = itemView.findViewById(R.id.videoCount)
        
        fun bind(folder: FolderItem) {
            folderName.text = folder.name
            videoCount.text = "${folder.videoCount} videos"
            // 2x2 collage from up to 4 videos
            val previews = listOf(folderPreview1, folderPreview2, folderPreview3, folderPreview4)
            previews.forEachIndexed { index, imageView ->
                val v = folder.videos.getOrNull(index)
                if (imageView != null) {
                    if (v != null) {
                        Glide.with(itemView.context)
                            .load(v.uri)
                            .placeholder(R.drawable.ic_video_placeholder)
                            .centerCrop()
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.ic_video_placeholder)
                    }
                }
            }
            
            itemView.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.videoTitle)
        private val metaText: TextView? = itemView.findViewById(R.id.videoMeta)
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val completionIcon: ImageView = itemView.findViewById(R.id.completionIcon)

        fun bind(video: VideoFile) {
            val cleanTitle = try {
                val fileName = File(video.path).nameWithoutExtension
                if (fileName.isNotBlank()) fileName else video.title.substringBeforeLast('.', video.title)
            } catch (_: Exception) {
                video.title.substringBeforeLast('.', video.title)
            }
            titleText.text = cleanTitle
            metaText?.text = formatDuration(video.duration)
            
            // Show progress indicators
            updateProgressIndicators(video)

            Glide.with(itemView.context)
                .load(video.uri)
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .centerCrop()
                .into(thumbnailImage)
        }
        
        private fun updateProgressIndicators(video: VideoFile) {
            android.util.Log.d("FoldersAdapter", "Updating progress for ${video.title}: progress=${video.watchProgress}, isWatched=${video.isWatched}, isCompleted=${video.isCompleted}")
            
            // Always reset visibility first
            progressBar.visibility = View.GONE
            completionIcon.visibility = View.GONE
            
            if (video.isCompleted) {
                // Show completion checkmark
                android.util.Log.d("FoldersAdapter", "Showing completion checkmark for ${video.title}")
                completionIcon.visibility = View.VISIBLE
            } else if (video.isWatched) {
                // Show progress bar
                android.util.Log.d("FoldersAdapter", "Showing progress bar for ${video.title}: ${video.progressPercentage}%")
                progressBar.visibility = View.VISIBLE
                progressBar.progress = video.progressPercentage
            } else {
                // Hide both indicators for unwatched videos
                android.util.Log.d("FoldersAdapter", "Hiding indicators for unwatched video: ${video.title}")
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
