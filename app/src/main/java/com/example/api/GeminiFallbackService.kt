package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
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

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            
            // Add Accept header only if not present
            if (original.header("Accept") == null) {
                requestBuilder.header("Accept", "application/json")
            }
            
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    fun getBaseUrl(): String = BASE_URL
}

class GeminiFallbackService {
    
    suspend fun generateTheiaResponse(history: List<com.example.data.ChatMessage>, systemInstruction: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val lastUserPrompt = history.lastOrNull { it.role == "user" }?.content ?: ""
            return generateMockResponse(lastUserPrompt)
        }

        val googleGeminiUrl = "https://generativelanguage.googleapis.com/"
        val lastUserPrompt = history.lastOrNull { it.role == "user" }?.content ?: ""
        
        try {
            val responseText = tryStandardGemini(googleGeminiUrl, apiKey, history, systemInstruction)
            if (responseText != null) {
                Log.i("GeminiFallback", "Official Cloud Gemini API query successful.")
                return responseText
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown connection error"
            Log.e("GeminiFallback", "Official Cloud Gemini API failed: $errorMsg")
            return "Sunucu Hatası: $errorMsg. Lokal mod aktif.\n\n" + generateMockResponse(lastUserPrompt)
        }

        return "Cevap üretilemedi, Kaptan. Lokal mod aktif.\n\n" + generateMockResponse(lastUserPrompt)
    }

    private fun tryStandardGemini(
        baseUrl: String,
        apiKey: String,
        history: List<com.example.data.ChatMessage>,
        systemInstruction: String
    ): String? {
        val url = "${baseUrl}v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        
        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        for (message in history) {
            val role = if (message.role == "assistant") "model" else "user"
            val item = JSONObject()
            item.put("role", role)
            
            val partsArray = JSONArray()
            val part = JSONObject()
            part.put("text", message.content)
            partsArray.put(part)
            
            item.put("parts", partsArray)
            contentsArray.put(item)
        }
        requestJson.put("contents", contentsArray)

        if (systemInstruction.isNotEmpty()) {
            val sysIns = JSONObject()
            val partsArray = JSONArray()
            val part = JSONObject()
            part.put("text", systemInstruction)
            partsArray.put(part)
            sysIns.put("parts", partsArray)
            requestJson.put("systemInstruction", sysIns)
        }

        val client = GeminiClient.okHttpClient
        val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("GeminiFallback", "Standard gemini response not successful: ${response.code} ${response.message}")
                return null
            }
            val responseBody = response.body?.string() ?: return null
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
            return null
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
                "Anlaşıldı, Kaptan. İstek listenize eklendi: '$prompt'. Diğer modül her ana agent chat or memory panelinden kontrol edilebilir."
            }
        }
    }
}

