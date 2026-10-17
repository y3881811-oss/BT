package com.yourname.bt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.bt.data.NetworkModule
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 【核心】初始化网络模块
        NetworkModule.init(this)

        recyclerView = findViewById(R.id.recyclerView)
        loadingBar = findViewById(R.id.loadingBar)
        errorText = findViewById(R.id.errorText)
        btnLogin = findViewById(R.id.btnLogin)

        recyclerView.layoutManager = GridLayoutManager(this, 4)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                // 使用 NetworkModule.api
                val response = NetworkModule.api.getPopularVideos()
                if (response.code == 0) {
                    val videoList = response.data.list
                    
                    loadingBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    
                    recyclerView.adapter = VideoAdapter(videoList) { video ->
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                        intent.putExtra("bvid", video.bvid)
                        startActivity(intent)
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