package com.yourname.bt

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.bt.data.RetrofitClient
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

        // 【修改】设置网格布局，每行 4 个 (TV 端横屏通常 4-6 个比较合适)
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getPopularVideos()
                if (response.code == 0) {
                    val videoList = response.data.list
                    
                    loadingBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    
                    recyclerView.adapter = VideoAdapter(videoList) { video ->
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