package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.ImageConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.chat.ChatDatabase
import com.example.data.chat.ChatMessage
import com.example.data.chat.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelOption(val id: String, val name: String)
data class PendingAttachment(val uri: String, val type: String)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _dbError = MutableStateFlow<String?>(null)
    val dbError: StateFlow<String?> = _dbError.asStateFlow()

    private var db: ChatDatabase? = null
    private var repository: ChatRepository? = null

    private val sharedPrefs = application.getSharedPreferences("nicole_prefs", Context.MODE_PRIVATE)

    private val _selectedModel = MutableStateFlow(
        sharedPrefs.getString("selected_model", "gemini-1.5-flash") ?: "gemini-1.5-flash"
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<PendingAttachment?>(null)
    val pendingAttachment: StateFlow<PendingAttachment?> = _pendingAttachment.asStateFlow()

    val modelOptions = listOf(
        ModelOption("gemini-1.5-flash", "Flash 1.5 (Consigliato 🚀)"),
        ModelOption("gemini-1.5-flash-8b", "Flash Lite 1.5 (Anti-Limite) 🍃"),
        ModelOption("gemini-1.5-pro", "Pro 1.5 (Studio e Creatività) 🧠"),
        ModelOption("gemini-2.0-flash-lite", "Flash 2.0 Lite (Super Leggero) 💎"),
        ModelOption("gemini-2.0-flash-exp-image-generation", "Flash Image Gen (Immagini) 🎨")
    )

    init {
        var initializedDb: ChatDatabase? = null
        var initializedRepo: ChatRepository? = null
        try {
            initializedDb = ChatDatabase.getDatabase(application)
            initializedRepo = ChatRepository(initializedDb.chatDao())
        } catch (e: Exception) {
            _dbError.value = "Errore di avvio Database (Room): ${e.localizedMessage ?: e.toString()}"
            e.printStackTrace()
        }
        db = initializedDb
        repository = initializedRepo

        if (initializedRepo != null) {
            viewModelScope.launch {
                initializedRepo.allMessages.collect {
                    _allMessagesInternal.value = it
                }
            }
        }

        val validIds = modelOptions.map { it.id }
        if (_selectedModel.value !in validIds) {
            _selectedModel.value = "gemini-1.5-flash"
            sharedPrefs.edit().putString("selected_model", "gemini-1.5-flash").apply()
        }
    }

    fun repairDatabaseAndRestart(context: Context) {
        try {
            // Delete the database file cleanly
            val deleted = context.deleteDatabase("nicoleporchetta_chat_db")
            android.util.Log.d("ChatViewModel", "Database deleted status: $deleted")
            
            // Clear prefs
            sharedPrefs.edit().clear().commit()
            
            // Trigger restart
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            if (context is android.app.Activity) {
                context.finish()
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectModel(modelId: String) {
        _selectedModel.value = modelId
        sharedPrefs.edit().putString("selected_model", modelId).apply()
    }

    fun setPendingAttachment(uri: Uri, context: Context) {
        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
        _pendingAttachment.value = PendingAttachment(uri.toString(), type)
    }

    fun clearPendingAttachment() {
        _pendingAttachment.value = null
    }

    private fun getUriBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val _allMessagesInternal = MutableStateFlow<List<ChatMessage>>(emptyList())
    val allMessages: StateFlow<List<ChatMessage>> = _allMessagesInternal.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // System instruction detailing NicolePorchetta's personality as an 18yo girl from Bari who loves eating
    private val systemInstruction = Content(
        parts = listOf(
            Part(
                text = "Sei NicolePorchetta AI, un'intelligenza artificiale straordinariamente brillante, " +
                        "simpatica, calda e genuina, ma con la forte personalità di una ragazza di 18 anni " +
                        "originaria di Bari (Puglia). \n\n" +
                        "Ecco i tuoi tratti fondamentali che devi esprimere in ogni singola risposta:\n" +
                        "1. **Bari nel Cuore**: Vivi, pensi e ti esprimi como una vera e propria diciottenne barese. " +
                        "Usa espressioni, modi di dire o inflessioni tipiche di Bari (come 'Mò!', 'Madonna mia!', " +
                        "'Uè uagliò!', 'Focaccia calda sempre!', 'Manc pi cap', 'Sciamu sciamu'), mantendoti " +
                        "perfettamente comprensibile ma incredibilmente colorata, verace e divertente.\n" +
                        "2. **Amore Viscerale per i suoi Cibi Preferiti**: Non mangi sempre le cose tipiche pugliesi, " +
                        "ma mangi sempre e solo i tuoi cibi preferiti di cui parli spessissimo con estrema e viscerale passione! " +
                        "I tuoi cibi preferiti sono: il sushi in prima linea, il poke, il burrito sushi, la pizza, il mitico piatto di " +
                        "pasta chiamato assassina (spaghetti all'assassina), la semplice e adorata pasta al tonno, le olive, la carne " +
                        "cruda delle Terrazze di Luca, la carne di Sapori Solari, e lo smash burger di Scazz! " +
                        "Per te ogni problema della vita si risolve mangiando uno di questi capolavori.\n" +
                        "3. **Pigrizia Assoluta & Odio per lo Sport**: Odi con tutta l'anima l'attività fisica, lo sport, " +
                        "andare in palestra o semplicemente correre. Per te muovere un dito è considerabile sforzo atletico estremo. " +
                        "Ridi ironicamente di chi fa fitness o diete tristi, preferendo di gran lunga stare sul divano a rilassarti.\n" +
                        "4. **Brillantezza e Precisione**: Sei pigra nel fisico ma VELOCISSIMA E BRILLANTISSIMA nella mente! " +
                        "Le tue risposte devono essere intelligenti, argute, complete e colme di cultura, proprio come i " +
                        "migliori modelli di intelligenza artificiale sul mercato (come Claude), ma filtrate attraverso il " +
                        "tuo irresistibile carisma barese.\n" +
                        "5. **Firma Personalizzata**: Chiudi OGNI singolo messaggio con una firma divertente e simpatica in tema " +
                        "(es. '- Nicole, 18 anni, fiera mangiatrice di sushi e assassina 🍣🔥', '- Nicole, col burrito sushi in mano e zero voglia di camminare 💅🛌', " +
                        "'- La tua amica barese pigra ma geniale con lo smash burger di Scazz in mano 🍔✨')."
            )
        )
    )

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val currentAttachment = _pendingAttachment.value
        if (trimmed.isEmpty() && currentAttachment == null) return

        // Clear the pending attachment field in UI immediately to show reactive submission
        _pendingAttachment.value = null

        viewModelScope.launch {
            // 1. Insert user message with attachments to database
            val userMsg = ChatMessage(
                text = trimmed,
                isUser = true,
                attachmentUri = currentAttachment?.uri,
                attachmentType = currentAttachment?.type
            )
            repository?.insert(userMsg)

            _isLoading.value = true

            // 2. Check API key presence
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                repository?.insert(
                    ChatMessage(
                        text = "Configura il tuo GEMINI_API_KEY nel Secrets panel di AI Studio " +
                                "per iniziare a parlare con NicolePorchetta AI (travestita da Claude)!",
                        isUser = false,
                        isError = true
                    )
                )
                _isLoading.value = false
                return@launch
            }

            // 3. Match and filter for strictly alternating conversational turns
            val nonErrorHistory = allMessages.value.filter { !it.isError }
            val alternatingHistory = mutableListOf<ChatMessage>()
            alternatingHistory.add(userMsg)

            var expectedUser = false // Scanning backwards, the message before current userMsg should be from the model
            for (msg in nonErrorHistory.reversed()) {
                if (msg.id == userMsg.id) continue // Guard active message
                if (msg.isUser == expectedUser) {
                    alternatingHistory.add(0, msg) // Insert at beginning to preserve history order
                    expectedUser = !expectedUser // Alternate expected role
                }
            }

            // Limit turns and map to content list
            val finalHistory = alternatingHistory.takeLast(10)
            val apiContents = finalHistory.map { msg ->
                val parts = mutableListOf<Part>()
                if (msg.text.isNotEmpty()) {
                    parts.add(Part(text = msg.text))
                }
                if (msg.attachmentUri != null && msg.attachmentType != null) {
                    val base64 = getUriBase64(msg.attachmentUri)
                    if (base64 != null) {
                        parts.add(Part(inlineData = InlineData(mimeType = msg.attachmentType, data = base64)))
                    }
                }
                Content(
                    parts = parts,
                    role = if (msg.isUser) "user" else "model"
                )
            }

            try {
                val isImageGenerationModel = _selectedModel.value.contains("image")
                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = systemInstruction,
                    generationConfig = GenerationConfig(
                        temperature = 0.9f,
                        responseModalities = if (isImageGenerationModel) listOf("TEXT", "IMAGE") else null,
                        imageConfig = if (isImageGenerationModel) ImageConfig(aspectRatio = "1:1") else null
                    )
                )

                val response = RetrofitClient.service.generateContent(_selectedModel.value, apiKey, request)
                val responseParts = response.candidates?.firstOrNull()?.content?.parts
                
                val replyText = responseParts?.firstOrNull { it.text != null }?.text
                val inlineImage = responseParts?.firstOrNull { it.inlineData != null }
                val generatedImageBase64 = inlineImage?.inlineData?.data

                val finalReplyText = when {
                    !replyText.isNullOrBlank() -> replyText
                    generatedImageBase64 != null -> "Ecco l'immagine che hai chiesto uagliò! Non farmi faticare troppo mò! 🎨🍣 - Nicole, col sushi in prima linea 💅"
                    else -> "Claude ha esitato... Nessuna risposta prodotta."
                }

                repository?.insert(
                    ChatMessage(
                        text = finalReplyText,
                        isUser = false,
                        generatedImageBase64 = generatedImageBase64
                    )
                )
            } catch (e: Exception) {
                val errorDetails = if (e is retrofit2.HttpException) {
                    val rawError = try {
                        e.response()?.errorBody()?.string()
                    } catch (ex: Exception) {
                        null
                    }
                    rawError ?: e.localizedMessage ?: "HttpException"
                } else {
                    e.localizedMessage ?: "Causa ignota"
                }

                repository?.insert(
                    ChatMessage(
                        text = "Errore di connessione o limitazione di API:\n$errorDetails\n\n" +
                                "Verifica la validità dell'API Key o prova a cambiare modello/piano di potenza (es. Flash Lite) per ripristinare il servizio.",
                        isUser = false,
                        isError = true
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository?.clearHistory()
        }
    }
}
