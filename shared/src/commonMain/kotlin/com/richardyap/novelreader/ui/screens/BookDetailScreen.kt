package com.richardyap.novelreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardyap.novelreader.NovelRepository
import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.Bookmark
import com.richardyap.novelreader.ui.components.BookmarkItem
import com.richardyap.novelreader.ui.components.ChapterListItem
import com.richardyap.novelreader.ui.components.EmptyState

/**
 * 书籍详情页。
 *
 * 显示：
 * - 封面 / 标题 / 作者 / 简介
 * - 阅读进度
 * - 开始阅读 / 继续阅读按钮
 * - 目录列表
 * - 书签列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book,
    repository: NovelRepository,
    onBack: () -> Unit,
    onStartReading: (Book, Int) -> Unit,
) {
    val bookmarks = remember { repository.bookmarks.filter { it.bookId == book.id } }
    var showFullIntro by remember { mutableStateOf(false) }

    val progress = if (book.chapterCount > 0) {
        (book.currentChapter.toFloat() / book.chapterCount).coerceIn(0f, 1f)
    } else 0f

    val hasStarted = book.lastReadAt > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Header: cover + title + author ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Cover placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Color(
                                    ((book.title.hashCode() and 0xFF0000) shr 16) / 255f * 0.5f + 0.25f,
                                    ((book.title.hashCode() and 0x00FF00) shr 8) / 255f * 0.5f + 0.25f,
                                    (book.title.hashCode() and 0x0000FF) / 255f * 0.5f + 0.25f,
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = book.title.firstOrNull()?.toString() ?: "书",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = book.author.ifBlank { "未知作者" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))

                        // Source info
                        if (book.source != "local") {
                            Text(
                                text = "来源：${book.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // ── Progress + action buttons ───────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (hasStarted) {
                        Text(
                            text = "阅读进度",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "读到 ${book.currentChapterTitle.ifBlank { "第${book.currentChapter + 1}章" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                                    .padding(start = 8.dp),
                            )
                        }
                    } else {
                        Text(
                            text = "尚未开始阅读",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                val startChapter = if (hasStarted) book.currentChapter else 0
                                onStartReading(book, startChapter)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (hasStarted) "继续阅读" else "开始阅读")
                        }
                        if (hasStarted) {
                            Button(
                                onClick = { onStartReading(book, 0) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                ),
                            ) {
                                Text("从头开始")
                            }
                        }
                    }
                }
            }

            // ── Intro ───────────────────────────────────────────
            if (book.intro.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "简介",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = book.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (showFullIntro) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { showFullIntro = !showFullIntro },
                )
                if (book.intro.length > 300) {
                    Text(
                        text = if (showFullIntro) "收起" else "展开全部",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clickable { showFullIntro = !showFullIntro },
                    )
                }
            }

            // ── Directory (short preview) ───────────────────────
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Text(
                text = "目录（共 ${book.chapterCount} 章）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (book.chapterCount > 0) {
                // Show first 20 chapters max for preview
                val previewCount = minOf(book.chapterCount, 20)
                for (idx in 0 until previewCount) {
                    val isCurrent = idx == book.currentChapter
                    ChapterListItem(
                        index = idx,
                        title = if (idx == book.currentChapter && book.currentChapterTitle.isNotBlank())
                            book.currentChapterTitle
                        else "第${idx + 1}章",
                        isCurrent = isCurrent,
                        onClick = { onStartReading(book, idx) },
                    )
                }
                if (book.chapterCount > previewCount) {
                    Text(
                        text = "… 还有 ${book.chapterCount - previewCount} 章，开始阅读查看全部",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                EmptyState(
                    icon = "📄",
                    title = "暂无章节",
                    subtitle = "此书籍尚未解析章节",
                )
            }

            // ── Bookmarks ───────────────────────────────────────
            if (bookmarks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Text(
                    text = "书签（${bookmarks.size}）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                bookmarks.take(10).forEach { bm ->
                    BookmarkItem(
                        bookmark = bm,
                        onClick = { onStartReading(book, bm.chapterIndex) },
                    )
                }
                if (bookmarks.size > 10) {
                    Text(
                        text = "… 还有 ${bookmarks.size - 10} 个书签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
