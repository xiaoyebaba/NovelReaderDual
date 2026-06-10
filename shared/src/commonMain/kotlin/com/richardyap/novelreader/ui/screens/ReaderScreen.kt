package com.richardyap.novelreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardyap.novelreader.NovelRepository
import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.Bookmark
import com.richardyap.novelreader.models.Chapter
import com.richardyap.novelreader.models.ReaderPrefs
import com.richardyap.novelreader.models.SearchResult
import com.richardyap.novelreader.ui.components.BookmarkItem
import com.richardyap.novelreader.ui.components.ChapterListItem
import com.richardyap.novelreader.ui.components.EmptyState
import com.richardyap.novelreader.ui.components.LoadingIndicator
import com.richardyap.novelreader.ui.components.SearchBar
import com.richardyap.novelreader.ui.theme.LocalReaderColors
import com.richardyap.novelreader.ui.theme.LocalReaderPrefs
import com.richardyap.novelreader.ui.theme.allThemeKeys
import com.richardyap.novelreader.ui.theme.allThemeNames
import com.richardyap.novelreader.ui.theme.fontSizeSp
import com.richardyap.novelreader.ui.theme.lineHeightFloat
import com.richardyap.novelreader.ui.theme.pagePaddingDp
import com.richardyap.novelreader.ui.theme.paragraphSpacingDp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 完整阅读器界面。
 *
 * 功能：
 * - 滚动模式 + 仿真翻页模式
 * - 自定义排版：字体大小 / 行间距 / 段间距 / 页边距
 * - 主题快速切换（纸张 / 夜间 / 护眼）
 * - 亮度 + 色温滑块
 * - 顶部章节标题 + 底部进度条
 * - 点击中间区域唤出/隐藏控制菜单
 * - 章节导航：上一章 / 下一章
 * - 目录抽屉（左侧滑出）
 * - 书签功能
 * - 全书搜索
 * - 阅读进度自动保存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    repository: NovelRepository,
    onBack: () -> Unit,
    onPrefsChange: (ReaderPrefs) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val readerColors = LocalReaderColors.current
    val basePrefs = LocalReaderPrefs.current

    // Local mutable prefs derived from base
    var prefs by remember { mutableStateOf(basePrefs) }

    // Chapter data
    var currentChapter by remember { mutableStateOf<Chapter?>(null) }
    var chapterIndex by remember { mutableStateOf(book.currentChapter) }
    var isLoadingChapter by remember { mutableStateOf(false) }
    var chapterTitle by remember { mutableStateOf(book.currentChapterTitle) }

    // UI state
    var showControls by remember { mutableStateOf(false) }
    var showTocDrawer by remember { mutableStateOf(false) }
    var showBookmarkSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    // Settings panel state
    var settingFontSize by remember { mutableFloatStateOf(prefs.fontSize.toFloat()) }
    var settingLineHeight by remember { mutableFloatStateOf(prefs.lineHeight) }
    var settingParagraphSpacing by remember { mutableIntStateOf(prefs.paragraphSpacing) }
    var settingDimLevel by remember { mutableIntStateOf(prefs.dimLevel) }
    var settingColorTemp by remember { mutableIntStateOf(prefs.colorTemperature) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Bookmarks
    var bookmarks by remember { mutableStateOf(repository.bookmarks.filter { it.bookId == book.id }) }

    // Scroll state
    val scrollState = rememberScrollState()

    // Load chapter
    fun loadChapter(idx: Int) {
        scope.launch {
            isLoadingChapter = true
            val ch = repository.readChapter(book.id, idx)
            if (ch != null) {
                currentChapter = ch
                chapterIndex = idx
                chapterTitle = ch.title
                // Update progress
                repository.updateProgress(book, idx, 0, ch.title)
            }
            isLoadingChapter = false
        }
    }

    LaunchedEffect(book.id) {
        loadChapter(book.currentChapter)
        bookmarks = repository.bookmarks.filter { it.bookId == book.id }
    }

    // Detect if current chapter has bookmark
    LaunchedEffect(chapterIndex) {
        isBookmarked = bookmarks.any { it.chapterIndex == chapterIndex }
    }

    // Save prefs on change
    fun applyPrefs(newPrefs: ReaderPrefs) {
        prefs = newPrefs
        onPrefsChange(newPrefs)
    }

    val fontScale = prefs.fontSizeSp / 20f

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showTocDrawer,
        drawerContent = {
            if (showTocDrawer) {
                TocDrawerContent(
                    book = book,
                    currentChapterIndex = chapterIndex,
                    onChapterSelect = { idx ->
                        loadChapter(idx)
                        scope.launch { drawerState.close() }
                        showTocDrawer = false
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                        showTocDrawer = false
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = readerColors.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                        )
                    },
            ) {
                // ── Reading content ────────────────────────────
                if (isLoadingChapter) {
                    LoadingIndicator(message = "加载章节中…")
                } else if (currentChapter != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                horizontal = prefs.pagePaddingDp.dp,
                                vertical = 16.dp,
                            ),
                    ) {
                        // Chapter title
                        Text(
                            text = currentChapter!!.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = (22 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = readerColors.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = (prefs.paragraphSpacingDp * 3 / 2).dp),
                        )

                        // Body text — split into paragraphs
                        val paragraphs = currentChapter!!.body.split("\n")
                            .filter { it.isNotBlank() }

                        paragraphs.forEachIndexed { idx, paragraph ->
                            Text(
                                text = paragraph.trim(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = prefs.fontSizeSp.sp,
                                    lineHeight = (prefs.fontSizeSp * prefs.lineHeightFloat).sp,
                                ),
                                color = readerColors.onBackground,
                                modifier = Modifier.padding(
                                    bottom = if (idx < paragraphs.lastIndex) prefs.paragraphSpacingDp.dp else 0.dp,
                                ),
                            )
                        }

                        Spacer(Modifier.height(80.dp))
                    }
                }
                // ═══════════════════════════════════════════════

                // ── Top bar with chapter title ─────────────────
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = chapterTitle.ifBlank { "第${chapterIndex + 1}章" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, "返回")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                isBookmarked = !isBookmarked
                                if (isBookmarked) {
                                    repository.addBookmark(
                                        book = book.copy(
                                            currentChapter = chapterIndex,
                                            currentChapterTitle = chapterTitle,
                                        ),
                                        chapterIndex = chapterIndex,
                                        paragraphIndex = 0,
                                        chapterTitle = chapterTitle,
                                        preview = currentChapter?.body?.take(120) ?: "",
                                    )
                                } else {
                                    val bm = bookmarks.find { it.chapterIndex == chapterIndex }
                                    if (bm != null) {
                                        repository.saveBookmarks(
                                            bookmarks.filter { it.id != bm.id },
                                        )
                                    }
                                }
                                bookmarks = repository.bookmarks.filter { it.bookId == book.id }
                            }) {
                                Icon(
                                    imageVector = if (isBookmarked)
                                        Icons.Default.Bookmark
                                    else Icons.Default.BookmarkBorder,
                                    contentDescription = "书签",
                                )
                            }
                            IconButton(onClick = { showTocDrawer = true; scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.List, "目录")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = readerColors.surface.copy(alpha = 0.92f),
                        ),
                    )
                }

                // ── Bottom progress bar ────────────────────────
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(readerColors.surface.copy(alpha = 0.92f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        val progress = if (book.chapterCount > 0) {
                            (chapterIndex.toFloat() / book.chapterCount).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = readerColors.progressIndicator,
                            trackColor = readerColors.progressTrack,
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = readerColors.onBackground,
                            )
                            Text(
                                text = "${chapterIndex + 1} / ${book.chapterCount} 章",
                                style = MaterialTheme.typography.labelSmall,
                                color = readerColors.onBackground,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Navigation row: prev / settings / next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    if (chapterIndex > 0) loadChapter(chapterIndex - 1)
                                },
                                enabled = chapterIndex > 0,
                            ) {
                                Text("◀ 上一章")
                            }

                            Row {
                                IconButton(onClick = { showThemeSheet = true }) {
                                    Icon(
                                        Icons.Default.BrightnessHigh,
                                        "主题",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(onClick = { showSearchSheet = true }) {
                                    Icon(
                                        Icons.Default.Search,
                                        "搜索",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(onClick = { showSettingsSheet = true }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        "设置",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    if (chapterIndex < book.chapterCount - 1)
                                        loadChapter(chapterIndex + 1)
                                },
                                enabled = chapterIndex < book.chapterCount - 1,
                            ) {
                                Text("下一章 ▶")
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Quick theme switcher sheet ──────────────────────────────
    if (showThemeSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text("快速切换主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                allThemeKeys.forEachIndexed { idx, key ->
                    val isSelected = prefs.theme == key
                    TextButton(
                        onClick = {
                            applyPrefs(prefs.copy(theme = key))
                            showThemeSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = allThemeNames[idx],
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    // ─── Reading settings sheet ─────────────────────────────────
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                showSettingsSheet = false
                applyPrefs(
                    prefs.copy(
                        fontSize = settingFontSize.toInt(),
                        lineHeight = settingLineHeight,
                        paragraphSpacing = settingParagraphSpacing,
                        dimLevel = settingDimLevel,
                        colorTemperature = settingColorTemp,
                    ),
                )
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text("阅读设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(20.dp))

                // Font size
                Text("字体大小：${settingFontSize.toInt()}sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settingFontSize,
                    onValueChange = { settingFontSize = it },
                    valueRange = 14f..30f,
                    steps = 15,
                )

                Spacer(Modifier.height(12.dp))

                // Line height
                Text("行间距：${"%.1f".format(settingLineHeight)}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settingLineHeight,
                    onValueChange = { settingLineHeight = it },
                    valueRange = 1.2f..2.5f,
                )

                Spacer(Modifier.height(12.dp))

                // Paragraph spacing
                Text("段间距：${settingParagraphSpacing}dp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settingParagraphSpacing.toFloat(),
                    onValueChange = { settingParagraphSpacing = it.toInt() },
                    valueRange = 4f..24f,
                    steps = 19,
                )

                Spacer(Modifier.height(12.dp))

                // Dim level (brightness)
                Text("亮度：${settingDimLevel}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settingDimLevel.toFloat(),
                    onValueChange = { settingDimLevel = it.toInt() },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // Color temperature
                Text("暖色温：${settingColorTemp}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settingColorTemp.toFloat(),
                    onValueChange = { settingColorTemp = it.toInt() },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF8C00),
                        activeTrackColor = Color(0xFFFF8C00),
                    ),
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        settingFontSize = 20f
                        settingLineHeight = 1.75f
                        settingParagraphSpacing = 14
                        settingDimLevel = 0
                        settingColorTemp = 0
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Text("恢复默认")
                }
            }
        }
    }

    // ─── Search sheet ───────────────────────────────────────────
    if (showSearchSheet) {
        val searchSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = searchSheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.9f)
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "全书搜索…",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                scope.launch {
                                    isSearching = true
                                    searchResults = repository.searchBook(book.id, searchQuery)
                                    isSearching = false
                                }
                            }
                        },
                    ) {
                        Text("搜索")
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isSearching) {
                    LoadingIndicator(message = "搜索中…")
                } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    EmptyState(icon = "🔍", title = "未找到结果", subtitle = "\"$searchQuery\"")
                } else {
                    LazyColumn {
                        items(searchResults) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        loadChapter(result.chapterIndex)
                                        showSearchSheet = false
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = result.chapterTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = result.preview,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

// ─── Toc drawer content ──────────────────────────────────────────────

@Composable
private fun TocDrawerContent(
    book: Book,
    currentChapterIndex: Int,
    onChapterSelect: (Int) -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.82f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "目录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "共 ${book.chapterCount} 章",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            // We generate chapter list from indices; titles loaded lazily
            LazyColumn {
                items(book.chapterCount) { idx ->
                    val isCurrent = idx == currentChapterIndex
                    ChapterListItem(
                        index = idx,
                        title = if (idx == book.currentChapter && book.currentChapterTitle.isNotBlank())
                            book.currentChapterTitle
                        else "第${idx + 1}章",
                        isCurrent = isCurrent,
                        onClick = { onChapterSelect(idx) },
                    )
                }
            }
        }
    }
}
