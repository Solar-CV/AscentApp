package com.ascent.climb.data.bank

/**
 * 题库相关配置（纯常量，无任何依赖）。
 * 单独成文件，便于 tools/contentcheck 直接复用版本号。
 */
object BankConfig {

    /**
     * 在线题库根地址。把 `manifest.json` 与 `bank.json` 放在这个目录下即可。
     *
     * 支持两类部署：
     *  1) 只放 bank.json —— 检查更新时整体下载后比较 version（简单，但每次检查都要传完整文件）
     *  2) 同时放 manifest.json —— 先取小文件比较版本，需要更新时才下载 bank.json（推荐）
     *
     * 可在 App 的开发者模式中临时改成别的地址做测试。
     */
    const val DEFAULT_BASE_URL = "https://example.com/ascent-bank"

    /** 内置题库版本号。每次改动内置内容（Unit1~5 / Expansion* / NewLessons）都应该 +1。 */
    const val BUNDLED_VERSION = 15

    /** 在线题库要求的 App 版本下限（写入导出文件的 minAppVersion 字段）。 */
    const val MIN_APP_VERSION = 6

    /** 网络超时（毫秒）。 */
    const val TIMEOUT_MS = 12_000

    /** 题库文件体积上限，防止异常文件撑爆内存。 */
    const val MAX_BYTES = 6 * 1024 * 1024

    /** 自动检查更新间隔（毫秒），默认 6 小时。 */
    const val AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
}
