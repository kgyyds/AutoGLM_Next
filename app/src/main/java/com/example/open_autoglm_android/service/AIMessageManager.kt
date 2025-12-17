// AIMessageManager.kt
package com.example.open_autoglm_android.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AIMessage(val content: String)

object AIMessageManager {
    private val _msgFlow = MutableStateFlow<List<AIMessage>>(emptyList())
    val msgFlow: StateFlow<List<AIMessage>> = _msgFlow

    fun postMessage(msg: String) {
        val current = _msgFlow.value.toMutableList()
        current.add(AIMessage(msg))
        _msgFlow.value = current
    }
}
