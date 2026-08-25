package com.example.data.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MarketplaceRepositoryImpl(
    private val marketplaceDao: MarketplaceDao,
    private val context: Context
) : MarketplaceRepository {
    override val allItems: Flow<List<MarketplaceItem>> = marketplaceDao.getAllItems()

    // Job-tracked downloads (Phase 1.3) — cancellable and per-item
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    override fun getItemsByType(type: String): Flow<List<MarketplaceItem>> =
        marketplaceDao.getItemsByType(type)

    override fun getItemById(id: String): Flow<MarketplaceItem?> =
        marketplaceDao.getItemByIdFlow(id)

    // Initializer to pre-populate database with elegant Arabic content on startup
    override suspend fun checkAndPrepulateDatabase() {
        withContext(Dispatchers.IO) {
            val count = marketplaceDao.getItemCount()
            if (count == 0) {
                Log.d("MarketplaceRepository", "Pre-populating database with rich Al-Waha Arabic catalog...")
                val sampleItems = listOf(
                    // 1. APPS
                    MarketplaceItem(
                        id = "app_zad_muslim",
                        titleAr = "زاد المسلم",
                        titleEn = "Zad Muslim",
                        type = "APP",
                        category = "إسلامي / أدوات",
                        sizeMb = 24.5,
                        rating = 4.9,
                        developerOrCreatorAr = "مطور البرمجيات الإسلامية",
                        descriptionAr = "حقيبة المسلم الشاملة: أذكار الصباح والمساء، سبحة إلكترونية، مواقيت الصلاة الدقيقة، واتجاه القبلة مع دعم كامل للتشغيل بدون إنترنت.",
                        simulatedVersionCode = 2,
                        installedVersionCode = 1, // simulates update available
                        downloadState = "UPDATE_AVAILABLE"
                    ),
                    MarketplaceItem(
                        id = "app_mushaf_madina",
                        titleAr = "مصحف المدينة المنورة",
                        titleEn = "Mushaf Al-Madinah",
                        type = "APP",
                        category = "إسلامي / كتب",
                        sizeMb = 85.0,
                        rating = 5.0,
                        developerOrCreatorAr = "مجمع الملك فهد لطباعة المصحف",
                        descriptionAr = "المصحف الشريف كاملاً بالرسم العثماني المطابق لمصحف المدينة النبوية، مع التفسير الميسر، أسباب النزول، وتلاوات صوتية لأشهر قراء العالم الإسلامي المتواترين.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "app_arabic_designer",
                        titleAr = "المصمم العربي المحترف",
                        titleEn = "Professional Arabic Designer",
                        type = "APP",
                        category = "أدوات / تصميم",
                        sizeMb = 45.2,
                        rating = 4.7,
                        developerOrCreatorAr = "تقنيات الإبداع العربي",
                        descriptionAr = "تطبيق رائد للكتابة والتعديل على الصور بتشكيلة ضخمة من الخطوط العربية الفاخرة (الرقعة، النسخ، الديواني، الكوفي، الثلث) ومجموعة ملصقات وإطارات مميزة.",
                        simulatedVersionCode = 3,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "app_translator",
                        titleAr = "المترجم الفوري الذكي",
                        titleEn = "Smart Instant Translator",
                        type = "APP",
                        category = "أدوات / إنتاجية",
                        sizeMb = 18.0,
                        rating = 4.6,
                        developerOrCreatorAr = "حلول اللغات الذكية",
                        descriptionAr = "مترجم صوتي ونصي متطور وفوري يدعم أكثر من 100 لغة، مصمم بمحركات ترجمة سريعة تعمل في وضع عدم الاتصال لتفادي استهلاك باقات البيانات.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 1, // simulates already installed
                        isInstalled = true,
                        downloadState = "INSTALLED"
                    ),

                    // 2. GAMES
                    MarketplaceItem(
                        id = "game_knight_hero",
                        titleAr = "فارس الصحراء والأساطير",
                        titleEn = "Desert Knight Hero",
                        type = "GAME",
                        category = "ألعاب / مغامرات",
                        sizeMb = 120.0,
                        rating = 4.8,
                        developerOrCreatorAr = "ستوديو ألعاب الرافدين",
                        descriptionAr = "انطلق في مغامرة شيقة في مرابع الجزيرة العربية القديمة، شق طريقك بين العواصف، ودافع عن حصونك، وحارب المخاطر لإنقاذ القافلة التائهة واستعادة الذهب التراثي.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "game_crosswords",
                        titleAr = "الكلمات المتقاطعة العربية",
                        titleEn = "Arabic Crosswords Challenge",
                        type = "GAME",
                        category = "ألعاب / ذكاء وتحدي",
                        sizeMb = 28.0,
                        rating = 4.5,
                        developerOrCreatorAr = "بيت الألعاب العربية",
                        descriptionAr = "أفضل لعبة لتمرين العقل واكتساب معارف لغوية وتاريخية وجغرافية بمستويات متدرجة الصعوبة وتصميم هندسي مريح ومحفز للذاكرة.",
                        simulatedVersionCode = 2,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "game_desert_storm",
                        titleAr = "عاصفة الصحراء: درع العرب",
                        titleEn = "Desert Storm: Shield of Arabs",
                        type = "GAME",
                        category = "ألعاب / استراتيجية",
                        sizeMb = 155.0,
                        rating = 4.9,
                        developerOrCreatorAr = "رادار هاب للبرمجيات",
                        descriptionAr = "لعبة تخطيط استراتيجي حربي، ابنِ قاعدتك وصنّع دباباتك وقد فرقتك العسكرية للدفاع عن الحدود وتأمين حقول الطاقة في بيئة صحراوية بالغة الدقة والتحديات.",
                        simulatedVersionCode = 4,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "game_camel_race",
                        titleAr = "سفينة الصحراء: سباق الهجن",
                        titleEn = "Camel Racing Live",
                        type = "GAME",
                        category = "ألعاب / رياضة وتراث",
                        sizeMb = 65.5,
                        rating = 4.7,
                        developerOrCreatorAr = "ألعاب البادية التراثية",
                        descriptionAr = "محاكي واقعي لسباقات الهجن الأصيلة بمضامير عربية شهيرة، طوّر قدرات جملك وتحكم بسرعة ركض السرعة للفوز بالجوائز الذهبية واعتلاء منصات الصدارة.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),

                    // 3. BOOKS
                    MarketplaceItem(
                        id = "book_ibn_khaldun",
                        titleAr = "مقدمة ابن خلدون",
                        titleEn = "Prolegomena of Ibn Khaldun",
                        type = "BOOK",
                        category = "كتب / تاريخ واجتماع",
                        sizeMb = 5.2,
                        rating = 4.9,
                        developerOrCreatorAr = "عبد الرحمن بن خلدون",
                        descriptionAr = "أعظم ما خط قلم في قيام الدول وعلم العمران البشري والاجتماع الإنساني، يقدم تحليلات رائدة للاقتصاد، السياسة، الحرف والصنائع والتفسير الحضاري للتاريخ.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "book_hayy_ibn_yaqdhan",
                        titleAr = "حي بن يقظان",
                        titleEn = "Hayy ibn Yaqdhan",
                        type = "BOOK",
                        category = "كتب / روايات وفلسفة",
                        sizeMb = 3.1,
                        rating = 4.6,
                        developerOrCreatorAr = "ابن طفيل الأندلسي",
                        descriptionAr = "رواية رمزية فلسفية فريدة، تروي حكاية طفل يترعرع في جزيرة نائية منعزلة ليدرك بالفطرة والعقل المجرد أسرار الوجود والخالق وحركة النجوم وعناصر الطبيعة.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "book_mutanabbi_poetry",
                        titleAr = "ديوان المتنبي كاملاً ومعانيه",
                        titleEn = "Poetry Collection of Al-Mutanabbi",
                        type = "BOOK",
                        category = "كتب / دواوين شعر",
                        sizeMb = 4.8,
                        rating = 4.8,
                        developerOrCreatorAr = "أبو الطيب المتنبي",
                        descriptionAr = "تحفة شعرية تضم قصائد حكيم العرب وشاعر الكبرياء العربي، مرتبة ومفسرة الأبيات والغريب من المفردات، تعكس روح الماجد العربي وعزة النفس والفخر اللغوي البليغ.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),

                    // 4. MOVIES
                    MarketplaceItem(
                        id = "movie_andalus",
                        titleAr = "تاريخ الأندلس: الفردوس المفقود",
                        titleEn = "Glory of Al-Andalus",
                        type = "MOVIE",
                        category = "أفلام / وثائقي تاريخي",
                        sizeMb = 350.0,
                        rating = 4.9,
                        developerOrCreatorAr = "إنتاج الجزيرة التوثيقي",
                        descriptionAr = "عمل وثائقي تفصيلي يستعرض مسيرة الحضارة الإسلامية في شبه الجزيرة الأيبيرية طوال 800 عام، مستعرضاً فنون العمارة، الترجمة، والفكر الفلسفي بالأندلس الشامخة.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "movie_desert_secrets",
                        titleAr = "رحلة دافئة في أعماق الصحراء الكبرى",
                        titleEn = "Secrets of Sahara expedition",
                        type = "MOVIE",
                        category = "أفلام / طبيعة ومغامرة",
                        sizeMb = 420.0,
                        rating = 4.7,
                        developerOrCreatorAr = "قناة الطبيعة العربية",
                        descriptionAr = "استكشف التنوع البيئي المذهل، الواحات المخفية، وأسرار طرائق البدو الرحل في الترحال في ظروف بالغة القسوة داخل الكثبان الفسيحة.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "movie_oasis_wonders",
                        titleAr = "روائع الطبيعة والوديان بالشرق للأوسط",
                        titleEn = "Nature Wonders of Middle East",
                        type = "MOVIE",
                        category = "أفلام / ترحال وسياحة",
                        sizeMb = 290.0,
                        rating = 4.8,
                        developerOrCreatorAr = "استوديوهات الواحة المرئية",
                        descriptionAr = "فيلم سينمائي ناطق بالجمال الطبيعي للوديان ومصبات السيول الفريدة كوديان سلطنة عمان ومدائن صالح الخضراء الساحرة.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),

                    // 5. MUSIC
                    MarketplaceItem(
                        id = "music_oriental_tones",
                        titleAr = "قصائد أنغام الشرق والأندلس",
                        titleEn = "Spiritual Oriental Tones",
                        type = "MUSIC",
                        category = "موسيقى / معزوفات تراثية",
                        sizeMb = 8.5,
                        rating = 4.8,
                        developerOrCreatorAr = "فرقة عود الأندلس المعاصرة",
                        descriptionAr = "باقة من أروع المعزوفات التراثية على آلات العود القديم، القانون، والناي في جلسات شرقية تجريدية تبعث على استرجاع عبق التاريخ المعاش والمجالس الشاعرية.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "music_birds_valley",
                        titleAr = "شدو وتغريد طيور الوادي",
                        titleEn = "valley Birds Peaceful Chants",
                        type = "MUSIC",
                        category = "موسيقى / أصوات طبيعة هادئة",
                        sizeMb = 6.2,
                        rating = 4.9,
                        developerOrCreatorAr = "أصوات رنين الطبيعة العربية",
                        descriptionAr = "تسجيل فائق النقاء لأصوات تغريد العصافير، جريان مياه الينابيع في الواحات العميقة لتهدئة الأعصاب وتحفيز النوم العميق وتخليص الجسد من توترات اليوم الصاخبة.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    ),
                    MarketplaceItem(
                        id = "music_calm_oasis",
                        titleAr = "استرخاء تجليات الواحة والنجوم",
                        titleEn = "Oasis Healing Atmosphere & Stars",
                        type = "MUSIC",
                        category = "موسيقى / تأمل واسترخاء",
                        sizeMb = 12.0,
                        rating = 4.7,
                        developerOrCreatorAr = "الملحن المبدع ياسر العربي",
                        descriptionAr = "جلسة موسيقى المحيط الهادلة مصممة بعناية مع ومضات تأملية لطيفة تساعد الدماغ البشري على رفع مستويات الرخاء والتركيز الحاد أثناء جلسات المذاكرة والعمل المعرفي الشاق.",
                        simulatedVersionCode = 1,
                        installedVersionCode = 0,
                        downloadState = "NONE"
                    )
                )
                marketplaceDao.insertAll(sampleItems)
            }
        }
    }

    // Function to simulate background downloading with progressive values — Job-tracked (1.3)
    override fun triggerDownloadProgress(itemId: String, scope: CoroutineScope) {
        // Cancel any prior job for this item (idempotent retry)
        downloadJobs[itemId]?.cancel()
        val job = scope.launch(Dispatchers.IO) {
            try {
                // Initialize state
                marketplaceDao.updateDownloadState(itemId, "DOWNLOADING", 0)

                // Simulate progressive download chunks (0% -> 100%)
                for (progress in 5..100 step 15) {
                    delay(300)
                    marketplaceDao.updateDownloadState(itemId, "DOWNLOADING", progress)
                }

                // Set download 100% complete
                marketplaceDao.updateDownloadState(itemId, "DOWNLOADING", 100)
                delay(400)

                // Start security scanning immediately to follow "الأمان والفحص (Security Scan)" requirement
                marketplaceDao.updateDownloadState(itemId, "SCANNING", 100)
                delay(1200) // simulating severe safety file signature hashing & scanning

                // Complete scanning successfully -> state DOWNLOADED
                val currentItem = marketplaceDao.getItemById(itemId)
                if (currentItem != null) {
                    val targetState = if (currentItem.type == "APP" || currentItem.type == "GAME") {
                        "DOWNLOADED" // ready to click INSTALL
                    } else {
                        "INSTALLED" // Media/Books are immediately added to local library
                    }
                    marketplaceDao.updateItem(
                        currentItem.copy(
                            isDownloaded = true,
                            downloadState = targetState,
                            downloadProgress = 100,
                            // If book/media, it is instantly ready
                            isInstalled = currentItem.type != "APP" && currentItem.type != "GAME"
                        )
                    )
                }
            } catch (e: Exception) {
                // Cancellation is expected when user cancels; don't reset to NONE if cancelled explicitly via resetDownloadState
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("MarketplaceRepository", "Download cancelled for $itemId")
                    throw e
                }
                Log.e("MarketplaceRepository", "Download simulation error for $itemId: ${e.message}")
                marketplaceDao.updateDownloadState(itemId, "NONE", 0)
            } finally {
                downloadJobs.remove(itemId)
            }
        }
        downloadJobs[itemId] = job
        // Clean up on completion
        job.invokeOnCompletion { downloadJobs.remove(itemId) }
    }

    override fun cancelDownload(itemId: String) {
        downloadJobs[itemId]?.cancel()
        downloadJobs.remove(itemId)
    }

    override fun cancelAllDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }

    // Function to simulate a trigger installation (PackageInstaller prompt simulator)
    override suspend fun installPackage(itemId: String) {
        withContext(Dispatchers.IO) {
            val item = marketplaceDao.getItemById(itemId)
            if (item != null) {
                marketplaceDao.updateDownloadState(itemId, "INSTALLING", 100)
                delay(1500) // installing APK files dynamically
                marketplaceDao.updateItem(
                    item.copy(
                        isInstalled = true,
                        downloadState = "INSTALLED",
                        installedVersionCode = item.simulatedVersionCode,
                        downloadProgress = 100
                    )
                )
            }
        }
    }

    // Cancel dynamic download / Reset download state
    override suspend fun resetDownloadState(itemId: String) {
        // Cancel ongoing job first (1.3)
        cancelDownload(itemId)
        withContext(Dispatchers.IO) {
            val item = marketplaceDao.getItemById(itemId)
            if (item != null) {
                marketplaceDao.updateItem(
                    item.copy(
                        isDownloaded = false,
                        isInstalled = false,
                        downloadState = "NONE",
                        downloadProgress = 0,
                        installedVersionCode = 0
                    )
                )
            }
        }
    }

    // Update bookmark status
    override suspend fun toggleBookmark(itemId: String) {
        withContext(Dispatchers.IO) {
            val item = marketplaceDao.getItemById(itemId)
            if (item != null) {
                marketplaceDao.updateIsBookmarked(itemId, !item.isBookmarked)
            }
        }
    }

    // Save reading progress of books
    override suspend fun saveBookProgress(itemId: String, lastPage: Int) {
        withContext(Dispatchers.IO) {
            marketplaceDao.updateBookProgress(itemId, lastPage)
        }
    }
}
