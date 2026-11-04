package com.ascent.climb.data.cloud

import android.content.Context
import com.ascent.climb.data.ProgressStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 一次同步的结果。界面只区分这几类，不需要知道 HTTP 状态码。 */
sealed interface SyncOutcome {
    /** 成功，服务端版本号推进到 [rev]。 */
    data class Synced(val rev: Int) : SyncOutcome

    /** 没登录或会话已失效，静默跳过。 */
    object NotLoggedIn : SyncOutcome

    /** 网络不可用。不是错误，恢复网络后下次接着来。 */
    object Offline : SyncOutcome

    /** 服务端异常或多次冲突。 */
    data class Failed(val code: String) : SyncOutcome
}

/**
 * 进度同步。
 *
 * 流程（CLOUD-PLAN.md §3.2）：
 *   GET /state → 本地与云端 merge → PUT /state（乐观锁 rev）
 *   409 说明别的设备刚写过，用响应里带回的最新状态重新合并重试。
 *
 * 因为 merge 是幂等且单调的，重试一定收敛；三次还不成就放弃，
 * 本地数据不动，下次再同步 —— **绝不用服务端覆盖本地未合并的内容**。
 *
 * access 过期不在这里处理：交给 [CloudApi] 自动刷新，这里只看业务结果。
 */
class SyncEngine(
    private val context: Context,
    private val store: ProgressStore
) {

    private val api = CloudApi(context)

    /** 手动同步：登录态下总是执行。 */
    suspend fun sync(): SyncOutcome = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext SyncOutcome.NotLoggedIn
        if (!api.hasSession()) return@withContext SyncOutcome.NotLoggedIn
        try {
            var rev = 0
            var remote: SyncPayload? = null

            repeat(MAX_ATTEMPTS) { attempt ->
                if (attempt == 0) {
                    val state = api.json("GET", "/state")
                    rev = state?.optInt("rev", 0) ?: 0
                    remote = state?.optJSONObject("payload")?.let { SyncPayload.fromJson(it) }
                }

                val local = store.snapshot()
                val merged = remote?.let { local.merge(it) } ?: local

                // 云端已经是这份内容，不必白写一次
                if (remote != null && merged == remote) {
                    commit(merged, rev)
                    return@withContext SyncOutcome.Synced(rev)
                }

                try {
                    val res = api.json(
                        "PUT", "/state",
                        JSONObject().put("rev", rev).put("payload", merged.toJson())
                    )
                    val newRev = res?.optInt("rev", rev + 1) ?: (rev + 1)
                    commit(merged, newRev)
                    return@withContext SyncOutcome.Synced(newRev)
                } catch (e: CloudException) {
                    if (e.status != 409) throw e
                    // 别的设备抢先写了：拿它的最新状态再合一次
                    rev = e.body?.optInt("rev", 0) ?: 0
                    remote = e.body?.optJSONObject("payload")?.let { SyncPayload.fromJson(it) }
                }
            }
            SyncOutcome.Failed("conflict")
        } catch (e: CloudException) {
            when {
                e.isNetwork -> SyncOutcome.Offline
                e.code == "unauthorized" || e.code == "invalid_token" -> SyncOutcome.NotLoggedIn
                else -> SyncOutcome.Failed(e.code)
            }
        }
    }

    /**
     * 启动 / 回到前台时的自动同步：距上次成功不足间隔就跳过。
     * 返回 null 表示「没必要同步」，调用方不用处理。
     */
    suspend fun syncIfDue(): SyncOutcome? {
        if (!CloudConfig.enabled) return null
        if (!api.hasSession()) return null
        if (System.currentTimeMillis() - store.lastSyncAt < CloudConfig.AUTO_SYNC_INTERVAL_MS) return null
        return sync()
    }

    private fun commit(merged: SyncPayload, rev: Int) {
        store.applySnapshot(merged)
        store.lastSyncRev = rev
        store.lastSyncAt = System.currentTimeMillis()
        // 合并后可能刚好满足某个成就条件：入队即可，弹窗由界面层在合适的时机播
        store.grantAchievements()
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
    }
}
