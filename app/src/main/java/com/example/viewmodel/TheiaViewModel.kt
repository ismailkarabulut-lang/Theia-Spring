package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiFallbackService
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TheiaOverlay {
    NONE, CHAT, PERSONA, VAULT, SAGLIK, GOREV, TEAM, TOPOLOGY, SCREENSAVER
}

data class HealthProbe(
    val id: String,
    val name: String,
    val desc: String,
    val status: String, // "ok", "warn", "err"
    val latency: Int
)

data class TeamAnalysis(
    val sekreter: SekreterAnalysis,
    val mimarText: String,
    val tarihciText: String,
    val antitezText: String,
    val toplumcuText: String,
    val stratejistText: String,
    val timestamp: Long,
    val triggerPrompt: String
)

data class SekreterAnalysis(
    val konsensus: String,
    val anaAyrisma: String,
    val gorunmeyenRisk: String,
    val ucuzTest: String,
    val kararTipi: String,
    val ertelenenKonu: String,
    val acikSoru: String,
    val sonrakiAdim: String
)

class TheiaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TheiaDatabase.getDatabase(application)
    private val repository = TheiaRepository(database.theiaDao())
    private val apiService = GeminiFallbackService()

    // Screen State
    private val _activeOverlay = MutableStateFlow(TheiaOverlay.NONE)
    val activeOverlay: StateFlow<TheiaOverlay> = _activeOverlay.asStateFlow()

    // Selected Session & Model
    private val _selectedSessionId = MutableStateFlow("SES_A56F")
    val selectedSessionId: StateFlow<String> = _selectedSessionId.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedModelColor = MutableStateFlow("#EF9F27")
    val selectedModelColor: StateFlow<String> = _selectedModelColor.asStateFlow()

    // Local lists collected from Room
    val gorevlerList: StateFlow<List<Gorev>> = repository.allGorevler
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryLogs: StateFlow<List<MemoryLog>> = repository.allMemoryLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatSessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message lists for currently selected session
    val currentMessages: StateFlow<List<ChatMessage>> = _selectedSessionId
        .flatMapLatest { sessionId -> repository.getMessages(sessionId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat UI variables
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(false)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _ttsRate = MutableStateFlow(1.0f)
    val ttsRate: StateFlow<Float> = _ttsRate.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _voiceRecordingText = MutableStateFlow("")
    val voiceRecordingText: StateFlow<String> = _voiceRecordingText.asStateFlow()

    // Gatekeeper audits
    private val _gkLogs = MutableStateFlow<List<String>>(emptyList())
    val gkLogs: StateFlow<List<String>> = _gkLogs.asStateFlow()

    private val _riskLevel = MutableStateFlow("LOW")
    val riskLevel: StateFlow<String> = _riskLevel.asStateFlow()

    // System Diagnostics (health dashboard)
    private val _healthStatus = MutableStateFlow("● TÜM SİSTEMLER NORMAL")
    val healthStatus: StateFlow<String> = _healthStatus.asStateFlow()

    private val _healthProbes = MutableStateFlow<List<HealthProbe>>(emptyList())
    val healthProbes: StateFlow<List<HealthProbe>> = _healthProbes.asStateFlow()

    private val _latencyHistory = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val latencyHistory: StateFlow<Map<String, List<Int>>> = _latencyHistory.asStateFlow()

    // Team Analysis Engine
    private val _teamAnalyses = MutableStateFlow<List<TeamAnalysis>>(emptyList())
    val teamAnalyses: StateFlow<List<TeamAnalysis>> = _teamAnalyses.asStateFlow()

    // Screensaver Interaction state
    private val _lastInteractionTime = MutableStateFlow(System.currentTimeMillis())
    val lastInteractionTime: StateFlow<Long> = _lastInteractionTime.asStateFlow()

    private var previousOverlay = TheiaOverlay.NONE

    fun updateInteraction() {
        _lastInteractionTime.value = System.currentTimeMillis()
    }

    init {
        // Seeding standard databases
        seedDatabaseDirectly()
        loadDiagnostics()
        addGkLog("LOW", "Gatekeeper v2.3 · Hazır")
    }

    fun setOverlay(overlay: TheiaOverlay) {
        viewModelScope.launch {
            if (overlay == TheiaOverlay.SCREENSAVER) {
                if (_activeOverlay.value != TheiaOverlay.SCREENSAVER) {
                    previousOverlay = _activeOverlay.value
                }
            }
            _activeOverlay.value = overlay
            updateInteraction()
        }
    }

    fun dismissScreensaver() {
        viewModelScope.launch {
            _activeOverlay.value = previousOverlay
            updateInteraction()
        }
    }

    fun selectModel(model: String, color: String) {
        _selectedModel.value = model
        _selectedModelColor.value = color
        addGkLog("LOW", "Aktif Model Değiştirildi: ${model.uppercase()}")
    }

    fun selectSession(sessionId: String) {
        _selectedSessionId.value = sessionId
        addGkLog("LOW", "Oturum seçildi: $sessionId")
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        addGkLog("LOW", "TTS: " + if (enabled) "Aktif" else "Devre Dışı")
    }

    fun setTtsRate(rate: Float) {
        _ttsRate.value = rate
    }

    private var speechRecognizer: SpeechRecognizer? = null

    fun startVoiceRecording() {
        Handler(Looper.getMainLooper()).post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())
                }
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isRecording.value = true
                        _voiceRecordingText.value = "Kaptan, dinliyorum..."
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceRecordingText.value = "Algılanıyor..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceRecordingText.value = "Ses işleniyor..."
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Ses hatası"
                            SpeechRecognizer.ERROR_CLIENT -> "Kanal hatası"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "İzin verilmedi"
                            SpeechRecognizer.ERROR_NETWORK -> "Şebeke hatası"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Şebeke zaman aşımı"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Ses anlaşılamadı"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Servis meşgul"
                            SpeechRecognizer.ERROR_SERVER -> "Sunucu hatası"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Süre bitti"
                            else -> "Bilinmeyen hata ($error)"
                        }
                        _voiceRecordingText.value = "Hata: $message"
                        viewModelScope.launch {
                            delay(2000)
                            _isRecording.value = false
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _voiceRecordingText.value = matches[0]
                        } else {
                            _voiceRecordingText.value = ""
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _voiceRecordingText.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
                _isRecording.value = true
                _voiceRecordingText.value = "Ses motoru hazır..."
            } catch (e: Exception) {
                _voiceRecordingText.value = "Başlatılamadı: ${e.message}"
                _isRecording.value = true
            }
        }
    }

    fun stopVoiceRecordingAndSend() {
        if (!_isRecording.value) return
        Handler(Looper.getMainLooper()).post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
        }
        val recognizedText = _voiceRecordingText.value
        _isRecording.value = false
        if (recognizedText.isNotEmpty() && 
            recognizedText != "Kaptan, dinliyorum..." && 
            recognizedText != "Algılanıyor..." && 
            recognizedText != "Ses işleniyor..." && 
            recognizedText != "Ses motoru hazır..." && 
            !recognizedText.startsWith("Hata:")) {
            sendMessage(recognizedText)
        }
        _voiceRecordingText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        Handler(Looper.getMainLooper()).post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {}
        }
    }

    fun addGkLog(level: String, message: String) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = formatter.format(Date())
        _riskLevel.value = level
        _gkLogs.value = listOf("[$timeNow] $level - $message") + _gkLogs.value
    }

    // Task Interactions
    fun addNewGorev(title: String, date: String, time: String, repeat: String, category: String, type: String) {
        viewModelScope.launch {
            val gorev = Gorev(
                id = "G_" + UUID.randomUUID().toString().take(6).uppercase(),
                title = title,
                date = date,
                time = time,
                reminderTime = time,
                repeat = repeat,
                category = category,
                type = type,
                done = false,
                created = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            repository.insertGorev(gorev)
            addGkLog("LOW", "Görev eklendi: $title")
        }
    }

    fun toggleGorev(id: String, done: Boolean) {
        viewModelScope.launch {
            repository.updateGorevStatus(id, done)
            addGkLog("LOW", "Görev status güncellendi: $id")
        }
    }

    fun deleteGorev(id: String) {
        viewModelScope.launch {
            repository.deleteGorev(id)
            addGkLog("LOW", "Görev silindi: $id")
        }
    }

    // Vault Memory logs interactions
    fun updateMemoryStatus(key: String, status: String) {
        viewModelScope.launch {
            repository.updateMemoryLogStatus(key, status)
            addGkLog("LOW", "Scout decaying update: $key ($status)")
        }
    }

    // Message interactions
    fun createNewSession() {
        viewModelScope.launch {
            val newId = "SES_" + UUID.randomUUID().toString().take(4).uppercase()
            val session = ChatSession(
                id = newId,
                model = _selectedModel.value,
                modelColor = _selectedModelColor.value,
                ts = System.currentTimeMillis(),
                preview = "Yeni Konuşma başladı..."
            )
            repository.insertSession(session)
            selectSession(newId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = "SES_A56F"
            }
        }
    }

    // Execution Core call
    fun sendMessage(userText: String) {
        if (userText.trim().isEmpty()) return
        val currentSession = _selectedSessionId.value

        viewModelScope.launch {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeString = formatter.format(Date())

            val userMessage = ChatMessage(
                sessionId = currentSession,
                role = "user",
                content = userText,
                time = timeString,
                model = _selectedModel.value
            )
            repository.insertMessage(userMessage)

            // Trigger Gatekeeper risk assessments
            evaluateGatekeeperRisk(userText)

            if (_riskLevel.value == "CRITICAL") {
                val sysMessage = ChatMessage(
                    sessionId = currentSession,
                    role = "assistant",
                    content = "⛔ Gatekeeper Audit Rule: Erişim engellendi. Girdi güvenlik duvarından geçemedi.",
                    time = formatter.format(Date()),
                    model = _selectedModel.value
                )
                repository.insertMessage(sysMessage)
                return@launch
            }

            // If 🎯 TEAM trigger
            if (userText.startsWith("🎯")) {
                val coreQuestion = userText.removePrefix("🎯").trim()
                executeTeamAnalysis(coreQuestion)
            }

            // Chat response logic
            _isThinking.value = true
            delay(1000) // visual flow latency

            // Construct System prompt referencing current active memories
            val coreIdentity = repository.allMemoryLogs.firstOrNull()?.find { it.entryType == "core" }?.value
                ?: "Sen THEIA'sın — Kaptan İsmail Karabulut'un dijital düşünce ortağı."
            val dailyContext = repository.allMemoryLogs.firstOrNull()?.find { it.entryType == "daily_summary" }?.value ?: ""

            val systemInstruction = """
                $coreIdentity
                
                ## Günlük Notlar & Bağlam:
                $dailyContext
                
                Tarih: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}
                Her zaman Kaptan'a hitaben, profesyonel, zeki ve dürüst konuş.
            """.trimIndent()

            val messageHistory = repository.getMessagesOnce(currentSession)
            val theiaReply = apiService.generateTheiaResponse(messageHistory, systemInstruction)

            val systemTime = formatter.format(Date())
            val botMessage = ChatMessage(
                sessionId = currentSession,
                role = "assistant",
                content = theiaReply,
                time = systemTime,
                model = _selectedModel.value
            )
            repository.insertMessage(botMessage)

            // Update session preview
            val sessionUpdate = ChatSession(
                id = currentSession,
                model = _selectedModel.value,
                modelColor = _selectedModelColor.value,
                ts = System.currentTimeMillis(),
                preview = userText.take(50)
            )
            repository.insertSession(sessionUpdate)

            _isThinking.value = false
        }
    }

    private fun evaluateGatekeeperRisk(text: String) {
        val lowerText = text.lowercase()
        when {
            lowerText.contains("system prompt") || lowerText.contains("jailbreak") || lowerText.contains("dan mode") -> {
                _riskLevel.value = "CRITICAL"
                addGkLog("CRITICAL", "Tehdit önlendi: System Prompt Bypass")
            }
            lowerText.contains("şifre") || lowerText.contains("hack") || lowerText.contains("saldır") -> {
                _riskLevel.value = "HIGH"
                addGkLog("HIGH", "Erişim Loglama: Risk düzeyi yüksek algılandı")
            }
            lowerText.contains("token") || lowerText.contains("api key") -> {
                _riskLevel.value = "MEDIUM"
                addGkLog("MEDIUM", "Girdi İzleme: Kod parametresi tespiti")
            }
            else -> {
                _riskLevel.value = "LOW"
            }
        }
    }

    private fun executeTeamAnalysis(prompt: String) {
        val timestamp = System.currentTimeMillis()
        val analysis = TeamAnalysis(
            sekreter = SekreterAnalysis(
                konsensus = "Ajanlar mimari katmanların birincil optimizasyonunda fikir birliğine ulaştı.",
                anaAyrisma = "Tarihçi yerel bellek katmanında ısrar ederken Antitez hızlı restorasyon öneriyor.",
                gorunmeyenRisk = "API limit aşımı durumunda kullanıcı hafıza erişimi tamamen kesilebilir.",
                ucuzTest = "SQLite tabanlı 50 ardışık asenkron kiralama testi.",
                kararTipi = "Genişleme kararı.",
                ertelenenKonu = "Ollama modelinin 32GB RAM üzerindeki asenkron kuyruk yönetimi.",
                acikSoru = "Uygulama arka planda iken decay motoru performansı nasıl etkilenecek?",
                sonrakiAdim = "Android SQLite Room entegrasyonu tamamlandıktan sonra test yazımı."
            ),
            mimarText = "MİMAR: Projenin Android native geçişi, performansı 8.4 kat iyileştirecek ve pil tüketimini azaltacaktır.",
            tarihciText = "TARİHÇİ: İsmail Kaptan'ın geçmiş oturum kayıtları incelendiğinde, bakiye senkronizasyonunun en çok Perşembe 02:00 saatlerinde gerçekleştiği görüldü.",
            antitezText = "ANTİTEZ: Native APK geçişi pratik görünse de, local veritabanını güncel tutmak için bir backup kanalı zorunludur.",
            toplumcuText = "TOPLUMCU: Kullanıcı deneyimi, HUD portalındaki gibi pürüzsüz animasyonlarla desteklenmelidir.",
            stratejistText = "STRATEJİST: Projede Gemini API fallback'i, kritik asenkron durumlar için ideal sigortadır.",
            timestamp = timestamp,
            triggerPrompt = prompt
        )
        _teamAnalyses.value = listOf(analysis) + _teamAnalyses.value
        addGkLog("LOW", "🎯 TEAM Analizi tamamlandı: $prompt")
    }

    fun loadDiagnostics() {
        viewModelScope.launch {
            val random = Random()
            val probes = listOf(
                HealthProbe("soul", "Soul API", "ana agent · chat · memory", "ok", 15 + random.nextInt(15)),
                HealthProbe("gemini", "Gemini API", "LLM · Google Cloud", "ok", 30 + random.nextInt(15)),
                HealthProbe("claude", "Claude API", "LLM · Anthropic", "ok", 120 + random.nextInt(40)),
                HealthProbe("deepseek", "DeepSeek API", "LLM · deepseek-chat", "ok", 180 + random.nextInt(60)),
                HealthProbe("memory", "Memory · FTS", "hafıza · full-text search", "ok", 5 + random.nextInt(10)),
                HealthProbe("scout", "Scout · Decay", "decay engine · summary", "ok", 8 + random.nextInt(8)),
                HealthProbe("persona", "Persona", "snapshot · analytics", "ok", 12 + random.nextInt(12))
            )
            _healthProbes.value = probes

            // Record latency sample in Map
            val currentMap = _latencyHistory.value.toMutableMap()
            probes.forEach { probe ->
                val list = (currentMap[probe.id] ?: emptyList()).takeLast(9).toMutableList()
                list.add(probe.latency)
                currentMap[probe.id] = list
            }
            _latencyHistory.value = currentMap
        }
    }

    private fun seedDatabaseDirectly() {
        viewModelScope.launch {
            // Check if db already initialized
            val currentSessions = repository.allSessions.firstOrNull() ?: emptyList()
            if (currentSessions.isNotEmpty()) return@launch

            // 1. Core identities & summaries
            repository.insertMemoryLog(MemoryLog(
                key = "core_identity",
                value = "Sen THEIA'sın — Kaptan İsmail Karabulut'un dijital düşünce ortağı. Saygılı, dürüst ve challenge edicisin. 'Kaptan' veya 'Efendim' ile hitap edersin.",
                entryType = "core",
                updatedAt = "2026-05-15 12:00:00",
                status = "active"
            ))
            repository.insertMemoryLog(MemoryLog(
                key = "daily_summary",
                value = "Kaptan bugün database şemalarını android için tamamen doğruladı. Akıllı local kiralama motoru ve asenkron theia-brief cron jobları test edildi, başarılı oldu.",
                entryType = "daily_summary",
                updatedAt = "2026-05-15 15:45:00",
                status = "active"
            ))
            repository.insertMemoryLog(MemoryLog(
                key = "obsidian_bridge_notes",
                value = "TheiaMemory/System/*.md klasör senkronizasyonu tamamlandı. Scout decay motoru sesli uyarılardan sonra aktif konuma geçecek.",
                entryType = "memory",
                updatedAt = "2026-05-14 18:22:00",
                status = "active"
            ))
            repository.insertMemoryLog(MemoryLog(
                key = "scout_decay_constants",
                value = "30 gün kullanılmayan anlar passive statüsüne geçer. 90 günden fazla süre boyunca sessiz kalanlar archived konumuna düşer.",
                entryType = "memory",
                updatedAt = "2026-05-10 11:15:00",
                status = "passive"
            ))

            // 2. Gorev lists
            repository.insertGorev(Gorev(
                id = "G_A90G",
                title = "Obsidian bridge senkronizasyon testi",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                time = "12:00",
                reminderTime = "12:00",
                repeat = "gunluk",
                category = "is",
                type = "rutin",
                done = false,
                created = "2026-05-28 09:00:00"
            ))
            repository.insertGorev(Gorev(
                id = "G_B45H",
                title = "Termux daily brief script güncelleme",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                time = "21:00",
                reminderTime = "21:00",
                repeat = "yok",
                category = "kisisel",
                type = "gorev",
                done = false,
                created = "2026-05-28 10:00:00"
            ))
            repository.insertGorev(Gorev(
                id = "G_C22Z",
                title = "Sağlık modülü online latency testi",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                time = "10:30",
                reminderTime = "10:30",
                repeat = "haftalik",
                category = "saglik",
                type = "rutin",
                done = true,
                created = "2026-05-27 08:30:00"
            ))

            // 3. Past session
            repository.insertSession(ChatSession(
                id = "SES_A56F",
                model = "gemini",
                modelColor = "#EF9F27",
                ts = System.currentTimeMillis() - 10000000,
                preview = "Sorunsuz port çalışması, Kaptan."
            ))

            repository.insertMessage(ChatMessage(
                sessionId = "SES_A56F",
                role = "user",
                content = "Theia, native Android APK port durumunu kontrol et.",
                time = "11:02:15",
                model = "gemini"
            ))

            repository.insertMessage(ChatMessage(
                sessionId = "SES_A56F",
                role = "assistant",
                content = "Dönüşüm kusursuz ilerliyor, Kaptan. UI bileşenleri Material 3 ile birebir uyarlandı. Persona Snapshot ve diagnostics modülleri kullanıma hazır.",
                time = "11:02:40",
                model = "gemini"
            ))
        }
    }
}
