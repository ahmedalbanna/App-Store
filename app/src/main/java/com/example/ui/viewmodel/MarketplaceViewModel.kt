package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GeminiConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatDao
import com.example.data.database.ChatMessageEntity
import com.example.data.database.MarketplaceItem
import com.example.data.auth.AuthRepository
import com.example.data.database.MarketplaceRepository
import com.example.data.datastore.SearchPreferences
import com.example.data.rag.RagRetriever
import com.example.data.telemetry.Telemetry
import com.example.ui.navigation.MarketplaceTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun ChatMessage.toEntity() = ChatMessageEntity(sender = sender, text = text, timestamp = timestamp)
fun ChatMessageEntity.toModel() = ChatMessage(sender = sender, text = text, timestamp = timestamp)

class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    private val searchPreferences: SearchPreferences? = null,
    private val chatDao: ChatDao? = null,
    private val telemetry: Telemetry? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    companion object {
        fun provideFactory(
            repository: MarketplaceRepository,
            searchPreferences: SearchPreferences? = null,
            chatDao: ChatDao? = null,
            telemetry: Telemetry? = null,
            authRepository: AuthRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MarketplaceViewModel(repository, searchPreferences, chatDao, telemetry, authRepository) as T
                }
            }
    }

    private val _currentTab = MutableStateFlow(MarketplaceTab.APPS.route)
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()
    val currentTabEnum: StateFlow<MarketplaceTab> = _currentTab
        .map { MarketplaceTab.fromRoute(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MarketplaceTab.APPS)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    @OptIn(FlowPreview::class)
    private val _debouncedQuery: StateFlow<String> = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val debouncedQuery: StateFlow<String> = _debouncedQuery
    val recentQueries: StateFlow<List<String>> = searchPreferences?.recentQueries
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    // Telemetry opt-in (Phase 3.4) — exposes DataStore Flow, default false until user opts in
    val isTelemetryOptedIn: StateFlow<Boolean> = telemetry?.isOptedIn
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        ?: MutableStateFlow(false)

    fun setTelemetryOptIn(enabled: Boolean) {
        viewModelScope.launch { telemetry?.setOptIn(enabled) }
    }

    private val _selectedItem = MutableStateFlow<MarketplaceItem?>(null)
    val selectedItem: StateFlow<MarketplaceItem?> = _selectedItem.asStateFlow()

    val allItems: StateFlow<List<MarketplaceItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phase 4.1/4.3: Remote sync + updates
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _availableUpdates = MutableStateFlow<List<MarketplaceItem>>(emptyList())
    val availableUpdates: StateFlow<List<MarketplaceItem>> = _availableUpdates.asStateFlow()
    val hasUpdates: StateFlow<Boolean> = _availableUpdates.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            repository.checkAndPrepulateDatabase()
            // Phase 4.2: Auth — anonymous sign-in (best effort)
            try {
                authRepository?.signInAnonymously()
            } catch (e: Exception) {
                Log.w("MarketplaceViewModel", "Auth sign-in failed: ${e.message}")
            }
            // Phase 4.1: Remote sync (Room as cache, Retrofit as remote) — best effort, keeps local seed if offline
            _isSyncing.value = true
            try {
                repository.syncRemoteCatalog()
            } catch (e: Exception) {
                Log.w("MarketplaceViewModel", "Remote sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
            // Phase 4.3: Check for updates after sync
            try {
                _availableUpdates.value = repository.checkForUpdates()
                if (_availableUpdates.value.isNotEmpty()) {
                    Log.d("MarketplaceViewModel", "Updates available: ${_availableUpdates.value.size}")
                }
            } catch (e: Exception) {
                Log.e("MarketplaceViewModel", "checkForUpdates failed: ${e.message}")
            }
        }
        // Phase 3.3: Load persisted chat history if available, else keep welcome message
        viewModelScope.launch {
            chatDao?.let { dao ->
                if (dao.getCount() == 0) {
                    dao.insertMessage(ChatMessageEntity(sender = "AI", text = "مرحباً بك في متجر الواحة! أنا مساعدك الذكي لتصفح التطبيقات وقراءة واقتراح محتوى تفريحي رائع. كيف يمكنني مساعدتك اليوم؟"))
                }
                dao.getAllMessages().collect { entities ->
                    _chatMessages.value = entities.map { it.toModel() }
                }
            }
        }
        GeminiConfig.logStatus()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncRemoteCatalog()
            _availableUpdates.value = repository.checkForUpdates()
            _isSyncing.value = false
        }
    }

    fun checkForUpdatesNow() {
        viewModelScope.launch {
            _availableUpdates.value = repository.checkForUpdates()
        }
    }

    val appsList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.type == "APP" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val gamesList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.type == "GAME" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val booksList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.type == "BOOK" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val moviesList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.type == "MOVIE" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val musicList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.type == "MUSIC" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookmarkedList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.isBookmarked } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloadedList: StateFlow<List<MarketplaceItem>> = allItems.map { it.filter { it.isDownloaded } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tabToType: Map<String, String> = MarketplaceTab.entries.filter { it.itemType != null }.associate { it.route to it.itemType!! }

    val filteredItems: StateFlow<List<MarketplaceItem>> = combine(allItems, _debouncedQuery, currentTab) { list, query, tab ->
        var res = list
        when {
            tab == "LIBRARY" -> res = res.filter { it.isBookmarked || it.isDownloaded || it.isInstalled }
            tabToType.containsKey(tab) -> res = res.filter { it.type == tabToType[tab] }
            tab in setOf("ASSISTANT", "READ_BOOK_VIEW", "WATCH_MOVIE_VIEW", "PLAY_MUSIC_VIEW") -> res = emptyList()
            else -> res = emptyList()
        }
        if (query.isNotEmpty()) {
            res = res.filter {
                it.titleAr.contains(query, ignoreCase = true) ||
                it.titleEn.contains(query, ignoreCase = true) ||
                it.descriptionAr.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.developerOrCreatorAr.contains(query, ignoreCase = true)
            }
        }
        res
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("AI", "مرحباً بك في متجر الواحة! أنا مساعدك الذكي لتصفح التطبيقات وقراءة واقتراح محتوى تفريحي رائع. كيف يمكنني مساعدتك اليوم؟")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()
    private val _lastUserQuery = MutableStateFlow<String?>(null)

    private val _currentPlayingMusic = MutableStateFlow<MarketplaceItem?>(null)
    val currentPlayingMusic: StateFlow<MarketplaceItem?> = _currentPlayingMusic.asStateFlow()
    private val _isPlayingMusic = MutableStateFlow(false)
    val isPlayingMusic: StateFlow<Boolean> = _isPlayingMusic.asStateFlow()
    private val _musicPlaybackProgress = MutableStateFlow(0.35f)
    val musicPlaybackProgress: StateFlow<Float> = _musicPlaybackProgress.asStateFlow()
    private val _activeReadingBook = MutableStateFlow<MarketplaceItem?>(null)
    val activeReadingBook: StateFlow<MarketplaceItem?> = _activeReadingBook.asStateFlow()
    val bookPageCount = 312
    @Deprecated("Use _activeReadingBook")
    val activeReadingBookLegacy: MutableStateFlow<MarketplaceItem?> get() = _activeReadingBook
    private val _activeWatchingMovie = MutableStateFlow<MarketplaceItem?>(null)
    val activeWatchingMovie: StateFlow<MarketplaceItem?> = _activeWatchingMovie.asStateFlow()
    private val _isPlayingMovie = MutableStateFlow(false)
    val isPlayingMovie: StateFlow<Boolean> = _isPlayingMovie.asStateFlow()
    private val _moviePlaybackProgress = MutableStateFlow(0.12f)
    val moviePlaybackProgress: StateFlow<Float> = _moviePlaybackProgress.asStateFlow()

    fun changeTab(newTab: String) {
        _currentTab.value = newTab
        _searchQuery.value = ""
    }
    fun changeTab(tab: MarketplaceTab) { changeTab(tab.route) }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun onSearchSubmitted(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchPreferences?.saveQuery(query.trim())
            telemetry?.logSearch(query, filteredItems.value.size)
        }
    }
    fun clearRecentQueries() { viewModelScope.launch { searchPreferences?.clearRecents() } }
    fun selectItem(item: MarketplaceItem?) { _selectedItem.value = item }

    fun handleMainActionClick(item: MarketplaceItem) {
        when (item.downloadState) {
            "NONE", "UPDATE_AVAILABLE" -> {
                Log.d("MarketplaceViewModel", "Registering download triggers for: ${item.id}")
                repository.triggerDownloadProgress(item.id, viewModelScope)
                viewModelScope.launch { telemetry?.logSearch("download_${item.type}", 1) }
            }
            "DOWNLOADED" -> viewModelScope.launch { repository.installPackage(item.id); telemetry?.logInstall(item.id, item.type) }
            "INSTALLED" -> { openContent(item); viewModelScope.launch { telemetry?.logInstall(item.id, item.type) } }
            "DOWNLOADING" -> viewModelScope.launch { repository.resetDownloadState(item.id) }
            else -> {}
        }
    }
    fun openContent(item: MarketplaceItem) {
        when (item.type) {
            "BOOK" -> { _activeReadingBook.value = item; _currentTab.value = "READ_BOOK_VIEW" }
            "MOVIE" -> { _activeWatchingMovie.value = item; _isPlayingMovie.value = true; _currentTab.value = "WATCH_MOVIE_VIEW" }
            "MUSIC" -> { _currentPlayingMusic.value = item; _isPlayingMusic.value = true; _currentTab.value = "PLAY_MUSIC_VIEW" }
            else -> {}
        }
    }
    fun setIsPlayingMusic(playing: Boolean) { _isPlayingMusic.value = playing }
    fun toggleIsPlayingMusic() { _isPlayingMusic.value = !_isPlayingMusic.value }
    fun setMusicPlaybackProgress(progress: Float) { _musicPlaybackProgress.value = progress.coerceIn(0f, 1f) }
    fun setIsPlayingMovie(playing: Boolean) { _isPlayingMovie.value = playing }
    fun toggleIsPlayingMovie() { _isPlayingMovie.value = !_isPlayingMovie.value }
    fun setMoviePlaybackProgress(progress: Float) { _moviePlaybackProgress.value = progress.coerceIn(0f, 1f) }
    fun setActiveReadingBook(item: MarketplaceItem?) { _activeReadingBook.value = item }
    fun toggleBookmark(item: MarketplaceItem) {
        viewModelScope.launch {
            repository.toggleBookmark(item.id)
            val currentSelected = _selectedItem.value
            if (currentSelected != null && currentSelected.id == item.id) {
                _selectedItem.value = currentSelected.copy(isBookmarked = !currentSelected.isBookmarked)
            }
            // Phase 4.2: Sync library to cloud
            try {
                val bookmarked = try { bookmarkedList.first().map { it.id } } catch (e: Exception) { emptyList() }
                val downloaded = try { downloadedList.first().map { it.id } } catch (e: Exception) { emptyList() }
                authRepository?.syncLibraryToCloud(bookmarked, downloaded)
            } catch (e: Exception) {
                Log.w("MarketplaceViewModel", "Library sync failed: ${e.message}")
            }
        }
    }
    fun saveBookProgress(itemId: String, page: Int) {
        viewModelScope.launch {
            repository.saveBookProgress(itemId, page)
            val active = _activeReadingBook.value
            if (active != null && active.id == itemId) {
                _activeReadingBook.value = active.copy(lastReadPage = page)
            }
        }
    }
    fun uninstallApp(item: MarketplaceItem) {
        viewModelScope.launch {
            repository.resetDownloadState(item.id)
            if (_selectedItem.value?.id == item.id) { _selectedItem.value = null }
        }
    }

    // Phase 3.1/3.2: RAG + centralized key + streaming simulation
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        _lastUserQuery.value = text
        val userMsg = ChatMessage("USER", text)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true
        persistChatMessage(userMsg)
        viewModelScope.launch { telemetry?.logChatMessage(true) }

        viewModelScope.launch(Dispatchers.IO) {
            val catalog = try { allItems.first() } catch (e: Exception) { emptyList() }
            val ragItems = RagRetriever.retrieve(text, catalog, topK = 3)
            val ragContext = RagRetriever.buildContextBlock(ragItems)
            val ragSystemInstruction = RagRetriever.buildRagSystemInstruction(ragContext)

            val output = if (!GeminiConfig.isConfigured) {
                Log.e("MarketplaceViewModel", "Gemini key ${GeminiConfig.keyStatus}, using RAG offline")
                delay(800)
                generateRagOfflineResponse(text, ragItems)
            } else {
                try {
                    // Try Firebase AI first if available (Phase 3.1), else REST
                    if (GeminiConfig.shouldUseFirebaseAi()) {
                        // Firebase AI path — for now fallback to REST as FirebaseApp may not be configured
                        // In real app: val model = Firebase.aiGenerativeModel("gemini-1.5-flash"); model.generateContent(...)
                        throw Exception("Firebase AI not configured, fallback to REST")
                    }
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = text)))),
                        systemInstruction = Content(parts = listOf(Part(text = ragSystemInstruction)))
                    )
                    val response = RetrofitClient.service.generateContent(GeminiConfig.apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: generateRagOfflineResponse(text, ragItems)
                } catch (e: Exception) {
                    Log.e("MarketplaceViewModel", "Gemini error: ${e.message}")
                    delay(600)
                    generateRagOfflineResponse(text, ragItems)
                }
            }

            // Simulate streaming: chunked emit for more lively UX (Phase 3.1)
            val chunked = output.chunked(40)
            var streamed = ""
            withContext(Dispatchers.Main) {
                val aiMsg = ChatMessage("AI", "")
                _chatMessages.value = _chatMessages.value + aiMsg
                _isChatLoading.value = false
                // Stream chunks
                for (chunk in chunked) {
                    streamed += chunk
                    _chatMessages.value = _chatMessages.value.dropLast(1) + ChatMessage("AI", streamed)
                    delay(30)
                }
                // Persist final AI message
                persistChatMessage(ChatMessage("AI", streamed))
                viewModelScope.launch { telemetry?.logChatMessage(false) }
            }
        }
    }

    private fun persistChatMessage(msg: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            try { chatDao?.insertMessage(msg.toEntity()) } catch (e: Exception) { Log.e("MarketplaceViewModel", "Persist chat failed: ${e.message}") }
        }
    }

    fun retryLastMessage() {
        val last = _lastUserQuery.value
        if (last != null) {
            // Remove last AI message if it was error/empty, then resend
            val msgs = _chatMessages.value.toMutableList()
            if (msgs.isNotEmpty() && msgs.last().sender == "AI") {
                msgs.removeAt(msgs.lastIndex)
                _chatMessages.value = msgs
            }
            sendChatMessage(last)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao?.clearAll()
            withContext(Dispatchers.Main) {
                _chatMessages.value = listOf(ChatMessage("AI", "مرحباً بك في متجر الواحة! كيف يمكنني مساعدتك اليوم؟"))
            }
        }
    }

    fun copyChatMessage(text: String): String = text

    // Phase 3.2: RAG offline now uses retrieved items, not just keyword
    private fun generateRagOfflineResponse(query: String, ragItems: List<MarketplaceItem>): String {
        if (ragItems.isNotEmpty()) {
            val top = ragItems.first()
            val others = ragItems.drop(1).joinToString(" و ") { "“${it.titleAr}”" }
            val extra = if (others.isNotEmpty()) " وكذلك $others" else ""
            return when (top.type) {
                "APP" -> "أنصحك بـ “${top.titleAr}” — ${top.descriptionAr.take(120)} (التقييم ${top.rating} ⭐، ${top.sizeMb}MB) $extra. اضغط \"فتح\" بعد التحميل لتجربته."
                "GAME" -> "لعبة “${top.titleAr}” مثالية لك: ${top.descriptionAr.take(120)} $extra. التقييم ${top.rating} ⭐."
                "BOOK" -> "كتاب “${top.titleAr}” لـ ${top.developerOrCreatorAr}: ${top.descriptionAr.take(150)} $extra. متاح للقراءة مع حفظ الصفحة."
                "MOVIE" -> "فيلم “${top.titleAr}”: ${top.descriptionAr.take(130)} $extra. بث HD متوفر."
                "MUSIC" -> "معزوفة “${top.titleAr}” — ${top.descriptionAr.take(120)} $extra. شغّلها في مشغل الواحة."
                else -> "رشحت لك “${top.titleAr}”: ${top.descriptionAr.take(120)} $extra."
            }
        }
        // Fallback to old keyword logic if no RAG hit
        return generateOfflineSmartResponse(query)
    }

    private fun generateOfflineSmartResponse(query: String): String {
        return when {
            query.contains("تطبيق", ignoreCase = true) || query.contains("تطبيقات", ignoreCase = true) -> "يسعدني ترشيح تطبيق 'زاد المسلم' للأذكار والتلاوات، أو تطبيق 'المصمم العربي المحترف' للكتابة والتشكيل على الصور بأفخم الخطوط العربية التراثية للبدء الفوري!"
            query.contains("لعبة", ignoreCase = true) || query.contains("ألعاب", ignoreCase = true) -> "بصفتي مساعدك، أنصحك بشدة بصب تركيزك على لعبة 'عاصفة الصحراء الاستراتيجية' لتبني درعك الحربي الفريد وتدافع عن حصونك، أو 'الكلمات المتقاطعة التراثية' لتدوير وتمرين ذكائك المعرفي."
            query.contains("كتاب", ignoreCase = true) || query.contains("رواية", ignoreCase = true) || query.contains("كتب", ignoreCase = true) -> "سجلنا العظيم يحتوي على 'مقدمة ابن خلدون' الفيلسوف الفريد في تأسيس علم الاجتماع البشري، ورواية 'حي بن يقظان' الرمزية للأندلسي ابن طفيل. متوفرة بالكامل للقراءة التفاعلية مع حفظ آخر صفحة!"
            query.contains("فيلم", ignoreCase = true) || query.contains("أفلام", ignoreCase = true) -> "يسرني أن أعرض لك الفيلم الوثائقي 'تاريخ الأندلس: الفردوس المفقود' لتفاصيل عريقة عن الصروح الأيبيرية وحضاراتنا الساطعة، أو 'رحلة في أعماق الصحراء' لمشاهد طبيعية خلابة."
            query.contains("موسيقى", ignoreCase = true) || query.contains("أغنية", ignoreCase = true) || query.contains("صوت", ignoreCase = true) -> "أقترح عليك تشغيل معزوفة 'أنغام الشرق والأندلس' على آلة العود والناي للراحة، أو 'شدو العصافير في واحات المياه' لأصوات طبيعية باهرة الجودة تساعدك على التركيز والرخاء."
            else -> "أهلاً بك يا صديقي! أسئلتك تسعدني جداً. الواحة متكاملة ومصممة بأجهزة وبرمجيات متميزة للغاية. تصفح التبويبات (تطبيقات، ألعاب، كتب، أفلام، موسيقى) للمزيد من الروائع!"
        }
    }
}
