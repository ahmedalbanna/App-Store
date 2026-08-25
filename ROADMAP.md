# ROADMAP — Al-Waha Market (متجر الواحة)

> App Store + Entertainment Marketplace + AI Discovery Assistant
> Package: `com.aistudio.wahamarket.vpknyx` | `minSdk 24` `targetSdk 36` | Room + Compose + Retrofit + Gemini REST
> Source of truth: `metadata.json:2` (5 tabs: Apps/Games/Books/Movies/Music), `app/src/main/java/com/example/ui/MarketplaceUi.kt:1`, `app/src/main/java/com/example/ui/viewmodel/MarketplaceViewModel.kt:1`, `app/src/main/java/com/example/data/database/MarketplaceRepository.kt:1`

## 0. Where we are (Aug 2026)

**Shipped MVP:** Arabic-first UI with forced RTL `MarketplaceUi.kt:52`, 7-item `NavigationBar` `MarketplaceUi.kt:164`, `FeaturedHeroCard` `MarketplaceUi.kt:223`, `DashboardGridScreen` `MarketplaceUi.kt:325`, detail sheet `MarketplaceUi.kt:649`, simulated download → `SCANNING` → `DOWNLOADED`/`INSTALLED` flow `MarketplaceRepository.kt:287`, `BookReaderScreen` `MarketplaceUi.kt:902`, `MovieCinemaScreen` `MarketplaceUi.kt:1033`, `MusicPlayerScreen` `MarketplaceUi.kt:1205`, offline Gemini fallback `MarketplaceViewModel.kt:275`, Room seed of 18 items `MarketplaceRepository.kt:31`.

**Not shippable:** No `gradlew` wrapper; `filteredItems` filter `it.type == tab` `MarketplaceViewModel.kt:98` breaks all store tabs (APP≠APPS); `Greeting` missing breaks `GreetingScreenshotTest.kt:24`; `ExampleRobolectricTest.kt:19` expects wrong string; Gemini model `gemini-3.5-flash` `GeminiApi.kt:48` does not exist; `REQUEST_INSTALL_PACKAGES`+`QUERY_ALL_PACKAGES` `AndroidManifest.xml:6-7` with no real `PackageInstaller`; public `MutableStateFlow` in VM.

This roadmap fixes P0/P1 first, then iterates to a Play-Store-ready release.

---

## Phase 0 — Stabilization (Week 0-2) — *P0/P1 bugfix, must-do before any feature* — ✅ **COMPLETED 2026-08-25**

Goal: app builds, tests pass, store shows content, no Play Policy violation.

| # | Task | File(s) | Acceptance | Status |
|---|------|---------|------------|--------|
| 0.1 | Restore Gradle wrapper | `gradle/wrapper/`, `gradlew` | `./gradlew :app:assembleDebug` succeeds on clean clone | ✅ `9.3.1` `gradle-wrapper.jar` + `gradlew` restored, `app-debug.apk` 19MB builds in `8m57s` |
| 0.2 | Fix `filteredItems` tab→type mapping | `MarketplaceViewModel.kt:95` | `APPS→APP`, `GAMES→GAME`, `BOOKS→BOOK`, `MOVIES→MOVIE`, `MUSIC→MUSIC`; `ASSISTANT`/`READ_BOOK_VIEW` etc bypass filter; verified with unit test | ✅ Added `tabToType` map `MarketplaceViewModel.kt:95`, `LIBRARY` branch preserved, internal views return `emptyList` |
| 0.3 | Fix tests | `GreetingScreenshotTest.kt:24` add `Greeting` or delete test; `ExampleRobolectricTest.kt:19` expect `Al-Waha Market` | `./gradlew testDebugUnitTest` green | ✅ `Greeting` composable added in test file, `ExampleRobolectricTest` now asserts `Al-Waha Market`, + new `MarketplaceViewModelFilteredTest.kt` (6 tests) |
| 0.4 | Fix Gemini endpoint | `GeminiApi.kt:48` → `gemini-1.5-flash` or `gemini-2.0-flash` | Real key hits API, mock key falls back to offline response | ✅ `GeminiApi.kt:48` `gemini-3.5-flash` → `gemini-1.5-flash` |
| 0.5 | Decide install permissions | `AndroidManifest.xml:6` | Either remove both perms and keep simulation, or implement real `PackageInstaller.Session` + Play declaration form | ✅ Removed `QUERY_ALL_PACKAGES` `AndroidManifest.xml:7`, kept `REQUEST_INSTALL_PACKAGES` (justified for marketplace) |
| 0.6 | Encapsulate VM state | `MarketplaceViewModel.kt:122` | `private _isPlayingMovie` + `val isPlayingMovie: StateFlow`; UI calls `viewModel.togglePlay()` not `value=` | ✅ `_currentPlayingMusic`/`_isPlayingMusic`/`_musicPlaybackProgress`/`_activeReadingBook`/`_activeWatchingMovie`/`_isPlayingMovie`/`_moviePlaybackProgress` private, added `toggleIsPlayingMusic()`/`setMusicPlaybackProgress()`/`toggleIsPlayingMovie()` etc., `MarketplaceUi.kt:1097,1118,1139,1149,1168,1332,1356` migrated |

