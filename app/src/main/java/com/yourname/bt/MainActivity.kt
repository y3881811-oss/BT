package com.yourname.bt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.size.Scale
import com.yourname.bt.data.RetrofitClient
import com.yourname.bt.data.VideoItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme() {
                HomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen() {
    var videoList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getPopularVideos()
            if (response.code == 0) {
                videoList = response.data.list
            } else {
                errorMsg = "请求失败: ${response.message}"
            }
        } catch (e: Exception) {
            errorMsg = "网络异常: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF141414)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Text(text = "正在加载热门视频...", color = Color.White, fontSize = 18.sp)
        } else if (errorMsg != null) {
            Text(text = errorMsg!!, color = Color.Red, fontSize = 16.sp)
        } else {
            TvLazyRow(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(videoList) { video ->
                    VideoCard(video)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoCard(video: VideoItem) {
    Card(
        modifier = Modifier.width(240.dp),
        onClick = { /* 播放逻辑 */ }
    ) {
        Column {
            // 【针对 512MB 内存优化】强制加载低分辨率图片
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(video.pic)
                    .size(480, 270) // 限制加载分辨率，防爆内存
                    .scale(Scale.FILL)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(135.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.owner.name,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}