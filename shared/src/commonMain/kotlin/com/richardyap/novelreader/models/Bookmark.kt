package com.richardyap.novelreader.models

data class Bookmark(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val chapterTitle: String,
    val preview: String,
    val createdAt: Long,
)