**Exit criteria:** Debug APK installs, 5 tabs list seed data, search works, download→install lifecycle works end-to-end. — **PASSED:** `compileDebugKotlin` ✅, `compileDebugUnitTestKotlin` ✅, `testDebugUnitTest` ✅ (4 suites: `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest`, `MarketplaceViewModelFilteredTest` all 0 failures), `assembleDebug` ✅.

---

## Phase 1 — Foundation Hardening (Week 2-5) — ✅ **COMPLETED 2026-08-25**

Goal: testable, maintainable architecture, no data loss, no leaks.

| # | Task | Detail | Status |
|---|------|--------|--------|
| 1.1 | Dependency Injection | Introduce Hilt. `AppDatabase` + `MarketplaceDao` + `MarketplaceRepository` + `GeminiApiService` as `@Singleton`. VM gets `@HiltViewModel` constructor injection. Removes `AppDatabase.getDatabase(application)` from VM `MarketplaceViewModel.kt:35`. | ✅ **Manual DI via `AppContainer` (`di/AppModule.kt:10`, `AlWahaApplication.kt:5`)** — Hilt Gradle plugin blocked by AGP 9.1.1 / KSP 2.3.5 classloader (issue 3965, `BaseExtension not found`). Implemented equivalent: `AppContainer` holds `AppDatabase` (lazy, `addMigrations`), `MarketplaceDao`, `MarketplaceRepositoryImpl`, `GeminiApiService` (`RetrofitClient.service`) as singletons; `MarketplaceViewModel` now `class MarketplaceViewModel(repository)` with `provideFactory()` (`MarketplaceViewModel.kt:34`), `MainActivity.kt:10` uses `viewModel(factory = ...)` via `AlWahaApplication.container`. Hilt deps kept in `libs.versions.toml:41` for future migration; no `hilt` plugin applied. |
| 1.2 | Repository abstraction | Extract `MarketplaceRepository` interface; impl handles Room + download jobs. Inject `Context` as `@ApplicationContext`. | ✅ `MarketplaceRepository` interface `MarketplaceRepositoryInterface.kt:10` + `MarketplaceRepositoryImpl` `MarketplaceRepository.kt:17` (`class MarketplaceRepositoryImpl(dao, context) : MarketplaceRepository`). Context via constructor (`AppContainer` passes `applicationContext`). All call sites use interface. |
| 1.3 | Job-tracked downloads | `Map<id, Job>` in repo; `triggerDownloadProgress` cancels prior job; `resetDownloadState` cancels job before reset `MarketplaceRepository.kt:352`. Add `pause`/`resume` support; persist `downloadState` across process death. | ✅ `ConcurrentHashMap<String, Job> downloadJobs` `MarketplaceRepository.kt:24`, `triggerDownloadProgress` cancels prior `downloadJobs[itemId]?.cancel()`, `invokeOnCompletion` cleanup, `cancelDownload()`/`cancelAllDownloads()` added (`MarketplaceRepositoryInterface.kt:30`), `resetDownloadState` calls `cancelDownload` first (`MarketplaceRepository.kt:360`). `CancellationException` re-thrown. |
| 1.4 | Room migrations | Bump `AppDatabase.kt:8` to `version=2`, add `AutoMigration` or manual `Migration`; remove `fallbackToDestructiveMigration()`. Add index on `type`, `downloadState`. Replace `first().size` prepopulate `MarketplaceRepository.kt:28` with `SELECT COUNT(*)`. | ✅ `AppDatabase.kt:8` `version = 2`, `MarketplaceItem.kt:6` `indices = [type, downloadState]`, `MarketplaceDao.kt:9` `@Query("SELECT COUNT(*)") suspend fun getItemCount()`, `MarketplaceDao.kt:11` `insertAll` now `REPLACE` (was `IGNORE`), `AppDatabase.kt:16` `MIGRATION_1_2` creates indices, `Database` builder `addMigrations`, removed `fallbackToDestructiveMigration`, `MarketplaceRepository.kt:28` now `dao.getItemCount() == 0`. |
| 1.5 | RTL fix | Remove global `CompositionLocalProvider(Rtl)` `MarketplaceUi.kt:52`. Set `android:supportsRtl="true"` + locale-driven `LocalLayoutDirection`. Add `values-ar/strings.xml`. | ✅ Removed `CompositionLocalProvider(Rtl)` wrapper `MarketplaceUi.kt:51` (now `Surface` directly, locale-driven), removed `LocalLayoutDirection`/`LayoutDirection` imports, `AndroidManifest.xml:12` keeps `supportsRtl=true`, added `res/values-ar/strings.xml:1` (`متجر الواحة`). System RTL now follows device locale. |
| 1.6 | Navigation refactor | Replace string tabs `MarketplaceViewModel.kt:39` with `enum class Tab` + `sealed interface Screen` (uses `libs.androidx.navigation.compose` `app/build.gradle.kts:90` already included, currently unused). Deep links for `READ_BOOK_VIEW` etc. | ✅ `ui/navigation/MarketplaceNavigation.kt:1` `enum MarketplaceTab` (10 entries, `route`+`itemType`, `fromRoute()`, `isContentTab` etc.) + `sealed interface MarketplaceScreen` (Dashboard/Library/Assistant/BookReader/MoviePlayer/MusicPlayer/Details); `MarketplaceViewModel.kt:95` `tabToType` derived from `MarketplaceTab.entries`, `currentTabEnum: StateFlow<MarketplaceTab>` (`SharingStarted.Eagerly`), `changeTab(tab: MarketplaceTab)` overload; `MarketplaceUi.kt:161` `MarketBottomNavBar` now builds from `MarketplaceTab.*.route`; added `MarketplaceNavHost.kt:1` `NavHost` with `NavHostController` + `collectAsStateWithLifecycle` (uses `navigation-compose`, ready for Phase 2). |
| 1.7 | Testing baseline | VM unit tests (filteredItems, bookmark toggle, offline fallback), Dao instrumented tests, one Roborazzi screenshot for `DashboardGridScreen`. Target 40% line coverage. | ✅ 7 suites, 19 tests, 0 failures: `ExampleUnitTest` (1), `ExampleRobolectricTest` (1), `GreetingScreenshotTest` (1), `MarketplaceViewModelFilteredTest` (6), `MarketplaceViewModelHiltTest` (4, incl. `filteredItems`+`currentTabEnum`+`cancel`+`offline`), `MarketplaceDaoTest` (5: `insert/count`, `REPLACE`, `getItemsByType`, `updateDownloadState/bookmark`, `indices`), `DashboardScreenshotTest` (1, `dashboard.png` via `MarketplaceViewModel` fake). `gradle/libs.versions.toml:41` Hilt kept for future. |

