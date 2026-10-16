package com.ascent.climb.data.bank

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** 在线 manifest。 */
data class RemoteManifest(
    val version: Int,
    val file: String,
    val sha256: String?,
    val minAppVersion: Int,
    val updatedAt: String
)

/** 检查更新的结果。 */
sealed interface UpdateCheck {
    /** 有新版本可用（已下载但尚未安装）。 */
    data class Downloaded(val version: Int, val updatedAt: String, val questionCount: Int) : UpdateCheck

    /** 已是最新。 */
    data class UpToDate(val version: Int) : UpdateCheck

    /** 在线题库要求的 App 版本更高。 */
    data class NeedAppUpgrade(val requiredAppVersion: Int) : UpdateCheck

    /** 检查失败（网络、格式、校验等）。 */
    data class Failed(val message: String) : UpdateCheck
}

/**
 * 题库的远端访问层。
 * 只用 JDK 自带的 HttpURLConnection，不引入任何网络库。
 */
class BankRemote {

    /** 下载并解析 manifest；不存在时返回 null（调用方会退化为直接取 bank.json）。 */
    suspend fun fetchManifest(baseUrl: String): RemoteManifest? = withContext(Dispatchers.IO) {
        val text = httpGet(join(baseUrl, "manifest.json")) ?: return@withContext null
        try {
            val o = JSONObject(text)
            RemoteManifest(
                version = o.optInt("version", 0),
                file = o.optString("file", "bank.json"),
                sha256 = o.optString("sha256", "").ifEmpty { null },
                minAppVersion = o.optInt("minAppVersion", 0),
                updatedAt = o.optString("updatedAt", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    /** 直接下载 bank.json。 */
    suspend fun fetchBank(baseUrl: String, file: String = "bank.json"): String? =
        withContext(Dispatchers.IO) {
            httpGet(join(baseUrl, file))
        }

    suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) { httpGet(url) }

    /** 计算文本的 sha256（十六进制小写）。 */
    fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(Character.forDigit(v shr 4, 16))
            sb.append(Character.forDigit(v and 0x0F, 16))
        }
        return sb.toString()
    }

    private fun join(base: String, path: String): String {
        val b = if (base.endsWith("/")) base.dropLast(1) else base
        return "$b/$path"
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = BankConfig.TIMEOUT_MS
                readTimeout = BankConfig.TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (conn.responseCode !in 200..299) return null

            // 先看 Content-Length，超限直接放弃
            val declared = conn.contentLengthLong
            if (declared > BankConfig.MAX_BYTES) return null

            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                val buf = CharArray(8192)
                var total = 0
                while (true) {
                    val n = reader.read(buf)
                    if (n <= 0) break
                    total += n
                    if (total > BankConfig.MAX_BYTES) return null
                    sb.append(buf, 0, n)
                }
            }
            sb.toString()
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
