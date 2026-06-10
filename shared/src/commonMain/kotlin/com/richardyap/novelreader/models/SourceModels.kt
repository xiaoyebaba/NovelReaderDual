package com.richardyap.novelreader.models

/**
 * 书源记录 —— 一个已安装的在线书源。
 */
data class BookSourceRecord(
    val id: String,
    val name: String,
    val version: String = "",
    val author: String = "",
    val url: String = "",
    val group: String = "",
    val logo: String = "",
    val type: String = "novel",
    val enabled: Boolean = true,
    val minDelay: Int = 0,
    val tags: List<String> = emptyList(),
    val description: String = "",
    val updateUrl: String = "",
    val requireUrls: List<String> = emptyList(),
    val scriptFileName: String = "",
    val installedAt: Long = 0L,
)

/**
 * 在线书籍搜索结果条目。
 */
data class OnlineBookItem(
    val name: String,
    val bookUrl: String,
    val author: String = "",
    val coverUrl: String = "",
    val tocUrl: String = "",
    val intro: String = "",
    val latestChapter: String = "",
    val latestChapterUrl: String = "",
    val wordCount: String = "",
    val chapterCount: Int = 0,
    val updateTime: String = "",
    val status: String = "",
    val kind: String = "",
)

/**
 * 在线章节目录条目。
 */
data class OnlineChapterInfo(
    val name: String,
    val url: String,
    val vip: Boolean = false,
)

/**
 * 书源健康检测结果。
 */
data class SourceHealthResult(
    val ok: Boolean,
    val message: String,
)

/**
 * 旧规则 → JS 转换结果。
 */
data class LegacyConversionResult(
    val script: String,
    val warnings: List<String> = emptyList(),
)

/**
 * 已缓存的在线书籍内容。
 */
data class CachedOnlineBook(
    val bookId: String,
    val sourceId: String,
    val sourceName: String,
    val bookUrl: String,
    val tocUrl: String,
    val chapters: List<OnlineChapterInfo>,
    val cachedAt: Long,
)
