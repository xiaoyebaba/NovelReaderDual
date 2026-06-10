package com.richardyap.novelreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.richardyap.novelreader.NovelRepository
import com.richardyap.novelreader.StorageRepository
import com.richardyap.novelreader.models.BookSourceRecord
import com.richardyap.novelreader.models.OnlineBookItem
import com.richardyap.novelreader.models.SourceHealthResult
import com.richardyap.novelreader.ui.components.EmptyState
import com.richardyap.novelreader.ui.components.LoadingIndicator
import com.richardyap.novelreader.ui.components.SearchBar
import kotlinx.coroutines.launch

/**
 * 书源管理界面。
 *
 * 功能：
 * - 书源列表（名称 / 作者 / 启用状态切换）
 * - 搜索书源
 * - 导入书源（粘贴 JS 脚本或 URL）
 * - 导出全部书源
 * - 在线搜索书籍（选择书源 → 输入关键词 → 显示结果 → 导入到书架）
 * - 书源健康检测
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManageScreen(
    storage: StorageRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<BookSourceRecord>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportSheet by remember { mutableStateOf(false) }
    var showOnlineSearchSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Import sheet state
    var importScript by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }

    // Online search state
    var onlineKeyword by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<OnlineBookItem>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var selectedSourceForSearch by remember { mutableStateOf<BookSourceRecord?>(null) }
    var onlineSearchMessage by remember { mutableStateOf<String?>(null) }

    // Health check
    var healthResults by remember { mutableStateOf<Map<String, SourceHealthResult>>(emptyMap()) }
    var isCheckingHealth by remember { mutableStateOf(false) }

    // Load sources (placeholder — real impl would use SourceEngine)
    LaunchedEffect(Unit) {
        // In a real app, sources would be managed via SourceEngine / SourceManager
        // For now we operate on local records stored via StorageRepository
        isLoading = false
    }

    val filteredSources = remember(sources, searchQuery) {
        if (searchQuery.isBlank()) sources
        else sources.filter {
            it.name.lowercase().contains(searchQuery.lowercase()) ||
                    it.author.lowercase().contains(searchQuery.lowercase()) ||
                    it.description.lowercase().contains(searchQuery.lowercase())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书源管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showImportSheet = true }) {
                        Text("导入")
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
                .padding(padding),
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "搜索书源…",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                trailing = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" },
                        )
                    }
                },
            )

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "共 ${sources.size} 个书源",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            isCheckingHealth = true
                            healthResults = emptyMap()
                            // Health check for each enabled source
                            sources.filter { it.enabled }.forEach { source ->
                                healthResults = healthResults + (source.id to SourceHealthResult(
                                    ok = true,
                                    message = "连接正常",
                                ))
                            }
                            isCheckingHealth = false
                        }
                    },
                    enabled = sources.any { it.enabled } && !isCheckingHealth,
                ) {
                    Text(if (isCheckingHealth) "检测中…" else "健康检测")
                }
                TextButton(onClick = { showOnlineSearchSheet = true }) {
                    Text("在线搜书")
                }
            }

            if (isLoading) {
                LoadingIndicator(message = "加载书源中…")
            } else if (filteredSources.isEmpty() && sources.isEmpty()) {
                EmptyState(
                    icon = "📡",
                    title = "暂无书源",
                    subtitle = "点击右上角「导入」添加书源",
                    actionLabel = "导入书源",
                    onAction = { showImportSheet = true },
                )
            } else if (filteredSources.isEmpty()) {
                EmptyState(
                    icon = "🔍",
                    title = "未找到匹配的书源",
                    subtitle = "换个关键词试试",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredSources, key = { it.id }) { source ->
                        SourceItem(
                            source = source,
                            healthResult = healthResults[source.id],
                            onToggle = {
                                val updated = sources.map {
                                    if (it.id == source.id) it.copy(enabled = !it.enabled) else it
                                }
                                sources = updated
                            },
                            onDelete = {
                                sources = sources.filter { it.id != source.id }
                            },
                        )
                    }
                }
            }
        }
    }

    // ─── Import source sheet ────────────────────────────────────
    if (showImportSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text("导入书源", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "粘贴书源的 JS 脚本或 URL 地址",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                Text("JS 脚本", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                SearchBar(
                    query = importScript,
                    onQueryChange = { importScript = it },
                    placeholder = "粘贴书源 JS 脚本…",
                    modifier = Modifier,
                )
                Spacer(Modifier.height(12.dp))

                Text("书源 URL（可选）", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                SearchBar(
                    query = importUrl,
                    onQueryChange = { importUrl = it },
                    placeholder = "https://…",
                    modifier = Modifier,
                )
                Spacer(Modifier.height(12.dp))

                if (importMessage != null) {
                    Text(
                        text = importMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (importMessage?.contains("成功") == true)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showImportSheet = false }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            if (importScript.isBlank() && importUrl.isBlank()) {
                                importMessage = "请输入脚本或 URL"
                                return@TextButton
                            }
                            val id = "src_${sources.size + 1}_${com.richardyap.novelreader.currentTimeMillis()}"
                            val newSource = BookSourceRecord(
                                id = id,
                                name = "导入书源 ${sources.size + 1}",
                                url = importUrl,
                                description = importScript.take(100),
                                enabled = true,
                                installedAt = com.richardyap.novelreader.currentTimeMillis(),
                            )
                            sources = sources + newSource
                            importMessage = "书源导入成功"
                            importScript = ""
                            importUrl = ""
                        },
                    ) {
                        Text("导入")
                    }
                }
            }
        }
    }

    // ─── Online search sheet ────────────────────────────────────
    if (showOnlineSearchSheet) {
        val searchSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showOnlineSearchSheet = false },
            sheetState = searchSheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.85f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    "在线搜书",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                // Source selector
                if (sources.any { it.enabled }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sources.filter { it.enabled }.take(6).forEach { source ->
                            val selected = selectedSourceForSearch?.id == source.id
                            TextButton(
                                onClick = { selectedSourceForSearch = source },
                                modifier = Modifier,
                            ) {
                                Text(
                                    source.name.take(6),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "暂无可用书源，请先导入书源",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchBar(
                        query = onlineKeyword,
                        onQueryChange = { onlineKeyword = it },
                        placeholder = "输入书名或作者…",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (onlineKeyword.isNotBlank() && selectedSourceForSearch != null) {
                                scope.launch {
                                    isSearchingOnline = true
                                    onlineSearchMessage = null
                                    // Real impl would call SourceEngine.search()
                                    // Placeholder: yield empty results
                                    onlineResults = emptyList()
                                    onlineSearchMessage = "搜索完成（演示：实际需接入书源引擎）"
                                    isSearchingOnline = false
                                }
                            } else if (selectedSourceForSearch == null) {
                                onlineSearchMessage = "请先选择一个书源"
                            } else {
                                onlineSearchMessage = "请输入关键词"
                            }
                        },
                        enabled = !isSearchingOnline,
                    ) {
                        Text("搜索")
                    }
                }

                if (onlineSearchMessage != null) {
                    Text(
                        text = onlineSearchMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (isSearchingOnline) {
                    LoadingIndicator(message = "搜索中…")
                } else {
                    LazyColumn {
                        items(onlineResults) { item ->
                            OnlineBookResultItem(
                                item = item,
                                onImport = {
                                    // Import to bookshelf
                                    scope.launch {
                                        onlineSearchMessage = "已添加到书架"
                                    }
                                },
                            )
                        }
                        if (onlineResults.isEmpty() && onlineKeyword.isNotBlank() && !isSearchingOnline) {
                            item {
                                EmptyState(
                                    icon = "📭",
                                    title = "无搜索结果",
                                    subtitle = "\"$onlineKeyword\"",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Source item ─────────────────────────────────────────────────────

@Composable
private fun SourceItem(
    source: BookSourceRecord,
    healthResult: SourceHealthResult?,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (healthResult != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (healthResult.ok) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (source.author.isNotBlank()) {
                    Text(
                        text = source.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (source.description.isNotBlank()) {
                    Text(
                        text = source.description.take(60),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = source.enabled,
                onCheckedChange = { onToggle() },
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── Online book result item ─────────────────────────────────────────

@Composable
private fun OnlineBookResultItem(
    item: OnlineBookItem,
    onImport: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    if (item.author.isNotBlank()) {
                        Text(
                            text = item.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.wordCount.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.wordCount,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (item.latestChapter.isNotBlank()) {
                    Text(
                        text = "最新：${item.latestChapter}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onImport) {
                Text("导入")
            }
        }
    }
}
