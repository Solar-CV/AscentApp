package com.ascent.climb.data.bank

import android.content.Context
import com.ascent.climb.BuildConfig
import com.ascent.climb.data.Lesson
import com.ascent.climb.data.Track
import com.ascent.climb.data.Unit
import com.ascent.climb.data.buildBundledTracks
import java.io.File

/**
 * 题库仓库 —— App 的「题库区块」唯一入口。
 *
 * 三级来源，优先级由高到低：
 *   1. 本地缓存副本（之前从在线题库下载并校验通过的 JSON）
 *   2. App 内置题库（随 APK 一起编译进来，永远可用）
 *
 * 在线 JSON 只做「增量更新」：有网时检查版本，比本地新才下载覆盖缓存；
 * 拉取失败、校验不通过、版本更旧，一律保留现状。因此离线行为与纯离线 App 完全一致。
 */
object BankRepository {

    private const val PREFS = "ascent_bank"
    private const val KEY_CACHE_VERSION = "cache_version"
    private const val KEY_CACHE_UPDATED = "cache_updated"
    private const val KEY_CUSTOM_URL = "custom_url"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LAST_RESULT = "last_result"
    private const val CACHE_FILE = "bank_cache.json"

    private val remote = BankRemote()

    private var appContext: Context? = null

    private var tracks: List<Track> = emptyList()
    private var units: List<Unit> = emptyList()
    private var lessons: List<Lesson> = emptyList()
    private var version: Int = 0
    private var sourceLabel: String = "内置题库"
    private var updatedAt: String = ""

    /** 最近一次检查更新的结果描述，用于界面展示。 */
    var lastResult: String = ""
        private set

    val allTracks: List<Track> get() = tracks
    val allUnits: List<Unit> get() = units
    val allLessons: List<Lesson> get() = lessons
    val bankVersion: Int get() = version
    val sourceDescription: String get() = sourceLabel
    val bankUpdatedAt: String get() = updatedAt
    val totalQuestions: Int get() = lessons.sumOf { it.questions.size }

    /** 当前使用的在线题库地址（可在开发者模式中修改）。 */
    val baseUrl: String
        get() {
            val c = appContext ?: return BankConfig.DEFAULT_BASE_URL
            val custom = prefs(c).getString(KEY_CUSTOM_URL, "") ?: ""
            return custom.ifBlank { BankConfig.DEFAULT_BASE_URL }
        }

    fun setBaseUrl(url: String) {
        val c = appContext ?: return
        prefs(c).edit().putString(KEY_CUSTOM_URL, url.trim()).apply()
    }

    /** 是否配置了可用的在线地址。 */
    val hasRemoteConfigured: Boolean
        get() = baseUrl.isNotBlank() && !baseUrl.contains("example.com")

    /** 是否需要自动检查（距上次成功检查超过 6 小时）。避免每次启动都发请求。 */
    fun shouldAutoCheck(): Boolean {
        val c = appContext ?: return false
        val last = prefs(c).getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last > BankConfig.AUTO_CHECK_INTERVAL_MS
    }

