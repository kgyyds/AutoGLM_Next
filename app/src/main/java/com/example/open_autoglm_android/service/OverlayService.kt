package com.example.open_autoglm_android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.open_autoglm_android.R
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var toggleBtn: View

    private val adapter = AIMessageAdapter()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var collapsed = false
    private lateinit var params: WindowManager.LayoutParams

    companion object {
        const val CHANNEL_ID = "overlay_service_channel"
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, buildNotification())

        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_layout, null)

        recyclerView = overlayView.findViewById(R.id.overlay_recycler)
        toggleBtn = overlayView.findViewById(R.id.btn_toggle)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        // 拖动悬浮窗
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var lastX = 0
            private var lastY = 0
            private var downX = 0f
            private var downY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = params.x
                        lastY = params.y
                        downX = event.rawX
                        downY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = lastX + (event.rawX - downX).toInt()
                        params.y = lastY + (event.rawY - downY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return false
                    }
                }
                return false
            }
        })

        // 折叠 / 展开
        toggleBtn.setOnClickListener {
            collapsed = !collapsed
            recyclerView.visibility = if (collapsed) View.GONE else View.VISIBLE
        }

        windowManager.addView(overlayView, params)

        // 订阅 AI 消息
        scope.launch {
            AIMessageManager.msgFlow.collect { list ->
                adapter.submitList(list)
                recyclerView.scrollToPosition(list.size - 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::overlayView.isInitialized) {
            windowManager.removeViewImmediate(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Service")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}