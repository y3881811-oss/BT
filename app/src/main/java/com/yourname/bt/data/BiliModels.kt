package com.yourname.bt.data

data class BiliResponse(val code: Int, val message: String, val data: BiliData)
data class BiliData(val list: List<VideoItem>)
data class VideoItem(val bvid: String, val title: String, val pic: String, val duration: Int, val owner: Owner, val stat: VideoStat)
data class Owner(val name: String)
data class VideoStat(val view: Int, val danmaku: Int)

data class PageListResponse(val code: Int, val data: List<PageData>)
data class PageData(val cid: Long)

data class PlayUrlResponse(val code: Int, val data: PlayUrlData)
data class PlayUrlData(val dash: DashData?, val durl: List<Durl>? )
data class DashData(val video: List<DashMedia>, val audio: List<DashMedia>?)
data class DashMedia(val id: Int, val baseUrl: String, val codecs: String, val bandwidth: Int)
data class Durl(val url: String)

// ======== 新增：扫码登录模型 ========
data class QrCodeResponse(val code: Int, val data: QrCodeData)
data class QrCodeData(val url: String, val qrcode_key: String)

data class QrPollResponse(val code: Int, val data: QrPollData)
data class QrPollData(val url: String, val refresh_token: String, val timestamp: Long, val code: Int, val message: String)