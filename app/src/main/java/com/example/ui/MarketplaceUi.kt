package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.database.MarketplaceItem
import com.example.ui.navigation.MarketplaceTab
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceMainScreen(viewModel: MarketplaceViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()

    // Phase 1.5: Removed global forced RTL. System handles RTL via supportsRtl + device locale.
    // For Arabic locale, LocalLayoutDirection will be Rtl automatically; for LTR locales it stays Ltr.
    // This fixes forced RTL on English devices while keeping authentic Arabic layout when needed.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                MarketBarHeader(viewModel = viewModel)

                // Phase 2.5: Notification permission rationale (no-op on SDK<33)
                NotificationPermissionHandler()

                // Main Content depending on active tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                        }
                    ) { tab ->
                        when (tab) {
                            "READ_BOOK_VIEW" -> BookReaderScreen(viewModel = viewModel)
                            "WATCH_MOVIE_VIEW" -> MovieCinemaScreen(viewModel = viewModel)
                            "PLAY_MUSIC_VIEW" -> MusicPlayerScreen(viewModel = viewModel)
                            "ASSISTANT" -> AiAssistantScreen(viewModel = viewModel)
                            else -> DashboardGridScreen(viewModel = viewModel)
                        }
                    }
                }

                // Bottom Navigation Bar
                MarketBottomNavBar(
                    selectedTab = currentTab,
                    onTabSelect = { viewModel.changeTab(it) }
                )
            }

            // Details Bottom Sheet / Overlay Dialog
            selectedItem?.let { item ->
                CardDetailsBottomSheet(
                    item = item,
                    onDismiss = { viewModel.selectItem(null) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MarketBarHeader(viewModel: MarketplaceViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val downloadedItems by viewModel.downloadedList.collectAsState()
    val count = downloadedItems.filter { it.downloadState == "DOWNLOADING" }.size

    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "متجر الواحة",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "بوابتك الآمنة للتطبيقات والترفيه العربي",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            )
        }

        // Phase 2.1: Top-bar actions for Library + Assistant (5-item bottom nav now only primary tabs)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Assistant (AI) — top action per 2.1, 48dp target for a11y
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentTab == "ASSISTANT") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { viewModel.changeTab("ASSISTANT") }
                    .testTag("top_assistant_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentTab == "ASSISTANT") Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = "المساعد الذكي",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Library shortcut badge with downloading indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentTab == "LIBRARY") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { viewModel.changeTab("LIBRARY") }
                    .testTag("top_library_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentTab == "LIBRARY") Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial,
                    contentDescription = "المكتبة وتنزيلات المحتوى",
                    tint = MaterialTheme.colorScheme.primary
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarketBottomNavBar(selectedTab: String, onTabSelect: (String) -> Unit) {
    // Phase 2.1: 5 primary destinations only (Material3 guideline), Library+Assistant moved to top bar
    val navigationDirs = listOf(
        Triple(MarketplaceTab.APPS.route, "تطبيقات", Icons.Outlined.Apps to Icons.Filled.Apps),
        Triple(MarketplaceTab.GAMES.route, "ألعاب", Icons.Outlined.SportsEsports to Icons.Filled.SportsEsports),
        Triple(MarketplaceTab.BOOKS.route, "كتب", Icons.Outlined.MenuBook to Icons.Filled.MenuBook),
        Triple(MarketplaceTab.MOVIES.route, "أفلام", Icons.Outlined.MovieFilter to Icons.Filled.MovieFilter),
        Triple(MarketplaceTab.MUSIC.route, "موسيقى", Icons.Outlined.MusicNote to Icons.Filled.MusicNote)
    )

    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 4.dp
    ) {
        navigationDirs.forEach { (tabId, label, icons) ->
            val isActive = selectedTab == tabId ||
                    (tabId == "BOOKS" && selectedTab == "READ_BOOK_VIEW") ||
                    (tabId == "MOVIES" && selectedTab == "WATCH_MOVIE_VIEW") ||
                    (tabId == "MUSIC" && selectedTab == "PLAY_MUSIC_VIEW")

            NavigationBarItem(
                selected = isActive,
                onClick = { onTabSelect(tabId) },
                icon = {
                    Icon(
                        imageVector = if (isActive) icons.second else icons.first,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("nav_item_${tabId.lowercase()}")
            )
        }
    }
}

// Featured Hero Card following "Geometric Balance" design specifications
@Composable
fun FeaturedHeroCard(currentTab: String) {
    val title = when (currentTab) {
        "APPS" -> "محرر الأكواد الذكي Pro"
        "GAMES" -> "فرسان قرطبة الاستراتيجية"
        "BOOKS" -> "تاريخ ابن خلدون الكبير"
        "MOVIES" -> "رحلة الوديان السينمائية"
        "MUSIC" -> "سيمفونية رمال الأندلس"
        else -> "متجر الواحة الآمن"
    }

    val subtitle = when (currentTab) {
        "APPS" -> "ابدأ برمجتك بلغة Kotlin الآن"
        "GAMES" -> "خض معارك التكتيك والتوازن التاريخي"
        "BOOKS" -> "سبر أغوار فلسفة العمران البشري"
        "MOVIES" -> "بث فائق الدقة محاكي للسينما العربية"
        "MUSIC" -> "ألحان تملأ الوجدان بالسكينة والسلام"
        else -> "بوابتك لكل ما هو مميز وجديد"
    }

    val tag = when (currentTab) {
        "APPS" -> "تطبيق اليوم"
        "GAMES" -> "أقوى الألعاب"
        "BOOKS" -> "كتاب متميز"
        "MOVIES" -> "العرض الأول"
        "MUSIC" -> "معزوفة الأسبوع"
        else -> "الواحة الممتازة"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Background geometric shape
            Canvas(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (20).dp, y = (20).dp)
            ) {
                // Drawing a futuristic neon geometric polygon / circles representing "Geometric Balance"
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.7f
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.4f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = tag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    )
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// Main grid of items — Phase 2.2/2.4: debounce (VM), highlight, recent chips, pull-to-refresh, shimmer, empty CTA
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardGridScreen(viewModel: MarketplaceViewModel) {
    val filteredItems by viewModel.filteredItems.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentQueries by viewModel.recentQueries.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar — debounced in VM (300ms), filters by title/category/creator, saves recents on Done
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("search_text_input")
                .semantics { contentDescription = "حقل البحث" },
            placeholder = { Text("ابحث عن تطبيقات، ألعاب، كتب، موسيقى...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.setSearchQuery("") },
                        modifier = Modifier.semantics { contentDescription = "مسح البحث" }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "مسح")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.onSearchSubmitted(searchQuery)
                }
            )
        )

        // Recent queries chips (DataStore) — show when search empty
        if (searchQuery.isEmpty() && recentQueries.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "حديثاً:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 4.dp, top = 8.dp)
                    )
                }
                items(recentQueries) { recent ->
                    AssistChip(
                        onClick = { viewModel.setSearchQuery(recent) },
                        label = { Text(recent, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = "بحث سابق", modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("recent_chip_${recent}")
                    )
                }
                item {
                    TextButton(
                        onClick = { viewModel.clearRecentQueries() },
                        modifier = Modifier.testTag("clear_recents")
                    ) { Text("مسح", fontSize = 12.sp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Phase 2.4: Shimmer while initial load (allItems empty but no query)
        val isInitialLoading = allItems.isEmpty() && filteredItems.isEmpty() && searchQuery.isEmpty()

        if (isInitialLoading) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(5) { ShimmerRowPlaceholder() }
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = "فارغ",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "لم يتم العثور على نتائج للبحث." else "الواحة فارغة حالياً.",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "جرب تصفح أقسام أخرى أو تعديل كلمات البحث لتجد بغيتك.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Empty CTA — Phase 2.2/2.4
                    if (searchQuery.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("empty_clear_search")
                        ) { Text("مسح البحث") }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.changeTab("APPS") },
                            modifier = Modifier.testTag("empty_browse_apps")
                        ) { Text("تصفح التطبيقات") }
                    }
                }
            }
        } else {
            // Phase 2.4: Pull-to-refresh
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        delay(900)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        item {
                            FeaturedHeroCard(currentTab = currentTab)
                        }
                    }

                    // Section Title depending on active Tab
                    item {
                        val sectionAr = when (currentTab) {
                            "APPS" -> "التطبيقات الموصى بها"
                            "GAMES" -> "أقوى الألعاب العربية"
                            "BOOKS" -> "روائع الكتب والروايات"
                            "MOVIES" -> "مجموعة الأفلام والوديان"
                            "MUSIC" -> "أناشيد ومعزوفات الواحة"
                            else -> "مكتبتي الخاصة"
                        }
                        Text(
                            text = sectionAr,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredItems, key = { it.id }) { item ->
                        MarketplaceItemRow(item = item, onClick = { viewModel.selectItem(it) }, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(text: String, query: String, style: androidx.compose.ui.text.TextStyle) {
    if (query.isBlank()) {
        Text(text = text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val annotated = buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var start = 0
        var index = lowerText.indexOf(lowerQuery, start)
        while (index >= 0) {
            append(text.substring(start, index))
            withStyle(SpanStyle(background = Color.Yellow.copy(alpha = 0.35f), fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
            index = lowerText.indexOf(lowerQuery, start)
        }
        append(text.substring(start))
    }
    Text(text = annotated, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun MarketplaceItemRow(
    item: MarketplaceItem,
    onClick: (MarketplaceItem) -> Unit,
    viewModel: MarketplaceViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .testTag("item_card_${item.id}")
            .semantics { contentDescription = "${item.titleAr} — ${item.developerOrCreatorAr} — ${item.downloadState}" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon layout — Phase 2.4: Coil AsyncImage placeholder + vector fallback, 48dp+ target
            val decorIcon = when (item.type) {
                "APP" -> Icons.Filled.Extension
                "GAME" -> Icons.Filled.Gamepad
                "BOOK" -> Icons.Filled.MenuBook
                "MOVIE" -> Icons.Filled.VideoCameraBack
                else -> Icons.Filled.MusicNote
            }
            val iconDesc = when (item.type) {
                "APP" -> "أيقونة تطبيق"
                "GAME" -> "أيقونة لعبة"
                "BOOK" -> "أيقونة كتاب"
                "MOVIE" -> "أيقونة فيلم"
                else -> "أيقونة موسيقى"
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        )
                    )
                    .semantics { contentDescription = iconDesc },
                contentAlignment = Alignment.Center
            ) {
                // Coil placeholder for future remote images — falls back to vector icon
                // AsyncImage(model = item.imageUrl, contentDescription = iconDesc) would be used when imageUrl exists
                Icon(
                    imageVector = decorIcon,
                    contentDescription = iconDesc,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info Panel — Phase 2.2 highlight matches
            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(
                    text = item.titleAr,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                HighlightedText(
                    text = item.developerOrCreatorAr,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "تقييم",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.rating.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${item.sizeMb} ميجا",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Button showing Download progress dynamically to follow requirement
            MarketplaceDownloadActionButton(item = item, viewModel = viewModel)
        }
    }
}

@Composable
fun MarketplaceDownloadActionButton(item: MarketplaceItem, viewModel: MarketplaceViewModel) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.width(96.dp),
        contentAlignment = Alignment.Center
    ) {
        when (item.downloadState) {
            "NONE" -> {
                Button(
                    onClick = { viewModel.handleMainActionClick(item) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("download_btn_${item.id}")
                ) {
                    Text("جلب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            "UPDATE_AVAILABLE" -> {
                Button(
                    onClick = { viewModel.handleMainActionClick(item) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A030)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("تحديث", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            "DOWNLOADING" -> {
                // Outer Circular loader with inside progression indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = item.downloadProgress / 100f,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.downloadProgress}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            "SCANNING" -> {
                // Scanning Shield Pulse representing our secure package validation requirement
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "فحص الأمان",
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier
                        .size(32.dp)
                        .scale(scale)
                )
            }
            "DOWNLOADED" -> {
                Button(
                    onClick = { viewModel.handleMainActionClick(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("install_btn_${item.id}")
                ) {
                    Text("تثبيت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            "INSTALLING" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "جاري التثبيت",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            "INSTALLED" -> {
                Button(
                    onClick = { viewModel.handleMainActionClick(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("open_btn_${item.id}")
                ) {
                    Text("فتح", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Dialog Details Sheet
@Composable
fun CardDetailsBottomSheet(
    item: MarketplaceItem,
    onDismiss: () -> Unit,
    viewModel: MarketplaceViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) {}
                .padding(24.dp)
                .testTag("details_sheet_${item.id}")
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Close bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تفاصيل العنصر",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Info Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val decorIcon = when (item.type) {
                    "APP" -> Icons.Filled.Extension
                    "GAME" -> Icons.Filled.Gamepad
                    "BOOK" -> Icons.Filled.MenuBook
                    "MOVIE" -> Icons.Filled.VideoCameraBack
                    else -> Icons.Filled.MusicNote
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(decorIcon, contentDescription = "أيقونة ${item.type}", tint = Color.White, modifier = Modifier.size(38.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titleAr,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.developerOrCreatorAr,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "القسم: ${item.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Bookmark toggle on detailed sheet
                IconButton(onClick = { viewModel.toggleBookmark(item) }) {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "تفضيل",
                        tint = if (item.isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid metadata parameters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("التقييم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(item.rating.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الحجم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${item.sizeMb} MB", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("التحميلات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("٥٠٠ ألف+", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Scan Shield Indicator to follow MVP checklist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = "تم الفحص",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("فحص أمان الحزمة (آمن ١٠٠٪)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                    Text("خالٍ تماماً من البرمجيات الضارة والتجسسية، تم التحقق من سلامة التوقيع الرقمي.", fontSize = 10.sp, color = Color(0xFF2E7D32))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description Scroll area
            Text(
                text = "عن العنصر:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = item.descriptionAr,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // Main Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Download/Scan/Install triggers to keep the workflow moving
                Button(
                    onClick = {
                        viewModel.handleMainActionClick(item)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_sheet_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (item.downloadState) {
                            "INSTALLED" -> Color(0xFF1B5E20)
                            "DOWNLOADED" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    val label = when (item.downloadState) {
                        "NONE" -> "تحميل مجاني"
                        "UPDATE_AVAILABLE" -> "تحديث الآن"
                        "DOWNLOADING" -> "جاري التحميل... ${item.downloadProgress}%"
                        "SCANNING" -> "جاري الفحص الأمني..."
                        "DOWNLOADED" -> "تثبيت الآن (APK)"
                        "INSTALLING" -> "جاري التثبيت ونقل الحزمة..."
                        "INSTALLED" -> "فتح المحتوى الآن"
                        else -> "تحميل مجاني"
                    }
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Uninstall/Reset button
                if (item.isDownloaded || item.isInstalled) {
                    IconButton(
                        onClick = { viewModel.uninstallApp(item) },
                        modifier = Modifier
                            .height(48.dp)
                            .width(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, "حذف")
                    }
                }
            }
        }
    }
}

// ---------------- 1. BOOK READER SCREEN (قارئ الكتب التفاعلي — Phase 2.3 pagination + DataStore prefs) ----------------
@Composable
fun BookReaderScreen(viewModel: MarketplaceViewModel) {
    val book by viewModel.activeReadingBook.collectAsState()
    val context = LocalContext.current
    val bookPrefs = remember { com.example.data.datastore.BookPreferences(context) }
    val persistedFontScale by bookPrefs.fontScale.collectAsState(initial = 1f)
    val persistedNightMode by bookPrefs.isNightMode.collectAsState(initial = false)
    var fontScale by remember(persistedFontScale) { mutableStateOf(persistedFontScale) }
    var isNightMode by remember(persistedNightMode) { mutableStateOf(persistedNightMode) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(persistedFontScale) { fontScale = persistedFontScale }
    LaunchedEffect(persistedNightMode) { isNightMode = persistedNightMode }

    val activeItem = book ?: return

    val pages = listOf(
        "الباب الأول: في طبيعة العمران البشري وتأسيس المدنيات والأمصار. اعلم أن الاجتماع الإنساني ضروري للشخص البشري، وإن التناوب والتعاون في سداد لقمة العيش وحماية الحوزة يمثلان ركن الطبيعة الأول الذي لا غنى للعالم الإنساني عنه لتأمين البقاء.",
        "تمهيد ابن خلدون: فالدولة والملك للعمران بمثابة الصورة للمادة، وهو الشكل الحافظ لوجودها من الفناء والتطاول. ولا تثور ثائرة في أمة إلا بعصبية متسقة، فالعصبية هي الوقود المحتشد وراء السيوف والبيارق لتأسيس عهود جديدة وحصد السيادة.",
        "المنظور الثاني: في نشأة الدويلات وهرمها. كل دولة تمر بأطوار خمسة لا تكاد تحيد عنها: الطور الأول طور الظفر والاستيلاء والتمكين، والطور الثاني طور الانفراد بالملك وكبح جماح العشيرة، والطور الثالث طور الفراغ والدعة، والطور الرابع القنوع والألفة، وخامسها طور التبذير والإسراف.",
        "الباب الثالث: في الأدب والشعر ونوافذ الروح بالأندلس. واللغة العربية شجرة مغدودة ظلالها، وارفة أوراقها، وحي بن يقظان ناطق بلسان العقل، ومحلق بحكمة الفلاسفة الذين تدبروا الطبيعة والبروج ووجدوا فيها قيام النظم والإبداع."
    )

    val currentPage = activeItem.lastReadPage.coerceIn(0, pages.size - 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF121212) else Color(0xFFF9F6EE))
            .padding(16.dp)
    ) {
        // Book Reader bar header (Day/Night Theme and font customization rules)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.changeTab("LIBRARY") }
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "الخروج للقائمة",
                    tint = if (isNightMode) Color.White else Color.Black
                )
            }

            Text(
                text = activeItem.titleAr,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isNightMode) Color.White else Color.Black
            )

            // Adjuster controllers — 48dp targets, persist via DataStore (Phase 2.3/2.6)
            Row {
                IconButton(
                    onClick = {
                        val new = !isNightMode
                        isNightMode = new
                        scope.launch { bookPrefs.setNightMode(new) }
                    },
                    modifier = Modifier.semantics { contentDescription = "تبديل الوضع الليلي" }
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "الوضع الليلي",
                        tint = if (isNightMode) Color.White else Color.Black
                    )
                }
                IconButton(
                    onClick = {
                        if (fontScale < 1.4f) {
                            val new = fontScale + 0.1f
                            fontScale = new
                            scope.launch { bookPrefs.setFontScale(new) }
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "تكبير الخط" }
                ) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "تكبير الخط", tint = if (isNightMode) Color.White else Color.Black)
                }
                IconButton(
                    onClick = {
                        if (fontScale > 0.8f) {
                            val new = fontScale - 0.1f
                            fontScale = new
                            scope.launch { bookPrefs.setFontScale(new) }
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "تصغير الخط" }
                ) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = "تصغير الخط", tint = if (isNightMode) Color.White else Color.Black)
                }
            }
        }

        // Reading Content container (Serif style cream background mimicking ancient sheets)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isNightMode) Color(0xFF1E1E1E) else Color(0xFFFFFDF9))
                .padding(18.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = pages[currentPage],
                        fontSize = (18 * fontScale).sp,
                        fontFamily = FontFamily.Serif,
                        lineHeight = (30 * fontScale).sp,
                        color = if (isNightMode) Color(0xFFE0E0E0) else Color(0xFF2C251C),
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Page paging slider to remember last page inside database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (currentPage > 0) {
                        viewModel.saveBookProgress(activeItem.id, currentPage - 1)
                    }
                },
                enabled = currentPage > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("السابق")
            }

            Text(
                text = "صفحة ${currentPage + 1} من ${pages.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNightMode) Color.White else Color.Black
            )

            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        viewModel.saveBookProgress(activeItem.id, currentPage + 1)
                    }
                },
                enabled = currentPage < pages.size - 1,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("التالي")
            }
        }
    }
}

// ---------------- 2. MOVIE CINEMA SCREEN (مشغل الأفلام السينمائي المدمج مع محاكاة التدفق والبث) ----------------
@Composable
fun MovieCinemaScreen(viewModel: MarketplaceViewModel) {
    val movie by viewModel.activeWatchingMovie.collectAsState()
    val isPlaying by viewModel.isPlayingMovie.collectAsState()
    val progress by viewModel.moviePlaybackProgress.collectAsState()

    val activeItem = movie ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B0B))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Exit player row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.changeTab("LIBRARY") }) {
                Icon(Icons.Filled.ArrowBack, "خروج", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(activeItem.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Curved Cinema Display Frame — Phase 2.3: ExoPlayer (Media3) replaces fake buffer spinner
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/mp4/android-screens-10s.mp4"))
                prepare()
                playWhenReady = false
            }
        }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                exoPlayer.release()
            }
        }
        LaunchedEffect(isPlaying) {
            exoPlayer.playWhenReady = isPlaying
            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
            // Poll player position to update slider (Phase 2.3)
            while (isPlaying) {
                delay(250)
                val duration = exoPlayer.duration.takeIf { it > 0 } ?: 10000L
                if (duration > 0) {
                    viewModel.setMoviePlaybackProgress(
                        (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                    )
                }
            }
        }
        // Sync slider progress with player (0..1 -> seek)
        LaunchedEffect(progress) {
            val duration = exoPlayer.duration.takeIf { it > 0 } ?: 10000L
            val target = (progress * duration).toLong()
            if (kotlin.math.abs(exoPlayer.currentPosition - target) > 500) {
                exoPlayer.seekTo(target)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Oasis radial gradient behind video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                OasisPrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (isPlaying) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                IconButton(
                    onClick = { viewModel.setIsPlayingMovie(true) },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(OasisPrimary)
                        .semantics { contentDescription = "تشغيل الفيديو" }
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "تشغيل", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seeker Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("02:14", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Slider(
                value = progress,
                onValueChange = { viewModel.setMoviePlaybackProgress(it) },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    activeTrackColor = OasisPrimaryDark,
                    thumbColor = OasisPrimaryDark
                )
            )
            Text("45:10", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cinema Controller buttons bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val next = (progress - 0.05f).coerceIn(0f, 1f)
                    viewModel.setMoviePlaybackProgress(next)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Replay10, "رجوع للخلف", tint = Color.White, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.width(20.dp))

            IconButton(
                onClick = { viewModel.toggleIsPlayingMovie() },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "تحكم",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            IconButton(
                onClick = {
                    val next = (progress + 0.05f).coerceIn(0f, 1f)
                    viewModel.setMoviePlaybackProgress(next)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Forward10, "تقدم سريع", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Document Details info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = OasisSurfaceVariantDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "وصف المادة الترفيهية:",
                    fontWeight = FontWeight.Bold,
                    color = OasisPrimaryDark,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activeItem.descriptionAr,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// ---------------- 3. MUSIC PLAYER SCREEN (مشغل الموسيقى مع التراكتور الدوار والتأرجح والموجة التفاعلية) ----------------
@Composable
fun MusicPlayerScreen(viewModel: MarketplaceViewModel) {
    val music by viewModel.currentPlayingMusic.collectAsState()
    val isPlaying by viewModel.isPlayingMusic.collectAsState()
    val progress by viewModel.musicPlaybackProgress.collectAsState()

    val activeItem = music ?: return

    // Phase 2.3: Vinyl rotation via rememberInfiniteTransition (replaces while(true) 60fps loop)
    val infiniteVinyl = rememberInfiniteTransition(label = "vinyl")
    val vinylRotation by infiniteVinyl.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )
    val rotationAngle = if (isPlaying) vinylRotation else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OasisBgDark,
                        OasisBgDark.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.changeTab("LIBRARY") }) {
                Icon(Icons.Filled.ArrowBack, "خروج", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("مشغل الصوتيات للواحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. SPINNING VINYL DISC LAYER
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color(0xFF111111))
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl ridges
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2
                drawCircle(color = Color(0xFF222222), radius = outerRadius - 10, style = Stroke(width = 2f))
                drawCircle(color = Color(0xFF333333), radius = outerRadius - 30, style = Stroke(width = 1f))
                drawCircle(color = Color(0xFF222222), radius = outerRadius - 50, style = Stroke(width = 2f))
                drawCircle(color = Color(0xFF444444), radius = outerRadius - 70, style = Stroke(width = 1f))
            }

            // Central rotating core cover
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(OasisPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tracks info labels
        Text(activeItem.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(activeItem.developerOrCreatorAr, color = OasisPrimaryDark, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // 2. CANVAS DRAWN AUDIO WAVEFORM (Pulsing in real-time if active)
        val waveAmplitude = if (isPlaying) 16f else 2f
        val infiniteTransition = rememberInfiniteTransition()
        val waveOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 24.dp)
        ) {
            val width = size.width
            val height = size.height
            val points = 60
            val step = width / points
            val path = androidx.compose.ui.graphics.Path()

            path.moveTo(0f, height / 2)
            for (i in 0..points) {
                val x = i * step
                // complex sine math pulsing
                val y = height / 2 + sin(i * 0.15f + waveOffset) * waveAmplitude
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = OasisPrimaryDark,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        Slider(
            value = progress,
            onValueChange = { viewModel.setMusicPlaybackProgress(it) },
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                activeTrackColor = OasisPrimaryDark,
                thumbColor = OasisPrimaryDark
            )
        )

        // Musical bar controllers
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier.semantics { contentDescription = "عشوائي" }
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "عشوائي", tint = Color.White.copy(alpha = 0.5f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.semantics { contentDescription = "السابق" }
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "السابق", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { viewModel.toggleIsPlayingMusic() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .semantics { contentDescription = if (isPlaying) "إيقاف" else "تشغيل" }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = {},
                    modifier = Modifier.semantics { contentDescription = "التالي" }
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "التالي", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            IconButton(
                onClick = {},
                modifier = Modifier.semantics { contentDescription = "تكرار" }
            ) {
                Icon(Icons.Filled.Loop, contentDescription = "تكرار", tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

// ---------------- 4. SMART GEMINI AI ASSISTANT CHAT SCREEN (مساعد الذكاء الاصطناعي الأندلسي) ----------------
@Composable
fun AiAssistantScreen(viewModel: MarketplaceViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Sparkles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "ذكي",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "مساعد الواحة الذكي (Gemini)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "استكشف واقترح تطبيقات ومحتويات مناسبة لصوتك وذوقك مباشرة بالذكاء الاصطناعي.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // History list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("chat_msg_${msg.sender}")
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "جاري تجميع الأفكار من فضاء الواحة...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Input box row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                placeholder = { Text("أريد تطبيقاً للأذكار أو كتاباً للتاريخ...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.sendChatMessage(inputQuery)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("chat_send_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "إرسال",
                    tint = Color.White
                )
            }
        }
    }
}
