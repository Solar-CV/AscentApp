package com.ascent.climb.data.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 头像的云端往返。
 *
 * 存云端而不是只存本机，是为了换机登录后能直接恢复 —— 头像属于「人」，不属于设备。
 * 有本地文件时优先用本地的：离线期间换的头像，下次同步前不该被云端旧图覆盖。
 */
class CloudProfile(private val context: Context) {

    private val api = CloudApi(context)

    /** 当前账号的公开资料（昵称、头像版本）。未登录或失败返回 null。 */
    suspend fun me(): JSONObject? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        if (!api.hasSession()) return@withContext null
        runCatching { api.json("GET", "/auth/me")?.optJSONObject("user") }.getOrNull()
    }

    /** 上传成功返回新版本号；未登录或失败返回 null。 */
    suspend fun uploadAvatar(jpeg: ByteArray): Int? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        if (jpeg.isEmpty() || jpeg.size > CloudConfig.MAX_AVATAR_BYTES) return@withContext null
        if (!api.hasSession()) return@withContext null
        runCatching {
            api.uploadJpeg("/avatar", jpeg)?.optInt("version", 0)
        }.getOrNull()?.takeIf { it > 0 }
    }

    /** 拉取云端头像；没有或失败返回 null。 */
    suspend fun downloadAvatar(): ByteArray? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        if (!api.hasSession()) return@withContext null
        runCatching { api.download("/avatar") }.getOrNull()
    }

    /** 删除云端头像。 */
    suspend fun deleteAvatar(): Boolean = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext false
        if (!api.hasSession()) return@withContext false
        runCatching { api.json("DELETE", "/avatar") }.isSuccess
    }

    /** 已登录设备列表。未登录或失败返回 null（区别于「登录了但只有一台设备」的空列表）。 */
    suspend fun devices(): List<CloudDevice>? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        if (!api.hasSession()) return@withContext null
        runCatching {
            val arr = api.json("GET", "/me/devices")?.optJSONArray("devices") ?: return@runCatching emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                CloudDevice(
                    id = o.optString("id", ""),
                    platform = o.optString("platform", ""),
                    appVersion = o.optString("app_version", ""),
                    createdAt = o.optLong("created_at", 0),
                    lastUsedAt = o.optLong("last_used_at", 0),
                    current = o.optBoolean("current", false)
                )
            }.filter { it.id.isNotBlank() }
        }.getOrNull()
    }

    /**
     * 撤销某台设备的会话。
     *
     * @return null 表示请求失败；否则返回「撤销的是不是当前设备」。
     *         撤的若是自己，调用方必须清掉本机会话并退回未登录 —— 服务端已经把
     *         access / refresh 都标记失效，留着登录态只会让下一次请求收到 401。
     */
    suspend fun revokeDevice(id: String): Boolean? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        if (!api.hasSession()) return@withContext null
        runCatching {
            api.json("DELETE", "/me/devices/$id")?.optBoolean("self", false) ?: false
        }.getOrNull()
    }
}

/** 一台已登录设备。字段与 `/me/devices` 一一对应，没有硬件标识。 */
data class CloudDevice(
    val id: String,
    val platform: String,
    val appVersion: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val current: Boolean
)
