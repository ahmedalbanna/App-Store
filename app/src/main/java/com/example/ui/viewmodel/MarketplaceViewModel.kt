package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.AppDatabase
import com.example.data.database.MarketplaceItem
import com.example.data.database.MarketplaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MarketplaceRepository(database.marketplaceDao(), application)

    // Current screen orientation / Tab choice
    private val _currentTab = MutableStateFlow("APPS") // "APPS", "GAMES", "BOOKS", "MOVIES", "MUSIC", "ASSISTANT", "LIBRARY"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Query for generic app search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Chosen item details card (for overlay sheets)
    private val _selectedItem = MutableStateFlow<MarketplaceItem?>(null)
    val selectedItem: StateFlow<MarketplaceItem?> = _selectedItem.asStateFlow()

    // 1. All Items
    val allItems: StateFlow<List<MarketplaceItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate the database if it is empty
        viewModelScope.launch {
            repository.checkAndPrepulateDatabase()
        }
    }

    // Dynamic Filter lists
    val appsList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.type == "APP" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gamesList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.type == "GAME" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val booksList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.type == "BOOK" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moviesList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.type == "MOVIE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val musicList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.type == "MUSIC" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.isBookmarked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedList: StateFlow<List<MarketplaceItem>> = allItems.map { list ->
        list.filter { it.isDownloaded }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Flow for filtering results with Search Box
    val filteredItems: StateFlow<List<MarketplaceItem>> = combine(allItems, searchQuery, currentTab) { list, query, tab ->
        var res = list
        if (tab != "LIBRARY") {
            res = res.filter { it.type == tab }
        } else {
            // For Library view, show either booked or downloaded
            res = res.filter { it.isBookmarked || it.isDownloaded || it.isInstalled }
        }
        if (query.isNotEmpty()) {
            res = res.filter {
                it.titleAr.contains(query, ignoreCase = true) ||
                it.titleEn.contains(query, ignoreCase = true) ||
                it.descriptionAr.contains(query, ignoreCase = true)
            }
        }
        res
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chatbot States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("AI", "مرحباً بك في متجر الواحة! أنا مساعدك الذكي لتصفح التطبيقات وقراءة واقتراح محتوى تفريحي رائع. كيف يمكنني مساعدتك اليوم؟")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Current Media Player values
    val currentPlayingMusic = MutableStateFlow<MarketplaceItem?>(null)
    val isPlayingMusic = MutableStateFlow(false)
    val musicPlaybackProgress = MutableStateFlow(0.35f) // simulated volume / progress

    // Active Book progress states
    val activeReadingBook = MutableStateFlow<MarketplaceItem?>(null)
    val bookPageCount = 312 // simulated page counts

    // Cinema screen states
    val activeWatchingMovie = MutableStateFlow<MarketplaceItem?>(null)
    val isPlayingMovie = MutableStateFlow(false)
    val moviePlaybackProgress = MutableStateFlow(0.12f)

    fun changeTab(newTab: String) {
        _currentTab.value = newTab
        _searchQuery.value = "" // clear search on shift
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectItem(item: MarketplaceItem?) {
        _selectedItem.value = item
    }

    // Trigger action based on item type and state
    fun handleMainActionClick(item: MarketplaceItem) {
        when (item.downloadState) {
            "NONE", "UPDATE_AVAILABLE" -> {
                Log.d("MarketplaceViewModel", "Registering download triggers for: ${item.id}")
                repository.triggerDownloadProgress(item.id, viewModelScope)
            }
            "DOWNLOADED" -> {
                // If App or Game, prompt Installer simulator
                viewModelScope.launch {
                    repository.installPackage(item.id)
                }
            }
            "INSTALLED" -> {
                // Open content!
                openContent(item)
            }
            "DOWNLOADING" -> {
                // pause or reset
                viewModelScope.launch {
                    repository.resetDownloadState(item.id)
                }
            }
            else -> {
                // Other states
            }
        }
    }

    fun openContent(item: MarketplaceItem) {
        when (item.type) {
            "BOOK" -> {
                activeReadingBook.value = item
                _currentTab.value = "READ_BOOK_VIEW"
            }
            "MOVIE" -> {
                activeWatchingMovie.value = item
                isPlayingMovie.value = true
                _currentTab.value = "WATCH_MOVIE_VIEW"
            }
            "MUSIC" -> {
                currentPlayingMusic.value = item
                isPlayingMusic.value = true
                _currentTab.value = "PLAY_MUSIC_VIEW"
            }
            else -> {
                // Apps / Games: Open simulated launch dialog or message Toast!
            }
        }
    }

    fun toggleBookmark(item: MarketplaceItem) {
        viewModelScope.launch {
            repository.toggleBookmark(item.id)
            // Synchronize overlay sheet if shown
            val currentSelected = _selectedItem.value
            if (currentSelected != null && currentSelected.id == item.id) {
                _selectedItem.value = currentSelected.copy(isBookmarked = !currentSelected.isBookmarked)
            }
        }
    }

    fun saveBookProgress(itemId: String, page: Int) {
        viewModelScope.launch {
            repository.saveBookProgress(itemId, page)
            val active = activeReadingBook.value
            if (active != null && active.id == itemId) {
                activeReadingBook.value = active.copy(lastReadPage = page)
            }
        }
    }

    fun uninstallApp(item: MarketplaceItem) {
        viewModelScope.launch {
            repository.resetDownloadState(item.id)
            if (_selectedItem.value?.id == item.id) {
                _selectedItem.value = null
            }
        }
    }

    // Call Gemini API Option B (Direct REST with graceful key failure protection)
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return

        // Append text message to the thread
        val updatedList = _chatMessages.value.toMutableList().apply {
            add(ChatMessage("USER", text))
        }
        _chatMessages.value = updatedList
        _isChatLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // Read from env / secret Gradle plugin
            val key = BuildConfig.GEMINI_API_KEY
            val output = if (key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY") {
                Log.e("MarketplaceViewModel", "Gemini API key is not set or empty. Providing offline simulator intelligence.")
                delay(1000)
                generateOfflineSmartResponse(text)
            } else {
                try {
                    val systemInstruction = "أنت مساعد ذكي ولطيف لمتجر تطبيقات ووسائط ترفيهية يسمى 'متجر الواحة'. قم بالرد باللغة العربية بأسلوب راقٍ لمساعدة المستخدم في العثور على أفضل التطبيقات الإسلامية والألعاب والمحتوى الأدبي والسينمائي المتوفر بالمتجر. اجعل إجاباتك مختصرة ومحفزة وممتعة."
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = text)))),
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                    )
                    val response = RetrofitClient.service.generateContent(key, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "عذراً يا صديقي، لم أستطع استخلاص جواب مناسب من الخادم حالياً. هل تفضل فحص مجموعة أخرى؟"
                } catch (e: Exception) {
                    Log.e("MarketplaceViewModel", "Error fetching Gemini: ${e.message}")
                    delay(1200)
                    generateOfflineSmartResponse(text)
                }
            }

            withContext(Dispatchers.Main) {
                val finalMessages = _chatMessages.value.toMutableList().apply {
                    add(ChatMessage("AI", output))
                }
                _chatMessages.value = finalMessages
                _isChatLoading.value = false
            }
        }
    }

    private fun generateOfflineSmartResponse(query: String): String {
        return when {
            query.contains("تطبيق", ignoreCase = true) || query.contains("تطبيقات", ignoreCase = true) -> {
                "يسعدني ترشيح تطبيق 'زاد المسلم' للأذكار والتلاوات، أو تطبيق 'المصمم العربي المحترف' للكتابة والتشكيل على الصور بأفخم الخطوط العربية التراثية للبدء الفوري!"
            }
            query.contains("لعبة", ignoreCase = true) || query.contains("ألعاب", ignoreCase = true) -> {
                "بصفتي مساعدك، أنصحك بشدة بصب تركيزك على لعبة 'عاصفة الصحراء الاستراتيجية' لتبني درعك الحربي الفريد وتدافع عن حصونك، أو 'الكلمات المتقاطعة التراثية' لتدوير وتمرين ذكائك المعرفي."
            }
            query.contains("كتاب", ignoreCase = true) || query.contains("رواية", ignoreCase = true) || query.contains("كتب", ignoreCase = true) -> {
                "سجلنا العظيم يحتوي على 'مقدمة ابن خلدون' الفيلسوف الفريد في تأسيس علم الاجتماع البشري، ورواية 'حي بن يقظان' الرمزية للأندلسي ابن طفيل. متوفرة بالكامل للقراءة التفاعلية مع حفظ آخر صفحة!"
            }
            query.contains("فيلم", ignoreCase = true) || query.contains("أفلام", ignoreCase = true) -> {
                "يسرني أن أعرض لك الفيلم الوثائقي 'تاريخ الأندلس: الفردوس المفقود' لتفاصيل عريقة عن الصروح الأيبيرية وحضاراتنا الساطعة، أو 'رحلة في أعماق الصحراء' لمشاهد طبيعية خلابة."
            }
            query.contains("موسيقى", ignoreCase = true) || query.contains("أغنية", ignoreCase = true) || query.contains("صوت", ignoreCase = true) -> {
                "أقترح عليك تشغيل معزوفة 'أنغام الشرق والأندلس' على آلة العود والناي للراحة، أو 'شدو العصافير في واحات المياه' لأصوات طبيعية باهرة الجودة تساعدك على التركيز والرخاء."
            }
            else -> {
                "أهلاً بك يا صديقي! أسئلتك تسعدني جداً. الواحة متكاملة ومصممة بأجهزة وبرمجيات متميزة للغاية. تصفح التبويبات (تطبيقات، ألعاب، كتب، أفلام، موسيقى) للمزيد من الروائع!"
            }
        }
    }
}
