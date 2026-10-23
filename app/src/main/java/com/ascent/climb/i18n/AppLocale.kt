package com.ascent.climb.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * 应用语言的解析与应用。
 *
 * 优先级：用户手动选择 > 系统语言列表（按用户偏好排序）> 回退英文。
 *
 * 语言偏好存于 ProgressStore.languageMode：
 *   - "auto"  跟随系统（默认值，因此老用户升级后行为不变）
 *   - 其余为 BCP-47 语言标签，如 "zh-CN" / "zh-TW" / "en"
 */
object AppLocale {

    const val AUTO = "auto"

    /** 兜底语言：设备语言不受支持时使用。 */
    val FALLBACK: Locale = Locale.ENGLISH

    /**
     * 支持的 10 种语言。顺序即语言选择页的展示顺序。
     *
     * nativeName 用该语言自身的文字书写（选择页要求），
     * englishName 仅用于内部排查与文档。
     */
    val SUPPORTED: List<Lang> = listOf(
        Lang("zh-CN", "简体中文", "Chinese (Simplified)"),
        Lang("zh-TW", "繁體中文", "Chinese (Traditional)"),
        Lang("en", "English", "English"),
        Lang("ja", "日本語", "Japanese"),
        Lang("ru", "Русский", "Russian"),
        Lang("fr", "Français", "French"),
        Lang("sv", "Svenska", "Swedish"),
        Lang("de", "Deutsch", "German"),
        Lang("es", "Español", "Spanish"),
        Lang("it", "Italiano", "Italian")
    )

    data class Lang(
        val tag: String,
        val nativeName: String,
        val englishName: String
    )

    /** 除中文外的支持语言码，用于按语言码匹配。 */
    private val SIMPLE_LANGS = setOf("en", "ja", "ru", "fr", "sv", "de", "es", "it")

    /** 被视为繁体中文的地区。 */
    private val HANT_REGIONS = setOf("TW", "HK", "MO")

    fun isSupported(mode: String): Boolean =
        mode == AUTO || SUPPORTED.any { it.tag == mode }

    fun nativeName(tag: String): String =
        if (tag == AUTO) "" else SUPPORTED.firstOrNull { it.tag == tag }?.nativeName ?: tag

    /**
     * 解析出最终生效的 Locale。
     *
     * @param mode 用户偏好（AUTO 或语言标签）
     * @param system 系统语言列表，按用户偏好排序
     */
    fun resolve(mode: String, system: LocaleList): Locale {
        if (mode != AUTO) {
            // 手动选择：直接用该标签；万一存了脏数据，回退兜底语言
            return SUPPORTED.firstOrNull { it.tag == mode }
                ?.let { Locale.forLanguageTag(it.tag) }
                ?: FALLBACK
        }
        for (i in 0 until system.size()) {
            matchSupported(system[i])?.let { return it }
        }
        return FALLBACK
    }

    /** 便捷重载：直接取系统默认语言列表。 */
    fun resolve(mode: String): Locale = resolve(mode, LocaleList.getDefault())

    /**
     * 把一个系统 Locale 映射到受支持的语言，不支持则返回 null。
     *
     * 中文要区分简繁，判定顺序是**脚本优先于地区**：
     *
     * - 显式 `Hant` → 繁体；显式 `Hans` → 简体（`zh-Hans-HK` / `zh-Hans-MO`
     *   是「港澳用简体」，不能因为地区落在繁体区就翻成繁体）
     * - 没有脚本时才看地区：zh-TW / zh-HK / zh-MO → 繁体，其余 → 简体
     */
    fun matchSupported(locale: Locale): Locale? {
        if (locale.language == "zh") {
            val script = locale.script
            val country = locale.country.uppercase(Locale.ROOT)
            val traditional = when {
                script.equals("Hant", ignoreCase = true) -> true
                script.equals("Hans", ignoreCase = true) -> false
                else -> country in HANT_REGIONS
            }
            return if (traditional) Locale.forLanguageTag("zh-TW")
            else Locale.forLanguageTag("zh-CN")
        }
        if (locale.language in SIMPLE_LANGS) {
            return Locale.forLanguageTag(locale.language)
        }
        return null
    }

    /**
     * 用指定 Locale 创建一个包装过的 Context。
     *
     * Compose 的 stringResource() 读的是 LocalContext.resources，
     * 所以把 LocalContext 换成这个包装 Context 即可让整棵 UI 树切换到新语言，
     * 且不需要重建 Activity（即时生效）。
     */
    fun localizedContext(base: Context, locale: Locale): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
