package com.example.open_autoglm_android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.open_autoglm_android.data.PreferencesRepository
import com.example.open_autoglm_android.util.AccessibilityServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "https://open.bigmodel.cn/api/paas/v4",
    val modelName: String = "autoglm-phone",
    val isAccessibilityEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean? = null,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesRepository = PreferencesRepository(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        checkAccessibilityService()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.apiKey.collect { apiKey ->
                _uiState.value = _uiState.value.copy(apiKey = apiKey ?: "")
            }
        }
        viewModelScope.launch {
            preferencesRepository.baseUrl.collect { baseUrl ->
                _uiState.value = _uiState.value.copy(baseUrl = baseUrl ?: "https://open.bigmodel.cn/api/paas/v4")
            }
        }
        viewModelScope.launch {
            preferencesRepository.modelName.collect { modelName ->
                _uiState.value = _uiState.value.copy(modelName = modelName ?: "autoglm-phone")
            }
        }
    }
    
    fun checkAccessibilityService() {
        // 同时检查系统设置和服务实例
        val enabledInSettings = AccessibilityServiceHelper.isAccessibilityServiceEnabled(getApplication())
        val serviceRunning = AccessibilityServiceHelper.isServiceRunning()
        // 服务必须在系统设置中启用，并且实例正在运行
        val enabled = enabledInSettings && serviceRunning
        _uiState.value = _uiState.value.copy(isAccessibilityEnabled = enabled)
    }
    
    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey)
    }
    
    fun updateBaseUrl(baseUrl: String) {
        _uiState.value = _uiState.value.copy(baseUrl = baseUrl)
    }
    
    fun updateModelName(modelName: String) {
        _uiState.value = _uiState.value.copy(modelName = modelName)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            
            try {
                preferencesRepository.saveApiKey(_uiState.value.apiKey)
                preferencesRepository.saveBaseUrl(_uiState.value.baseUrl)
                preferencesRepository.saveModelName(_uiState.value.modelName)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "保存失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, saveSuccess = false)
    }
}
