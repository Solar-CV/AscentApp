package com.ascent.climb.data

/**
 * 成就系统（纯 Kotlin，不依赖 Android，可被 tools/contentcheck 直接校验）。
 * 设计文档见仓库根目录 ACHIEVEMENTS.md。
 *
 * 判定与 UI 完全分离：事件发生后由调用方构建 [AchievementStats]，
 * 用 [evaluate] 算出「新解锁」列表；已解锁集合由 ProgressStore 持久化，
 * 保证幂等（同一成就不会重复发放）。
 */

enum class Rarity(val label: String) {
    Common("普通"), Rare("稀有"), Epic("史诗"), Legendary("传说")
}

enum class AchCategory(val label: String) {
    Progress("课程进度"), Habit("学习习惯"), Depth("学习深度"), Explore("探索里程")
}

/** 判定时的全部输入。由数据层组装，成就定义只读它。 */
data class AchievementStats(
    /** 已完成课程 id */
    val completed: Set<String>,
    /** 每课最高星级（3 = 全对） */
    val stars: Map<String, Int>,
    /** 历史最长连续学习天数 */
    val maxStreak: Int,
    /** 有学习行为的累计天数 */
    val activeDays: Int,
    /** 累计完成课次（含重做） */
    val lessonRuns: Int,
    /** 复习答对总次数 */
    val reviewDone: Int,
    /** 当前复习队列长度 */
    val reviewQueueSize: Int,
    /** 当前海拔（由 completed 派生） */
    val altitude: Int,
    /** 「连续 ≥3 天后中断」标记 */
    val streakBroken: Boolean,
    /** 开发者模式已解锁 */
    val devUnlocked: Boolean,
    /** 单元进度：unitId -> (完成数, 总数)，覆盖**全部轨道** */
    val unitProgress: Map<String, Pair<Int, Int>>,
    /** 轨道进度：trackId -> (完成数, 总数)，与当前选中轨道无关 */
    val trackProgress: Map<String, Pair<Int, Int>> = emptyMap()
)

data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val category: AchCategory,
    val rarity: Rarity,
    /** 徽章字形键（对应 ui/Badges.kt 的 glyph 映射，源自 lucide） */
    val glyph: String,
    /** 可读条件（开发者模式展示用） */
    val conditionText: String,
    val hidden: Boolean = false,
    /** 进度分母（=1 表示非进度型） */
    val target: Int = 1,
    val progress: (AchievementStats) -> Int,
    val unlocked: (AchievementStats) -> Boolean
)

private fun lesson(id: String): (AchievementStats) -> Boolean = { id in it.completed }

private fun unitAll(uid: String): (AchievementStats) -> Boolean =
    { s -> s.unitProgress[uid]?.let { (d, t) -> t > 0 && d >= t } == true }

/*
 * 两条轨道的单元 id。
 *
 * 第 1 版（66 课时代）只有攀登一条线，`five_zones` / `full_route` 直接数
 * 「所有单元」，语义是对的。补上徒步之后这个写法就塌了：
 * 徒步只有 4 个单元，数「所有单元」会把徒步也算进攀登的大满贯，
 * 于是徒步走完 21 课就能拿到「理论大满贯」。所以这里把两条线的单元显式列出来，
 * 各自的成就只认自己那串 id。
 */
private val CLIMB_UNITS = listOf("m5i", "m5a", "m6", "m7", "m7h", "m8")
private val TREK_UNITS = listOf("t1", "t2", "t3", "t4")

/** 指定单元里已完成的课数合计。 */
private fun unitsDone(s: AchievementStats, ids: List<String>): Int =
    ids.sumOf { s.unitProgress[it]?.first ?: 0 }

/** 指定单元是否全部完成（缺数据一律判否，避免题库缺单元时误发）。 */
private fun unitsAllDone(s: AchievementStats, ids: List<String>): Boolean =
    ids.all { id -> s.unitProgress[id]?.let { (d, t) -> t > 0 && d >= t } == true }

/** 指定单元里「至少完成 1 课」的单元数。 */
private fun unitsTouched(s: AchievementStats, ids: List<String>): Int =
    ids.count { id -> s.unitProgress[id]?.let { (d, t) -> t > 0 && d >= 1 } == true }

private fun trackDone(s: AchievementStats, trackId: String): Int =
    s.trackProgress[trackId]?.first ?: 0

