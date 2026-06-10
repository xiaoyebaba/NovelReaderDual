package com.richardyap.novelreader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.richardyap.novelreader.platform.AndroidFileRepository
import com.richardyap.novelreader.platform.AndroidStorageRepository
import com.richardyap.novelreader.ui.NovelApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var storageRepository: AndroidStorageRepository
    private lateinit var fileRepository: AndroidFileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        storageRepository = AndroidStorageRepository(applicationContext)
        fileRepository = AndroidFileRepository(applicationContext)

        setContent {
            NovelApp(
                storage = storageRepository,
                fileRepo = fileRepository,
            )
        }

        handleImportIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?) {
        if (intent == null) return

        val path: String? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.toString()
            Intent.ACTION_SEND -> extractSharedUri(intent)?.toString()
            else -> null
        }

        if (path != null) {
            importBook(path)
        }
    }

    private fun extractSharedUri(intent: Intent): Uri? {
        val sharedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        return sharedUri ?: intent.data
    }

    private fun importBook(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val book = fileRepository.importBook(path)
                val books = storageRepository.loadBooks()
                    .filter { it.filePath != path }
                    .toMutableList()
                books.add(0, book)
                storageRepository.saveBooks(books)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
