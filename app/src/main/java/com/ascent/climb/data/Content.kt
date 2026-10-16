package com.ascent.climb.data

import com.ascent.climb.data.bank.BankRepository

/**
 * 界面使用的统一数据入口。
 *
 * 数据实际来源由 [BankRepository] 决定，优先级：
 *   在线题库（本地缓存副本） → 内置题库（buildBundledUnits）
 *
 * 这里只保留「读取」与「路线规则」，题库的构建见 BundledBank.kt，
 * 在线更新的逻辑见 bank/BankRepository.kt。
 */

/** 全部课程轨道（高山攀登，以及后续的潜水等）。 */
val ALL_TRACKS: List<Track> get() = BankRepository.allTracks

/** 全部营地（当前轨道的海拔单元）。 */
val ALL_UNITS: List<Unit> get() = BankRepository.allUnits

/** 全部课程，按路线展开后的线性顺序。 */
val ALL_LESSONS: List<Lesson> get() = BankRepository.allLessons

/** 路线总题量。 */
val TOTAL_QUESTIONS: Int get() = BankRepository.totalQuestions

/** 当前题库版本号。 */
val BANK_VERSION: Int get() = BankRepository.bankVersion

/** 当前题库来源描述，例如「内置题库」「在线题库 v3」。 */
val BANK_SOURCE: String get() = BankRepository.sourceDescription

// ---------------- 技能书（目前仍为内置，后续可一并外置） ----------------

val ALL_ARTICLES: List<HandbookArticle> = HANDBOOK_A + HANDBOOK_B + HANDBOOK_C + HANDBOOK_D + HANDBOOK_E

val HANDBOOK_CATEGORIES: List<String> = ALL_ARTICLES.map { it.category }.distinct()

// ---------------- 路线参数 ----------------

/** 路线顶端海拔（用于进度可视化）。 */
const val SUMMIT_ALTITUDE = 8000

/** 规划中的其他课程轨道 —— 数据层已支持，界面会显示为「即将开放」。 */
data class PlannedTrack(val id: String, val title: String, val subtitle: String, val icon: String)

val PLANNED_TRACKS: List<PlannedTrack> = listOf(
    PlannedTrack(DIVE_TRACK, "开放水域潜水", "OW / AOW · 潜水理论与安全", "\uD83E\uDD3F"),
    PlannedTrack(TREK_TRACK, "长途徒步", "高原徒步与长距离路线", "\uD83E\uDD7E"),
    PlannedTrack(SKI_TRACK, "登山滑雪", "滑雪登山与雪崩地形", "\u26F7\uFE0F")
)

/** 单题海拔奖励：已废弃 —— 海拔改为按课授予，见 altitudeAwardFor()。 */
@Deprecated("海拔改为按课授予")
fun questionReward(safetyCritical: Boolean): Int = if (safetyCritical) 80 else 50

// ============================================================
// 海拔曲线
// ============================================================

// 海拔曲线（BASE_ALTITUDE / FIRST_LESSON_TARGET / SUMMIT_TARGET、
// altitudeAwardFor、altitudeFor）已移到 Altitude.kt ——
// 那里是纯逻辑，能被 tools/contentcheck 直接校验。

// ---------------- 查询与解锁 ----------------

fun lessonIndex(id: String): Int = ALL_LESSONS.indexOfFirst { it.id == id }

fun lessonById(id: String): Lesson? = ALL_LESSONS.firstOrNull { it.id == id }

fun unitOfLesson(id: String): Unit? = ALL_UNITS.firstOrNull { u -> u.lessons.any { it.id == id } }

/**
 * 解锁规则：路线是线性的——前面所有课程都完成，本节才解锁。
 * 这是多邻国式路径的核心约束，保证内容顺序与前置知识。
 *
 * @param unlockAll 开发者模式：跳过前置要求，直接解锁全部关卡。
 */
fun isUnlocked(id: String, completed: Set<String>, unlockAll: Boolean = false): Boolean {
    if (unlockAll) return true
    val i = lessonIndex(id)
    if (i <= 0) return true
    return ALL_LESSONS.take(i).all { it.id in completed }
}

/** 当前应该学习的课程（第一门未完成的课程）。 */
fun currentLessonId(completed: Set<String>): String? =
    ALL_LESSONS.firstOrNull { it.id !in completed }?.id

fun allLessonsCompleted(completed: Set<String>): Boolean =
    ALL_LESSONS.all { it.id in completed }
