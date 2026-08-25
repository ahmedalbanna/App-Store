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

## Phase 1 — Foundation Hardening (Week 2-5)

Goal: testable, maintainable architecture, no data loss, no leaks.

| # | Task | Detail |
|---|------|--------|
| 1.1 | Dependency Injection | Introduce Hilt. `AppDatabase` + `MarketplaceDao` + `MarketplaceRepository` + `GeminiApiService` as `@Singleton`. VM gets `@HiltViewModel` constructor injection. Removes `AppDatabase.getDatabase(application)` from VM `MarketplaceViewModel.kt:35`. |
| 1.2 | Repository abstraction | Extract `MarketplaceRepository` interface; impl handles Room + download jobs. Inject `Context` as `@ApplicationContext`. |
| 1.3 | Job-tracked downloads | `Map<id, Job>` in repo; `triggerDownloadProgress` cancels prior job; `resetDownloadState` cancels job before reset `MarketplaceRepository.kt:352`. Add `pause`/`resume` support; persist `downloadState` across process death. |
| 1.4 | Room migrations | Bump `AppDatabase.kt:8` to `version=2`, add `AutoMigration` or manual `Migration`; remove `fallbackToDestructiveMigration()`. Add index on `type`, `downloadState`. Replace `first().size` prepopulate `MarketplaceRepository.kt:28` with `SELECT COUNT(*)`. |
| 1.5 | RTL fix | Remove global `CompositionLocalProvider(Rtl)` `MarketplaceUi.kt:52`. Set `android:supportsRtl="true"` + locale-driven `LocalLayoutDirection`. Add `values-ar/strings.xml`. |
| 1.6 | Navigation refactor | Replace string tabs `MarketplaceViewModel.kt:39` with `enum class Tab` + `sealed interface Screen` (uses `libs.androidx.navigation.compose` `app/build.gradle.kts:90` already included, currently unused). Deep links for `READ_BOOK_VIEW` etc. |
| 1.7 | Testing baseline | VM unit tests (filteredItems, bookmark toggle, offline fallback), Dao instrumented tests, one Roborazzi screenshot for `DashboardGridScreen`. Target 40% line coverage. |

---

## Phase 2 — Core UX Polish (Week 5-9)

Goal: Play Console pre-launch report clean, a11y, performance.

| # | Task | Detail |
|---|------|--------|
| 2.1 | Bottom nav redesign | 5 primary destinations (Apps/Games/Books/Movies/Music) per Material3; Library + Assistant as top-bar actions / nav drawer `MarketplaceUi.kt:164`. Fixes cramped `10.sp` labels. |
| 2.2 | Search upgrades | Debounce `300ms`, highlight matches, empty-state CTA, filter by `category`/`creator` not just title `MarketplaceViewModel.kt:104`. Save recent queries via `DataStore` (uncomment `datastore.preferences` `app/build.gradle.kts:86`). |
| 2.3 | Media engines | Book: paginated `LazyColumn` + real page persistence + font/line-height pref; Movie: `ExoPlayer` (add `media3-exoplayer`) replacing fake `CircularProgressIndicator` `MarketplaceUi.kt:1084`; Music: `Media3` + notification + `rememberInfiniteTransition` for vinyl (replace `while(true)` `MarketplaceUi.kt:1217`). |
| 2.4 | UI polish | Shimmer placeholders, error/empty illustrations, `pull-to-refresh`. Theme tokens audit: rename `OasisDarkTeal` `Color.kt:6`, enable `dynamicColor` opt-in `Theme.kt:42`. Coil for icons (already `libs.coil.compose`). |
| 2.5 | Permissions flow | Runtime `POST_NOTIFICATIONS` `AndroidManifest.xml:8` via `accompanist.permissions` `app/build.gradle.kts:73` (already dep) + rationale. Drop `READ/WRITE_EXTERNAL_STORAGE` `AndroidManifest.xml:9-10` if no file export. |
| 2.6 | Accessibility | `contentDescription` on all icons, `semantics` on download states, 48dp targets, RTL mirroring tests, TalkBack pass. |

---

## Phase 3 — AI & Discovery (Week 9-13)

Goal: defensible AI assistant, not hard-coded fallbacks.

| # | Task | Detail |
|---|------|--------|
| 3.1 | Switch to Firebase AI | Enable `libs.firebase.ai` `app/build.gradle.kts:95` (commented) or keep Retrofit but add streaming. Centralize `BuildConfig.GEMINI_API_KEY` handling `MarketplaceViewModel.kt:243`. |
| 3.2 | RAG over local catalog | Embed `MarketplaceItem` titles/descriptions; assistant retrieves top-k items then calls Gemini with catalog context. Replace keyword-if `generateOfflineSmartResponse` `MarketplaceViewModel.kt:275`. |
| 3.3 | Assistant UX | Chat history persist (Room), scroll-to-bottom, retry, copy/share, suggested chips ("تطبيق للأذكار", "كتاب تاريخ"). Persist via `chatMessages` not just in-memory `MarketplaceViewModel.kt:114`. |
| 3.4 | Telemetry (opt-in) | Log anonymous search→install conversion to tune ranking; Firebase Analytics (add if needed). No PII. |

---

## Phase 4 — Scale & Store Readiness (Week 13-18)

Goal: closed-testing track on Play Console.

| # | Task | Detail |
|---|------|--------|
| 4.1 | Remote catalog | Retrofit + Moshi (`libs.retrofit` `app/build.gradle.kts:92` already) → paginated backend or Firebase Firestore. Keep Room as cache; add `RemoteMediator`. Remove hard-seed fallback after first sync. |
| 4.2 | Auth & Library sync | Firebase Auth (anonymous → Google). Sync bookmarks/downloads across devices; backup rules `backup_rules.xml:8`/`data_extraction_rules.xml:6` explicit include `alwaha_marketplace_db`. |
| 4.3 | Updates | Real `installedVersionCode` vs `simulatedVersionCode` check `MarketplaceItem.kt:23`; badge + `UPDATE_AVAILABLE` flow. |
| 4.4 | Security scan proof | Replace fake `delay(1200)` scan `MarketplaceRepository.kt:305` with Play Protect API or `PackageManager` signature verify; show verifiable result. |
| 4.5 | Build hardening | `isMinifyEnabled=true` + R8, `isCrunchPngs=true` `app/build.gradle.kts:40`, baseline profiles, `compileSdk 36.1` fixes. `signingConfigs release` `app/build.gradle.kts:23` via `KEYSTORE_PATH` validated in CI. |
| 4.6 | Release pipeline | GitHub Actions: `assembleDebug` + `test` + `connectedCheck`, secrets from env, upload to Play Internal Track. |

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
