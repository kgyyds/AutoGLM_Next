package com.example.open_autoglm_android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.open_autoglm_android.R
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var toggleBtn: ImageView
    private lateinit var params: WindowManager.LayoutParams

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var collapsed = false

    private val adapter = AIMessageAdapter()

    companion object {
        const val CHANNEL_ID = "overlay_service_channel"

        private const val EXPANDED_WIDTH = 200
        private const val EXPANDED_HEIGHT = 300
        private const val COLLAPSED_SIZE = 48
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
            EXPANDED_WIDTH,
            EXPANDED_HEIGHT,
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
        params.y = 120

        setupDrag()
        setupToggle()

        windowManager.addView(overlayView, params)

        // 订阅 AI 消息
        scope.launch {
            AIMessageManager.msgFlow.collect { list ->
                if (!collapsed) {
                    adapter.submitList(list)
                    recyclerView.scrollToPosition(list.size - 1)
                }
            }
        }
    }

    /** 拖动逻辑 */
    private fun setupDrag() {
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var downX = 0f
            private var downY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        downX = event.rawX
                        downY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - downX).toInt()
                        params.y = initialY + (event.rawY - downY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    /** 折叠 / 展开 */
    private fun setupToggle() {
        toggleBtn.setOnClickListener {
            collapsed = !collapsed

            if (collapsed) {
                recyclerView.visibility = View.GONE
                params.width = COLLAPSED_SIZE
                params.height = COLLAPSED_SIZE
                toggleBtn.setImageResource(R.drawable.ic_expand)
            } else {
                recyclerView.visibility = View.VISIBLE
                params.width = EXPANDED_WIDTH
                params.height = EXPANDED_HEIGHT
                toggleBtn.setImageResource(R.drawable.ic_collapse)
            }

            windowManager.updateViewLayout(overlayView, params)
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