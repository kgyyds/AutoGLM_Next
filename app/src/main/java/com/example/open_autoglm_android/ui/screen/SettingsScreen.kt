package com.example.open_autoglm_android.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.open_autoglm_android.ui.viewmodel.SettingsViewModel
import androidx.core.content.ContextCompat
import com.example.open_autoglm_android.service.OverlayService
import com.example.open_autoglm_android.model.ModelParams

@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var overlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    // 当屏幕可见时检查服务状态
    LaunchedEffect(Unit) {
        viewModel.checkAccessibilityService()
    }
    
    // 使用 DisposableEffect 在每次进入设置页面时刷新状态
    DisposableEffect(Unit) {
        viewModel.checkAccessibilityService()
        onDispose { }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 无障碍服务状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isAccessibilityEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "无障碍服务",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (uiState.isAccessibilityEnabled) {
                                "已启用"
                            } else {
                                "未启用 - 点击前往设置"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!uiState.isAccessibilityEnabled) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("前往设置")
                        }
                    }
                }
            }
        }
        // 悬浮窗权限卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (overlayEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                    
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("悬浮窗权限", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (overlayEnabled) "已授权" else "未授权 - 点击前往设置",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!overlayEnabled) {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            context.startActivity(intent)
                        }) {
                            Text("前往设置")
                        }
                    }
                    if (overlayEnabled) {
    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            val intent = Intent(context, OverlayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("启动悬浮窗")
    }
                    }
                }
            }
        }
        Divider()
        
        // API Key 设置
        OutlinedTextField(
            value = uiState.apiKey,
            onValueChange = { viewModel.updateApiKey(it) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("输入你的 API Key") }
        )
        
        // Base URL 设置
        OutlinedTextField(
            value = uiState.baseUrl,
            onValueChange = { viewModel.updateBaseUrl(it) },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://open.bigmodel.cn/api/paas/v4") }
        )
        
        // Model Name 设置
        OutlinedTextField(
            value = uiState.modelName,
            onValueChange = { viewModel.updateModelName(it) },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("autoglm-phone") }
        )
        
        // 保存按钮
        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("保存设置")
            }
        }
        
        // 成功/错误提示
        uiState.saveSuccess?.let {
            if (it) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "设置已保存",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("关闭")
                    }
                }
            }
        }
/// 添加一些滑动条，调整 AI 参数，变量作用于全局参数


// Max Tokens
Text("Max Tokens: ${ModelParams.maxTokens}")
Slider(
    value = ModelParams.maxTokens.toFloat(),
    onValueChange = { ModelParams.maxTokens = it.toInt() },
    valueRange = 1000f..8000f,
    steps = 7
)

// Temperature
Text("Temperature: ${"%.2f".format(ModelParams.temperature)}")
Slider(
    value = ModelParams.temperature.toFloat(),
    onValueChange = { ModelParams.temperature = it.toDouble() },
    valueRange = 0f..2f
)

// Top P
Text("Top P: ${"%.2f".format(ModelParams.topP)}")
Slider(
    value = ModelParams.topP.toFloat(),
    onValueChange = { ModelParams.topP = it.toDouble() },
    valueRange = 0f..1f
)

// Frequency Penalty
Text("Frequency Penalty: ${"%.2f".format(ModelParams.frequencyPenalty)}")
Slider(
    value = ModelParams.frequencyPenalty.toFloat(),
    onValueChange = { ModelParams.frequencyPenalty = it.toDouble() },
    valueRange = 0f..2f
)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 说明文字
        Text(
            text = "说明：\n" +
                    "1. 请先开启无障碍服务\n" +
                    "2. 在智谱平台申请 API Key\n" +
                    "3. 填写 API Key 和其他配置\n" +
                    "4. 保存设置后即可使用",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
