package com.ascent.climb.data.cloud

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * 带登录态的 HTTP 入口。
 *
 * 每个需要鉴权的请求都从这里走，它负责两件事：
 *  1. 自动带上 access token；
 *  2. access 过期（401 `token_expired` / `unauthorized`）时**自动刷新一次**再重试。
 *
 * refresh 不轮换（CLOUD-PLAN.md §10.1 A4），所以并发刷新是安全的 —— 最多多签几个
 * access，不会把别人已经拿到的会话作废。同步层和头像层因此不需要各自处理 401。
 */
class CloudApi(private val context: Context) {

    private val http = CloudHttp(CloudConfig.BASE_URL)
    private val refreshMutex = Mutex()

    suspend fun json(method: String, path: String, body: JSONObject? = null): JSONObject? =
        withAccess { token -> http.call(method, path, token, body) }

    suspend fun uploadJpeg(path: String, bytes: ByteArray): JSONObject? =
        withAccess { token -> http.uploadJpeg(path, token, bytes) }

    suspend fun download(path: String): ByteArray? =
        withAccess { token -> http.download(path, token) }

    /** 本地是否还持有可用的登录态（refresh 有效即算）。 */
    fun hasSession(): Boolean = TokenStore.hasSession(context)

    private suspend fun <T> withAccess(block: suspend (String) -> T): T {
        // access 缺失但 refresh 还在（例如只解出了后一半），先换一次再放弃
        val token = TokenStore.access(context) ?: refreshAccess()
            ?: throw CloudException("unauthorized", 401)
        return try {
            block(token)
        } catch (e: CloudException) {
            if (!e.isExpiredAccess) throw e
            val fresh = refreshAccess() ?: throw CloudException("unauthorized", 401)
            block(fresh)
        }
    }

    private suspend fun refreshAccess(): String? = refreshMutex.withLock {
        val refresh = TokenStore.refresh(context) ?: return@withLock null
        val res = try {
            http.call("POST", "/auth/refresh", body = JSONObject().put("refresh_token", refresh))
        } catch (e: CloudException) {
            // 只有服务端明确说这个 refresh 不认了才清本地；网络问题原样抛出，
            // 交给同步层判成「离线」，否则一次断网就会把用户登出。
            if (e.status == 401) {
                TokenStore.clear(context)
                return@withLock null
            }
            throw e
        }
        val access = res?.optString("access_token", "")?.takeIf { it.isNotBlank() } ?: return@withLock null
        TokenStore.save(context, access, refresh)
        access
    }
}

/** access 过期（可以刷新）与彻底无效（要重新登录）在这里区分。 */
private val CloudException.isExpiredAccess: Boolean
    get() = status == 401 && (code == "token_expired" || code == "unauthorized")
