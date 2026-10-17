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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.yourname.bt.data.RetrofitClient
import kotlinx.coroutines.launch

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

        val bvid = intent.getStringExtra("bvid")
        if (bvid.isNullOrEmpty()) {
            showError("视频ID为空")
            return
        }

        loadVideoUrl(bvid)
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

                val videoStream = dash.video.maxByOrNull { it.id }
                if (videoStream == null) {
                    showError("无法解析视频流")
                    return@launch
                }

                loadingBar.visibility = View.GONE
                startPlay(videoStream, dash.audio?.maxByOrNull { it.bandwidth })

            } catch (e: Exception) {
                showError("网络异常: ${e.message}")
            }
        }
    }

    private fun startPlay(video: com.yourname.bt.data.DashMedia, audio: com.yourname.bt.data.DashMedia?) {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer

        val videoMediaItem = MediaItem.Builder()
            .setUri(video.baseUrl)
            .setMimeType(MimeTypes.VIDEO_MP4) 
            .build()
        val videoSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(videoMediaItem)

        val mediaSource = if (audio != null) {
            val audioMediaItem = MediaItem.Builder()
                .setUri(audio.baseUrl)
                .setMimeType(MimeTypes.AUDIO_MP4)
                .build()
            val audioSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(audioMediaItem)
            MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }

        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // 如果黑屏报错，这里会显示错误代码
                showError("播放出错: ${error.errorCodeName}")
            }
        })
    }

    private fun showError(msg: String) {
        loadingBar.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = msg
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}