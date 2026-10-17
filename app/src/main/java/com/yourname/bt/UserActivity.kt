package com.yourname.bt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.yourname.bt.data.NetworkModule
import com.yourname.bt.data.UserPrefs
import kotlinx.coroutines.launch

class UserActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUname: TextView
    private lateinit var tvVip: TextView
    private lateinit var tvUid: TextView
    private lateinit var tvLevel: TextView
    private lateinit var pbExp: ProgressBar
    private lateinit var tvFollowing: TextView
    private lateinit var tvFollower: TextView
    private lateinit var tvCoins: TextView
    private lateinit var tvSign: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvUname = findViewById(R.id.tvUname)
        tvVip = findViewById(R.id.tvVip)
        tvUid = findViewById(R.id.tvUid)
        tvLevel = findViewById(R.id.tvLevel)
        pbExp = findViewById(R.id.pbExp)
        tvFollowing = findViewById(R.id.tvFollowing)
        tvFollower = findViewById(R.id.tvFollower)
        tvCoins = findViewById(R.id.tvCoins)
        tvSign = findViewById(R.id.tvSign)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
        
        btnLogout.setOnClickListener {
            // 清空本地 Cookie
            UserPrefs.saveCookie(this, "")
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
            // 返回主界面
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loadUserInfo()
    }

    private fun loadUserInfo() {
        val cookie = UserPrefs.getCookie(this)
        if (cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val response = NetworkModule.api.getUserInfo()
                if (response.code == 0 && response.data.isLogin) {
                    val user = response.data
                    
                    tvUname.text = user.uname
                    tvUid.text = "UID: ${user.mid}"
                    
                    // 加载头像
                    Glide.with(this@UserActivity)
                        .load(user.face)
                        .apply(RequestOptions.circleCropTransform()) // 圆形头像
                        .into(ivAvatar)

                    // 大会员状态
                    if (user.vip?.status == 1) {
                        tvVip.visibility = View.VISIBLE
                        tvVip.text = user.vip.label?.text ?: "大会员"
                    } else {
                        tvVip.visibility = View.GONE
                    }

                    // 等级与经验
                    user.level_info?.let { level ->
                        tvLevel.text = "LV${level.current_level}"
                        // 计算百分比：当前经验 / 升级所需经验
                        val expTotal = level.next_exp.toFloat()
                        val progress = if (expTotal > 0) ((level.current_exp / expTotal) * 100).toInt() else 100
                        pbExp.progress = progress
                    }

                    // 统计数据
                    user.stat?.let { stat ->
                        tvFollowing.text = stat.following.toString()
                        tvFollower.text = stat.follower.toString()
                    }
                    tvCoins.text = user.coins?.toString() ?: "0.0"
                    
                    tvSign.text = user.sign.let { if (it.isNullOrEmpty()) "这个人很懒，什么都没写" else it }

                } else {
                    Toast.makeText(this@UserActivity, "获取信息失败，请重新登录", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}