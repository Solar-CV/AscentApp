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
/**
 * 题库状态提示的语义码。
 *
 * 数据层不拼接面向用户的文案：这里只记录「要说什么」，
 * 由 UI 层按当前语言查字符串资源（见 ui/BankText.kt）。
 * 语言切换后提示也会跟着变。
 */
data class BankMsg(
    val code: String,
    val n1: Int = 0,
    val n2: Int = 0,
    val s1: String = ""
)

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
    private var sourceKindValue: String = "bundled"
    private var updatedAt: String = ""

    /** 最近一次检查更新的结果，用于界面展示（由 UI 按当前语言解析）。 */
    var lastResult: BankMsg? = null
        private set

    val allTracks: List<Track> get() = tracks
    val allUnits: List<Unit> get() = units
    val allLessons: List<Lesson> get() = lessons
    val bankVersion: Int get() = version
    /** 题库来源："bundled" 或 "online"，由 UI 层翻译成当前语言。 */
    val sourceKind: String get() = sourceKindValue
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
        lastResult = decodeMsg(prefs(c).getString(KEY_LAST_RESULT, "") ?: "")

        // 已经载入过就不重复解析（Activity 重建时会再次调用）
        if (units.isNotEmpty()) return

        val cacheFile = File(c.filesDir, CACHE_FILE)
        if (cacheFile.exists()) {
            val cachedVersion = prefs(c).getInt(KEY_CACHE_VERSION, 0)
            val payload = runCatching { BankJson.decode(cacheFile.readText()) }.getOrNull()
            if (payload != null && payload.tracks.isNotEmpty()) {
                install(payload.tracks, payload.version, "online", payload.updatedAt)
                return
            }
            // 缓存损坏：丢弃，回退内置
            cacheFile.delete()
            prefs(c).edit().remove(KEY_CACHE_VERSION).remove(KEY_CACHE_UPDATED).apply()
        }
        install(buildBundledTracks(), BankConfig.BUNDLED_VERSION, "bundled", "")
    }

    /** 恢复为内置题库（开发者模式 / 排障用）。 */
    fun resetToBundled() {
        val c = appContext ?: return
        File(c.filesDir, CACHE_FILE).delete()
        prefs(c).edit().remove(KEY_CACHE_VERSION).remove(KEY_CACHE_UPDATED).apply()
        install(buildBundledTracks(), BankConfig.BUNDLED_VERSION, "bundled", "")
        lastResult = BankMsg("restored")
        prefs(c).edit().putString(KEY_LAST_RESULT, encodeMsg(lastResult)).apply()
    }

    /**
     * 检查在线题库是否有更新。
     * 若比本地新，会下载、校验并直接切换为当前题库（同时写入缓存供下次启动使用）。
     */
    suspend fun checkForUpdate(): UpdateCheck {
        val c = appContext ?: return UpdateCheck.Failed("", BankMsg("init_missing"))
        val base = baseUrl
        if (base.isBlank()) return fail(c, BankMsg("no_url"))

        // 1) 优先取 manifest（小文件，只为了比版本）
        val manifest = remote.fetchManifest(base)

        if (manifest != null && manifest.minAppVersion > BuildConfig.VERSION_CODE) {
            fail(c, BankMsg("need_app", manifest.minAppVersion))
            return UpdateCheck.NeedAppUpgrade(manifest.minAppVersion)
        }

        val manifestVersion = manifest?.version ?: 0
        val fileName = manifest?.file ?: "bank.json"

        // 2) manifest 版本不比本地新就先不下载大文件
        if (manifest != null && manifestVersion in 1..version) {
            succeed(c, BankMsg("latest", version))
            return UpdateCheck.UpToDate(version)
        }

        // 3) 下载题库
        val text = remote.fetchBank(base, fileName)
            ?: return fail(c, BankMsg("download_fail"))

        // 4) 校验
        manifest?.sha256?.let { expected ->
            val actual = remote.sha256(text)
            if (!actual.equals(expected, ignoreCase = true)) {
                return fail(c, BankMsg("sha_fail"))
            }
        }

        // 5) 解析
        val payload = BankJson.decode(text)
            ?: return fail(c, BankMsg("format_fail"))
        if (payload.schema > BankJson.SCHEMA) {
            return fail(c, BankMsg("schema_high", payload.schema, BankJson.SCHEMA))
        }
        if (payload.minAppVersion > BuildConfig.VERSION_CODE) {
            return UpdateCheck.NeedAppUpgrade(payload.minAppVersion)
        }
        if (payload.version <= version) {
            succeed(c, BankMsg("latest", version))
            return UpdateCheck.UpToDate(version)
        }

        // 6) 落盘 + 切换
        return try {
            File(c.filesDir, CACHE_FILE).writeText(text)
            prefs(c).edit()
                .putInt(KEY_CACHE_VERSION, payload.version)
                .putString(KEY_CACHE_UPDATED, payload.updatedAt)
                .apply()
            install(payload.tracks, payload.version, "online", payload.updatedAt)
            succeed(c, BankMsg("updated", payload.version))
            UpdateCheck.Downloaded(payload.version, payload.updatedAt, payload.questionCount)
        } catch (e: Exception) {
            fail(c, BankMsg("cache_fail", s1 = e.message ?: ""))
        }
    }

    private fun install(newTracks: List<Track>, v: Int, kind: String, updated: String) {
        tracks = newTracks
        units = newTracks.flatMap { it.units }
        lessons = units.flatMap { it.lessons }
        version = v
        sourceKindValue = kind
        updatedAt = updated
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val KNOWN_CODES = setOf(
        "restored", "init_missing", "no_url", "need_app", "latest",
        "download_fail", "sha_fail", "format_fail", "schema_high", "updated", "cache_fail"
    )

    private fun encodeMsg(m: BankMsg?): String =
        m?.let { "${it.code}|${it.n1}|${it.n2}|${it.s1}" } ?: ""

    private fun decodeMsg(raw: String): BankMsg? {
        if (raw.isBlank()) return null
        val p = raw.split("|", limit = 4)
        if (p.size >= 2 && p[0] in KNOWN_CODES) {
            return BankMsg(
                p[0],
                p.getOrNull(1)?.toIntOrNull() ?: 0,
                p.getOrNull(2)?.toIntOrNull() ?: 0,
                p.getOrNull(3) ?: ""
            )
        }
        // 旧版本存的是中文原文：原样展示，等下次检查成功后自然覆盖
        return BankMsg("legacy", s1 = raw)
    }

    private fun fail(c: Context, msg: BankMsg): UpdateCheck {
        lastResult = msg
        prefs(c).edit().putString(KEY_LAST_RESULT, encodeMsg(msg)).apply()
        return UpdateCheck.Failed("", msg)
    }

    private fun succeed(c: Context, msg: BankMsg) {
        lastResult = msg
        prefs(c).edit()
            .putString(KEY_LAST_RESULT, encodeMsg(msg))
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }
}
