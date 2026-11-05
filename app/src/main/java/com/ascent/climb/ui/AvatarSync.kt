package com.ascent.climb.ui

import android.content.Context
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.cloud.CloudConfig
import com.ascent.climb.data.cloud.CloudProfile
import com.ascent.climb.data.cloud.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 账号资料（昵称 + 头像）与云端之间的摆渡。
 *
 * 为什么单独一个文件：`CloudProfile` 只负责 HTTP（data 层不该依赖 `filesDir` 和 Bitmap），
 * 而「下载 → 落盘 → 触发重新解码」必须同时知道云端与本地文件。这个夹缝由 UI 层补。
 *
 * 冲突规则：
 *  - **头像**：本地改过（`avatarDirty`）就推上去，否则云端版本更新才下载。
 *    不用「版本号比大小」判断本地是否改过 —— 下载时也要 +1 触发重新解码，
 *    两个计数器必然错位，会变成「刚下载完又推一次」的来回打架。
 *  - **昵称**：账号是权威。本地没有未上传的改动（`displayName == syncedNickname`）时，
 *    直接接受账号里的名字；这样在别的设备改名，这边下次同步就能看到。
 */
object AvatarSync {

    /**
     * 拉齐一次账号资料。
     * @return 本地头像有没有被换掉（换了要重读头像，调用方据此刷新界面）
     */
    suspend fun reconcile(context: Context, store: ProgressStore): Boolean = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext false
        if (!TokenStore.hasSession(context)) return@withContext false

        val profile = CloudProfile(context)
        val me = runCatching { profile.me() }.getOrNull() ?: return@withContext false

        // ---- 归属 ----
        // 本机头像是不是别人的？退出登录时已经清过一次，这里是安全网：
        // 会话过期后直接换号、或哪条路径漏清，都能在这里发现并纠正。
        val accountId = me.optString("id", "")
        if (accountId.isNotEmpty()) {
            if (store.avatarOwner.isNotEmpty() && store.avatarOwner != accountId) {
                forgetLocal(context, store)
            }
            store.avatarOwner = accountId
        }

        // ---- 昵称 ----
        val nickname = me.optString("nickname", "").trim()
        if (nickname.isNotEmpty()) {
            // syncedNickname 为空 = 这台设备还没同步过（老版本升级上来），直接采纳账号名；
            // 只有「同步过、之后又本地改过还没推上去」才保护本地值。
            val localDirty = store.syncedNickname.isNotEmpty() && store.displayName != store.syncedNickname
            if (!localDirty && store.displayName != nickname) {
                store.displayName = nickname
            }
            store.syncedNickname = nickname
        }

        // ---- 头像 ----
        if (store.avatarDirty) {
            if (!AvatarStore.exists(context)) {
                // 标了脏但文件没了（删了又没同步成功）：当作没有头像
                store.avatarDirty = false
                store.cloudAvatarVersion = 0
                return@withContext false
            }
            val version = push(context, store)
            if (version != null) store.avatarDirty = false
            return@withContext false
        }

        val remoteVersion = me.optInt("avatarVersion", 0)
        if (remoteVersion <= 0) {
            // 账号里没有头像：可能是新账号，也可能是别的设备刚把它删了。
            // 本机如果还留着一张，就是过期的，清掉并 +1 让界面回到占位图。
            store.cloudAvatarVersion = 0
            if (!AvatarStore.exists(context)) return@withContext false
            AvatarStore.remove(context)
            store.avatarVersion = store.avatarVersion + 1
            return@withContext true
        }
        if (remoteVersion <= store.cloudAvatarVersion) return@withContext false

        val bytes = runCatching { profile.downloadAvatar() }.getOrNull()
        if (bytes == null) {
            // 云端说有这么个版本但取不到（404/网络）：先记下版本，免得每次同步都重试
            store.cloudAvatarVersion = remoteVersion
            return@withContext false
        }
        if (!AvatarStore.writeBytes(context, bytes)) return@withContext false

        store.avatarVersion = store.avatarVersion + 1
        store.cloudAvatarVersion = remoteVersion
        true
    }

    /**
     * 丢掉本机头像与所有相关计数。退出登录、撤销当前设备、换账号时调用。
     *
     * 版本号 **+1 而不是归零**：它是「重新解码」的开关，必须相对上一次组合可见的值变化。
     * 归零会让「A 版本 1 → 清空 → B 版本 1」撞回同一个 key，界面停在旧图上 ——
     * 正是这次要修的那个 bug。文件已删，重新解码的结果自然是占位图。
     */
    fun forgetLocal(context: Context, store: ProgressStore) {
        AvatarStore.remove(context)
        store.avatarVersion = store.avatarVersion + 1
        store.cloudAvatarVersion = 0
        store.avatarDirty = false
        store.avatarOwner = ""
    }

    /** 把本地头像推上去。成功返回云端版本号，未登录/失败返回 null。 */
    suspend fun push(context: Context, store: ProgressStore): Int? = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext null
        val bytes = AvatarStore.readBytes(context) ?: return@withContext null
        val version = CloudProfile(context).uploadAvatar(bytes) ?: return@withContext null
        store.cloudAvatarVersion = version
        version
    }

    /** 本地删了头像，云端也删。 */
    suspend fun removeRemote(context: Context, store: ProgressStore): Boolean = withContext(Dispatchers.IO) {
        if (!CloudConfig.enabled) return@withContext false
        val ok = CloudProfile(context).deleteAvatar()
        if (ok) {
            store.cloudAvatarVersion = 0
            store.avatarDirty = false
        }
        ok
    }
}
