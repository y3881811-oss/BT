package com.yourname.bt.data

import kotlinx.serialization.Serializable

@Serializable
data class BiliResponse(
    val code: Int,
    val message: String,
    val data: BiliData
)

@Serializable
data class BiliData(
    val list: List<VideoItem>
)

@Serializable
data class VideoItem(
    val bvid: String,
    val title: String,
    val pic: String,
    val owner: Owner
)

@Serializable
data class Owner(
    val name: String
)