private fun perfectCount(s: AchievementStats): Int = s.stars.count { it.value >= 3 }

val ACHIEVEMENTS: List<Achievement> = listOf(
    // ---------------- A · 课程进度与关键关卡 ----------------
    Achievement(
        "first_steps", "首次出发", "完成第一门课，迈出攀登之路的第一步。",
        AchCategory.Progress, Rarity.Common, "footprints", "完成第 1 课（高山环境与风险认知）",
        progress = { if ("l1" in it.completed) 1 else 0 },
        unlocked = lesson("l1")
    ),
    Achievement(
        "crampon_ready", "冰爪出师", "掌握冰爪法式、德式与混合技术。",
        AchCategory.Progress, Rarity.Common, "mountain-snow", "完成 l5（冰爪技术）",
        progress = { if ("l5" in it.completed) 1 else 0 },
        unlocked = lesson("l5")
    ),
    Achievement(
        "self_arrest", "制动本能", "学会冰镐自我制动。滑坠时能靠它止住。",
        AchCategory.Progress, Rarity.Common, "target", "完成 l6（冰镐使用与自我制动）",
        progress = { if ("l6" in it.completed) 1 else 0 },
        unlocked = lesson("l6")
    ),
    Achievement(
        "knot_tied", "绳结之约", "基础绳结全部到手。",
        AchCategory.Progress, Rarity.Common, "link", "完成 l9（基础绳结）",
        progress = { if ("l9" in it.completed) 1 else 0 },
        unlocked = lesson("l9")
    ),
    Achievement(
        "ams_guardian", "高原病守门人", "识别急性高原病，守住安全底线。",
        AchCategory.Progress, Rarity.Common, "heart-pulse", "完成 l13（急性高原病 AMS）",
        progress = { if ("l13" in it.completed) 1 else 0 },
        unlocked = lesson("l13")
    ),
    Achievement(
        "peak_scout", "目标锁定", "选定你的第一座目标雪山。",
        AchCategory.Progress, Rarity.Common, "map-pin", "完成 m05（目标山峰：入门雪山）",
        progress = { if ("m05" in it.completed) 1 else 0 },
        unlocked = lesson("m05")
    ),
    Achievement(
        "basecamp_grad", "大本营毕业", "5000 米入门单元全部完成。",
        AchCategory.Progress, Rarity.Rare, "tent", "单元 1（m5i）全部 16 课完成",
        target = 16,
        progress = { it.unitProgress["m5i"]?.first ?: 0 },
        unlocked = unitAll("m5i")
    ),
    Achievement(
        "tech_grad", "技术营毕业", "5000 米进阶单元全部完成。",
        AchCategory.Progress, Rarity.Rare, "backpack", "单元 2（m5a）全部 12 课完成",
        target = 12,
        progress = { it.unitProgress["m5a"]?.first ?: 0 },
        unlocked = unitAll("m5a")
    ),
    Achievement(
        "six_k_grad", "六千米毕业", "6000 米单元全部完成。",
        AchCategory.Progress, Rarity.Rare, "mountain", "单元 3（m6）全部 14 课完成",
        target = 14,
        progress = { it.unitProgress["m6"]?.first ?: 0 },
        unlocked = unitAll("m6")
    ),
    Achievement(
        "seven_k_grad", "七千米毕业", "7000 米单元全部完成。",
        AchCategory.Progress, Rarity.Epic, "snowflake", "7000 米两个营地（C3 + C4）共 16 课完成",
        target = 16,
        progress = { s -> (s.unitProgress["m7"]?.first ?: 0) + (s.unitProgress["m7h"]?.first ?: 0) },
        unlocked = { s -> unitAll("m7")(s) && unitAll("m7h")(s) }
    ),
    Achievement(
        "avalanche_eye", "雪崩之眼", "学会识别雪崩地形与风险。",
        AchCategory.Progress, Rarity.Rare, "cloud-snow", "完成 l18（雪崩基础与识别）",
        progress = { if ("l18" in it.completed) 1 else 0 },
        unlocked = lesson("l18")
    ),
    Achievement(
        "retreat_wise", "撤退的智慧", "该停的时候能停下来，也是一种本事。",
        AchCategory.Progress, Rarity.Rare, "compass", "完成 l19（攀登计划与撤退决策）",
        progress = { if ("l19" in it.completed) 1 else 0 },
        unlocked = lesson("l19")
    ),

    // ---------------- B · 持续学习与学习习惯 ----------------
    Achievement(
        "streak_3", "连续 3 天", "连续学习 3 天，节奏开始形成。",
        AchCategory.Habit, Rarity.Common, "flame", "连续学习 ≥ 3 天",
        target = 3, progress = { it.maxStreak },
        unlocked = { it.maxStreak >= 3 }
    ),
    Achievement(
        "streak_7", "连续 7 天", "连续学习 7 天，习惯开始扎根。",
        AchCategory.Habit, Rarity.Rare, "flame", "连续学习 ≥ 7 天",
        target = 7, progress = { it.maxStreak },
        unlocked = { it.maxStreak >= 7 }
    ),
    Achievement(
        "streak_14", "连续 14 天", "连续学习 14 天，自律登山者。",
        AchCategory.Habit, Rarity.Epic, "flame", "连续学习 ≥ 14 天",
        target = 14, progress = { it.maxStreak },
        unlocked = { it.maxStreak >= 14 }
    ),
    Achievement(
        "streak_30", "连续 30 天", "连续学习 30 天，把训练写进日常。",
        AchCategory.Habit, Rarity.Legendary, "flame", "连续学习 ≥ 30 天",
        target = 30, progress = { it.maxStreak },
        unlocked = { it.maxStreak >= 30 }
    ),
    Achievement(
        "days_7", "七日足迹", "累计 7 天有学习行为。",
        AchCategory.Habit, Rarity.Common, "calendar", "累计学习天数 ≥ 7（不要求连续）",
        target = 7, progress = { it.activeDays },
        unlocked = { it.activeDays >= 7 }
    ),
    Achievement(
        "days_21", "三周足迹", "累计 21 天有学习行为。",
        AchCategory.Habit, Rarity.Rare, "calendar", "累计学习天数 ≥ 21（不要求连续）",
        target = 21, progress = { it.activeDays },
        unlocked = { it.activeDays >= 21 }
    ),
    Achievement(
        "runs_20", "二十课次", "累计完成 20 课次（含重做）。",
        AchCategory.Habit, Rarity.Common, "dumbbell", "累计完成 20 课次（含重做）",
        target = 20, progress = { it.lessonRuns },
        unlocked = { it.lessonRuns >= 20 }
    ),
    Achievement(
        "comeback", "卷土重来", "连续学习中断后重新开始。中断不可怕，回来就好。",
        AchCategory.Habit, Rarity.Rare, "rotate-ccw", "连续 ≥3 天中断后重新开始学习",
        hidden = true,
        progress = { if (it.streakBroken) 1 else 0 },
        unlocked = { it.streakBroken }
    ),

    // ---------------- C · 学习深度与完成质量 ----------------
    Achievement(
        "perfect_1", "首个满分", "第一次全对通关。",
        AchCategory.Depth, Rarity.Common, "star", "任意 1 课获得 3 星（全对）",
        progress = { if (perfectCount(it) >= 1) 1 else 0 },
        unlocked = { perfectCount(it) >= 1 }
    ),
    Achievement(
        "perfect_5", "五次满分", "5 门课全对通关。",
        AchCategory.Depth, Rarity.Common, "star", "3 星课程数 ≥ 5",
        target = 5, progress = { perfectCount(it) },
        unlocked = { perfectCount(it) >= 5 }
    ),
    Achievement(
        "perfect_15", "十五满分", "15 门课全对通关。",
        AchCategory.Depth, Rarity.Rare, "star", "3 星课程数 ≥ 15",
        target = 15, progress = { perfectCount(it) },
        unlocked = { perfectCount(it) >= 15 }
    ),
    Achievement(
        "perfect_30", "三十满分", "30 门课全对通关。",
        AchCategory.Depth, Rarity.Epic, "star", "3 星课程数 ≥ 30",
        target = 30, progress = { perfectCount(it) },
        unlocked = { perfectCount(it) >= 30 }
    ),
    Achievement(
        "review_first", "温故知新", "第一次完成复习。",
        AchCategory.Depth, Rarity.Common, "book-open", "首次复习答对",
        progress = { if (it.reviewDone >= 1) 1 else 0 },
        unlocked = { it.reviewDone >= 1 }
    ),
    Achievement(
        "review_20", "复习二十", "复习答对累计 20 次。",
        AchCategory.Depth, Rarity.Rare, "book", "复习答对累计 ≥ 20 次",
        target = 20, progress = { it.reviewDone },
        unlocked = { it.reviewDone >= 20 }
    ),
    Achievement(
        "review_clear", "错题清零", "复习队列全部清空。",
        AchCategory.Depth, Rarity.Rare, "circle-check", "复习队列清空（至少答对过 1 次）",
        progress = { if (it.reviewDone >= 1 && it.reviewQueueSize == 0) 1 else 0 },
        unlocked = { it.reviewDone >= 1 && it.reviewQueueSize == 0 }
    ),
    Achievement(
        "altitude_6000", "海拔六千", "学习海拔突破 6,000 米。",
        AchCategory.Depth, Rarity.Common, "signal", "海拔 ≥ 6,000 m",
        target = 6000, progress = { it.altitude },
        unlocked = { it.altitude >= 6000 }
    ),
    Achievement(
        "final_exam", "终极考核", "通过终极综合考核（上、下）。",
        AchCategory.Depth, Rarity.Epic, "graduation-cap", "完成 m23 且 m24（终极综合考核）",
        target = 2,
        progress = { listOf("m23", "m24").count { id -> id in it.completed } },
        unlocked = { "m23" in it.completed && "m24" in it.completed }
    ),

    // ---------------- D · 探索与特殊里程碑 ----------------
    Achievement(
        "five_zones", "六区足迹", "攀登六个海拔单元都留下过完成记录。",
        AchCategory.Explore, Rarity.Rare, "map", "攀登 6 个单元各完成 ≥1 课（m5i / m5a / m6 / m7 / m7h / m8）",
        target = 6,
        progress = { unitsTouched(it, CLIMB_UNITS) },
        unlocked = { unitsTouched(it, CLIMB_UNITS) >= CLIMB_UNITS.size }
    ),
    Achievement(
        "eight_k_grad", "八千米毕业", "8000 米单元全部完成。",
        AchCategory.Explore, Rarity.Epic, "trophy", "单元 6（m8）全部 10 课完成",
        target = 10,
        progress = { it.unitProgress["m8"]?.first ?: 0 },
        unlocked = unitAll("m8")
    ),
    Achievement(
        "full_route", "攀登大满贯", "攀登 68 门课全部完成，理论路线走到终点。",
        AchCategory.Explore, Rarity.Legendary, "award", "攀登全部 68 课完成（m5i / m5a / m6 / m7 / m7h / m8 六个单元）",
        target = 68,
        progress = { unitsDone(it, CLIMB_UNITS) },
        unlocked = { unitsAllDone(it, CLIMB_UNITS) }
    ),
    Achievement(
        "fourteen_peaks", "十四座巡礼", "读完世界 14 座 8000 米级山峰。",
        AchCategory.Explore, Rarity.Epic, "flag", "完成 m21 且 m22（世界 14 座 8000 米）",
        target = 2,
        progress = { listOf("m21", "m22").count { id -> id in it.completed } },
        unlocked = { "m21" in it.completed && "m22" in it.completed }
    ),
    Achievement(
        "curious_dev", "好奇心", "发现了隐藏入口。",
        AchCategory.Explore, Rarity.Common, "lightbulb", "解锁开发者模式",
        hidden = true,
        progress = { if (it.devUnlocked) 1 else 0 },
        unlocked = { it.devUnlocked }
    ),
    Achievement(
        "safety_first", "安全至上", "高海拔两大杀手（HACE/HAPE、失温冻伤）全部学会。",
        AchCategory.Explore, Rarity.Rare, "square-check-big", "完成 l14 且 l16",
        target = 2,
        progress = { listOf("l14", "l16").count { id -> id in it.completed } },
        unlocked = { "l14" in it.completed && "l16" in it.completed }
    ),

    // ---------------- E · 徒步线（v0.19.27 新增） ----------------
    //
    // 徒步 21 课此前完全没有成就，走完整条线没有任何反馈。
    // 这里按「入门 → 两个单元 → 全线 → 跨线」四级铺开，
    // 与攀登的 l1 / 单元 / 大满贯同一套节奏。
    Achievement(
        "trail_first", "走出第一步", "完成第一门徒步课，路线从脚下开始。",
        AchCategory.Progress, Rarity.Common, "footprints", "完成 t01（线路难度与自我定位）",
        progress = { if ("t01" in it.completed) 1 else 0 },
        unlocked = lesson("t01")
    ),
    Achievement(
        "trail_camp", "扎营一夜", "多日徒步的营地那几件事，都过了一遍。",
        AchCategory.Progress, Rarity.Rare, "tent", "徒步单元 t2（多日与露营）全部 8 课完成",
        target = 8,
        progress = { it.unitProgress["t2"]?.first ?: 0 },
        unlocked = unitAll("t2")
    ),
    Achievement(
        "trail_high", "高原行走", "海拔升高之后，走路这件事本身要重新学。",
        AchCategory.Progress, Rarity.Rare, "mountain-snow", "徒步单元 t3（高原徒步）全部 5 课完成",
        target = 5,
        progress = { it.unitProgress["t3"]?.first ?: 0 },
        unlocked = unitAll("t3")
    ),
    Achievement(
        "trail_finish", "徒步毕业", "徒步 21 门课全部完成，长线也能自己带。",
        AchCategory.Progress, Rarity.Epic, "flag", "徒步全部 21 课完成（t1 / t2 / t3 / t4 四个单元）",
        target = 21,
        progress = { unitsDone(it, TREK_UNITS) },
        unlocked = { unitsAllDone(it, TREK_UNITS) }
    ),
    Achievement(
        "two_tracks", "双线并进", "攀登与徒步各走 10 课，两条路一起走。",
        AchCategory.Explore, Rarity.Epic, "map", "攀登完成 ≥10 课，且徒步完成 ≥10 课",
        target = 20,
        progress = { s ->
            minOf(trackDone(s, CLIMB_TRACK), 10) + minOf(trackDone(s, TREK_TRACK), 10)
        },
        unlocked = { s -> trackDone(s, CLIMB_TRACK) >= 10 && trackDone(s, TREK_TRACK) >= 10 }
    )
)

