package com.richardyap.novelreader

import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.Bookmark
import com.richardyap.novelreader.models.ReaderPrefs
import com.richardyap.novelreader.models.SyncPrefs

/**
 * 平台抽象 —— 存储层。
 * 负责书籍、书签、偏好设置的持久化。
 */
interface StorageRepository {
    fun loadBooks(): List<Book>
    fun saveBooks(books: List<Book>)
    fun loadBookmarks(): List<Bookmark>
    fun saveBookmarks(bookmarks: List<Bookmark>)
    fun loadReaderPrefs(): ReaderPrefs
    fun saveReaderPrefs(prefs: ReaderPrefs)
    fun loadSyncPrefs(): SyncPrefs
    fun saveSyncPrefs(prefs: SyncPrefs)

    /** 删除书籍及其关联的章节文件 */
    fun deleteBook(book: Book)
}

/**
 * 平台抽象 —— 文件访问层。
 * 负责本地文件读写、书籍导入。
 */
interface FileRepository {
    /**
     * 导入本地书籍文件。
     * @param path 文件路径（Android 为 content:// URI 或绝对路径，Desktop 为绝对路径）
     * @return 解析好的书籍元数据（不含正文内容）
     */
    fun importBook(path: String): Book

    /**
     * 读取指定章节的正文。
     * @param bookId 书籍 ID
     * @param chapterIndex 章节序号（从 0 开始）
     * @return 章节对象，如果文件不存在返回 null
     */
    fun loadChapter(bookId: String, chapterIndex: Int): String?

    /**
     * 获取书籍文件的绝对路径目录。
     */
    fun getBookDir(bookId: String): String
}

/**
 * 应用信息接口。
 */
interface AppInfoProvider {
    val appVersionName: String
    val appVersionCode: Int
    val appDataDir: String
}
