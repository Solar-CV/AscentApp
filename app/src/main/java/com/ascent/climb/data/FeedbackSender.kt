package com.ascent.climb.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 反馈提交（Formspree）。
 *
 * 走 JSON 接口：`POST` 到表单端点，`Accept: application/json` 让 Formspree 回 JSON
 * 而不是重定向到它的「谢谢」页面。成功是 2xx，失败会带 `errors` 数组。
 *
 * 用 HttpURLConnection + org.json 手写，不引入 OkHttp/Retrofit：
 * 这个工程一直保持「无第三方运行时依赖」，为一次 POST 加两个库不划算。
 */
object FeedbackSender {

    /** Formspree 表单端点。换表单只改这一行。 */
    private const val ENDPOINT = "https://formspree.io/f/mrppzwqp"

    sealed interface Result {
        /** 已投递。 */
        data object Success : Result

        /** @param reason 给用户看的简短原因（同时写进日志） */
        data class Failure(val reason: String) : Result
    }

    /**
     * 提交一条反馈。[payload] 的 key 即 Formspree 后台看到的字段名。
     *
     * 网络调用整体切到 IO 线程；任何异常都折叠成 [Result.Failure]，
     * 由界面决定重试还是改用邮件 —— 不往上抛，避免把反馈页打崩。
     */
    suspend fun submit(payload: Map<String, String>): Result = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            val json = JSONObject().apply { payload.forEach { (k, v) -> put(k, v) } }
            conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) {
                Result.Success
            } else {
                // 4xx/5xx 的正文通常是 {"errors":[...]}，太长不展示，只取状态码
                Result.Failure("HTTP $code")
            }
        } catch (t: Throwable) {
            Result.Failure(t.message ?: t::class.java.simpleName)
        } finally {
            conn?.disconnect()
        }
    }
}
