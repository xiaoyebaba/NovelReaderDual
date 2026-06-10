package com.richardyap.novelreader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.richardyap.novelreader.platform.AndroidStorageRepository
import com.richardyap.novelreader.platform.AndroidFileRepository
import com.richardyap.novelreader.ui.NovelApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android 主 Activity。
 * 初始化平台实现并启动 Compose 界面。
 */
class MainActivity : ComponentActivity() {

    private lateinit var storageRepository: AndroidStorageRepository
    private lateinit var fileRepository: AndroidFileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化平台实现
        storageRepository = AndroidStorageRepository(applicationContext)
        fileRepository = AndroidFileRepository(applicationContext)

        setContent {
            NovelApp(
                storage = storageRepository,
                fileRepo = fileRepository,
            )
        }

        // 处理文件导入 Intent
        handleImportIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleImportIntent(intent)
    }

    /**
     * 处理外部文件导入（通过分享或打开方式）。
     */
    private fun handleImportIntent(intent: Intent?) {
        if (intent == null) return

        val path: String? = when {
            // ACTION_VIEW: 直接打开文件
            intent.action == Intent.ACTION_VIEW -> {
                intent.data?.toString()
            }
            // ACTION_SEND: 分享文件
            intent.action == Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        }

        if (path != null) {
            importBook(path)
        }
    }

    /**
     * 异步导入书籍。
     */
    private fun importBook(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val book = fileRepository.importBook(path)
                val books = storageRepository.loadBooks()
                    .filter { it.filePath != path } // 避免重复
                    .toMutableList()
                books.add(0, book)
                storageRepository.saveBooks(books)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
