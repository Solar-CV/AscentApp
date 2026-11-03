package com.ascent.climb.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 云端 API 的最小 HTTP 客户端。
 *
 * 沿用 `data/bank/BankRemote` 的做法：只用 JDK 自带的 HttpURLConnection，
 * 不引 OkHttp / Retrofit。请求量很小（登录、同步、头像），不值得多一个依赖。
 *
 * 失败统一抛 [CloudException]，界面只需要认 [CloudException.code]：
 *  - `network`  连不上 / 超时（离线时最常见，不是错误，是常态）
 *  - `unauthorized` token 失效，应清登录态
 *  - 其余是服务端契约里的错误码（见 CLOUD-PLAN.md §3）
 */
class CloudException(
    val code: String,
    val status: Int = 0,
    /** 409 冲突时服务端会带回最新状态，这里原样留着，让同步层直接合并重试。 */
    val body: JSONObject? = null
) : Exception(code) {

    /** 网络层问题：离线、DNS、超时。调用方通常静默处理，不弹错。 */
    val isNetwork: Boolean get() = code == "network"
}

class CloudHttp(private val baseUrl: String) {

    suspend fun call(
        method: String,
        path: String,
        token: String? = null,
        body: JSONObject? = null
    ): JSONObject? = withContext(Dispatchers.IO) {
        val conn = open(method, path, token)
        try {
            if (body != null) {
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            readJson(conn)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun uploadJpeg(path: String, token: String, bytes: ByteArray): JSONObject? =
        withContext(Dispatchers.IO) {
            val conn = open("PUT", path, token)
            try {
                conn.setRequestProperty("Content-Type", "image/jpeg")
                conn.doOutput = true
                conn.outputStream.use { it.write(bytes) }
                readJson(conn)
            } finally {
                conn.disconnect()
            }
        }

    suspend fun download(path: String, token: String): ByteArray? = withContext(Dispatchers.IO) {
        val conn = open("GET", path, token)
        try {
            val status = conn.responseCode
            if (status == 404) return@withContext null
            if (status !in 200..299) throw errorFor(conn, status)
            conn.inputStream.use { it.readBytes() }
        } catch (e: IOException) {
            throw CloudException("network")
        } finally {
            conn.disconnect()
        }
    }

    private fun open(method: String, path: String, token: String?): HttpURLConnection {
        val url = URL(baseUrl.trimEnd('/') + path)
        return try {
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CloudConfig.TIMEOUT_MS
                readTimeout = CloudConfig.TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Ascent-Android")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
            }
        } catch (e: IOException) {
            throw CloudException("network")
        }
    }

    private fun readJson(conn: HttpURLConnection): JSONObject? {
        val status = try {
            conn.responseCode
        } catch (e: IOException) {
            throw CloudException("network")
        }
        val text = try {
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: IOException) {
            throw CloudException("network")
        }
        if (status !in 200..299) throw errorFor(status, text)
        if (text.isBlank()) return null
        return try {
            JSONObject(text)
        } catch (e: Exception) {
            throw CloudException("bad_response", status)
        }
    }

    private fun errorFor(conn: HttpURLConnection, status: Int): CloudException {
        val text = runCatching {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }.getOrDefault("")
        return errorFor(status, text)
    }

    private fun errorFor(status: Int, text: String): CloudException {
        val parsed = runCatching { JSONObject(text) }.getOrNull()
        val code = parsed?.optString("error", "") ?: ""
        return CloudException(
            code.ifBlank { if (status == 401) "unauthorized" else "server_error" },
            status,
            parsed
        )
    }
}