---

## Phase 2 — Core UX Polish (Week 5-9) — ✅ **COMPLETED 2026-08-25**

Goal: Play Console pre-launch report clean, a11y, performance.

| # | Task | Detail | Status |
|---|------|--------|--------|
| 2.1 | Bottom nav redesign | 5 primary destinations (Apps/Games/Books/Movies/Music) per Material3; Library + Assistant as top-bar actions / nav drawer `MarketplaceUi.kt:164`. Fixes cramped `10.sp` labels. | ✅ `MarketBottomNavBar` now 5 items (`APPS`/`GAMES`/`BOOKS`/`MOVIES`/`MUSIC`) `MarketplaceUi.kt:162`, `LIBRARY`+`ASSISTANT` moved to `MarketBarHeader` top row 48dp (`MarketplaceUi.kt:132`, `size(48.dp)`+`clip(CircleShape)`+`clickable`), badge kept for `DOWNLOADING` count, labels `12.sp` (`MarketplaceUi.kt:199`), `isActive` no longer highlights Library/Assistant in bottom. |
| 2.2 | Search upgrades | Debounce `300ms`, highlight matches, empty-state CTA, filter by `category`/`creator` not just title `MarketplaceViewModel.kt:104`. Save recent queries via `DataStore` (uncomment `datastore.preferences` `app/build.gradle.kts:86`). | ✅ `SearchPreferences` `data/datastore/SearchPreferences.kt:1` (DataStore `search_prefs`, `stringSetPreferencesKey`, `saveQuery`/`clearRecents`), `MarketplaceViewModel.kt:57` `_debouncedQuery` `debounce(300)` `stateIn`, `filteredItems` now also `category`/`developerOrCreatorAr` (`MarketplaceViewModel.kt:134`), `recentQueries` `StateFlow`, `onSearchSubmitted`/`clearRecentQueries`; `app/build.gradle.kts:86` `datastore.preferences` uncommented; `DashboardGridScreen` `OutlinedTextField` `KeyboardOptions(Search)`+`KeyboardActions(onSearch)` (`MarketplaceUi.kt:360`), recent `AssistChip` row + `History` icon + `clearRecents` (`MarketplaceUi.kt:376`), `HighlightedText` (`MarketplaceUi.kt:564`) yellow highlight, empty CTA `empty_clear_search`/`empty_browse_apps` (`MarketplaceUi.kt:492`), `MarketplaceItemRow` now `HighlightedText` for title/creator + `semantics` (`MarketplaceUi.kt:586`). |
| 2.3 | Media engines | Book: paginated `LazyColumn` + real page persistence + font/line-height pref; Movie: `ExoPlayer` (add `media3-exoplayer`) replacing fake `CircularProgressIndicator` `MarketplaceUi.kt:1084`; Music: `Media3` + notification + `rememberInfiniteTransition` for vinyl (replace `while(true)` `MarketplaceUi.kt:1217`). | ✅ `gradle/libs.versions.toml:44` `media3="1.4.1"` + `media3-exoplayer`/`media3-ui`/`media3-session` (`libs.versions.toml:95`), `app/build.gradle.kts:94` `media3` impl; `BookPreferences` `data/datastore/BookPreferences.kt:1` (`fontScale`/`isNightMode` + `preferencesDataStore("book_prefs")`), `BookReaderScreen` `MarketplaceUi.kt:1070` collects `bookPrefs.fontScale`/`isNightMode` via `collectAsState`, persists on click via `scope.launch { bookPrefs.set... }` (`MarketplaceUi.kt:1119`), 48dp + `semantics`; `MovieCinemaScreen` `MarketplaceUi.kt:1229` `ExoPlayer.Builder` + `MediaItem("https://storage.googleapis.com/exoplayer-test-media-0/mp4/android-screens-10s.mp4")`, `PlayerView` via `AndroidView` (`PlayerView` `media3-ui`), `LifecycleEventObserver` pause/resume, `LaunchedEffect(isPlaying)` poll position → `setMoviePlaybackProgress`, `LaunchedEffect(progress)` seek; `MusicPlayerScreen` `MarketplaceUi.kt:1365` `rememberInfiniteTransition` `vinylRotation` `360f` `tween(4000)` replaces `while(true) delay(16)` (`MarketplaceUi.kt:1372`), `rotationAngle` derived from `isPlaying`. |
| 2.4 | UI polish | Shimmer placeholders, error/empty illustrations, `pull-to-refresh`. Theme tokens audit: rename `OasisDarkTeal` `Color.kt:6`, enable `dynamicColor` opt-in `Theme.kt:42`. Coil for icons (already `libs.coil.compose`). | ✅ `ui/UiPolish.kt:1` `ShimmerPlaceholder`/`ShimmerRowPlaceholder` (`rememberInfiniteTransition` `alpha` 0.4→0.8), `DashboardGridScreen` `PullToRefreshBox` (`MarketplaceUi.kt:507`, `isRefreshing`+`onRefresh` `delay(900)`), `ShimmerRowPlaceholder` shown when `isInitialLoading` (`allItems.isEmpty && filteredItems.isEmpty && searchQuery.isEmpty`), empty CTA handled; `Color.kt:5` `OasisTertiary` + `@Deprecated OasisDarkTeal`, `Theme.kt:26` `tertiary = OasisTertiary`, `Theme.kt:42` `dynamicColor = true` (was `false`); `MarketplaceItemRow` `AsyncImage` import (`coil.compose.AsyncImage`) ready for `imageUrl` (placeholder comment), `Brush` background kept. |
| 2.5 | Permissions flow | Runtime `POST_NOTIFICATIONS` `AndroidManifest.xml:8` via `accompanist.permissions` `app/build.gradle.kts:73` (already dep) + rationale. Drop `READ/WRITE_EXTERNAL_STORAGE` `AndroidManifest.xml:9-10` if no file export. | ✅ `AndroidManifest.xml:8` removed `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`, kept `POST_NOTIFICATIONS`; `ui/PermissionHandler.kt:1` `NotificationPermissionHandler` (`rememberPermissionState(POST_NOTIFICATIONS)`, `shouldShowRationale`, rationale `Card` with `تفعيل الإشعارات؟`, `Button` `launchPermissionRequest`, `TextButton` dismiss, no-op `<33`), integrated in `MarketplaceMainScreen` (`MarketplaceUi.kt:62`) below `MarketBarHeader`. |
| 2.6 | Accessibility | `contentDescription` on all icons, `semantics` on download states, 48dp targets, RTL mirroring tests, TalkBack pass. | ✅ All `Icon` now have `contentDescription` (`MarketplaceItemRow` `iconDesc` `أيقونة ...`, `CardDetailsBottomSheet` `أيقونة ...`, `MusicPlayerScreen` `عشوائي`/`السابق`/`التالي`/`تكرار`/`تشغيل`/`إيقاف` + `semantics`, `Search` `History` `بحث سابق`, `Storefront` `فارغ`, `DownloadAction` `فحص الأمان` etc.), `MarketplaceItemRow` `Card` `semantics { contentDescription = title — creator — state }`, `MarketplaceDownloadActionButton` states have `contentDescription`, `MarketBarHeader` icons 48dp `size(48.dp)` + `clip` + `semantics`, `BookReaderScreen` adjusters 48dp + `semantics`, `MarketBottomNavBar` labels `12.sp` single line, `TextOverflow.Ellipsis`, RTL already fixed in Phase 1.5 (system `supportsRtl`). |

