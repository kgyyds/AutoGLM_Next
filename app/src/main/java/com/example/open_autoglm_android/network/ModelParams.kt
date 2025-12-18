package com.example.open_autoglm_android.model

object ModelParams {
    var maxTokens: Int = 3000           // Int 保留，方便逻辑处理
    var temperature: Double = 0.0       // Double，符合 API 调用要求
    var topP: Double = 0.85
    var frequencyPenalty: Double = 0.2
}