package com.richardyap.novelreader

import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.Bookmark
import com.richardyap.novelreader.models.Chapter
import com.richardyap.novelreader.models.SearchResult
import com.richardyap.novelreader.util.ChapterParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 小说仓库 —— 协调存储层和文件层的业务逻辑。
 */
class NovelRepository(
    private val storage: StorageRepository,
    private val fileRepo: FileRepository,
) {
    val books: List<Book> get() = storage.loadBooks()
    val bookmarks: List<Bookmark> get() = storage.loadBookmarks()

    fun saveBooks(list: List<Book>) = storage.saveBooks(list)
    fun saveBookmarks(list: List<Bookmark>) = storage.saveBookmarks(list)
    fun deleteBook(book: Book) = storage.deleteBook(book)

    /**
     * 导入本地书籍。
     */
    suspend fun importBook(path: String): Book = withContext(Dispatchers.IO) {
        fileRepo.importBook(path)
    }

    /**
     * 读取指定章节内容。
     */
    suspend fun readChapter(bookId: String, chapterIndex: Int): Chapter? = withContext(Dispatchers.IO) {
        val text = fileRepo.loadChapter(bookId, chapterIndex) ?: return@withContext null
        // 解析章节标题和正文
        val lines = text.lines()
        val title = lines.firstOrNull()?.trim()?.take(80) ?: "第${chapterIndex + 1}章"
        val body = if (lines.size > 1) lines.drop(1).joinToString("\n") else text
        Chapter(title = title, body = body, index = chapterIndex)
    }

    /**
     * 添加书签。
     */
    fun addBookmark(book: Book, chapterIndex: Int, paragraphIndex: Int, chapterTitle: String, preview: String): Bookmark {
        val bm = Bookmark(
            id = generateUUID(),
            bookId = book.id,
            bookTitle = book.title,
            chapterIndex = chapterIndex,
            paragraphIndex = paragraphIndex,
            chapterTitle = chapterTitle,
            preview = preview.take(120),
            createdAt = currentTimeMillis(),
        )
        storage.saveBookmarks(listOf(bm) + storage.loadBookmarks())
        return bm
    }

    /**
     * 更新阅读进度。
     */
    fun updateProgress(book: Book, chapterIndex: Int, paragraphIndex: Int, chapterTitle: String) {
        val updated = book.copy(
            currentChapter = chapterIndex,
            paragraphIndex = paragraphIndex,
            currentChapterTitle = chapterTitle,
            lastReadAt = currentTimeMillis(),
        )
        val books = storage.loadBooks().map { if (it.id == book.id) updated else it }
            .sortedByDescending { it.lastReadAt }
        storage.saveBooks(books)
    }

    /**
     * 全书搜索。
     */
    suspend fun searchBook(bookId: String, keyword: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val book = storage.loadBooks().firstOrNull { it.id == bookId } ?: return@withContext emptyList()
            val chapters = (0 until book.chapterCount).mapNotNull { idx ->
                val text = fileRepo.loadChapter(bookId, idx) ?: return@mapNotNull null
                val lines = text.lines()
                val title = lines.firstOrNull()?.trim()?.take(80) ?: "第${idx + 1}章"
                val body = if (lines.size > 1) lines.drop(1).joinToString("\n") else text
                Chapter(title = title, body = body, index = idx)
            }
            ChapterParser.searchAll(chapters, keyword)
        }
}

/** 跨平台时间戳获取 */
internal expect fun currentTimeMillis(): Long

/** 跨平台 UUID 生成 */
internal expect fun generateUUID(): String
