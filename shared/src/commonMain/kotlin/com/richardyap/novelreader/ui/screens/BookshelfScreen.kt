package com.richardyap.novelreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.richardyap.novelreader.FileRepository
import com.richardyap.novelreader.NovelRepository
import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.ui.components.BookCard
import com.richardyap.novelreader.ui.components.EmptyState
import com.richardyap.novelreader.ui.components.SearchBar
import kotlinx.coroutines.launch

/**
 * 书籍书架主界面。
 *
 * 功能：
 * - 搜索栏
 * - 分类标签（全部 / 未读 / 已读 / 分类）
 * - 2 列网格展示书籍卡片
 * - 长按弹出菜单（置顶 / 删除 / 详情）
 * - FAB 导入按钮
 * - 导入对话框（TXT / EPUB 选择）
 * - 点击进入阅读器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    repository: NovelRepository,
    fileRepository: FileRepository,
    onBookClick: (Book) -> Unit,
    onBookDetail: (Book) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf(repository.books) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0=全部, 1=未读, 2=已读
    var showImportSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Book?>(null) }
    var contextMenuBook by remember { mutableStateOf<Book?>(null) }
    var importPath by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }

    // Refresh books when visible
    fun refreshBooks() {
        books = repository.books
    }

    val tabs = listOf("全部", "未读", "已读")

    val filteredBooks = remember(books, searchQuery, selectedTab) {
        var result = books
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
            }
        }
        when (selectedTab) {
            1 -> result = result.filter { it.lastReadAt == 0L }
            2 -> result = result.filter { it.lastReadAt > 0L }
        }
        result.sortedByDescending { it.lastReadAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("我的书架", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    importPath = ""
                    importMessage = null
                    showImportSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "导入书籍")
            }
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
                placeholder = "搜索书名或作者…",
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

            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    TextButton(
                        onClick = { selectedTab = index },
                        modifier = Modifier,
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "共 ${filteredBooks.size} 本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            // Book grid
            if (filteredBooks.isEmpty()) {
                EmptyState(
                    icon = "📖",
                    title = if (searchQuery.isNotBlank()) "未找到匹配的书籍" else "书架空空",
                    subtitle = if (searchQuery.isNotBlank()) "换个关键词试试"
                    else "点击右下角 + 导入你的第一本书",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        Box {
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book) },
                                onLongClick = { contextMenuBook = book },
                            )

                            // Context menu
                            DropdownMenu(
                                expanded = contextMenuBook == book,
                                onDismissRequest = { contextMenuBook = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("置顶") },
                                    onClick = {
                                        val updated = books.toMutableList()
                                        updated.removeAll { it.id == book.id }
                                        updated.add(0, book)
                                        repository.saveBooks(updated)
                                        refreshBooks()
                                        contextMenuBook = null
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("书籍详情") },
                                    onClick = {
                                        contextMenuBook = null
                                        onBookDetail(book)
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "删除",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        contextMenuBook = null
                                        showDeleteDialog = book
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Import bottom sheet ─────────────────────────────────────
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
                Text(
                    text = "导入书籍",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "输入 TXT 或 EPUB 文件的路径（桌面端为绝对路径，Android 端为文件 URI）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                SearchBar(
                    query = importPath,
                    onQueryChange = { importPath = it },
                    placeholder = "文件路径，例如 /path/to/book.txt",
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
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (importPath.isBlank()) {
                                importMessage = "请输入文件路径"
                                return@TextButton
                            }
                            scope.launch {
                                try {
                                    val book = repository.importBook(importPath)
                                    importMessage = "导入成功：《${book.title}》"
                                    refreshBooks()
                                } catch (e: Exception) {
                                    importMessage = "导入失败：${e.message}"
                                }
                            }
                        },
                    ) {
                        Text("导入")
                    }
                }
            }
        }
    }

    // ─── Delete confirmation dialog ─────────────────────────────
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除《${showDeleteDialog!!.title}》吗？\n此操作不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteBook(showDeleteDialog!!)
                        refreshBooks()
                        showDeleteDialog = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            },
        )
    }
}
