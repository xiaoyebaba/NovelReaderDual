package com.richardyap.novelreader.engine

import com.richardyap.novelreader.currentTimeMillis
import com.richardyap.novelreader.models.BookSourceRecord

/**
 * 书源工具函数 —— 用于解析 Legado 书源脚本。
 * 这些函数在 commonMain 中可用（纯 Kotlin 实现，不依赖平台 API）。
 */
object LegadoUtils {

    /**
     * 解析 Legado 书源脚本中的元数据注释头。
     * 注释头部以 // 开头，包含 @name、@url、@version 等标记。
     *
     * @param script 完整的 JS 书源脚本
     * @return 元数据键值对 map
     */
    fun parseLegadoSourceMeta(script: String): Map<String, String> {
        val meta = mutableMapOf<String, String>()
        val lines = script.lines()

        for (line in lines) {
            val trimmed = line.trim()
            // 只解析以 // @ 开头的注释行
            if (!trimmed.startsWith("// @") && !trimmed.startsWith("//@")) continue

            val content = if (trimmed.startsWith("// @")) {
                trimmed.removePrefix("// @").trim()
            } else {
                trimmed.removePrefix("//@").trim()
            }

            // 分割键值对：@key value 或 @key:value 或 @key=value
            val separatorIndex = content.indexOfAny(charArrayOf(' ', ':', '=', '\t'))
            if (separatorIndex < 0) {
                // 无值标记（如 @search @bookinfo 等布尔标记）
                meta[content.trim()] = "true"
                continue
            }

            val key = content.substring(0, separatorIndex).trim()
            val value = content.substring(separatorIndex + 1).trim()

            if (key.isNotEmpty()) {
                meta[key] = value
            }
        }

        return meta
    }

    /**
     * 从书源元数据创建 BookSourceRecord。
     */
    fun createBookSourceRecord(id: String, meta: Map<String, String>, scriptFileName: String): BookSourceRecord {
        return BookSourceRecord(
            id = id,
            name = meta["name"] ?: meta["title"] ?: "未命名书源",
            version = meta["version"] ?: "",
            author = meta["author"] ?: "",
            url = meta["url"] ?: "",
            group = meta["group"] ?: "默认",
            logo = meta["logo"] ?: "",
            type = meta["type"] ?: "novel",
            enabled = meta["enabled"]?.lowercase() != "false",
            minDelay = meta["minDelay"]?.toIntOrNull() ?: 0,
            tags = meta["tags"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            description = meta["description"] ?: meta["desc"] ?: "",
            updateUrl = meta["updateUrl"] ?: "",
            requireUrls = meta["requireUrls"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            scriptFileName = scriptFileName,
            installedAt = currentTimeMillis(),
        )
    }

    /**
     * URL 拼接工具。
     * 处理相对路径和绝对路径的解析。
     */
    fun resolveUrl(base: String, path: String): String {
        if (base.isBlank()) return path
        if (path.isBlank()) return base

        // 已经是完整 URL
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) {
            return path
        }

        // 协议相对 URL: //example.com/path
        if (path.startsWith("//")) {
            val protocol = if (base.startsWith("https://")) "https:" else "http:"
            return protocol + path
        }

        // 绝对路径: /path/to/resource
        if (path.startsWith("/")) {
            val root = extractRoot(base)
            return root + path
        }

        // 相对路径: ../path 或 path
        val baseDir = base.substringBeforeLast('/')
        val parts = mutableListOf<String>()
        parts.addAll(baseDir.split('/').filter { it.isNotEmpty() })

        var resolved = path
        while (resolved.startsWith("../")) {
            if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
            resolved = resolved.removePrefix("../")
        }
        while (resolved.startsWith("./")) {
            resolved = resolved.removePrefix("./")
        }

        val root = if (parts.isNotEmpty()) {
            val protocol = base.substringBefore("://")
            protocol + "://" + parts.joinToString("/")
        } else {
            extractRoot(base)
        }

        return "$root/$resolved"
    }

    /**
     * 提取 URL 的根（协议+主机+端口）。
     */
    fun extractRoot(url: String): String {
        val doubleSlash = url.indexOf("://")
        if (doubleSlash < 0) return url

        val afterProtocol = url.substring(doubleSlash + 3)
        val slash = afterProtocol.indexOf('/')
        return if (slash >= 0) {
            url.substring(0, doubleSlash + 3 + slash)
        } else {
            url
        }
    }

    /**
     * 从文本中分割多个书源脚本（以分隔符或独立 JS 块分割）。
     * 多个书源可能在一个文本中，需要按注释头分割。
     */
    fun splitSourceScripts(text: String): List<String> {
        val scripts = mutableListOf<String>()
        val lines = text.lines()
        val current = StringBuilder()
        var inJs = false

        for (line in lines) {
            val trimmed = line.trim()
            // 检测新书源的开始：以 // @name 或 // @Name 开头的注释
            if ((trimmed.startsWith("// @name") || trimmed.startsWith("// @Name") ||
                 trimmed.startsWith("//@name") || trimmed.startsWith("//@Name")) && current.isNotEmpty()) {
                scripts.add(current.toString().trim())
                current.clear()
            }
            current.appendLine(line)
            if (!trimmed.startsWith("//")) {
                inJs = true
            }
        }

        if (current.isNotBlank()) {
            scripts.add(current.toString().trim())
        }

        return scripts.ifEmpty { listOf(text) }
    }

    /**
     * 标准化书源脚本（清理空白、统一换行）。
     */
    fun normalizeScript(script: String): String {
        return script
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
    }
}

// currentTimeMillis() expect 声明在 NovelRepository.kt 中，这里直接引用
