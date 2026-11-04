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
}
