package com.ascent.climb.data.cloud

/**
 * 云端开关与地址。
 *
 * [BASE_URL] 留空表示**云端未启用**：登录退回本地模拟、不显示同步入口，
 * 行为与本机离线版完全一致。部署好服务端后把网关地址填进来即可，
 * 不需要改任何界面代码。
 */
object CloudConfig {

    /** 例如 "https://xxxx.service.tcloudbase.com/ascent"。末尾不要带斜杠。 */
    const val BASE_URL = ""

    val enabled: Boolean get() = BASE_URL.isNotBlank()

    const val TIMEOUT_MS = 15_000

    /** 上限与 CLOUD-PLAN.md §3.2 对齐，超出直接判定失败，不浪费流量。 */
    const val MAX_STATE_BYTES = 512 * 1024
    const val MAX_AVATAR_BYTES = 512 * 1024

    /** 自动同步的最小间隔：太频繁既费电也没意义。 */
    const val AUTO_SYNC_INTERVAL_MS = 5 * 60 * 1000L
}
