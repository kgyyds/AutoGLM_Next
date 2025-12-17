package com.example.open_autoglm_android.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.open_autoglm_android.service.AutoGLMAccessibilityService

object AccessibilityServiceHelper {
    
    /**
     * 检查无障碍服务是否已启用（通过系统设置检查）
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val serviceName = ComponentName(
            context.packageName,
            AutoGLMAccessibilityService::class.java.name
        ).flattenToString()
        
        return enabledServices.contains(serviceName)
    }
    
    /**
     * 检查无障碍服务是否正在运行（通过实例检查）
     */
    fun isServiceRunning(): Boolean {
        return AutoGLMAccessibilityService.isServiceEnabled()
    }
    
    /**
     * 获取无障碍服务信息
     */
    fun getServiceInfo(context: Context): AccessibilityServiceInfo? {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return null
        
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val serviceName = AutoGLMAccessibilityService::class.java.name
        
        return enabledServices.firstOrNull { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo.name == serviceName
        }
    }
}

