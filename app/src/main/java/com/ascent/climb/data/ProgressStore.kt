package com.ascent.climb.data

import android.content.Context
import java.time.LocalDate

/** 复习队列中的一项。level 越高，下次间隔越长。 */
data class ReviewItem(val key: String, val level: Int, val due: Long)

/**
 * Lightweight local progress store backed by SharedPreferences.
 * Deliberately dependency-free so the test build stays small.
 */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("ascent_progress", Context.MODE_PRIVATE)

    companion object {
        const val MAX_ROPE = 5
    }

    var completed: MutableSet<String>
        get() = prefs.getStringSet("completed", emptySet())!!.toMutableSet()
        private set(value) = prefs.edit().putStringSet("completed", value).apply()

    var wrong: MutableSet<String>
        get() = prefs.getStringSet("wrong", emptySet())!!.toMutableSet()
        private set(value) = prefs.edit().putStringSet("wrong", value).apply()

    // ---------------- 间隔重复复习 ----------------

    /** 复习间隔（天）。第 0 级答错当天就要重做；等级越高间隔越长。 */
    private val intervals = intArrayOf(0, 1, 3, 7, 16, 35)

    private fun loadReviews(): MutableMap<String, ReviewItem> {
        val raw = prefs.getStringSet("reviews", emptySet())!!
        val m = HashMap<String, ReviewItem>(raw.size)
        raw.forEach { s ->
            val p = s.split('|')
            if (p.size == 3) {
                val lvl = p[1].toIntOrNull() ?: 0
                val due = p[2].toLongOrNull() ?: 0L
                m[p[0]] = ReviewItem(p[0], lvl, due)
            }
        }
        return m
    }

    private fun saveReviews(m: Map<String, ReviewItem>) {
        prefs.edit()
            .putStringSet("reviews", m.values.map { "${it.key}|${it.level}|${it.due}" }.toSet())
            .apply()
    }

    /**
     * 复习答对时升级并推后到期日；答错则回到第 0 级（当天再练）。
     * 这是间隔重复的核心：只把「该复习的」推给你，而不是一次性堆一大堆错题。
     */
    fun gradeReview(key: String, correct: Boolean) {
        val m = loadReviews()
        val today = LocalDate.now().toEpochDay()
        if (!correct) {
            m[key] = ReviewItem(key, 0, today)
        } else {
            val cur = m[key]?.level ?: 0
            val next = (cur + 1).coerceAtMost(intervals.size - 1)
            m[key] = ReviewItem(key, next, today + intervals[next])
            reviewDone += 1
        }
        saveReviews(m)
    }

    /** 到期需要复习的题目键，按紧迫程度排序（等级低、到期早的在前）。 */
    fun dueReviewKeys(): List<String> {
        val today = LocalDate.now().toEpochDay()
        return loadReviews().values
            .filter { it.due <= today }
            .sortedWith(compareBy({ it.level }, { it.due }))
            .map { it.key }
    }

    /** 复习队列中的总题数（含未到期）。 */
    fun reviewTotal(): Int = loadReviews().size

    /** 某题的复习等级，0 表示新错题。 */
    fun reviewLevel(key: String): Int = loadReviews()[key]?.level ?: 0

    /** 某题的下次到期日（返回“几天后”，已到期返回 0）。 */
    fun daysUntilDue(key: String): Int {
        val it = loadReviews()[key] ?: return 0
        val d = it.due - LocalDate.now().toEpochDay()
        return if (d < 0) 0 else d.toInt()
    }

    /** Best stars (1..3) earned per lesson id. Stored as "id:stars" entries. */
    private var starEntries: MutableSet<String>
        get() = prefs.getStringSet("stars", emptySet())!!.toMutableSet()
        set(value) = prefs.edit().putStringSet("stars", value).apply()

    /**
     * 当前海拔：由已完成课程实时累加得出，不是独立存储的计数器。
     * 好处是调整海拔曲线后所有旧存档会自动重算，不会残留旧值。
     */
    val altitude: Int
        get() = altitudeFor(completed)

    /** 某条轨道上的海拔（各轨道曲线不同）。 */
    fun altitudeOn(trackId: String): Int = altitudeForTrack(trackId, completed)

    var streak: Int
        get() = prefs.getInt("streak", 0)
        private set(value) = prefs.edit().putInt("streak", value).apply()

    /** 历史最长连续天数（成就判定用，streak 中断不影响它）。 */
    var maxStreak: Int
        get() = prefs.getInt("maxStreak", 0)
        private set(value) = prefs.edit().putInt("maxStreak", value).apply()

    /** 「连续 ≥3 天后中断」标记（隐藏成就 comeback 的判定依据，一旦置位不再清除）。 */
    var streakBroken: Boolean
        get() = prefs.getBoolean("streakBroken", false)
        private set(value) = prefs.edit().putBoolean("streakBroken", value).apply()

    var lastDay: String
        get() = prefs.getString("lastDay", "") ?: ""
        private set(value) = prefs.edit().putString("lastDay", value).apply()

    /** 有学习行为的日期集合（累计学习天数口径）。 */
    var activeDays: MutableSet<String>
        get() = prefs.getStringSet("activeDays", emptySet())!!.toMutableSet()
        private set(value) = prefs.edit().putStringSet("activeDays", value).apply()

    /** 累计完成课次（含重做）。 */
    var lessonRuns: Int
        get() = prefs.getInt("lessonRuns", 0)
        private set(value) = prefs.edit().putInt("lessonRuns", value).apply()

    /** 复习答对总次数。 */
    var reviewDone: Int
        get() = prefs.getInt("reviewDone", 0)
        private set(value) = prefs.edit().putInt("reviewDone", value).apply()

    private fun markActiveToday() {
        val days = activeDays
        days.add(LocalDate.now().toString())
        activeDays = days
    }

    var rope: Int
        get() = prefs.getInt("rope", MAX_ROPE)
        private set(value) = prefs.edit().putInt("rope", value).apply()

    /** 开发者模式是否已解锁（在「我的」页连点版本号 7 次）。 */
    var devUnlocked: Boolean
        get() = prefs.getBoolean("devUnlocked", false)
        set(value) = prefs.edit().putBoolean("devUnlocked", value).apply()

    /** 开发者模式开关：解锁全部关卡。 */
    var devUnlockAll: Boolean
        get() = prefs.getBoolean("devUnlockAll", false)
        set(value) = prefs.edit().putBoolean("devUnlockAll", value).apply()

    /** 开发者模式：显示每道题的正确答案标记。 */
    var devShowAnswers: Boolean
        get() = prefs.getBoolean("devShowAnswers", false)
        set(value) = prefs.edit().putBoolean("devShowAnswers", value).apply()

    fun starsFor(lessonId: String): Int =
        starEntries.firstOrNull { it.startsWith("$lessonId:") }
            ?.substringAfter(":")?.toIntOrNull() ?: 0

    /** 全部课程的最高星级（成就判定用）。 */
    fun allStars(): Map<String, Int> =
        starEntries.mapNotNull {
            val p = it.split(":")
            if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null
        }.toMap()

    fun isCompleted(lessonId: String) = lessonId in completed

    /** Called once per app launch: maintains the daily streak and refills the safety rope. */
    fun touchDay() {
        val today = LocalDate.now().toString()
        if (lastDay != today) {
            val yesterday = LocalDate.now().minusDays(1).toString()
            if (lastDay == yesterday) {
                streak += 1
            } else {
                // 连续 ≥3 天的记录被中断：为隐藏成就「卷土重来」留下凭据
                if (streak >= 3) streakBroken = true
                streak = 1
            }
            if (streak > maxStreak) maxStreak = streak
            lastDay = today
            rope = MAX_ROPE
        }
        markActiveToday()
    }

    /**
     * 记录一课通关（或重做）。
     * 海拔改为**按课授予**：只有首次通关才加海拔，重做不再重复获得。
     * @return 本次实际获得的海拔（重做为 0）
     */
    fun finishLesson(lessonId: String, stars: Int): Int {
        val c = completed
        val firstTime = lessonId !in c
        c.add(lessonId)
        completed = c

        val existing = starsFor(lessonId)
        if (stars > existing) {
            val s = starEntries.filterNot { it.startsWith("$lessonId:") }.toMutableSet()
            s.add("$lessonId:$stars")
            starEntries = s
        }

        lessonRuns += 1
        markActiveToday()

        return if (firstTime) altitudeAwardForTrack(trackOfLesson(lessonId), lessonId) else 0
    }

    fun markWrong(key: String) {
        // 新错题：进入复习队列第 0 级，当天就会出现在复习中心
        gradeReview(key, false)
    }

    fun clearWrong(key: String) {
        val m = loadReviews()
        m.remove(key)
        saveReviews(m)
    }

    fun loseRope() {
        if (rope > 0) rope -= 1
    }

    fun refillRope() {
        rope = MAX_ROPE
    }

    // ---------------- 成就系统 ----------------

    /** 已解锁成就 id 集合（持久化，保证不重复发放）。 */
    var unlockedAchievements: MutableSet<String>
        get() = prefs.getStringSet("achievements", emptySet())!!.toMutableSet()
        private set(value) = prefs.edit().putStringSet("achievements", value).apply()

    /** 待播放弹窗的成就 id 队列（有序、持久化，进程死亡不丢弹窗）。 */
    var pendingAchievements: MutableList<String>
        get() = (prefs.getString("pendingAchievements", "") ?: "")
            .split(',').filter { it.isNotBlank() }.toMutableList()
        private set(value) = prefs.edit()
            .putString("pendingAchievements", value.joinToString(",")).apply()

    /** 组装成就判定输入。 */
    fun achievementStats(units: List<Unit>): AchievementStats {
        val done = completed
        return AchievementStats(
            completed = done,
            stars = allStars(),
            maxStreak = maxStreak,
            activeDays = activeDays.size,
            lessonRuns = lessonRuns,
            reviewDone = reviewDone,
            reviewQueueSize = reviewTotal(),
            altitude = altitude,
            streakBroken = streakBroken,
            devUnlocked = devUnlocked,
            unitProgress = units.associate { u ->
                u.id to (u.lessons.count { it.id in done } to u.lessons.size)
            }
        )
    }

    /** 事件后统一判定：把新解锁写入库 + 追加到待播队列，返回新解锁列表（已按稀有度排序）。 */
    fun grantAchievements(units: List<Unit>): List<Achievement> {
        val newly = evaluate(achievementStats(units), unlockedAchievements)
        if (newly.isEmpty()) return emptyList()
        val u = unlockedAchievements
        u.addAll(newly.map { it.id })
        unlockedAchievements = u
        val q = pendingAchievements
        q.addAll(newly.map { it.id })
        pendingAchievements = q
        return newly
    }

    /** 弹窗播完一个：从待播队列移除。 */
    fun dequeuePendingAchievement(id: String) {
        val q = pendingAchievements
        if (q.remove(id)) pendingAchievements = q
    }

    /** 外观模式："auto"（跟随系统）/ "light" / "dark"。 */
    var themeMode: String
        get() = prefs.getString("themeMode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("themeMode", value).apply()

    /**
     * 语言模式："auto"（跟随系统）或 BCP-47 语言标签（如 "zh-CN" / "en"）。
     *
     * 默认 "auto"，因此已安装的用户升级后行为不变、不会莫名丢偏好。
     * 键名与 SharedPreferences 文件均未改动，属纯新增字段。
     */
    var languageMode: String
        get() = prefs.getString("languageMode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("languageMode", value).apply()

    /**
     * 当前课程大类：climb（高山攀登）/ trek（徒步）。
     * 只影响「看哪条路线」，进度本身按课程 id 存，两条轨道互不干扰。
     */
    var trackId: String
        get() = prefs.getString("trackId", CLIMB_TRACK) ?: CLIMB_TRACK
        set(value) = prefs.edit().putString("trackId", value).apply()

    /** 开发者模式：清空待播弹窗队列（解卡用，不影响已解锁记录）。 */
    fun clearPendingAchievements() {
        if (pendingAchievements.isNotEmpty()) pendingAchievements = mutableListOf()
    }

    /** 开发者模式：强制解锁（不判定条件，会进待播队列可预览弹窗）。 */
    fun forceUnlockAchievement(id: String) {
        val u = unlockedAchievements
        if (u.add(id)) unlockedAchievements = u
    }

    /** 开发者模式：重置单个成就（含从待播队列移除）。 */
    fun resetAchievement(id: String) {
        val u = unlockedAchievements
        if (u.remove(id)) unlockedAchievements = u
        val q = pendingAchievements
        if (q.remove(id)) pendingAchievements = q
    }

    /** 开发者模式：重置全部成就（只清成就字段，绝不动课程进度）。 */
    fun resetAllAchievements() {
        unlockedAchievements = mutableSetOf()
        pendingAchievements = mutableListOf()
    }

    fun reset() {
        // 重置学习进度不应关掉开发者模式
        val dev = devUnlocked
        val unlock = devUnlockAll
        val answers = devShowAnswers
        prefs.edit().clear().apply()
        devUnlocked = dev
        devUnlockAll = unlock
        devShowAnswers = answers
    }
}
