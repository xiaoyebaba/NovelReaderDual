package com.richardyap.novelreader.util

/**
 * 编码检测工具。
 * 用于自动识别导入的 TXT 文件编码（UTF-8 / GBK / GB2312 等）。
 * 在 commonMain 中提供纯算法实现，平台实现层负责读取原始字节。
 */
object EncodingDetector {

    /**
     * 根据字节 BOM 和内容模式猜测编码名称。
     * @param bytes 文件头部字节（至少需要前 4 个字节）
     */
    fun guessEncoding(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) return "UTF-8"

        if (bytes.size >= 2) {
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return "UTF-16BE"
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return "UTF-16LE"
        }

        if (bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
            bytes[2] == 0xFE.toByte() && bytes[3] == 0xFF.toByte()
        ) return "UTF-32BE"

        // 启发式检测：GBK/GB2312 高字节特征
        var gbkScore = 0
        var utf8Score = 0
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b < 0x80 -> { i++ }
                b in 0xC0..0xDF -> {
                    if (i + 1 < bytes.size) {
                        val b2 = bytes[i + 1].toInt() and 0xFF
                        if (b2 in 0x80..0xBF) utf8Score += 2 else gbkScore++
                    }
                    i += 2
                }
                b in 0xE0..0xEF -> {
                    if (i + 2 < bytes.size) {
                        val b2 = bytes[i + 1].toInt() and 0xFF
                        val b3 = bytes[i + 2].toInt() and 0xFF
                        if (b2 in 0x80..0xBF && b3 in 0x80..0xBF) utf8Score += 3 else gbkScore++
                    }
                    i += 3
                }
                b in 0xF0..0xF7 -> {
                    if (i + 3 < bytes.size) utf8Score += 4
                    i += 4
                }
                else -> { gbkScore++; i++ }
            }
        }
        return if (utf8Score >= gbkScore) "UTF-8" else "GBK"
    }
}
