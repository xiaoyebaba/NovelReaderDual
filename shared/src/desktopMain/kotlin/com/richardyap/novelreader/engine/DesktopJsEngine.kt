package com.richardyap.novelreader.engine

import com.richardyap.novelreader.models.Book
import com.richardyap.novelreader.models.OnlineBookItem
import com.richardyap.novelreader.models.OnlineChapterInfo
import com.richardyap.novelreader.currentTimeMillis
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.NativeArray
import org.jsoup.Jsoup
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/**
 * Desktop actual JsEngine 实现 —— 使用 Mozilla Rhino。
 * 与 Android 实现结构相似，但使用 Desktop JVM 的 HTTP 客户端和 Jsoup。
 */
actual class JsEngine actual constructor() {

    private var rhinoContext: Context? = null
    private var scope: ScriptableObject? = null
    private var sourceId: String = ""
    private var baseUrl: String = ""

    /**
     * 加载并初始化书源脚本。
     */
    actual fun loadSourceScript(sourceId: String, script: String) {
        this.sourceId = sourceId

        // 解析脚本头部获取 baseUrl
        val meta = LegadoUtils.parseLegadoSourceMeta(script)
        this.baseUrl = meta["url"] ?: ""

        val normalized = LegadoUtils.normalizeScript(script)

        rhinoContext = Context.enter()
        rhinoContext?.optimizationLevel = -1 // 解释模式，兼容性最好
        rhinoContext?.languageVersion = Context.VERSION_ES6

        scope = rhinoContext?.initStandardObjects() as? ScriptableObject

        // 注入 legado 全局对象
        injectLegadoApi()

        // 执行书源脚本
        rhinoContext?.evaluateString(scope, normalized, sourceId, 1, null)
    }

    /**
     * 搜索书籍。
     */
    actual fun search(keyword: String): List<OnlineBookItem> {
        val scp = scope ?: return emptyList()

        return try {
            val result = invokeJsFunction("search", scp, keyword)
            parseSearchResult(result)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取书籍详情。
     */
    actual fun bookInfo(bookUrl: String): Pair<Book, List<OnlineChapterInfo>> {
        val scp = scope ?: return Book(
            id = "", title = "", source = "source:$sourceId",
        ) to emptyList()

        return try {
            val result = invokeJsFunction("bookInfo", scp, bookUrl)
            parseBookInfoResult(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Book(id = "", title = "", source = "source:$sourceId") to emptyList()
        }
    }

    /**
     * 获取章节目录。
     */
    actual fun chapterList(tocUrl: String): List<OnlineChapterInfo> {
        val scp = scope ?: return emptyList()

        return try {
            val result = invokeJsFunction("chapterList", scp, tocUrl)
            parseChapterListResult(result)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取章节内容。
     */
    actual fun chapterContent(chapterUrl: String): String {
        val scp = scope ?: return ""

        return try {
            val result = invokeJsFunction("chapterContent", scp, chapterUrl)
            result?.toString() ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 释放引擎资源。
     */
    actual fun close() {
        try {
            Context.exit()
        } catch (e: Exception) {
            // ignore
        }
        rhinoContext = null
        scope = null
    }

    // ==================== 内部辅助 ====================

    private fun invokeJsFunction(name: String, scopeObj: ScriptableObject, vararg args: Any): Any? {
        val func = scopeObj.get(name, scopeObj) as? Function ?: return null
        val jsArgs = args.map { Context.javaToJS(it, scopeObj) }.toTypedArray()
        return func.call(rhinoContext, scopeObj, scopeObj, jsArgs)
    }

    /**
     * 注入 Legado 兼容 API（getHttp, getDom 等）。
     */
    private fun injectLegadoApi() {
        val legadoObj = rhinoContext?.initStandardObjects() as? ScriptableObject ?: return

        // legado.getHttp(u, headers)
        val getHttp = object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                if (args.isEmpty()) return ""
                val urlStr = args[0]?.toString() ?: return ""
                val resolvedUrl = LegadoUtils.resolveUrl(baseUrl, urlStr)

                try {
                    val uri = URI.create(resolvedUrl)
                    val url = uri.toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    conn.setRequestProperty("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

                    // 处理自定义 headers
                    if (args.size > 1 && args[1] is NativeObject) {
                        val headers = args[1] as NativeObject
                        for (id in headers.ids) {
                            val key = id.toString()
                            val value = headers.get(key, headers)?.toString() ?: continue
                            conn.setRequestProperty(key, value)
                        }
                    }

                    // 处理重定向
                    conn.instanceFollowRedirects = true

                    val inputStream: InputStream = try {
                        conn.inputStream
                    } catch (e: Exception) {
                        conn.errorStream ?: throw e
                    }

                    val body = inputStream.bufferedReader(Charsets.UTF_8).readText()
                    conn.disconnect()
                    return body
                } catch (e: Exception) {
                    e.printStackTrace()
                    return ""
                }
            }
        }

        // legado.getDom(u, headers) -> 返回 HTML 字符串（由 JS 脚本自行解析）
        val getDom = object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                val html = getHttp.call(cx, scope, thisObj, args)?.toString() ?: return ""
                val doc = Jsoup.parse(html)

                // 返回一个提供 DOM 方法的 JS 对象
                val domObj = cx.newObject(scope)

                ScriptableObject.putProperty(domObj, "html", html)

                // 注入 Jsoup 选择器方法
                val selectFn = object : org.mozilla.javascript.BaseFunction() {
                    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                        if (args.isEmpty()) return NativeArray(0)
                        val cssQuery = args[0].toString()
                        val elements = doc.select(cssQuery)
                        val arr = cx.newArray(scope, elements.size)
                        elements.forEachIndexed { idx, el ->
                            val elObj = cx.newObject(scope)
                            ScriptableObject.putProperty(elObj, "text", el.text())
                            ScriptableObject.putProperty(elObj, "html", el.html())
                            ScriptableObject.putProperty(elObj, "ownText", el.ownText())
                            arr.put(idx, arr, elObj)
                        }
                        return arr
                    }
                }

                val selectFirstFn = object : org.mozilla.javascript.BaseFunction() {
                    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                        if (args.isEmpty()) return ""
                        val cssQuery = args[0].toString()
                        val el = doc.selectFirst(cssQuery) ?: return ""
                        val elObj = cx.newObject(scope)
                        ScriptableObject.putProperty(elObj, "text", el.text())
                        ScriptableObject.putProperty(elObj, "html", el.html())
                        ScriptableObject.putProperty(elObj, "ownText", el.ownText())
                        return elObj
                    }
                }

                domObj.put("select", domObj, selectFn)
                domObj.put("selectFirst", domObj, selectFirstFn)

                return domObj
            }
        }

        // legado.postHttp(u, body, headers)
        val postHttp = object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                if (args.isEmpty()) return ""
                val urlStr = args[0]?.toString() ?: return ""
                val resolvedUrl = LegadoUtils.resolveUrl(baseUrl, urlStr)
                val body = args.getOrNull(1)?.toString() ?: ""

                try {
                    val uri = URI.create(resolvedUrl)
                    val url = uri.toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                    if (args.size > 2 && args[2] is NativeObject) {
                        val headers = args[2] as NativeObject
                        for (id in headers.ids) {
                            val key = id.toString()
                            val value = headers.get(key, headers)?.toString() ?: continue
                            conn.setRequestProperty(key, value)
                        }
                    }

                    conn.outputStream.use { os ->
                        os.write(body.toByteArray(Charsets.UTF_8))
                    }

                    val inputStream = try {
                        conn.inputStream
                    } catch (e: Exception) {
                        conn.errorStream ?: throw e
                    }
                    val result = inputStream.bufferedReader(Charsets.UTF_8).readText()
                    conn.disconnect()
                    return result
                } catch (e: Exception) {
                    e.printStackTrace()
                    return ""
                }
            }
        }

        // legado.encodeUrl(str)
        val encodeUrl = object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                if (args.isEmpty()) return ""
                return try {
                    URLEncoder.encode(args[0].toString(), "UTF-8")
                } catch (e: Exception) {
                    args[0]?.toString() ?: ""
                }
            }
        }

        // legado.resolveUrl(base, path)
        val resolveUrlFn = object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any {
                val b = args.getOrNull(0)?.toString() ?: baseUrl
                val p = args.getOrNull(1)?.toString() ?: ""
                return LegadoUtils.resolveUrl(b, p)
            }
        }

        legadoObj.put("getHttp", legadoObj, getHttp)
        legadoObj.put("getDom", legadoObj, getDom)
        legadoObj.put("postHttp", legadoObj, postHttp)
        legadoObj.put("encodeUrl", legadoObj, encodeUrl)
        legadoObj.put("resolveUrl", legadoObj, resolveUrlFn)

        // 注入到全局 scope
        scope?.put("legado", scope, legadoObj)
    }

    private fun parseSearchResult(result: Any?): List<OnlineBookItem> {
        if (result == null) return emptyList()
        if (result is NativeArray) {
            val list = mutableListOf<OnlineBookItem>()
            for (i in 0 until result.length.toInt()) {
                val item = result.get(i, result)
                if (item is NativeObject) {
                    list.add(parseOnlineBookItem(item))
                }
            }
            return list
        }
        if (result is NativeObject) {
            return listOf(parseOnlineBookItem(result))
        }
        return emptyList()
    }

    private fun parseOnlineBookItem(obj: NativeObject): OnlineBookItem {
        return OnlineBookItem(
            name = obj.get("name", obj)?.toString()
                ?: obj.get("title", obj)?.toString() ?: "",
            bookUrl = obj.get("bookUrl", obj)?.toString()
                ?: obj.get("url", obj)?.toString() ?: "",
            author = obj.get("author", obj)?.toString() ?: "",
            coverUrl = obj.get("coverUrl", obj)?.toString()
                ?: obj.get("cover", obj)?.toString() ?: "",
            tocUrl = obj.get("tocUrl", obj)?.toString()
                ?: obj.get("chapterUrl", obj)?.toString() ?: "",
            intro = obj.get("intro", obj)?.toString()
                ?: obj.get("description", obj)?.toString() ?: "",
            latestChapter = obj.get("latestChapter", obj)?.toString()
                ?: obj.get("lastChapter", obj)?.toString() ?: "",
            latestChapterUrl = obj.get("latestChapterUrl", obj)?.toString() ?: "",
            wordCount = obj.get("wordCount", obj)?.toString() ?: "",
            chapterCount = (obj.get("chapterCount", obj) as? Number)?.toInt() ?: 0,
            updateTime = obj.get("updateTime", obj)?.toString() ?: "",
            status = obj.get("status", obj)?.toString() ?: "",
            kind = obj.get("kind", obj)?.toString()
                ?: obj.get("type", obj)?.toString() ?: "",
        )
    }

    private fun parseBookInfoResult(result: Any?): Pair<Book, List<OnlineChapterInfo>> {
        if (result == null || result !is NativeObject) {
            return Book(id = "", title = "", source = "source:$sourceId") to emptyList()
        }

        val book = Book(
            id = "",
            title = result.get("name", result)?.toString()
                ?: result.get("title", result)?.toString() ?: "",
            author = result.get("author", result)?.toString() ?: "",
            coverUrl = result.get("coverUrl", result)?.toString()
                ?: result.get("cover", result)?.toString() ?: "",
            intro = result.get("intro", result)?.toString()
                ?: result.get("description", result)?.toString() ?: "",
            chapterCount = (result.get("chapterCount", result) as? Number)?.toInt() ?: 0,
            source = "source:$sourceId",
            sourceUrl = result.get("bookUrl", result)?.toString()
                ?: result.get("url", result)?.toString() ?: "",
            createdAt = currentTimeMillis(),
            lastReadAt = currentTimeMillis(),
        )

        val tocUrl = result.get("tocUrl", result)?.toString()
            ?: result.get("chapterUrl", result)?.toString() ?: ""
        val chapters = if (tocUrl.isNotBlank()) {
            chapterList(tocUrl)
        } else {
            emptyList()
        }

        return book to chapters
    }

    private fun parseChapterListResult(result: Any?): List<OnlineChapterInfo> {
        if (result == null) return emptyList()
        if (result is NativeArray) {
            val list = mutableListOf<OnlineChapterInfo>()
            for (i in 0 until result.length.toInt()) {
                val item = result.get(i, result)
                if (item is NativeObject) {
                    list.add(
                        OnlineChapterInfo(
                            name = item.get("name", item)?.toString()
                                ?: item.get("title", item)?.toString() ?: "",
                            url = item.get("url", item)?.toString()
                                ?: item.get("chapterUrl", item)?.toString() ?: "",
                            vip = item.get("vip", item)?.toString()?.lowercase() == "true",
                        )
                    )
                }
            }
            return list
        }
        if (result is NativeObject) {
            return listOf(
                OnlineChapterInfo(
                    name = result.get("name", result)?.toString()
                        ?: result.get("title", result)?.toString() ?: "",
                    url = result.get("url", result)?.toString()
                        ?: result.get("chapterUrl", result)?.toString() ?: "",
                )
            )
        }
        return emptyList()
    }
}
