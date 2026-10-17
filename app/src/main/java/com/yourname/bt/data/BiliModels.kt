package com.yourname.bt.data

data class BiliResponse(
    val code: Int,
    val message: String,
    val data: BiliData
)

data class BiliData(
    val list: List<VideoItem>
)

data class VideoItem(
    val bvid: String,
    val title: String,
    val pic: String,
    val duration: Int, // 时长（秒）
    val owner: Owner,
    val stat: VideoStat // 统计数据
)

data class Owner(
    val name: String
)

data class VideoStat(
    val view: Int, // 播放量
    val danmaku: Int // 弹幕数
)