fun achievementById(id: String): Achievement? = ACHIEVEMENTS.firstOrNull { it.id == id }

/** 合法徽章字形键（与 ui/Badges.kt 的图标映射保持一致；contentcheck 用它校验定义）。 */
val ACHIEVEMENT_GLYPHS: Set<String> = setOf(
    "footprints", "mountain-snow", "target", "link", "heart-pulse", "map-pin", "tent",
    "backpack", "mountain", "snowflake", "cloud-snow", "compass", "flame", "calendar",
    "dumbbell", "rotate-ccw", "star", "book-open", "book", "circle-check", "signal",
    "graduation-cap", "map", "trophy", "award", "flag", "lightbulb", "square-check-big"
)

/**
 * 统一判定入口：返回「新解锁」的成就（按稀有度升序，传说最后登场）。
 * 幂等：入参已解锁集合中的成就不会再次返回。
 */
fun evaluate(stats: AchievementStats, alreadyUnlocked: Set<String>): List<Achievement> =
    ACHIEVEMENTS
        .filter { it.id !in alreadyUnlocked && it.unlocked(stats) }
        .sortedBy { it.rarity.ordinal }

/** 开发者模式自检：返回全部问题（空列表 = 健康）。 */
fun validateAchievements(validLessonIds: Set<String>, validUnitIds: Set<String>, glyphKeys: Set<String>): List<String> {
    val problems = mutableListOf<String>()
    val ids = ACHIEVEMENTS.map { it.id }
    ids.groupingBy { it }.eachCount().filter { it.value > 1 }.forEach {
        problems += "重复成就 ID：${it.key}"
    }
    ACHIEVEMENTS.forEach { a ->
        if (a.glyph !in glyphKeys) problems += "${a.id} 缺少徽章字形 ${a.glyph}"
        if (a.title.isBlank() || a.desc.isBlank()) problems += "${a.id} 名称或描述为空"
        """[lm]\d+[a-z]*""".toRegex().findAll(a.conditionText).forEach { m ->
            val ref = m.value
            if (ref !in validLessonIds && ref !in validUnitIds) {
                problems += "${a.id} 条件引用了不存在的课程/单元：$ref"
            }
        }
    }
    return problems
}
