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
        val durationText: TextView = view.findViewById(R.id.durationText)
        val statText: TextView = view.findViewById(R.id.statText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        
        holder.titleText.text = video.title
        holder.ownerText.text = video.owner.name

        // 时长格式化：120 -> 02:00
        val minutes = video.duration / 60
        val seconds = video.duration % 60
        holder.durationText.text = String.format("%02d:%02d", minutes, seconds)

        // 播放量格式化：32000 -> 3.2万
        val viewStr = if (video.stat.view >= 10000) {
            String.format("%.1f万", video.stat.view / 10000f)
        } else {
            video.stat.view.toString()
        }
        holder.statText.text = "▶ $viewStr  💬 ${video.stat.danmaku}"

        // 给图片加上圆角
        val roundOptions = RequestOptions().transform(RoundedCorners(12))

        Glide.with(holder.itemView.context)
            .load(video.pic)
            .apply(roundOptions.override(480, 270))
            .into(holder.coverImage)

        holder.itemView.setOnClickListener {
            onClick(video)
        }
    }

    override fun getItemCount() = videoList.size
}