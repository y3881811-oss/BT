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
    val owner: Owner
)

data class Owner(
    val name: String
)