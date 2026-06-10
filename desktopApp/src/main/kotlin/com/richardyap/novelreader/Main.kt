package com.richardyap.novelreader

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.richardyap.novelreader.platform.DesktopStorageRepository
import com.richardyap.novelreader.platform.DesktopFileRepository
import com.richardyap.novelreader.ui.NovelApp

/**
 * Desktop 应用入口。
 * 初始化平台实现并启动 Compose Desktop 窗口。
 */
fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(900.dp, 680.dp),
        position = WindowPosition(Alignment.Center),
    )

    // 初始化平台实现
    val storageRepository = DesktopStorageRepository()
    val fileRepository = DesktopFileRepository()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "NovelReader",
        resizable = true,
    ) {
        NovelApp(
            storage = storageRepository,
            fileRepo = fileRepository,
        )
    }
}