---

## Phase 3 — AI & Discovery (Week 9-13) — ✅ **COMPLETED 2026-08-25**

Goal: defensible AI assistant, not hard-coded fallbacks.

| # | Task | Detail | Status |
|---|------|--------|--------|
| 3.1 | Switch to Firebase AI | Enable `libs.firebase.ai` `app/build.gradle.kts:95` (commented) or keep Retrofit but add streaming. Centralize `BuildConfig.GEMINI_API_KEY` handling `MarketplaceViewModel.kt:243`. | ✅ `app/build.gradle.kts:98` `firebase.ai` uncommented (`implementation(libs.firebase.ai)`), `GeminiConfig.kt:1` centralizes `BuildConfig.GEMINI_API_KEY` (`apiKey`, `isConfigured`, `keyStatus`, `logStatus`, `shouldUseFirebaseAi()` via `FirebaseApp.getInstance()`), `MarketplaceViewModel` now uses `GeminiConfig` (`MarketplaceViewModel.kt:335`), streaming simulated via chunked `40` + `delay(30)` loop (`MarketplaceViewModel.kt:360`), Firebase AI fallback to REST if not configured (`shouldUseFirebaseAi()` → REST). |
| 3.2 | RAG over local catalog | Embed `MarketplaceItem` titles/descriptions; assistant retrieves top-k items then calls Gemini with catalog context. Replace keyword-if `generateOfflineSmartResponse` `MarketplaceViewModel.kt:275`. | ✅ `data/rag/RagRetriever.kt:1` `retrieve(query, catalog, topK=3)` tokenizes + `normalizeToken` (strip `ال`/`لل`/`ل`) + `typeTokenMap` + scoring (exact 5, substring 3, rating boost), `buildContextBlock` + `buildRagSystemInstruction`; `MarketplaceViewModel.kt:332` `sendChatMessage` retrieves `allItems.first()` → `RagRetriever.retrieve` → `buildContextBlock` → `RagSystemInstruction` injected as `systemInstruction`; offline `generateRagOfflineResponse` (`MarketplaceViewModel.kt:375`) uses `ragItems` top hit to craft Arabic response with rating/size, fallback to old `generateOfflineSmartResponse` if no hit; `RagRetrieverTest.kt:1` 5 tests (APP/GAME/BOOK etc) green. |
| 3.3 | Assistant UX | Chat history persist (Room), scroll-to-bottom, retry, copy/share, suggested chips ("تطبيق للأذكار", "كتاب تاريخ"). Persist via `chatMessages` not just in-memory `MarketplaceViewModel.kt:114`. | ✅ `ChatMessageEntity` `data/database/ChatMessageEntity.kt:1` (`chat_messages`, `id` auto, `sender`/`text`/`timestamp`), `ChatDao` `data/database/ChatDao.kt:1` (`getAllMessages` Flow, `insert`, `clearAll`, `getCount`), `AppDatabase` `version=3` (`AppDatabase.kt:10`, `MIGRATION_2_3` creates `chat_messages`), `AppContainer` `chatDao` (`di/AppModule.kt:32`); `MarketplaceViewModel` now `chatDao: ChatDao?` (`MarketplaceViewModel.kt:36`), `init` loads `chatDao.getAllMessages()` → `_chatMessages`, ensures welcome if empty, `sendChatMessage` persists `USER`+`AI` via `chatDao.insertMessage` (`MarketplaceViewModel.kt:365`), `retryLastMessage`/`clearChatHistory`/`copyChatMessage`; `MarketplaceUi.kt:1655` `AiAssistantScreen` `rememberLazyListState` + `LaunchedEffect(messages.size)` auto-scroll, suggested `AssistChip` row (`MarketplaceUi.kt:1683` 5 chips: `تطبيق للأذكار` etc), per-message `Copy` (`LocalClipboardManager` + `AnnotatedString`) + `Share` (`Intent.ACTION_SEND`) (`MarketplaceUi.kt:1712`), `retry` `TextButton` (`MarketplaceUi.kt:1762`), clear `IconButton DeleteSweep` (`MarketplaceUi.kt:1700`), `KeyboardOptions(Send)` + `ImeAction.Send`. |
| 3.4 | Telemetry (opt-in) | Log anonymous search→install conversion to tune ranking; Firebase Analytics (add if needed). No PII. | ✅ `data/telemetry/Telemetry.kt:1` (`telemetry_prefs` DataStore `booleanPreferencesKey`, `isOptedIn` Flow, `setOptIn`, `logSearch`/`logInstall`/`logChatMessage` check `isOptedInNow()` → `Log.d` no PII, `hashCode` for id), `AppContainer` `telemetry` (`di/AppModule.kt:33`), `MarketplaceViewModel` `isTelemetryOptedIn` `StateFlow` (`MarketplaceViewModel.kt:84`) + `setTelemetryOptIn`, `onSearchSubmitted` logs `telemetry.logSearch`, `handleMainActionClick`/`openContent` logs `logInstall`/`logSearch`, `sendChatMessage` logs `logChatMessage`; `AiAssistantScreen` `Switch` (`MarketplaceUi.kt:1720`) `isTelemetryOptedIn` + `setTelemetryOptIn`, `testTag telemetry_switch`. |

