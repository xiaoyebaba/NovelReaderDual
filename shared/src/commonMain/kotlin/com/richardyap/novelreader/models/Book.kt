package com.richardyap.novelreader.models

/**
 * 一本书的元数据，不包含内容。
 */
data class Book(
    val id: String,
    val title: String,
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val filePath: String = "",
    val chapterCount: Int = 0,
    val currentChapter: Int = 0,
    val paragraphIndex: Int = 0,
    val currentChapterTitle: String = "",
    val createdAt: Long = 0L,
    val lastReadAt: Long = 0L,
    /** 来源：local | online | source:{sourceId} */
    val source: String = "local",
    val sourceUrl: String = "",
    val totalCharCount: Long = 0L,
)
