package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenRouterMessage>,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "max_tokens") val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessage
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    @Json(name = "choices") val choices: List<OpenRouterChoice>? = null,
    @Json(name = "error") val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    @Json(name = "message") val message: String? = null,
    @Json(name = "code") val code: Int? = null
)
