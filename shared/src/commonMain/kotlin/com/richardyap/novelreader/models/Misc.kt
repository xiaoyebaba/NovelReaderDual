package com.richardyap.novelreader.models

data class SearchResult(
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val chapterTitle: String,
    val preview: String,
)

data class SyncPrefs(
    val webDavUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remoteFile: String = "novelreader-backup.json",
    val autoCheckUpdate: Boolean = false,
    val githubOwner: String = "",
    val githubRepo: String = "",
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val releaseUrl: String = "",
)
