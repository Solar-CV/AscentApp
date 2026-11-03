package com.ascent.climb.data.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 头像的云端往返。
 *
 * 存云端而不是只存本机，是为了换机登录后能直接恢复 —— 头像属于「人」，不属于设备。
 * 有本地文件时优先用本地的：离线期间换的头像，下次同步前不该被云端旧图覆盖。
 */
class CloudProfile(private val context: Context) {

    private val http = CloudHttp(CloudConfig.BASE_URL)

    /** 上传成功返回新版本号；未登录或失败返回 null。 */
    suspend fun uploadAvatar(jpeg: ByteArray): Int? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        if (jpeg.isEmpty() || jpeg.size > CloudConfig.MAX_AVATAR_BYTES) return@withContext null
        val token = TokenStore.load(context) ?: return@withContext null
        runCatching {
            http.uploadJpeg("/avatar", token, jpeg)?.optInt("version", 0)
        }.getOrNull()?.takeIf { it > 0 }
    }

    /** 拉取云端头像；没有或失败返回 null。 */
    suspend fun downloadAvatar(): ByteArray? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        val token = TokenStore.load(context) ?: return@withContext null
        runCatching { http.download("/avatar", token) }.getOrNull()
    }

    /** 删除云端头像。 */
    suspend fun deleteAvatar(): Boolean = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext false
        val token = TokenStore.load(context) ?: return@withContext false
        runCatching { http.call("DELETE", "/avatar", token) }.isSuccess
    }
}
