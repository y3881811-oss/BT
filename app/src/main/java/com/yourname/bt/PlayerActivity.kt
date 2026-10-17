package com.yourname.bt

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.yourname.bt.data.DashMedia
import com.yourname.bt.data.RetrofitClient
import com.yourname.bt.data.UserPrefs
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var errorText: TextView
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        loadingBar = findViewById(R.id.loadingBar)
        errorText = findViewById(R.id.errorText)

        try {
            val bvid = intent.getStringExtra("bvid")
            if (bvid.isNullOrEmpty()) {
                showError("视频ID为空")
                return
            }
            loadVideoUrl(bvid)
        } catch (e: Throwable) {
            showError("初始化异常: ${e.message}")
        }
    }

    private fun loadVideoUrl(bvid: String) {
        lifecycleScope.launch {
            try {
                val cidResponse = RetrofitClient.api.getCid(bvid)
                if (cidResponse.code != 0 || cidResponse.data.isEmpty()) {
                    showError("获取 CID 失败")
                    return@launch
                }
                val cid = cidResponse.data[0].cid

                // 接口已更新为 WBI 版本，拦截器会自动加签
                val playResponse = RetrofitClient.api.getPlayUrl(bvid, cid)
                if (playResponse.code != 0) {
                    showError("获取播放地址失败: ${playResponse.code}")
                    return@launch
                }

                val dash = playResponse.data.dash
                if (dash == null || dash.video.isEmpty()) {
                    showError("未获取到 DASH 视频流")
                    return@launch
                }

                val videoStream = dash.video.firstOrNull { it.codecs.contains("avc") } 
                    ?: dash.video.firstOrNull() 

                if (videoStream == null) {
                    showError("无法解析视频流")
                    return@launch
                }

                loadingBar.visibility = View.GONE
                startPlay(videoStream, dash.audio?.firstOrNull())

            } catch (e: Exception) {
                showError("网络异常: ${e.message}")
            }
        }
    }

    private fun startPlay(video: DashMedia, audio: DashMedia?) {
        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val localCookie = UserPrefs.getCookie(this)

            val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                .setDefaultRequestProperties(mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to "https://www.bilibili.com/",
                    "Cookie" to localCookie
                ))

            val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)
            exoPlayer = ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
            playerView.player = exoPlayer

            // 清理转义字符
            var finalVideoUrl = video.baseUrl.replace("\\u0026", "&")
            var finalAudioUrl = audio?.baseUrl?.replace("\\u0026", "&")

            // 老电视兼容：如果 https 失败，可尝试 http（但现代B站多强制https）
            // 这里保留原样，因为强制http可能导致403

            val videoMediaItem = MediaItem.Builder()
                .setUri(finalVideoUrl)
                .setMimeType(MimeTypes.VIDEO_MP4) 
                .build()
            val videoSource = ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(videoMediaItem)

            val mediaSource = if (audio != null && finalAudioUrl != null) {
                val audioMediaItem = MediaItem.Builder()
                    .setUri(finalAudioUrl)
                    .setMimeType(MimeTypes.AUDIO_MP4)
                    .build()
                val audioSource = ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(audioMediaItem)
                MergingMediaSource(videoSource, audioSource)
            } else {
                videoSource
            }

            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    showError("播放出错: ${error.errorCodeName}\n${error.cause?.message}")
                }
            })
        } catch (e: Throwable) {
            showError("播放器异常: ${e.message}")
        }
    }

    private fun showError(msg: String) {
        loadingBar.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = msg
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {}
    }
}