    /** 记录一次检查时间（手动检查时也调用）。 */
    fun touchCheckTime() {
        val c = appContext ?: return
        prefs(c).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    /**
     * 启动时调用（同步）。优先载入本地缓存副本，没有则使用内置题库。
     * 内置题库由 Content.buildBundledUnits() 构建，保证与旧版本行为一致。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        val c = appContext!!
        lastResult = prefs(c).getString(KEY_LAST_RESULT, "") ?: ""

        // 已经载入过就不重复解析（Activity 重建时会再次调用）
        if (units.isNotEmpty()) return

        val cacheFile = File(c.filesDir, CACHE_FILE)
        if (cacheFile.exists()) {
            val cachedVersion = prefs(c).getInt(KEY_CACHE_VERSION, 0)
            val payload = runCatching { BankJson.decode(cacheFile.readText()) }.getOrNull()
            if (payload != null && payload.tracks.isNotEmpty()) {
                install(payload.tracks, payload.version, "在线题库 v${payload.version}", payload.updatedAt)
                return
            }
            // 缓存损坏：丢弃，回退内置
            cacheFile.delete()
            prefs(c).edit().remove(KEY_CACHE_VERSION).remove(KEY_CACHE_UPDATED).apply()
        }
        install(buildBundledTracks(), BankConfig.BUNDLED_VERSION, "内置题库", "")
    }

    /** 恢复为内置题库（开发者模式 / 排障用）。 */
    fun resetToBundled() {
        val c = appContext ?: return
        File(c.filesDir, CACHE_FILE).delete()
        prefs(c).edit().remove(KEY_CACHE_VERSION).remove(KEY_CACHE_UPDATED).apply()
        install(buildBundledTracks(), BankConfig.BUNDLED_VERSION, "内置题库", "")
        lastResult = "已恢复内置题库"
        prefs(c).edit().putString(KEY_LAST_RESULT, lastResult).apply()
    }

    /**
     * 检查在线题库是否有更新。
     * 若比本地新，会下载、校验并直接切换为当前题库（同时写入缓存供下次启动使用）。
     */
    suspend fun checkForUpdate(): UpdateCheck {
        val c = appContext ?: return UpdateCheck.Failed("尚未初始化")
        val base = baseUrl
        if (base.isBlank()) return fail(c, "未配置在线题库地址")

        // 1) 优先取 manifest（小文件，只为了比版本）
        val manifest = remote.fetchManifest(base)

        if (manifest != null && manifest.minAppVersion > BuildConfig.VERSION_CODE) {
            fail(c, "在线题库需要 App v${manifest.minAppVersion} 以上")
            return UpdateCheck.NeedAppUpgrade(manifest.minAppVersion)
        }

        val manifestVersion = manifest?.version ?: 0
        val fileName = manifest?.file ?: "bank.json"

        // 2) manifest 版本不比本地新就先不下载大文件
        if (manifest != null && manifestVersion in 1..version) {
            succeed(c, "已是最新（v${version}）")
            return UpdateCheck.UpToDate(version)
        }

        // 3) 下载题库
        val text = remote.fetchBank(base, fileName)
            ?: return fail(c, "无法下载题库文件")

        // 4) 校验
        manifest?.sha256?.let { expected ->
            val actual = remote.sha256(text)
            if (!actual.equals(expected, ignoreCase = true)) {
                return fail(c, "题库校验失败（sha256 不匹配）")
            }
        }

        // 5) 解析
        val payload = BankJson.decode(text)
            ?: return fail(c, "题库格式不正确，已保留原题库")
        if (payload.schema > BankJson.SCHEMA) {
            return fail(c, "题库 schema ${payload.schema} 高于本机支持（${BankJson.SCHEMA}）")
        }
        if (payload.minAppVersion > BuildConfig.VERSION_CODE) {
            return UpdateCheck.NeedAppUpgrade(payload.minAppVersion)
        }
        if (payload.version <= version) {
            succeed(c, "已是最新（v${version}）")
            return UpdateCheck.UpToDate(version)
        }

        // 6) 落盘 + 切换
        return try {
            File(c.filesDir, CACHE_FILE).writeText(text)
            prefs(c).edit()
                .putInt(KEY_CACHE_VERSION, payload.version)
                .putString(KEY_CACHE_UPDATED, payload.updatedAt)
                .apply()
            install(payload.tracks, payload.version, "在线题库 v${payload.version}", payload.updatedAt)
            succeed(c, "已更新到 v${payload.version}")
            UpdateCheck.Downloaded(payload.version, payload.updatedAt, payload.questionCount)
        } catch (e: Exception) {
            fail(c, "写入缓存失败：${e.message}")
        }
    }

    private fun install(newTracks: List<Track>, v: Int, label: String, updated: String) {
        tracks = newTracks
        units = newTracks.flatMap { it.units }
        lessons = units.flatMap { it.lessons }
        version = v
        sourceLabel = label
        updatedAt = updated
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun fail(c: Context, msg: String): UpdateCheck {
        lastResult = msg
        prefs(c).edit().putString(KEY_LAST_RESULT, msg).apply()
        return UpdateCheck.Failed(msg)
    }

    private fun succeed(c: Context, msg: String) {
        lastResult = msg
        prefs(c).edit()
            .putString(KEY_LAST_RESULT, msg)
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }
}
