package com.yourname.bt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.bt.data.NetworkModule
import com.yourname.bt.data.UserPrefs
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnUser: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NetworkModule.init(this)

        recyclerView = findViewById(R.id.recyclerView)
        loadingBar = findViewById(R.id.loadingBar)
        errorText = findViewById(R.id.errorText)
        btnLogin = findViewById(R.id.btnLogin)
        btnUser = findViewById(R.id.btnUser)

        recyclerView.layoutManager = GridLayoutManager(this, 4)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnUser.setOnClickListener {
            // 如果已经登录，去个人中心；否则去登录页
            if (UserPrefs.getCookie(this).isNotEmpty()) {
                startActivity(Intent(this, UserActivity::class.java))
            } else {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        // 如果登录状态改变了，可以在这里刷新按钮状态
        val isLoggedIn = UserPrefs.getCookie(this).isNotEmpty()
        btnLogin.text = if (isLoggedIn) "切换账号" else "扫码登录"
        btnUser.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
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