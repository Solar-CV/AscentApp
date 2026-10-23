package com.ascent.climb.data.i18n

/**
 * 内容层多语言覆盖。
 *
 * UI 文案走 Android 资源（res/values*），但课程名、成就名、技能书标题这些
 * 属于**内容数据**，不在字符串资源里。这里用一份按语言分文件的覆盖表：
 *
 *   key 形如 "lesson.m01.title" / "ach.first_steps.desc" / "hb.h1.title"
 *
 * 查找顺序：当前语言 → 英文 → 原文（数据层的中文）。
 * 因此**未翻译的条目会明确回退到英文**，而不是掉回中文，
 * 也不会让「没翻译」被误认为「已翻译」。
 *
 * 例外：简体中文（zh-CN）本身就是原文，不做英文回退，
 * 否则简中界面会出现英文课程名。
 */
object ContentPack {

    private val packs: Map<String, Map<String, String>> = mapOf(
        "en" to PACK_EN + PACK_EN_BRIEF + PACK_EN_HANDBOOK + PACK_EN_QUIZ,
        "zh-TW" to PACK_ZH_TW + PACK_ZH_TW_BRIEF + PACK_ZH_TW_ENTRY + PACK_ZH_TW_HANDBOOK + PACK_ZH_TW_QUIZ + PACK_ZH_TW_QUIZ_B + PACK_ZH_TW_QUIZ_C,
        "ja" to PACK_JA + PACK_JA_BRIEF + PACK_JA_ENTRY + PACK_JA_HANDBOOK + PACK_JA_QUIZ + PACK_JA_QUIZ_B + PACK_JA_QUIZ_C,
        "ru" to PACK_RU + PACK_RU_BRIEF + PACK_RU_ENTRY + PACK_RU_HANDBOOK + PACK_RU_QUIZ + PACK_RU_QUIZ_B + PACK_RU_QUIZ_C,
        "fr" to PACK_FR + PACK_FR_BRIEF + PACK_FR_ENTRY + PACK_FR_HANDBOOK + PACK_FR_QUIZ + PACK_FR_QUIZ_B + PACK_FR_QUIZ_C,
        "sv" to PACK_SV + PACK_SV_BRIEF + PACK_SV_ENTRY + PACK_SV_HANDBOOK + PACK_SV_QUIZ + PACK_SV_QUIZ_B + PACK_SV_QUIZ_C,
        "de" to PACK_DE + PACK_DE_BRIEF + PACK_DE_ENTRY + PACK_DE_HANDBOOK + PACK_DE_QUIZ + PACK_DE_QUIZ_B + PACK_DE_QUIZ_C,
        "es" to PACK_ES + PACK_ES_BRIEF + PACK_ES_ENTRY + PACK_ES_HANDBOOK + PACK_ES_QUIZ + PACK_ES_QUIZ_B + PACK_ES_QUIZ_C,
        "it" to PACK_IT + PACK_IT_BRIEF + PACK_IT_ENTRY + PACK_IT_HANDBOOK + PACK_IT_QUIZ + PACK_IT_QUIZ_B + PACK_IT_QUIZ_C
    )

    /**
     * 把 BCP-47 标签归一到 pack 的键；中文区分简繁。
     *
     * 脚本优先于地区：`zh-Hans-HK` / `zh-Hans-MO` 显式写了简体，
     * 不能因为地区在港澳就判成繁体（与 AppLocale.matchSupported 同一条规则）。
     */
    fun normalize(tag: String): String {
        val t = tag.replace('_', '-')
        if (t.startsWith("zh", ignoreCase = true)) {
            val u = t.uppercase()
            val hant = when {
                u.contains("HANT") -> true
                u.contains("HANS") -> false
                else -> u.contains("TW") || u.contains("HK") || u.contains("MO")
            }
            return if (hant) "zh-TW" else "zh-CN"
        }
        return t.substringBefore('-').lowercase()
    }

    /** 单个 key 的覆盖值；null 表示需要回退。 */
    fun get(tag: String, key: String): String? {
        val t = normalize(tag)
        // 简体中文就是数据层原文，没有覆盖包。若继续走英文回退，
        // 简中界面会显示英文课程名，所以这里直接返回 null 交给原文。
        if (t == "zh-CN" || t == "zh") return null
        packs[t]?.get(key)?.let { return it }
        if (t != "en") packs["en"]?.get(key)?.let { return it }
        return null
    }

    /** 该语言当前覆盖了多少条目（用于覆盖率报告）。 */
    fun sizeOf(tag: String): Int = packs[normalize(tag)]?.size ?: 0
}
