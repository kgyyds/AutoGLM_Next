package com.example.open_autoglm_android.network

import com.example.open_autoglm_android.network.dto.ChatRequest
import com.example.open_autoglm_android.network.dto.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AutoGLMApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