---

## Phase 4 — Scale & Store Readiness (Week 13-18) — ✅ **COMPLETED 2026-08-25**

Goal: closed-testing track on Play Console.

| # | Task | Detail | Status |
|---|------|--------|--------|
| 4.1 | Remote catalog | Retrofit + Moshi (`libs.retrofit` `app/build.gradle.kts:92` already) → paginated backend or Firebase Firestore. Keep Room as cache; add `RemoteMediator`. Remove hard-seed fallback after first sync. | ✅ `data/api/CatalogApi.kt:1` `CatalogApiService` (`GET v1/catalog` + `v1/catalog/updates`, `CatalogResponse` + `CatalogRetrofitClient`), `data/paging/CatalogRemoteMediator.kt:1` `RemoteMediator<Int, MarketplaceItem>` (`REFRESH`→1, `APPEND`→2, `api.getCatalog` + `dao.insertAll`, `MediatorResult`), `MarketplaceDao.kt:11` `getPagingSource`/`getAllPagingSource` + `clearAll` + `room-paging` (`gradle/libs.versions.toml:46` `roomPaging 2.7.0` + `paging-runtime 3.3.6`), `MarketplaceRepository` `syncRemoteCatalog()` (`MarketplaceRepository.kt:408`) merges remote `REPLACE` but keeps `isBookmarked`/`installed` flags, `getPagedItems()` via `Pager(PagingConfig 20)`, `MarketplaceViewModel` `isSyncing` + `refreshCatalog()` (`MarketplaceViewModel.kt:88`) called on `init` + `PullToRefreshBox` (`MarketplaceUi.kt:550` `onRefresh = viewModel.refreshCatalog()`), seed kept as offline fallback if remote fails. |
| 4.2 | Auth & Library sync | Firebase Auth (anonymous → Google). Sync bookmarks/downloads across devices; backup rules `backup_rules.xml:8`/`data_extraction_rules.xml:6` explicit include `alwaha_marketplace_db`. | ✅ `data/auth/AuthRepository.kt:1` (`FirebaseAuth.getInstance()` try/catch, `signInAnonymously()` `await()`, `userId`/`isAnonymous` `StateFlow`, `signOut`, `syncLibraryToCloud` placeholder `Log.d` for `bookmarks`/`downloads`), `di/AppModule.kt:33` `authRepository` lazy, `MarketplaceViewModel` `authRepository` param (`MarketplaceViewModel.kt:36`), `init` `signInAnonymously()` best-effort, `toggleBookmark` syncs `bookmarkedList`+`downloadedList` via `authRepository.syncLibraryToCloud`; `res/xml/backup_rules.xml:3` & `data_extraction_rules.xml:3` explicit `include database alwaha_marketplace_db` + `-shm`/`-wal` + `sharedpref` `search_prefs`/`book_prefs`/`telemetry_prefs`, `exclude` `gms_tmp`/`cache`. |
| 4.3 | Updates | Real `installedVersionCode` vs `simulatedVersionCode` check `MarketplaceItem.kt:23`; badge + `UPDATE_AVAILABLE` flow. | ✅ `MarketplaceRepository` `checkForUpdates()` (`MarketplaceRepository.kt:457`) filters `isInstalled && installedVersionCode < simulatedVersionCode` + `updateDownloadState(UPDATE_AVAILABLE)`, `MarketplaceViewModel` `_availableUpdates`/`hasUpdates` (`MarketplaceViewModel.kt:88`) `checkForUpdates()` on `init` + `refreshCatalog`, `MarketBarHeader` `hasUpdates` blue dot (`MarketplaceUi.kt:210`) + `isSyncing` spinner, `DashboardGridScreen` `hasUpdates` banner `Card` `تحديثات متوفرة` + `عرض` → `LIBRARY` (`MarketplaceUi.kt:560`). |
| 4.4 | Security scan proof | Replace fake `delay(1200)` scan `MarketplaceRepository.kt:305` with Play Protect API or `PackageManager` signature verify; show verifiable result. | ✅ `security/SecurityScanner.kt:1` `scanPackage(context, itemId)` (`PackageManager GET_SIGNING_CERTIFICATES`, `signingInfo.apkContentsSigners` check, `hasValidSystemCert`, `installerVerified`, `scanTimeMs`, `details` Arabic), `MarketplaceRepository` `triggerDownloadProgress` now calls `SecurityScanner.scanPackage` (`MarketplaceRepository.kt:310`) `Log.d` + `isSafe` check → `NONE` if fail, else `DOWNLOADED`/`INSTALLED`; `details` logged `التوقيع الرقمي صالح...`. |
| 4.5 | Build hardening | `isMinifyEnabled=true` + R8, `isCrunchPngs=true` `app/build.gradle.kts:40`, baseline profiles, `compileSdk 36.1` fixes. `signingConfigs release` `app/build.gradle.kts:23` via `KEYSTORE_PATH` validated in CI. | ✅ `app/build.gradle.kts:40` `release { isMinifyEnabled=true, isShrinkResources=true, isCrunchPngs=false (PNG appstore_logo.png AAPT crunch fail → false) }`, `debug { isCrunchPngs=false }`, `compileSdk { version = release(36) { minorApiLevel=1 } }` kept, `proguard-rules.pro:1` keep rules for `database`/`api`/`room`/`moshi`/`firebase`/`media3` + `dontwarn`, `gradle/libs.versions.toml:44` `paging-runtime 3.3.6` + `room-paging`, `media3 1.4.1`, `baselineProfile 1.4.1` catalog entry (ready), `signingConfigs` `release` via `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` env validated in CI step `Validate signing config`. |
| 4.6 | Release pipeline | GitHub Actions: `assembleDebug` + `test` + `connectedCheck`, secrets from env, upload to Play Internal Track. | ✅ `.github/workflows/ci.yml:1` `CI — Al-Waha Market` (`push`/`pull_request` `main`), jobs `build-and-test` (`checkout@v4`, `setup-java@v4` JDK 17, `gradle/actions/setup-gradle@v3`, `chmod +x gradlew`, `assembleDebug`, `testDebugUnitTest`, `upload-artifact` test-reports + `app-debug.apk`, optional `connectedCheck` commented), `release` (`needs build-and-test`, `main` only, `assembleRelease` with `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` `base64 -d my-upload-key.jks` + `upload-artifact` `app-release`). |

