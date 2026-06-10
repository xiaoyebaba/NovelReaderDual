package com.richardyap.novelreader.platform

import com.richardyap.novelreader.AppInfoProvider
import com.richardyap.novelreader.FileRepository
import com.richardyap.novelreader.StorageRepository
import com.richardyap.novelreader.currentTimeMillis
import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.Bookmark
import com.richardyap.novelreader.models.Chapter
import com.richardyap.novelreader.models.ReaderPrefs
import com.richardyap.novelreader.models.SyncPrefs
import com.richardyap.novelreader.util.ChapterParser
import com.richardyap.novelreader.util.EncodingDetector
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.UUID
import kotlin.text.RegexOption
import java.util.zip.ZipFile

// ==================== Desktop 数据目录 ====================

private val desktopDataDir: File
    get() = File(System.getProperty("user.home"), ".novelreader").also { dir ->
        if (!dir.exists()) dir.mkdirs()
    }

private val desktopChaptersDir: File
    get() = File(desktopDataDir, "chapters").also { dir ->
        if (!dir.exists()) dir.mkdirs()
    }

// ==================== Desktop Storage ====================

/**
 * Desktop 存储实现 —— 基于 JSON 文件。
 */
class DesktopStorageRepository : StorageRepository {

    private val booksFile: File get() = File(desktopDataDir, "books.json")
    private val bookmarksFile: File get() = File(desktopDataDir, "bookmarks.json")
    private val prefsFile: File get() = File(desktopDataDir, "prefs.json")
    private val syncFile: File get() = File(desktopDataDir, "sync.json")

