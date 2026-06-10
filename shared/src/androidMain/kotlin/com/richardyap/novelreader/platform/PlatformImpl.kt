package com.richardyap.novelreader.platform

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.richardyap.novelreader.AppInfoProvider
import com.richardyap.novelreader.FileRepository
import com.richardyap.novelreader.StorageRepository
import com.richardyap.novelreader.currentTimeMillis
import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.Bookmark
import com.richardyap.novelreader.models.ReaderPrefs
import com.richardyap.novelreader.models.SyncPrefs
import com.richardyap.novelreader.util.ChapterParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

/**
 * Android 存储实现 —— 基于 SharedPreferences + JSON。
 */
class AndroidStorageRepository(
    private val context: Context,
) : StorageRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("novelreader_prefs", Context.MODE_PRIVATE)

    override fun loadBooks(): List<Book> {
        val json = prefs.getString("books_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> parseBook(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun saveBooks(books: List<Book>) {
        val arr = JSONArray()
        books.forEach { arr.put(bookToJson(it)) }
        prefs.edit().putString("books_json", arr.toString()).apply()
    }

    override fun loadBookmarks(): List<Bookmark> {
        val json = prefs.getString("bookmarks_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> parseBookmark(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun saveBookmarks(bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        bookmarks.forEach { arr.put(bookmarkToJson(it)) }
        prefs.edit().putString("bookmarks_json", arr.toString()).apply()
    }

    override fun loadReaderPrefs(): ReaderPrefs {
        val json = prefs.getString("reader_prefs_json", null) ?: return ReaderPrefs()
        return try {
            parseReaderPrefs(JSONObject(json))
        } catch (e: Exception) {
            ReaderPrefs()
        }
    }

    override fun saveReaderPrefs(prefs: ReaderPrefs) {
        val obj = readerPrefsToJson(prefs)
        this.prefs.edit().putString("reader_prefs_json", obj.toString()).apply()
    }

    override fun loadSyncPrefs(): SyncPrefs {
        val json = prefs.getString("sync_prefs_json", null) ?: return SyncPrefs()
        return try {
            parseSyncPrefs(JSONObject(json))
        } catch (e: Exception) {
            SyncPrefs()
        }
    }

    override fun saveSyncPrefs(prefs: SyncPrefs) {
        val obj = syncPrefsToJson(prefs)
        this.prefs.edit().putString("sync_prefs_json", obj.toString()).apply()
    }

    override fun deleteBook(book: Book) {
        // 删除章节文件
        val chaptersDir = File(context.filesDir, "chapters/${book.id}")
        if (chaptersDir.exists()) {
            chaptersDir.deleteRecursively()
        }
        // 从列表中移除
        val books = loadBooks().filter { it.id != book.id }
        saveBooks(books)
        // 删除该书签
        val bookmarks = loadBookmarks().filter { it.bookId != book.id }
        saveBookmarks(bookmarks)
    }

    // --------------- 序列化辅助 ---------------

    private fun bookToJson(book: Book): JSONObject = JSONObject().apply {
        put("id", book.id)
        put("title", book.title)
        put("author", book.author)
        put("coverUrl", book.coverUrl)
        put("intro", book.intro)
        put("filePath", book.filePath)
        put("chapterCount", book.chapterCount)
        put("currentChapter", book.currentChapter)
        put("paragraphIndex", book.paragraphIndex)
        put("currentChapterTitle", book.currentChapterTitle)
        put("createdAt", book.createdAt)
        put("lastReadAt", book.lastReadAt)
        put("source", book.source)
        put("sourceUrl", book.sourceUrl)
        put("totalCharCount", book.totalCharCount)
    }

    private fun parseBook(obj: JSONObject): Book = Book(
        id = obj.optString("id", ""),
        title = obj.optString("title", ""),
        author = obj.optString("author", ""),
        coverUrl = obj.optString("coverUrl", ""),
        intro = obj.optString("intro", ""),
        filePath = obj.optString("filePath", ""),
        chapterCount = obj.optInt("chapterCount", 0),
        currentChapter = obj.optInt("currentChapter", 0),
        paragraphIndex = obj.optInt("paragraphIndex", 0),
        currentChapterTitle = obj.optString("currentChapterTitle", ""),
        createdAt = obj.optLong("createdAt", 0L),
        lastReadAt = obj.optLong("lastReadAt", 0L),
        source = obj.optString("source", "local"),
        sourceUrl = obj.optString("sourceUrl", ""),
        totalCharCount = obj.optLong("totalCharCount", 0L),
    )

    private fun bookmarkToJson(bm: Bookmark): JSONObject = JSONObject().apply {
        put("id", bm.id)
        put("bookId", bm.bookId)
        put("bookTitle", bm.bookTitle)
        put("chapterIndex", bm.chapterIndex)
        put("paragraphIndex", bm.paragraphIndex)
        put("chapterTitle", bm.chapterTitle)
        put("preview", bm.preview)
        put("createdAt", bm.createdAt)
    }

    private fun parseBookmark(obj: JSONObject): Bookmark = Bookmark(
        id = obj.optString("id", ""),
        bookId = obj.optString("bookId", ""),
        bookTitle = obj.optString("bookTitle", ""),
        chapterIndex = obj.optInt("chapterIndex", 0),
        paragraphIndex = obj.optInt("paragraphIndex", 0),
        chapterTitle = obj.optString("chapterTitle", ""),
        preview = obj.optString("preview", ""),
        createdAt = obj.optLong("createdAt", 0L),
    )

    private fun readerPrefsToJson(p: ReaderPrefs): JSONObject = JSONObject().apply {
        put("fontSize", p.fontSize)
        put("lineHeight", p.lineHeight.toDouble())
        put("paragraphSpacing", p.paragraphSpacing)
        put("pagePadding", p.pagePadding)
        put("theme", p.theme)
        put("pageMode", p.pageMode)
        put("dimLevel", p.dimLevel)
        put("colorTemperature", p.colorTemperature)
        put("customFontPath", p.customFontPath)
        put("customBackgroundPath", p.customBackgroundPath)
    }

    private fun parseReaderPrefs(obj: JSONObject): ReaderPrefs = ReaderPrefs(
        fontSize = obj.optInt("fontSize", 20),
        lineHeight = obj.optDouble("lineHeight", 1.75).toFloat(),
        paragraphSpacing = obj.optInt("paragraphSpacing", 14),
        pagePadding = obj.optInt("pagePadding", 22),
        theme = obj.optString("theme", "paper"),
        pageMode = obj.optString("pageMode", "scroll"),
        dimLevel = obj.optInt("dimLevel", 0),
        colorTemperature = obj.optInt("colorTemperature", 0),
        customFontPath = obj.optString("customFontPath", ""),
        customBackgroundPath = obj.optString("customBackgroundPath", ""),
    )

    private fun syncPrefsToJson(p: SyncPrefs): JSONObject = JSONObject().apply {
        put("webDavUrl", p.webDavUrl)
        put("username", p.username)
        put("password", p.password)
        put("remoteFile", p.remoteFile)
        put("autoCheckUpdate", p.autoCheckUpdate)
        put("githubOwner", p.githubOwner)
        put("githubRepo", p.githubRepo)
    }

    private fun parseSyncPrefs(obj: JSONObject): SyncPrefs = SyncPrefs(
        webDavUrl = obj.optString("webDavUrl", ""),
        username = obj.optString("username", ""),
        password = obj.optString("password", ""),
        remoteFile = obj.optString("remoteFile", "novelreader-backup.json"),
        autoCheckUpdate = obj.optBoolean("autoCheckUpdate", false),
        githubOwner = obj.optString("githubOwner", ""),
        githubRepo = obj.optString("githubRepo", ""),
    )
}

/**
 * Android 文件存储实现。
 */
class AndroidFileRepository(
    private val context: Context,
) : FileRepository {

    private val chaptersRoot: File
        get() = File(context.filesDir, "chapters")

    override fun importBook(path: String): Book {
        val text = readFileContent(path)
        val cleaned = ChapterParser.cleanText(text)
        val chapters = ChapterParser.splitChapters(cleaned)

        val fileName = path.substringAfterLast('/').substringAfterLast('\\')
        val title = fileName.substringBeforeLast('.').ifBlank { "未命名书籍" }
        val bookId = UUID.randomUUID().toString()

        // 创建章节目录
        val bookDir = getBookDirFile(bookId)
        if (!bookDir.exists()) {
            bookDir.mkdirs()
        }

        // 写入每章文件
        chapters.forEach { chapter ->
            val chapterFile = File(bookDir, "${chapter.index}.txt")
            chapterFile.writeText(
                chapter.title + "\n" + chapter.body,
                Charsets.UTF_8,
            )
        }

        val totalChars = chapters.sumOf { it.body.length.toLong() }

        return Book(
            id = bookId,
            title = title,
            filePath = path,
            chapterCount = chapters.size,
            currentChapter = 0,
            createdAt = currentTimeMillis(),
            lastReadAt = currentTimeMillis(),
            source = "local",
            totalCharCount = totalChars,
        )
    }

    override fun loadChapter(bookId: String, chapterIndex: Int): String? {
        val file = getChapterFile(bookId, chapterIndex)
        return if (file.exists()) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    override fun getBookDir(bookId: String): String {
        return getBookDirFile(bookId).absolutePath
    }

    private fun getBookDirFile(bookId: String): File {
        return File(chaptersRoot, bookId)
    }

    private fun getChapterFile(bookId: String, chapterIndex: Int): File {
        return File(getBookDirFile(bookId), "$chapterIndex.txt")
    }

    /**
     * 读取文件内容，支持 content:// URI 和文件路径。
     */
    private fun readFileContent(path: String): String {
        return if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: throw IllegalStateException("无法读取文件: $path")
        } else {
            File(path).readText(Charsets.UTF_8)
        }
    }
}

/**
 * Android 应用信息提供者。
 */
class AndroidAppInfo(
    private val context: Context,
) : AppInfoProvider {

    override val appVersionName: String
        get() = try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pkgInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

    override val appVersionCode: Int
        get() = try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pkgInfo.versionCode
        } catch (e: Exception) {
            1
        }

    override val appDataDir: String
        get() = context.filesDir.absolutePath
}