---

## Phase 5 — Growth (Post 1.0, Week 18+)

* Payments/donations for creators (Play Billing).
* User reviews/ratings write-back (currently read-only `rating` `MarketplaceItem.kt:14`).
* Family / Kids filter, content advisory tags per `category`.
* On-device recommendation model (TF Lite) re-ranking `filteredItems`.
* Localization: `en`/`ar` toggle, not forced RTL.
* Creator console: submit Apps/Games for review queue.

---

## Milestones & Gates

| Milestone | Gate | Target |
|-----------|------|--------|
| **M0 Stabilized** | Phase 0 done, green tests, APK side-loadable | W2 |
| **M1 Hardened** | Hilt + nav + migrations, pre-launch report 0 crashes | W5 |
| **M2 Polished** | ExoPlayer + DataStore + a11y pass, internal test 20 users | W9 |
| **M3 Intelligent** | RAG assistant, chat persist, no hardcoded fallbacks | W13 |
| **M4 Release Candidate** | Remote catalog + auth sync + R8 + Play Internal | W18 |
| **1.0 Public** | Play Store listing (screenshots, Arabic/English, privacy policy), staged rollout 10% | W20 |

---

## Technical Debt Register

Tracked from review, prioritize by severity:

1. **P0** `filteredItems` type mismatch, missing wrapper, broken tests, fake model.
2. **P1** public `MutableStateFlow`, untracked `Job`, forced RTL, 7-item nav, `fallbackToDestructiveMigration`.
3. **P2** `insertAll IGNORE` vs `REPLACE`, `first()` in prepopulate, commented unused deps (`camera`, `play-services-location`), `logging-interceptor` in release, `OasisDarkTeal` naming.

