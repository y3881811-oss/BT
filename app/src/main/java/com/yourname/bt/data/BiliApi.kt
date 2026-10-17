package com.yourname.bt.data

import retrofit2.http.GET
import retrofit2.http.Query

interface BiliApi {
    @GET("x/web-interface/popular?ps=20&pn=1")
    suspend fun getPopularVideos(): BiliResponse

    @GET("x/player/pagelist")
    suspend fun getCid(@Query("bvid") bvid: String): PageListResponse

    // BiliPai 风格的完整播放参数
    @GET("x/player/wbi/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 80,
        @Query("fnval") fnval: Int = 16,
        @Query("fnver") fnver: Int = 0,
        @Query("fourk") fourk: Int = 1,
        @Query("platform") platform: String = "pc",
        @Query("session") session: String = ""
    ): PlayUrlResponse

    @GET("x/passport-login/web/qrcode/generate")
    suspend fun getQrCode(): QrCodeResponse

    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(@Query("qrcode_key") key: String): QrPollResponse

    @GET("x/web-interface/nav")
    suspend fun getUserInfo(): UserInfoResponse
}