    override fun loadBooks(): List<Book> {
        if (!booksFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(booksFile.readText())
            (0 until arr.length()).map { i -> parseBook(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun saveBooks(books: List<Book>) {
        val arr = JSONArray()
        books.forEach { arr.put(bookToJson(it)) }
        booksFile.writeText(arr.toString(2))
    }

    override fun loadBookmarks(): List<Bookmark> {
        if (!bookmarksFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(bookmarksFile.readText())
            (0 until arr.length()).map { i -> parseBookmark(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun saveBookmarks(bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        bookmarks.forEach { arr.put(bookmarkToJson(it)) }
        bookmarksFile.writeText(arr.toString(2))
    }

    override fun loadReaderPrefs(): ReaderPrefs {
        if (!prefsFile.exists()) return ReaderPrefs()
        return try {
            parseReaderPrefs(JSONObject(prefsFile.readText()))
        } catch (e: Exception) {
            ReaderPrefs()
        }
    }

    override fun saveReaderPrefs(prefs: ReaderPrefs) {
        prefsFile.writeText(readerPrefsToJson(prefs).toString(2))
    }

    override fun loadSyncPrefs(): SyncPrefs {
        if (!syncFile.exists()) return SyncPrefs()
        return try {
            parseSyncPrefs(JSONObject(syncFile.readText()))
        } catch (e: Exception) {
            SyncPrefs()
        }
    }

    override fun saveSyncPrefs(prefs: SyncPrefs) {
        syncFile.writeText(syncPrefsToJson(prefs).toString(2))
    }

    override fun deleteBook(book: Book) {
        // 删除章节目录
        val bookDir = File(desktopChaptersDir, book.id)
        if (bookDir.exists()) {
            bookDir.deleteRecursively()
        }
        // 移除书籍记录
        val books = loadBooks().filter { it.id != book.id }
        saveBooks(books)
        // 移除书签
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

// ==================== Desktop File Repository ====================

/**
 * Desktop 文件存储实现。
 * 支持 TXT（UTF-8/GBK 自动检测）和 EPUB 导入。
 */
class DesktopFileRepository : FileRepository {

    private val chaptersRoot: File
        get() = desktopChaptersDir

    override fun importBook(path: String): Book {
        val file = File(path)
        if (!file.exists()) throw IllegalArgumentException("文件不存在: $path")

        val chapters: List<Chapter>
        val fileName = file.name
        val title: String

        when {
            fileName.endsWith(".epub", ignoreCase = true) -> {
                chapters = parseEpub(file)
                title = fileName.removeSuffix(".epub").removeSuffix(".EPUB")
            }
            else -> {
                val text = readTextWithDetection(file)
                val cleaned = ChapterParser.cleanText(text)
                chapters = ChapterParser.splitChapters(cleaned)
                title = fileName.substringBeforeLast('.').ifBlank { "未命名书籍" }
            }
        }

        val bookId = UUID.randomUUID().toString()
        val bookDir = File(chaptersRoot, bookId)
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
        val file = File(chaptersRoot, "$bookId/$chapterIndex.txt")
        return if (file.exists()) {
            file.readText(Charsets.UTF_8)
        } else {
            null
        }
    }

    override fun getBookDir(bookId: String): String {
        return File(chaptersRoot, bookId).absolutePath
    }

    /**
     * 读取文本文件，使用 EncodingDetector 自动检测编码。
     */
    private fun readTextWithDetection(file: File): String {
        // 读取前 4KB 用于编码检测
        val headSize = 4096
        val headBytes = FileInputStream(file).use { fis ->
            val buf = ByteArray(headSize.coerceAtMost(file.length().toInt()))
            fis.read(buf)
            buf
        }

        val encoding = EncodingDetector.guessEncoding(headBytes)
        val charset = try {
            Charset.forName(encoding)
        } catch (e: Exception) {
            Charsets.UTF_8
        }

        return file.readText(charset)
    }

    /**
     * 解析 EPUB 文件。
     * EPUB 本质是 ZIP 文件，内含 .xhtml/.html 章节。
     */
    private fun parseEpub(epubFile: File): List<Chapter> {
        ZipFile(epubFile).use { zip ->
            val entries = zip.entries().asSequence().toList()

            // 收集所有 .xhtml 和 .html 文件
            val contentEntries = entries.filter { entry ->
                val name = entry.name.lowercase()
                !entry.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))
            }.sortedBy { it.name }

            if (contentEntries.isEmpty()) {
                // 没有 HTML 文件，尝试读取所有文本内容
                val allText = entries.filter { !it.isDirectory }
                    .joinToString("\n") { entry ->
                        try {
                            zip.getInputStream(entry).bufferedReader().readText()
                        } catch (e: Exception) {
                            ""
                        }
                    }
                val cleaned = ChapterParser.cleanText(allText)
                return ChapterParser.splitChapters(cleaned)
            }

            val chapters = mutableListOf<Chapter>()
            var chapterIndex = 0

            for (entry in contentEntries) {
                try {
                    val rawText = zip.getInputStream(entry).bufferedReader().readText()
                    // 提取 body 中的文本
                    val text = extractTextFromHtml(rawText)
                    val cleaned = ChapterParser.cleanText(text)

                    if (cleaned.isBlank()) continue

                    // 尝试从文件名或内容中提取标题
                    val title = extractTitleFromHtml(rawText)
                        ?: entry.name.substringAfterLast('/').substringBeforeLast('.')

                    chapters.add(
                        Chapter(
                            title = title,
                            body = cleaned,
                            index = chapterIndex++,
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (chapters.isEmpty()) {
                return listOf(
                    Chapter(
                        title = "正文",
                        body = "（无法解析 EPUB 内容）",
                        index = 0,
                    )
                )
            }

            return chapters
        }
    }

    /**
     * 从 HTML 中提取标题（<title> 或 <h1>）。
     */
    private fun extractTitleFromHtml(html: String): String? {
        val titleRegex = Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE)
        val h1Regex = Regex("""<h1[^>]*>(.*?)</h1>""", RegexOption.IGNORE_CASE)

        titleRegex.find(html)?.groupValues?.get(1)?.let { return it.trim() }
        h1Regex.find(html)?.groupValues?.get(1)?.let { return it.trim() }
        return null
    }

    /**
     * 从 HTML 中提取纯文本（移除标签，保留段落）。
     */
    private fun extractTextFromHtml(html: String): String {
        // 将常见块级元素替换为换行
        var text = html
            .replace(Regex("""<head[^>]*>.*?</head>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</?p[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</?div[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</?h[1-6][^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</?li[^>]*>""", RegexOption.IGNORE_CASE), "\n")

        // 移除所有 HTML 标签
        text = text.replace(Regex("""<[^>]+>"""), "")

        // HTML 实体解码
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace(Regex("""&#(\d+);""")) { it.groupValues[1].toInt().toChar().toString() }
            .replace(Regex("""&#x([0-9a-fA-F]+);""")) { it.groupValues[1].toInt(16).toChar().toString() }

        // 清理多余的空白
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }
}

/**
 * Desktop 应用信息提供者。
 */
class DesktopAppInfo : AppInfoProvider {
    override val appVersionName: String = "1.0.0"
    override val appVersionCode: Int = 1
    override val appDataDir: String
        get() = desktopDataDir.absolutePath
}
