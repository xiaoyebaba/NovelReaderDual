package com.richardyap.novelreader

/**
 * Desktop actual 声明 —— 需要在与 expect 相同的包中。
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun generateUUID(): String = java.util.UUID.randomUUID().toString()
