package com.example.open_autoglm_android.util

import android.graphics.Bitmap
import android.util.Log

object BitmapUtils {
    /**
     * 检查位图是否全黑或几乎全黑
     * @param bitmap 要检查的位图
     * @param threshold 阈值，如果黑色像素比例超过此值，认为是全黑（默认 0.98，即 98%）
     * @return true 如果位图是全黑的
     */
    fun isBitmapBlack(bitmap: Bitmap, threshold: Double = 0.98): Boolean {
        if (bitmap.width == 0 || bitmap.height == 0) {
            Log.w("BitmapUtils", "Bitmap 尺寸为 0")
            return true
        }
        
        // 如果 Bitmap 是 HARDWARE 格式，需要先转换
        val accessibleBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            Log.d("BitmapUtils", "转换 HARDWARE Bitmap 为 ARGB_8888")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            null
        }
        
        val targetBitmap = accessibleBitmap ?: bitmap
        
        try {
            // 采样检查，使用更多的采样点以获得更准确的结果
            // 在 1080x1920 的屏幕上，采样约 100 个点
            val samplePoints = 100
            val stepX = maxOf(1, targetBitmap.width / 10)
            val stepY = maxOf(1, targetBitmap.height / 10)
            
            var blackPixels = 0
            var totalPixels = 0
            var minR = 255
            var minG = 255
            var minB = 255
            var maxR = 0
            var maxG = 0
            var maxB = 0
            
            for (y in 0 until targetBitmap.height step stepY) {
                for (x in 0 until targetBitmap.width step stepX) {
                    val pixel = targetBitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    // 记录 RGB 值的范围
                    minR = minOf(minR, r)
                    minG = minOf(minG, g)
                    minB = minOf(minB, b)
                    maxR = maxOf(maxR, r)
                    maxG = maxOf(maxG, g)
                    maxB = maxOf(maxB, b)
                    
                    // 如果 RGB 值都很低（小于 10），认为是黑色
                    if (r < 10 && g < 10 && b < 10) {
                        blackPixels++
                    }
                    totalPixels++
                }
            }
            
            val blackRatio = blackPixels.toDouble() / totalPixels
            Log.d("BitmapUtils", "截图检测: 尺寸=${targetBitmap.width}x${targetBitmap.height}, " +
                    "采样点数=$totalPixels, 黑色像素=$blackPixels, 黑色比例=${String.format("%.2f%%", blackRatio * 100)}, " +
                    "RGB范围: R[$minR-$maxR] G[$minG-$maxG] B[$minB-$maxB], " +
                    "阈值=${String.format("%.2f%%", threshold * 100)}")
            
            val isBlack = blackRatio >= threshold
            if (isBlack) {
                Log.w("BitmapUtils", "检测到截图是全黑的 (${String.format("%.2f%%", blackRatio * 100)} 黑色像素)")
            }
            
            return isBlack
        } finally {
            // 如果创建了临时 Bitmap，需要回收
            accessibleBitmap?.recycle()
        }
    }
}

