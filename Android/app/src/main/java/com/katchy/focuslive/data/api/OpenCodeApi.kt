package com.katchy.focuslive.data.api

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenCodeApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: OpenCodeRequest
    ): OpenCodeResponse
}

@Keep
data class OpenCodeRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<OpenCodeMessage>,
    @SerializedName("temperature")
    val temperature: Double = 0.7
)

@Keep
data class OpenCodeMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

@Keep
data class OpenCodeResponse(
    @SerializedName("choices")
    val choices: List<OpenCodeChoice>? = null
)

@Keep
data class OpenCodeChoice(
    @SerializedName("message")
    val message: OpenCodeMessage? = null
)
