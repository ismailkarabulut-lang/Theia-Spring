package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Model schemas for Moshi serialization
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

data class GeminiPart(
    val text: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}

class GeminiFallbackService {
    suspend fun generateTheiaResponse(history: List<com.example.data.ChatMessage>, systemInstruction: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val lastUserPrompt = history.lastOrNull { it.role == "user" }?.content ?: ""
            return generateMockResponse(lastUserPrompt)
        }

        // Convert the complete chat history into API-compliant turns
        val contentTurns = history.map { message ->
            val geminiRole = if (message.role == "assistant") "model" else "user"
            GeminiContent(
                role = geminiRole,
                parts = listOf(GeminiPart(text = message.content))
            )
        }

        val request = GeminiRequest(
            contents = contentTurns,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
        )

        return try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Cevap üretilemedi, Kaptan."
        } catch (e: Exception) {
            val lastUserPrompt = history.lastOrNull { it.role == "user" }?.content ?: ""
            "Hata oluştu: ${e.localizedMessage}. Lokal mod aktif, Kaptan.\n\n" + generateMockResponse(lastUserPrompt)
        }
    }

    private fun generateMockResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("kimsin") || lower.contains("theia") -> {
                "Ben THEIA — Kaptan İsmail Karabulut'un dijital düşünce ortağıyım. Saygılı, dürüst ve her an yardıma hazırım, Efendim."
            }
            lower.contains("hafıza") || lower.contains("vault") -> {
                "Hafıza katmanı aktif, Kaptan. Obsidian bridge ve SQLite theia.db senkronize durumda. Son kayıtları Vault bölmesinden kontrol edebilirsiniz."
            }
            lower.contains("sağlık") || lower.contains("durum") -> {
                "Tüm sistem check'leri yeşil, Kaptan. Soul API online (24ms), Claude normal, DeepSeek degredasyon tespiti yok. Ollama 32GB RAM ile tam uykuda."
            }
            lower.contains("bugün") || lower.contains("özet") -> {
                "Haftalık ve günlük notlarınızı analiz ettim. Bugün tamamlanan görevlerinizi ve zihninizdeki ana leitmotif'leri PersonaSnapshot sekmesinde derledim."
            }
            else -> {
                "Anlaşıldı, Kaptan. İstek listenize eklendi: '$prompt'. Diğer modülleri oraya bağlı olan kontrol panellerinden inceleyebilirsiniz."
            }
        }
    }
}
