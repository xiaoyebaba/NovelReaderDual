package com.richardyap.novelreader.util

import com.richardyap.novelreader.models.Chapter
import com.richardyap.novelreader.models.SearchResult

/**
 * TXT 章节自动识别解析器。
 * 支持中文数字/阿拉伯数字章节标题，也支持英文 Chapter 格式。
 */
object ChapterParser {

    private val chineseNums = "零〇一二三四五六七八九十百千万两"
    private val chapterTitlePattern = Regex(
        """(?im)^[ \t　]*(第[${chineseNums}\d]+[章节卷回部集篇][^\n]{0,48}|Chapter\s+\d+[^\n]{0,48}|\d{1,4}[、.．\-\s]+[^\n]{1,48})[ \t　]*$"""
    )
    private val specialTitlePattern = Regex(
        """(?im)^[ \t　]*(序章|楔子|引子|前言|正文|尾声|后记|番外[^\n]{0,40}|Prologue|Epilogue|附录[^\n]{0,40})[ \t　]*$"""
    )

    /**
     * 将原始文本按章节切分。
     */
    fun splitChapters(rawText: String): List<Chapter> {
        val cleaned = cleanText(rawText)
        val lines = cleaned.lines()

        // 找出所有章节标题位置
        data class TitlePos(val index: Int, val title: String, val lineIndex: Int)
        val titles = mutableListOf<TitlePos>()

        lines.forEachIndexed { i, line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEachIndexed

            val match = chapterTitlePattern.find(trimmed) ?: specialTitlePattern.find(trimmed)
            if (match != null) {
                val title = match.value.trim()
                // 避免误识别过短的行（比如单独的 "第1章"）
                if (title.length >= 3 || match.value.contains(Regex("""[章节卷回部集篇]"""))) {
                    titles.add(TitlePos(titles.size, title, i))
                }
            }
        }

        if (titles.isEmpty()) {
            // 没有检测到章节，整本书作为一个章节
            val body = cleaned.trim()
            val title = if (body.length > 30) body.take(30) + "…" else body
            return listOf(Chapter(title = title, body = body, index = 0))
        }

        // 如果第一个标题之前还有内容，作为"前言"
        val chapters = mutableListOf<Chapter>()

        if (titles.first().lineIndex > 0) {
            val preamble = lines.take(titles.first().lineIndex).joinToString("\n").trim()
            if (preamble.isNotBlank()) {
                chapters.add(Chapter(title = "前言", body = preamble, index = 0))
            }
        }

        val startIdx = if (chapters.isNotEmpty()) 0 else -1

        titles.forEachIndexed { i, titlePos ->
            val startLine = titlePos.lineIndex + 1 // 跳过标题行本身
            val endLine = if (i + 1 < titles.size) titles[i + 1].lineIndex else lines.size
            val body = lines.subList(startLine, endLine).joinToString("\n").trim()
            if (body.isNotBlank()) {
                chapters.add(
                    Chapter(
                        title = titlePos.title,
                        body = body,
                        index = startIdx + i + 1,
                    )
                )
            }
        }

        return chapters.ifEmpty {
            listOf(Chapter(title = "正文", body = cleaned.trim(), index = 0))
        }
    }

    /**
     * 全书关键词搜索。
     */
    fun searchAll(chapters: List<Chapter>, keyword: String): List<SearchResult> {
        if (keyword.isBlank()) return emptyList()
        val results = mutableListOf<SearchResult>()
        chapters.forEach { chapter ->
            var startIdx = 0
            val lowerBody = chapter.body.lowercase()
            val lowerKw = keyword.lowercase()
            while (true) {
                val pos = lowerBody.indexOf(lowerKw, startIdx)
                if (pos < 0) break
                val paragraphIdx = chapter.body.substring(0, pos).count { it == '\n' }
                val preview = buildString {
                    val start = (pos - 40).coerceAtLeast(0)
                    val end = (pos + keyword.length + 60).coerceAtMost(chapter.body.length)
                    if (start > 0) append("…")
                    append(chapter.body.substring(start, end).replace("\n", " "))
                    if (end < chapter.body.length) append("…")
                }
                results.add(
                    com.richardyap.novelreader.models.SearchResult(
                        chapterIndex = chapter.index,
                        paragraphIndex = paragraphIdx,
                        chapterTitle = chapter.title,
                        preview = preview,
                    )
                )
                startIdx = pos + keyword.length
                // 每章最多返回 20 条
                if (results.size >= 500) return results
            }
        }
        return results
    }

    /**
     * 清理文本：统一换行符、合并连续空行、去除首尾空白。
     */
    fun cleanText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("""\n{4,}"""), "\n\n\n")
            .replace(Regex("""(?m)^[ \t]+$"""), "")
            .trim()
    }
}
