package com.ascent.climb.data

import android.content.Context
import com.ascent.climb.data.cloud.DrillRecord
import com.ascent.climb.data.cloud.SyncPayload
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

        /**
         * 训练项目的稳定 id。快照按这个列表枚举（prefs 里没法遍历 key），
         * 所以**新增 drill 时这里必须同步加**，否则它不会被同步。
         * 用字符串而不是资源 id：换语言、调顺序都不会变。
         */
        val DRILL_IDS = listOf("breath44", "step", "gear", "weather")
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

    /**
     * 组装成就判定输入。
     *
     * 单元/轨道进度一律按**全部轨道**统计（[ALL_UNITS_EVERY_TRACK] / [ALL_TRACKS]），
     * 与当前选中哪条路线无关 —— 成就库里只有一套解锁状态，
     * 若按当前轨道统计，切轨道会让进度忽大忽小，大满贯这类成就会误判。
     */
    fun achievementStats(): AchievementStats {
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
            unitProgress = ALL_UNITS_EVERY_TRACK.associate { u ->
                u.id to (u.lessons.count { it.id in done } to u.lessons.size)
            },
            trackProgress = ALL_TRACKS.associate { t ->
                val ls = t.units.flatMap { it.lessons }
                t.id to (ls.count { it.id in done } to ls.size)
            }
        )
    }

    /** 事件后统一判定：把新解锁写入库 + 追加到待播队列，返回新解锁列表（已按稀有度排序）。 */
    fun grantAchievements(): List<Achievement> {
        val newly = evaluate(achievementStats(), unlockedAchievements)
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

    /** 天气背景是否自动跟随所在地天气（需要定位权限；默认关闭，只用静态晴天）。 */
    var weatherAuto: Boolean
        get() = prefs.getBoolean("weatherAuto", false)
        set(value) = prefs.edit().putBoolean("weatherAuto", value).apply()

    /**
     * 天气背景的调试覆盖：空字符串 = 跟随真实天气（尚未接入，暂按晴天处理），
     * 其余为 clear / overcast / rain / storm / snow。只由 debug 包里的调试按钮写入。
     */
    var weatherOverride: String
        get() = prefs.getString("weatherOverride", "") ?: ""
        set(value) = prefs.edit().putString("weatherOverride", value).apply()

    /** 外观模式："auto"（跟随系统）/ "light" / "dark"。 */
    var themeMode: String
        get() = prefs.getString("themeMode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("themeMode", value).apply()

    /**
     * 3D 徽章展示页的背景："dark"（默认）/ "light"。
     * 只影响那一个全屏页，不动 App 主题；手动切换后记住，下次进来沿用。
     *
     * 暗色是默认：金属与珐琅在深底上反光更清楚，亮底会吃掉高光。
     * 旧版本存过的 "auto"（跟随系统）现在按暗色处理 —— 否则浅色主题的用户
     * 升级后仍然看到亮色，等于默认没改。
     */
    var badgeStageBg: String
        get() = prefs.getString("badgeStageBg", "dark") ?: "dark"
        set(value) = prefs.edit().putString("badgeStageBg", value).apply()

    /**
     * 首次启动的偏好引导是否已完成。
     *
     * 只在「真·新装」时弹；老用户升级上来时 SharedPreferences 里早有 streak / lastDay，
     * 再问一遍「你要攀登还是徒步」是冒犯。
     */
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboardingDone", false)
        set(value) = prefs.edit().putBoolean("onboardingDone", value).apply()

    init {
        // 首次运行就把结论落盘，而不是每次靠"prefs 是否为空"去猜：
        // App 一启动就会写 streak / lastDay，那个判据只成立一瞬间，
        // 用户中途杀掉进程就再也见不到引导了。
        if (!prefs.contains("onboardingDone")) {
            val returning = prefs.contains("lastDay") ||
                prefs.getInt("streak", 0) > 0 ||
                prefs.contains("completed")
            prefs.edit().putBoolean("onboardingDone", returning).apply()
        }
    }

    /** 是否需要弹首启引导。 */
    val needsOnboarding: Boolean
        get() = !onboardingDone

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
     * 本地用户名。空串表示没设过，界面回退到默认称呼（见 R.string.pf_default_name）。
     *
     * 与账号系统（data/Auth.kt）无关：那边是模拟登录、等后端接入，
     * 这里是纯本地的显示名，卸载前一直有效。
     */
    var displayName: String
        get() = prefs.getString("displayName", "") ?: ""
        set(value) = prefs.edit().putString("displayName", value.trim().take(16)).apply()

    /**
     * 头像版本号。头像文件路径固定（filesDir/avatar.jpg），文件名不变的话
     * Compose 不会重新解码 —— 换头像后靠这个数 +1 触发重读。0 表示没有头像。
     */
    var avatarVersion: Int
        get() = prefs.getInt("avatarVersion", 0)
        set(value) = prefs.edit().putInt("avatarVersion", value).apply()

    /**
     * 已经落到本机的云端头像版本。
     *
     * 云端的 `avatarVersion` 比它大，说明别的设备换过头像，才需要重新下载；
     * 否则每次同步都白拉一次 40 KB 的 JPEG。
     */
    var cloudAvatarVersion: Int
        get() = prefs.getInt("cloudAvatarVersion", 0)
        set(value) = prefs.edit().putInt("cloudAvatarVersion", value).apply()

    /**
     * 本机头像改动还没推上去。上传成功后清掉。
     *
     * 为什么不用「avatarVersion > cloudAvatarVersion」判断：下载时也要 +1 触发重新解码，
     * 两个计数器必然错位，会变成「刚下载完就以为本地更新、又推一次」的来回打架。
     */
    var avatarDirty: Boolean
        get() = prefs.getBoolean("avatarDirty", false)
        set(value) = prefs.edit().putBoolean("avatarDirty", value).apply()

    /**
     * 本机这张头像属于哪个账号（用户 id）。空串 = 还没归属过任何账号
     *（离线时自己设的，或从老版本升级上来还没同步过）。
     *
     * 头像文件只有一份，但它语义上属于某个账号。没有这个字段就会出现
     *「A 退出、B 登录后还显示 A 的脸」；更糟的是脏标记还在时，
     * 会把 A 的头像直接推到 B 的账号上。
     */
    var avatarOwner: String
        get() = prefs.getString("avatarOwner", "") ?: ""
        set(value) = prefs.edit().putString("avatarOwner", value).apply()

    /**
     * 本机这份学习进度属于哪个账号。退出登录时**不清**——用来区分
     *「同一个账号回来」和「换了个人」：
     *  - 同一个账号：正常合并（退出期间离线学的课要并回去）
     *  - 换了账号：见 [pendingCloudReplace]
     */
    var progressOwner: String
        get() = prefs.getString("progressOwner", "") ?: ""
        set(value) = prefs.edit().putString("progressOwner", value).apply()

    /**
     * 下一次同步用云端覆盖本地（只生效一次）。
     *
     * 登录到与 [progressOwner] 不同的账号时置位。不做合并是因为本地这份
     * 是**上一个账号**的进度，合并等于把别人的学习记录写进新账号。
     * 成功覆盖后清除；中途离线则保留，下次同步继续尝试。
     */
    var pendingCloudReplace: Boolean
        get() = prefs.getBoolean("pendingCloudReplace", false)
        set(value) = prefs.edit().putBoolean("pendingCloudReplace", value).apply()

    /**
     * Google 首次登录时返回的资料图地址，等着被下载成本地头像。
     *
     * 为什么不在登录响应里直接下：登录在 data 层，写文件在 UI 层（要 Bitmap）。
     * 这里只转手一次，由 `AvatarSync.reconcile` 取走并清空。
     */
    var pendingAvatarUrl: String
        get() = prefs.getString("pendingAvatarUrl", "") ?: ""
        set(value) = prefs.edit().putString("pendingAvatarUrl", value).apply()

    /**
     * 上一次从账号同步下来的昵称。
     *
     * 用来判断「本地这个名字是不是用户刚改、还没推上去」：只有 `displayName` 与它相同，
     * 才说明本地没有未上传的改动，可以安全地接受账号里的新名字（别的设备改的）。
     */
    var syncedNickname: String
        get() = prefs.getString("syncedNickname", "") ?: ""
        set(value) = prefs.edit().putString("syncedNickname", value.trim().take(16)).apply()

    /**
     * 当前课程大类：climb（高山攀登）/ trek（徒步）。
     * 只影响「看哪条路线」，进度本身按课程 id 存，两条轨道互不干扰。
     */
    var trackId: String
        get() = prefs.getString("trackId", CLIMB_TRACK) ?: CLIMB_TRACK
        set(value) = prefs.edit().putString("trackId", value).apply()

    // ---------------- 训练记录 ----------------
    //
    // 两步走：v0.22 先把「练过几次、累计多久」存下来，v0.23 的头衔要用它；
    // 所以这里不是单纯为了页面显示，读的时候别删。
    // 键名 drill_<id>_<field>，id 用稳定字符串（breath44 / step），
    // **不要用资源 id 或下标** —— 语言切换、顺序调整都会变。

    /** 记一次完成的训练：次数 +1、累计秒数增加、记下当天。 */
    fun recordDrill(id: String, seconds: Int) {
        val c = drillCount(id) + 1
        val s = drillTotalSeconds(id) + seconds.coerceAtLeast(0)
        prefs.edit()
            .putInt("drill_${id}_count", c)
            .putInt("drill_${id}_seconds", s)
            .putLong("drill_${id}_last", LocalDate.now().toEpochDay())
            .apply()
    }

    fun drillCount(id: String): Int = prefs.getInt("drill_${id}_count", 0)

    fun drillTotalSeconds(id: String): Int = prefs.getInt("drill_${id}_seconds", 0)

    /**
     * 记一组题库型训练（装备默写 / 天气判读）：次数 +1、累计秒数增加、刷新最好成绩。
     *
     * 最好成绩存百分比而不是「答对数」：以后调整每组题量（10 → 12）时，
     * 旧记录不会因为分母变了而失真。
     */
    fun recordDrillScore(id: String, seconds: Int, correct: Int, total: Int) {
        val pct = if (total > 0) (correct * 100 / total).coerceIn(0, 100) else 0
        val c = drillCount(id) + 1
        val s = drillTotalSeconds(id) + seconds.coerceAtLeast(0)
        val best = maxOf(drillBest(id), pct)
        prefs.edit()
            .putInt("drill_${id}_count", c)
            .putInt("drill_${id}_seconds", s)
            .putInt("drill_${id}_best", best)
            .putLong("drill_${id}_last", LocalDate.now().toEpochDay())
            .apply()
    }

    /** 最好成绩（百分比 0..100）；0 表示还没练过或全错。 */
    fun drillBest(id: String): Int = prefs.getInt("drill_${id}_best", 0)

    /** 最近一次训练的 epochDay；0 表示没练过。 */
    fun drillLastDay(id: String): Long = prefs.getLong("drill_${id}_last", 0L)

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

    // ---------------- 云端同步（见 CLOUD-PLAN.md） ----------------

    /**
     * 本机标识。只在快照里用于区分「同一账号下不同设备」，
     * 不含任何硬件信息，也不能用来定位用户。
     */
    val deviceId: String
        get() {
            prefs.getString("deviceId", null)?.let { return it }
            val id = "dev_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("deviceId", id).apply()
            return id
        }

    /** 上次同步成功的时间（毫秒），0 表示从未同步。 */
    var lastSyncAt: Long
        get() = prefs.getLong("lastSyncAt", 0L)
        set(value) = prefs.edit().putLong("lastSyncAt", value).apply()

    /** 上次同步后服务端的版本号，写入时作为乐观锁的基准。 */
    var lastSyncRev: Int
        get() = prefs.getInt("lastSyncRev", 0)
        set(value) = prefs.edit().putInt("lastSyncRev", value).apply()

    /** 把当前进度打包成可上传 / 可导出的快照。 */
    fun snapshot(): SyncPayload {
        val reviews = loadReviews()
        return SyncPayload(
            completed = completed,
            stars = allStars(),
            reviews = reviews.values.toList(),
            achievements = unlockedAchievements,
            lessonRuns = lessonRuns,
            reviewDone = reviewDone,
            days = activeDays,
            streak = streak,
            maxStreak = maxStreak,
            streakBroken = streakBroken,
            lastDay = lastDay,
            drills = DRILL_IDS.map { id ->
                DrillRecord(
                    id = id,
                    count = drillCount(id),
                    seconds = drillTotalSeconds(id),
                    best = drillBest(id),
                    last = drillLastDay(id)
                )
            }
        )
    }

    /**
     * 把合并后的快照写回本机。**整体覆盖，不做二次合并** ——
     * 合并已经由 SyncPayload.merge 完成，这里再合一次只会让两处规则分叉。
     */
    fun applySnapshot(p: SyncPayload) {
        // 训练记录先整体清空再写：换账号覆盖时，上一个账号的记录不能残留
        prefs.edit().also { e ->
            DRILL_IDS.forEach { id ->
                e.remove("drill_${id}_count")
                e.remove("drill_${id}_seconds")
                e.remove("drill_${id}_best")
                e.remove("drill_${id}_last")
            }
        }.apply()
        completed = p.completed.toMutableSet()
        starEntries = p.stars.map { "${it.key}:${it.value}" }.toMutableSet()
        saveReviews(p.reviews.associateBy { it.key })
        unlockedAchievements = p.achievements.toMutableSet()
        lessonRuns = p.lessonRuns
        reviewDone = p.reviewDone
        activeDays = p.days.toMutableSet()
        streak = p.streak
        maxStreak = p.maxStreak
        streakBroken = p.streakBroken
        lastDay = p.lastDay
        p.drills.forEach { d ->
            prefs.edit()
                .putInt("drill_${d.id}_count", d.count)
                .putInt("drill_${d.id}_seconds", d.seconds)
                .putInt("drill_${d.id}_best", d.best)
                .putLong("drill_${d.id}_last", d.last)
                .apply()
        }
    }

    /**
     * 重置学习进度。
     *
     * 只清学习数据（课程、星级、海拔、连续天数、活跃日、成就、复习队列、训练记录），
     * 保留：
     *  - 外观与设备偏好：主题 / 语言 / 首启已完成 / 轨道 / 徽章页底色 / 开发者开关；
     *  - 身份：本地用户名、头像版本。
     *
     * 以前这里是「清空 prefs 再回写开发者开关」，结果重置进度会顺手把用户名、头像、
     * 主题、界面语言一起清掉（语言会跳回系统语言），跟按钮承诺的「重置学习进度」不符。
     * 真要把身份也删掉，走「删除账号」（[wipeUserData]）。
     */
    fun reset() {
        val theme = themeMode
        val lang = languageMode
        val track = trackId
        val onboarded = onboardingDone
        val stageBg = badgeStageBg
        val dev = devUnlocked
        val unlock = devUnlockAll
        val answers = devShowAnswers
        val name = displayName
        val avatar = avatarVersion

        prefs.edit().clear().apply()

        themeMode = theme
        languageMode = lang
        trackId = track
        onboardingDone = onboarded
        badgeStageBg = stageBg
        devUnlocked = dev
        devUnlockAll = unlock
        devShowAnswers = answers
        displayName = name
        avatarVersion = avatar
    }

    /**
     * 删除账号时清本机数据。
     *
     * 与 [reset] 的区别：这里要连本地用户名、头像版本一起清掉（它们属于「人」），
     * 但保留「这台设备怎么用」的偏好 —— 主题、界面语言、首启引导是否已过、当前轨道、
     * 徽章展示页底色、开发者开关。全清的话，删个账号界面语言会突然跳回系统语言，
     * 看起来像把 App 一起重置了。
     *
     * 真正的账号记录在 `MockAuthApi.deleteAccount()` 里删，不在这。
     */
    fun wipeUserData() {
        val keepTheme = themeMode
        val keepLang = languageMode
        val keepTrack = trackId
        val keepOnboarded = onboardingDone
        val keepStageBg = badgeStageBg
        val keepDev = devUnlocked
        val keepUnlockAll = devUnlockAll
        val keepAnswers = devShowAnswers

        prefs.edit().clear().apply()

        themeMode = keepTheme
        languageMode = keepLang
        trackId = keepTrack
        onboardingDone = keepOnboarded
        badgeStageBg = keepStageBg
        devUnlocked = keepDev
        devUnlockAll = keepUnlockAll
        devShowAnswers = keepAnswers
    }
}
