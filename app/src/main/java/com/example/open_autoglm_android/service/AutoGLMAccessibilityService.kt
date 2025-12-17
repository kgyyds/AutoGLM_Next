package com.example.open_autoglm_android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutoGLMAccessibilityService : AccessibilityService() {
    
    companion object {
        private var instance: AutoGLMAccessibilityService? = null
        
        fun getInstance(): AutoGLMAccessibilityService? = instance
        
        fun isServiceEnabled(): Boolean = instance != null
    }
    
    private val _currentApp = MutableStateFlow<String?>(null)
    val currentApp: StateFlow<String?> = _currentApp.asStateFlow()
    
    private val _latestScreenshot = MutableStateFlow<Bitmap?>(null)
    val latestScreenshot: StateFlow<Bitmap?> = _latestScreenshot.asStateFlow()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = it.packageName?.toString()
                    _currentApp.value = packageName
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // 服务中断
    }
    
    fun takeScreenshot(callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Log.d("AutoGLMService", "开始截图...")
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                            Log.d("AutoGLMService", "截图成功")
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val hardwareBuffer = result.hardwareBuffer
                                    val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                                    hardwareBuffer?.close()
                                    
                                    // 将 HARDWARE 格式的 Bitmap 转换为可访问的格式
                                    val bitmap = if (hardwareBitmap != null && hardwareBitmap.config == Bitmap.Config.HARDWARE) {
                                        // 转换为 ARGB_8888 格式以便访问像素
                                        hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    } else {
                                        hardwareBitmap
                                    }
                                    
                                    // 回收硬件 Bitmap（如果已转换）
                                    if (bitmap != hardwareBitmap) {
                                        hardwareBitmap?.recycle()
                                    }
                                    
                                    _latestScreenshot.value = bitmap
                                    Log.d("AutoGLMService", "截图转换成功，尺寸: ${bitmap?.width}x${bitmap?.height}, 格式: ${bitmap?.config}")
                                    callback(bitmap)
                                } else {
                                    Log.w("AutoGLMService", "Android版本不支持")
                                    callback(null)
                                }
                            } catch (e: Exception) {
                                Log.e("AutoGLMService", "处理截图失败", e)
                                callback(null)
                            }
                        }
                        
                        override fun onFailure(errorCode: Int) {
                            Log.e("AutoGLMService", "截图失败，错误码: $errorCode")
                            callback(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("AutoGLMService", "调用截图API失败", e)
                callback(null)
            }
        } else {
            Log.w("AutoGLMService", "Android版本低于R (API 30)，不支持截图。当前版本: ${Build.VERSION.SDK_INT}")
            callback(null)
        }
    }
    
    suspend fun takeScreenshotSuspend(): Bitmap? = suspendCancellableCoroutine { continuation ->
        takeScreenshot { bitmap ->
            continuation.resume(bitmap)
        }
    }
    
    fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow
    }
    
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }
    
    fun tap(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            
            dispatchGesture(gesture, null, null)
        }
    }
    
    fun longPress(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()
            
            dispatchGesture(gesture, null, null)
        }
    }
    
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            dispatchGesture(gesture, null, null)
        }
    }
    
    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    fun performClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 如果节点不可点击，尝试找到父节点
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.recycle()
                    return result
                }
                val oldParent = parent
                parent = parent.parent
                oldParent.recycle()
            }
            false
        }
    }
    
    fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            false
        }
    }
}
