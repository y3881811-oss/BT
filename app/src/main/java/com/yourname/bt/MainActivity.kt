package com.yourname.bt

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.bt.data.RetrofitClient
import com.yourname.bt.data.VideoItem
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        loadingBar = findViewById(R.id.loadingBar)
        errorText = findViewById(R.id.errorText)

        // 设置横向排列的列表
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getPopularVideos()
                if (response.code == 0) {
                    val videoList = response.data.list
                    
                    // 数据加载成功，隐藏提示，显示列表
                    loadingBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    
                    // 绑定适配器
                    recyclerView.adapter = VideoAdapter(videoList) { video ->
                        // 这里就是点击事件，我们后续在这里写播放逻辑
                        Toast.makeText(this@MainActivity, "点击了: ${video.title}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError("请求失败: ${response.message}")
                }
            } catch (e: Exception) {
                showError("网络异常: ${e.message}")
            }
        }
    }

    private fun showError(msg: String) {
        loadingBar.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = msg
    }
}