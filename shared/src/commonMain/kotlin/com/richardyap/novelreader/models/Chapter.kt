package com.richardyap.novelreader.models

data class Chapter(
    val title: String,
    val body: String,
    val wordCount: Int = body.length,
    val index: Int = 0,
)
