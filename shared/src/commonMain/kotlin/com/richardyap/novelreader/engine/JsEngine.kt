package com.richardyap.novelreader.engine

import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.OnlineBookItem
import com.richardyap.novelreader.models.OnlineChapterInfo

/**
 * JS 引擎抽象 —— 用于执行书源脚本。
 * 平台提供 Rhino 实现。
 */
expect class JsEngine() {

    /** 加载并初始化书源脚本 */
    fun loadSourceScript(sourceId: String, script: String)

    /** 搜索书籍，返回结果列表 */
    fun search(keyword: String): List<OnlineBookItem>

    /** 获取书籍详情 */
    fun bookInfo(bookUrl: String): Pair<Book, List<OnlineChapterInfo>>

    /** 获取章节目录 */
    fun chapterList(tocUrl: String): List<OnlineChapterInfo>

    /** 获取章节内容 */
    fun chapterContent(chapterUrl: String): String

    /** 释放引擎资源 */
    fun close()
}
