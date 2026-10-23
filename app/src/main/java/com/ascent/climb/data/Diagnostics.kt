package com.ascent.climb.data

import android.content.Context
import android.os.Build
import com.ascent.climb.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 反馈用的诊断信息采集。
 *
 * 只取排障必需的运行环境（版本 / 系统 / 机型 / 语言）与最近一次未捕获异常。
 * **不采集**课程内容、做题记录、账号、剪贴板与任何用户输入 —— 这段口径同时写在
 * 界面说明里，用户勾选前能看清楚。
 *
 * 全部内容拼进邮件正文，不做附件：这样不需要 FileProvider，也不需要额外权限。
 */
object Diagnostics {

    private const val CRASH_DIR = "crashes"

    /** 最多保留几条崩溃记录，避免无限增长。 */
    private const val MAX_CRASH_FILES = 5

    /** 邮件里最多带多少字符的堆栈，太长会把正文撑爆。 */
    private const val MAX_CRASH_CHARS = 2500

    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * 装一个兜底的未捕获异常记录器：落到文件里，再交还给系统原有处理。
     *
     * 只记录、不拦截 —— 崩溃该崩还是崩，不能因为加了日志把崩溃吞掉。
     * 必须在 Application/Activity 最早的位置调用一次。
     */
    fun installCrashHandler(context: Context) {
        val dir = File(context.filesDir, CRASH_DIR)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                dir.mkdirs()
                File(dir, "crash-${System.currentTimeMillis()}.txt").writeText(
                    buildString {
                        appendLine("time: ${stamp.format(Date())}")
                        appendLine("thread: ${thread.name}")
                        appendLine()
                        appendLine(error.stackTraceToString())
                    }
                )
                dir.listFiles()
                    ?.sortedByDescending { it.name }
                    ?.drop(MAX_CRASH_FILES)
                    ?.forEach { it.delete() }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** 最近一次崩溃的堆栈；没有则返回 null。 */
    fun lastCrash(context: Context): String? =
        File(context.filesDir, CRASH_DIR)
            .listFiles()
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?.let { runCatching { it.readText() }.getOrNull() }
            ?.take(MAX_CRASH_CHARS)

    /** 是否有可附带的崩溃记录（界面上用来提示「会附带一条错误记录」）。 */
    fun hasCrash(context: Context): Boolean =
        File(context.filesDir, CRASH_DIR).listFiles()?.isNotEmpty() == true

    /**
     * 运行环境，一行一项。
     *
     * @param extras 调用方补充的业务状态（题库版本、当前大类、进度等）
     */
    fun environment(context: Context, extras: List<Pair<String, String>> = emptyList()): String =
        buildString {
            appendLine("App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device ${Build.MANUFACTURER} ${Build.MODEL}")
            val locales = context.resources.configuration.locales
            if (!locales.isEmpty) appendLine("Locale ${locales[0].toLanguageTag()}")
            extras.forEach { (key, value) -> appendLine("$key $value") }
        }.trimEnd()
}
