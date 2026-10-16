import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.ACHIEVEMENT_GLYPHS
import com.ascent.climb.data.AchievementStats
import com.ascent.climb.data.BASE_ALTITUDE
import com.ascent.climb.data.CURRICULUM_LESSON_COUNT
import com.ascent.climb.data.FIRST_LESSON_TARGET
import com.ascent.climb.data.Lesson
import com.ascent.climb.data.QType
import com.ascent.climb.data.SUMMIT_TARGET
import com.ascent.climb.data.Track
import com.ascent.climb.data.Unit
import com.ascent.climb.data.altitudeAwardFor
import com.ascent.climb.data.bank.BankConfig
import com.ascent.climb.data.buildBundledTracks
import com.ascent.climb.data.evaluate
import com.ascent.climb.data.missingCurriculumLessons
import com.ascent.climb.data.validateAchievements
import java.io.File

/**
 * 内容校验 + 在线题库导出（纯 JVM，不依赖 Android）。
 * 直接复用 App 的真实内容源文件，因此结论与线上行为一致。
 */

private fun lessonIndexIn(list: List<Lesson>, id: String): Int = list.indexOfFirst { it.id == id }

private fun isUnlockedIn(list: List<Lesson>, id: String, done: Set<String>, unlockAll: Boolean = false): Boolean {
    if (unlockAll) return true
    val i = lessonIndexIn(list, id)
    if (i <= 0) return true
    return list.take(i).all { it.id in done }
}