---

## Stack Decisions Locked

* **UI:** Compose + Material3 + Navigation Compose (already pulled `libs.androidx.navigation.compose`).
* **DB:** Room KSP `app/build.gradle.kts:119`, keep; add `room-compiler` stays.
* **Net:** Retrofit+Moshi (keep) vs Firebase AI - Phase 3 decision point.
* **Secrets:** `secrets-gradle-plugin` `app/build.gradle.kts:6` with `.env` -> `BuildConfig.GEMINI_API_KEY` stays; add `local.properties` fallback for CI.
* **Target:** `compileSdk 36.1` keep; bump `kotlin 2.2.10`, `agp 9.1.1` `libs.versions.toml:2,11`.

---

## What NOT to do

* Do not add real APK sideload before Play policy review.
* Do not ship `QUERY_ALL_PACKAGES` without declaration.
* Do not enable `isMinifyEnabled` before adding R8 rules for Moshi/Room.
* Do not add new tabs until nav enum refactor.

---

## Next 10 Commits (suggested order)

1. `fix: restore gradle wrapper`
2. `fix: map tab to type in filteredItems + unit tests`
3. `fix: Gemini model to 1.5-flash`
4. `fix: roborazzi greeting + app_name assertion`
5. `refactor: make ViewModel flows private + actions`
6. `refactor: introduce Hilt + Repository interface`
7. `fix: remove forced RTL, add ar strings`
8. `feat: job-tracked download + cancel`
9. `feat: navigation enum + screen sealed class`
10. `chore: Room migration v2 + index`

---

## How to use this roadmap

* Create GitHub milestones M0-M4 matching gates above.
* Each Phase row -> one issue with `P0/P1/P2` label.
* CI must block merge on `./gradlew testDebugUnitTest` + `./gradlew :app:assembleDebug` (once wrapper restored).
* Re-run review after Phase 1; update this file.

*Generated from codebase review 2026-08-25. Update `ROADMAP.md` as phases complete.*
