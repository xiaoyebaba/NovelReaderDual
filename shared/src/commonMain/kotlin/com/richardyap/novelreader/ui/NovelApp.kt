package com.richardyap.novelreader.ui

import androidx.compose.runtime.Composable
import com.richardyap.novelreader.FileRepository
import com.richardyap.novelreader.NovelRepository
import com.richardyap.novelreader.StorageRepository

/**
 * 应用主 Composable 入口（兼容旧调用）。
 * 现在委托给 [NovelAppMain] 实现，带有完整的导航系统。
 *
 * @see NovelAppMain
 * @see AppState
 */
@Composable
fun NovelApp(
    storage: StorageRepository,
    fileRepo: FileRepository,
) {
    val repository = NovelRepository(storage, fileRepo)
    NovelAppMain(
        repository = repository,
        storage = storage,
        fileRepository = fileRepo,
    )
}
