package com.richardyap.novelreader.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.richardyap.novelreader.FileRepository
import com.richardyap.novelreader.NovelRepository
import com.richardyap.novelreader.StorageRepository
import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.ReaderPrefs
import com.richardyap.novelreader.ui.screens.BookDetailScreen
import com.richardyap.novelreader.ui.screens.BookshelfScreen
import com.richardyap.novelreader.ui.screens.ReaderScreen
import com.richardyap.novelreader.ui.screens.SettingsScreen
import com.richardyap.novelreader.ui.screens.SourceManageScreen
import com.richardyap.novelreader.ui.theme.NovelTheme

/**
 * 应用导航状态枚举。
 */
enum class AppScreen {
    Bookshelf,
    Reader,
    Settings,
    BookDetail,
    SourceManage,
}

/**
 * 应用全局导航状态。
 *
 * 管理当前屏幕、当前打开的书籍、阅读偏好等。
 */
class AppState(
    val repository: NovelRepository,
    val storage: StorageRepository,
    val fileRepository: FileRepository,
) {
    /** 当前屏幕 */
    var currentScreen by mutableStateOf(AppScreen.Bookshelf)
        private set

    /** 当前正在阅读的书籍（用于 ReaderScreen 和 BookDetailScreen） */
    var currentBook by mutableStateOf<Book?>(null)
        private set

    /** 当前阅读偏好（从存储加载） */
    var readerPrefs by mutableStateOf(storage.loadReaderPrefs())
        private set

    /** 导航历史栈（用于返回操作） */
    private val backStack = mutableListOf<AppScreen>()

    /** 导航到书架 */
    fun navigateToBookshelf() {
        backStack.clear()
        currentScreen = AppScreen.Bookshelf
    }

    /** 打开书籍进入阅读器（传入书籍对象） */
    fun openReader(book: Book) {
        currentBook = book
        backStack.add(currentScreen)
        currentScreen = AppScreen.Reader
    }

    /** 打开阅读器并跳转到指定章节 */
    fun openReaderAt(book: Book, chapterIndex: Int) {
        val updatedBook = book.copy(currentChapter = chapterIndex)
        currentBook = updatedBook
        backStack.add(currentScreen)
        currentScreen = AppScreen.Reader
    }

    /** 打开书籍详情页 */
    fun openBookDetail(book: Book) {
        currentBook = book
        backStack.add(currentScreen)
        currentScreen = AppScreen.BookDetail
    }

    /** 打开设置页 */
    fun openSettings() {
        backStack.add(currentScreen)
        currentScreen = AppScreen.Settings
    }

    /** 打开书源管理页 */
    fun openSourceManage() {
        backStack.add(currentScreen)
        currentScreen = AppScreen.SourceManage
    }

    /** 返回上一屏幕 */
    fun goBack(): Boolean {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.lastIndex)
            return true
        }
        return false
    }

    /** 更新阅读偏好并持久化 */
    fun updateReaderPrefs(prefs: ReaderPrefs) {
        readerPrefs = prefs
        storage.saveReaderPrefs(prefs)
    }

    /** 刷新当前书籍数据（进度更新后调用） */
    fun refreshCurrentBook() {
        val id = currentBook?.id ?: return
        currentBook = repository.books.firstOrNull { it.id == id }
    }
}

/**
 * NovelApp — 应用主入口 Composable。
 *
 * 根据 [AppState.currentScreen] 切换不同的顶级屏幕，
 * 并通过 [NovelTheme] 统一注入阅读主题偏好。
 *
 * 所有数据通过 [NovelRepository]、[FileRepository]、[StorageRepository] 注入。
 */
@Composable
fun NovelAppMain(
    repository: NovelRepository,
    storage: StorageRepository,
    fileRepository: FileRepository,
) {
    val appState = remember {
        AppState(
            repository = repository,
            storage = storage,
            fileRepository = fileRepository,
        )
    }

    NovelTheme(prefs = appState.readerPrefs) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Crossfade(
                targetState = appState.currentScreen,
                animationSpec = tween(300),
                label = "screenCrossfade",
            ) { screen ->
                when (screen) {
                    AppScreen.Bookshelf -> {
                        BookshelfScreen(
                            repository = repository,
                            fileRepository = fileRepository,
                            onBookClick = { book -> appState.openReader(book) },
                            onBookDetail = { book -> appState.openBookDetail(book) },
                        )
                    }

                    AppScreen.Reader -> {
                        val book = appState.currentBook
                        if (book != null) {
                            ReaderScreen(
                                book = book,
                                repository = repository,
                                onBack = {
                                    appState.refreshCurrentBook()
                                    appState.goBack()
                                },
                                onPrefsChange = { prefs ->
                                    appState.updateReaderPrefs(prefs)
                                },
                            )
                        } else {
                            // Fallback — shouldn't normally happen
                            appState.navigateToBookshelf()
                        }
                    }

                    AppScreen.Settings -> {
                        SettingsScreen(
                            storage = storage,
                            onBack = { appState.goBack() },
                            onNavigateToSources = { appState.openSourceManage() },
                        )
                    }

                    AppScreen.BookDetail -> {
                        val book = appState.currentBook
                        if (book != null) {
                            BookDetailScreen(
                                book = book,
                                repository = repository,
                                onBack = { appState.goBack() },
                                onStartReading = { b, chapterIdx ->
                                    appState.openReaderAt(b, chapterIdx)
                                },
                            )
                        } else {
                            appState.goBack()
                        }
                    }

                    AppScreen.SourceManage -> {
                        SourceManageScreen(
                            storage = storage,
                            onBack = { appState.goBack() },
                        )
                    }
                }
            }
        }
    }
}