fun main() {
    val tracks: List<Track> = buildBundledTracks()
    val units: List<Unit> = tracks.flatMap { it.units }
    val lessons: List<Lesson> = units.flatMap { it.lessons }
    val questions = lessons.flatMap { it.questions }

    println("=== 海拔单元结构 ===")
    units.forEach { u ->
        val q = u.lessons.sumOf { it.questions.size }
        val r = u.lessons.sumOf { l -> l.questions.count { it.review } }
        println("  ${u.code.padEnd(14)} 课程=${u.lessons.size}  题量=$q  复习=$r  山峰=${u.peaks.size}  平均每课=${if (u.lessons.isEmpty()) 0 else q / u.lessons.size}")
        println("        每课题数=${u.lessons.map { it.questions.size }}")
    }

    println("=== 总计 ===")
    println("  轨道=${tracks.size}  单元=${units.size}  课程=${lessons.size}  题量=${questions.size}")
    println("  目标山峰=${units.sumOf { it.peaks.size }}")

    // ---- 题型分布 ----
    println("=== 题型分布 ===")
    QType.values().forEach { t ->
        val n = questions.count { it.type == t }
        if (n > 0) println("  ${t.name.padEnd(8)} $n")
    }

    // ---- 每课题量是否达标 ----
    println("=== 每课题量 ===")
    val under = lessons.filter { it.questions.size < 10 }
    println("  低于 10 题的课程：${under.size} / ${lessons.size}")
    under.take(30).forEach { println("    ${it.id} ${it.title} → ${it.questions.size} 题") }

    // ---- 大纲进度 ----
    val missing = missingCurriculumLessons()
    println("=== 大纲进度 ===")
    println("  大纲规划课程=$CURRICULUM_LESSON_COUNT  已实现=${CURRICULUM_LESSON_COUNT - missing.size}  待补齐=${missing.size}")
    if (missing.isNotEmpty()) println("  待补齐 id：${missing.joinToString(" ")}")

    // ---- 数据完整性 ----
    println("=== 海拔曲线 ===")
    run {
        var alt = BASE_ALTITUDE
        val cum = ArrayList<Int>()
        lessons.forEach { l -> alt += altitudeAwardFor(l.id); cum.add(alt) }
        val first = cum.firstOrNull() ?: 0
        val last = cum.lastOrNull() ?: 0
        val ok = first == FIRST_LESSON_TARGET && last == SUMMIT_TARGET
        println("  起步 ${BASE_ALTITUDE} m　→　第一课通关后 $first m　→　最后一课通关后 $last m")
        println("  目标：第一课 ${FIRST_LESSON_TARGET} m　最后一课 ${SUMMIT_TARGET} m")
        println("  海拔曲线=" + if (ok) "OK" else "失败！")
        val samples = (listOf(0, 1, 5, 11, 20, 32) + (cum.size - 2) + (cum.size - 1))
            .filter { it in 0 until cum.size }.distinct()
        println("  节点抽样：" + samples.joinToString("　") { "第${it + 1}课=${cum[it]}m" })
        val dup = lessons.map { altitudeAwardFor(it.id) }
        println("  单课奖励范围：${dup.minOrNull()}～${dup.maxOrNull()} m　合计增幅=${last - BASE_ALTITUDE} m")
    }

    println("=== 数据完整性 ===")
    var bad = 0
    questions.forEach { q ->
        when (q.type) {
            QType.Single -> if (q.options.size < 2 || q.correct !in q.options.indices) { bad++; println("    ✗ single 越界：${q.prompt.take(24)}") }
            QType.Multi -> if (q.options.size < 2 || q.correctSet.isEmpty() || q.correctSet.any { it !in q.options.indices }) { bad++; println("    ✗ multi 越界：${q.prompt.take(24)}") }
            QType.Order -> if (q.correctOrder.size != q.options.size || q.correctOrder.toSet().size != q.options.size) { bad++; println("    ✗ order 顺序不完整：${q.prompt.take(24)}") }
            QType.Match -> if (q.pairsLeft.size < 2 || q.pairsLeft.size != q.pairsRight.size) { bad++; println("    ✗ match 左右不等：${q.prompt.take(24)}") }
            QType.Fill -> if (q.answers.isEmpty()) { bad++; println("    ✗ fill 无答案：${q.prompt.take(24)}") }
            QType.Calc -> if (q.answers.isEmpty() || q.answers.none { it.toDoubleOrNull() != null }) {
                bad++; println("    ✗ calc 无有效数值答案：${q.prompt.take(24)}")
            }
            QType.Hotspot -> {
                val img = q.image.orEmpty()
                val f = File("../../app/src/main/assets/bank/img/$img")
                when {
                    img.isBlank() -> { bad++; println("    ✗ hotspot 未指定题图：${q.prompt.take(24)}") }
                    q.hotspots.none { it.correct } -> { bad++; println("    ✗ hotspot 无正确区域：${q.prompt.take(24)}") }
                    !f.exists() -> { bad++; println("    ✗ hotspot 题图不存在：$img") }
                }
            }
            QType.Scenario -> if (q.steps.size < 2 ||
                q.steps.any { it.type != QType.Single && it.type != QType.Multi }) {
                bad++; println("    ✗ scenario 子问题不合法：${q.prompt.take(24)}")
            }
        }
    }
    println("  格式错误=$bad")
    println("  空解析=${questions.count { it.explanation.isBlank() }}")
    println("  空题干=${questions.count { it.prompt.isBlank() }}")
    println("  有来源标注=${questions.count { !it.source.isNullOrBlank() }} / ${questions.size}")
    println("  题图引用=${questions.count { !it.image.isNullOrBlank() }} 道")
    println("  情景题=${questions.count { it.type == QType.Scenario }} 道（含子题 ${questions.filter { it.type == QType.Scenario }.sumOf { it.steps.size }} 个）")
    val dupIds = lessons.groupBy { it.id }.filter { it.value.size > 1 }.keys
    println("  重复课程 id=${dupIds.size}")

    // ---- 解锁逻辑 ----
    println("=== 解锁逻辑 ===")
    val empty = emptySet<String>()
    val allDone = lessons.map { it.id }.toSet()
    println("  空进度解锁=${lessons.count { isUnlockedIn(lessons, it.id, empty) }}（应为 1）")
    println("  开发者模式解锁=${lessons.count { isUnlockedIn(lessons, it.id, empty, true) }}（应为 ${lessons.size}）")
    println("  全完成时解锁=${lessons.count { isUnlockedIn(lessons, it.id, allDone) }}")

    // ---- 成就系统 ----
    println("=== 成就系统 ===")
    val achProblems = validateAchievements(
        validLessonIds = lessons.map { it.id }.toSet(),
        validUnitIds = units.map { it.id }.toSet(),
        glyphKeys = ACHIEVEMENT_GLYPHS
    )
    println("  成就数量=${ACHIEVEMENTS.size}　定义有效性=" + if (achProblems.isEmpty()) "OK" else "失败！")
    achProblems.forEach { println("    ✗ $it") }

    val emptyStats = AchievementStats(
        completed = emptySet(), stars = emptyMap(), maxStreak = 0, activeDays = 0,
        lessonRuns = 0, reviewDone = 0, reviewQueueSize = 0, altitude = BASE_ALTITUDE,
        streakBroken = false, devUnlocked = false,
        unitProgress = units.associate { it.id to (0 to it.lessons.size) }
    )
    println("  空进度解锁=${evaluate(emptyStats, emptySet()).size}（应为 0）")

    val fullStats = AchievementStats(
        completed = allDone, stars = lessons.associate { it.id to 3 },
        maxStreak = 30, activeDays = 21, lessonRuns = 50, reviewDone = 25,
        reviewQueueSize = 0, altitude = SUMMIT_TARGET, streakBroken = true, devUnlocked = true,
        unitProgress = units.associate { u -> u.id to (u.lessons.size to u.lessons.size) }
    )
    println("  满进度解锁=${evaluate(fullStats, emptySet()).size} / ${ACHIEVEMENTS.size}（应全部可达）")
    println("  幂等检查=${evaluate(fullStats, ACHIEVEMENTS.map { it.id }.toSet()).size}（应为 0，不重复发放）")

    val firstStats = emptyStats.copy(
        completed = setOf("l1"),
        unitProgress = units.associate { u -> u.id to (u.lessons.count { it.id == "l1" } to u.lessons.size) }
    )
    println("  完成第 1 课解锁=${evaluate(firstStats, emptySet()).map { it.id }}（应含 first_steps）")

    // ---- 导出 ----
    val version = BankConfig.BUNDLED_VERSION
    val minApp = BankConfig.MIN_APP_VERSION
    val updatedAt = java.time.OffsetDateTime.now().toString()
    val json = BankExport.toJson(tracks, version, updatedAt, minApp)

    val outDir = File("../../bank")
    outDir.mkdirs()
    val bankFile = File(outDir, "bank.json")
    bankFile.writeText(json)

    val sha = sha256Hex(json)
    File(outDir, "manifest.json").writeText(buildString {
        append("{\n")
        append("  \"version\": ").append(version).append(",\n")
        append("  \"updatedAt\": \"").append(updatedAt).append("\",\n")
        append("  \"file\": \"bank.json\",\n")
        append("  \"sha256\": \"").append(sha).append("\",\n")
        append("  \"minAppVersion\": ").append(minApp).append("\n")
        append("}\n")
    })

    println("=== 导出 ===")
    println("  bank.json  ${bankFile.length() / 1024} KB   schema=${BankExport.SCHEMA} version=$version minAppVersion=$minApp")
    println("  sha256     $sha")
}

private fun sha256Hex(text: String): String {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        val v = b.toInt() and 0xFF
        sb.append(Character.forDigit(v shr 4, 16))
        sb.append(Character.forDigit(v and 0x0F, 16))
    }
    return sb.toString()
}
