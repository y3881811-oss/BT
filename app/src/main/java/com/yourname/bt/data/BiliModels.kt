package com.yourname.bt.data

data class BiliResponse(val code: Int, val message: String, val data: BiliData)
data class BiliData(val list: List<VideoItem>)
data class VideoItem(val bvid: String, val title: String, val pic: String, val duration: Int, val owner: Owner, val stat: VideoStat)
data class Owner(val name: String)
data class VideoStat(val view: Int, val danmaku: Int)

data class PageListResponse(val code: Int, val data: List<PageData>)
data class PageData(val cid: Long)

// ======== 新增：DASH 播放数据模型 ========
data class PlayUrlResponse(val code: Int, val data: PlayUrlData)

data class PlayUrlData(
    val dash: DashData?,
    val durl: List<Durl>? // 兼容旧版 mp4
)

data class DashData(
    val video: List<DashMedia>,
    val audio: List<DashMedia>?
)

data class DashMedia(
    val id: Int, // 画质代码
    val baseUrl: String, // 媒体流地址
    val codecs: String,  // 编码格式
    val bandwidth: Int
)

data class Durl(val url: String)