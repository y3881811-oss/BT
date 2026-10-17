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

data class QrCodeResponse(val code: Int, val data: QrCodeData)
data class QrCodeData(val url: String, val qrcode_key: String)

data class QrPollResponse(val code: Int, val data: QrPollData)
data class QrPollData(val url: String, val refresh_token: String, val timestamp: Long, val code: Int, val message: String)

data class NavResponse(val code: Int, val data: NavData)
data class NavData(val wbiImg: WbiImg?)
data class WbiImg(val imgUrl: String, val subUrl: String)

// ======== 新增：用户个人信息数据模型 ========
data class UserInfoResponse(val code: Int, val data: UserInfoData)

data class UserInfoData(
    val isLogin: Boolean,
    val mid: Long,
    val uname: String,
    val face: String, // 头像链接
    val vip: VipInfo?,
    val level_info: LevelInfo?,
    val stat: UserStat?,
    val coins: Double?,
    val sign: String? // 个人签名
)

data class VipInfo(val status: Int, val label: VipLabel?)
data class VipLabel(val text: String)

data class LevelInfo(val current_level: Int, val current_exp: Int, val next_exp: Int)

data class UserStat(
    val following: Int, // 关注数
    val follower: Int   // 粉丝数
)