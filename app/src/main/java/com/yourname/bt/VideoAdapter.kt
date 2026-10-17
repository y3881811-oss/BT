package com.yourname.bt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.yourname.bt.data.VideoItem

class VideoAdapter(
    private val videoList: List<VideoItem>,
    private val onClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverImage: ImageView = view.findViewById(R.id.coverImage)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val ownerText: TextView = view.findViewById(R.id.ownerText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        
        holder.titleText.text = video.title
        holder.ownerText.text = video.owner.name

        // 针对 512MB 内存优化：Glide 会自动处理低内存情况，并限制图片尺寸
        Glide.with(holder.itemView.context)
            .load(video.pic)
            .apply(RequestOptions().override(480, 270)) // 限制解码分辨率，防爆内存
            .into(holder.coverImage)

        holder.itemView.setOnClickListener {
            onClick(video)
        }
    }

    override fun getItemCount() = videoList.size
}