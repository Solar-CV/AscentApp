package com.ascent.climb.data.cloud

/**
 * 云端开关与地址。
 *
 * [BASE_URL] 留空表示**云端未启用**：登录退回本地模拟、不显示同步入口，
 * 行为与本机离线版完全一致。部署好服务端后把网关地址填进来即可，
 * 不需要改任何界面代码。
 */
object CloudConfig {

    /** CloudBase 网关默认域名（上海 / 个人版 / ascent-app 环境）。末尾不要带斜杠。 */
    const val BASE_URL = "https://ascent-app-d1g4gt73867789064-1503487069.ap-shanghai.app.tcloudbase.com"

    val enabled: Boolean get() = BASE_URL.isNotBlank()

    /**
     * Google 登录的 **Web client ID**（不是 Android client ID）。
     *
     * 客户端把它当 `serverClientId` 传进去，拿到的 idToken 里 `aud` 就是它；
     * 服务端 `GOOGLE_CLIENT_ID` 必须是同一个值，否则验票必然失败。
     * 这是公开标识，不是密钥，写进代码没有问题。
     */
    const val GOOGLE_WEB_CLIENT_ID =
        "123533249290-8n1l8d0foaph4pbo511n01jd7b1e60rq.apps.googleusercontent.com"

    const val TIMEOUT_MS = 15_000

    /** 上限与 CLOUD-PLAN.md §3.2 对齐，超出直接判定失败，不浪费流量。 */
    const val MAX_STATE_BYTES = 512 * 1024
    const val MAX_AVATAR_BYTES = 512 * 1024

    /**
     * 自动同步间隔：启动时先跑一次，之后每小时一次。
     * 太频繁既费电也没意义（进度是「练一课」级别的变化，不是秒级）。
     */
    const val AUTO_SYNC_INTERVAL_MS = 60 * 60 * 1000L
}
