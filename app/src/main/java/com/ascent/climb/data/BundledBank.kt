package com.ascent.climb.data

/**
 * 课程装配（纯 Kotlin，不依赖 Android）。
 *
 * 三件事：
 *  1. 把所有课程定义汇成一个「课程池」（按 id 索引）；
 *  2. 按 CLIMB_CURRICULUM 的大纲把课组装成海拔单元；
 *  3. 跨单元按全局序号插入复习题。
 *
 * 大纲里声明了但还没写出来的课程会被自动跳过，不影响构建；
 * `tools/contentcheck` 会列出缺哪些课，方便按批次补齐。
 */

/** 每课自动插入的复习题数量。 */
private const val REVIEW_PER_LESSON = 2

/** 从第几课开始插入复习题（第一单元作为入门，不插复习）。 */
private const val REVIEW_START_INDEX = 6

/**
 * 课程池：现有内容 + 扩充包 + 实体书内容 + 新大纲课程。
 * 后写入的覆盖先写入的，便于用新版本替换旧课。
 */
private val LESSON_POOL: Map<String, Lesson> = buildMap {
    listOf(UNIT_1, UNIT_2, UNIT_3, UNIT_4, UNIT_5).forEach { u ->
        u.lessons.forEach { put(it.id, it) }
    }
    EXTRA_LESSONS.values.forEach { list -> list.forEach { put(it.id, it) } }
    BOOK_LESSONS.values.forEach { list -> list.forEach { put(it.id, it) } }
    NEW_LESSONS.forEach { put(it.id, it) }
}

/** 把扩充包 / 实体书里追加的题目并进课程。 */
private fun withExtraQuestions(l: Lesson): Lesson {
    val extra = EXTRA_QUESTIONS[l.id].orEmpty() +
        EXTRA_QUESTIONS_2[l.id].orEmpty() +
        EXTRA_QUESTIONS_3[l.id].orEmpty() +
        EXTRA_QUESTIONS_4[l.id].orEmpty() +
        EXTRA_QUESTIONS_5[l.id].orEmpty() +
        EXTRA_QUESTIONS_6[l.id].orEmpty() +
        EXTRA_QUESTIONS_7[l.id].orEmpty() +
        EXTRA_QUESTIONS_8[l.id].orEmpty() +
        EXTRA_QUESTIONS_9[l.id].orEmpty() +
        EXTRA_QUESTIONS_10[l.id].orEmpty() +
        EXTRA_QUESTIONS_11[l.id].orEmpty() +
        EXTRA_QUESTIONS_12[l.id].orEmpty() +
        EXTRA_QUESTIONS_13[l.id].orEmpty() +
        BOOK_QUESTIONS[l.id].orEmpty()
    return if (extra.isEmpty()) l else l.copy(questions = l.questions + extra)
}

/**
 * 给进阶课程自动插入复习题。
 *
 * 做法与多邻国一致：越靠后的课程，会穿插前面学过的内容重新作答。
 * 抽取是确定性的（以全局课序号为游标），同一课每次打开复习题一致；
 * 游标随课序号推进，使复习内容在单元内均匀覆盖。
 */
private fun withReviewQuestions(lessons: List<Lesson>): List<Lesson> {
    val out = ArrayList<Lesson>(lessons.size)
    for (i in lessons.indices) {
        val lesson = lessons[i]
        if (i < REVIEW_START_INDEX) { out.add(lesson); continue }
        val pool = ArrayList<Question>()
        for (j in 0 until i) pool.addAll(lessons[j].questions)
        if (pool.isEmpty()) { out.add(lesson); continue }
        val ownPrompts = lesson.questions.mapTo(HashSet()) { it.prompt }
        val picked = ArrayList<Question>(REVIEW_PER_LESSON)
        var cursor = (i * 5) % pool.size
        var guard = 0
        while (picked.size < REVIEW_PER_LESSON && guard < pool.size) {
            val q = pool[cursor % pool.size]
            if (q.prompt !in ownPrompts && picked.none { it.prompt == q.prompt }) {
                picked.add(q.copy(review = true))
            }
            cursor++
            guard++
        }
        out.add(if (picked.isEmpty()) lesson else lesson.copy(questions = lesson.questions + picked))
    }
    return out
}

/** 按大纲组装一个轨道。 */
private fun assemble(specs: List<UnitSpec>, track: String): List<Unit> {
    val skeleton = specs.map { spec ->
        val lessons = spec.lessonIds.mapNotNull { id ->
            LESSON_POOL[id]?.let { withExtraQuestions(it) }
        }
        spec to lessons
    }

    val flat = skeleton.flatMap { it.second }
    val reviewed = withReviewQuestions(flat)

    var cursor = 0
    return skeleton.map { (spec, lessons) ->
        val slice = reviewed.subList(cursor, cursor + lessons.size)
        cursor += lessons.size
        Unit(
            id = spec.id,
            index = spec.index,
            code = spec.code,
            title = spec.title,
            altitude = spec.altitude,
            lessons = slice,
            track = track,
            subtitle = spec.subtitle,
            peaks = spec.peaks
        )
    }.filter { it.lessons.isNotEmpty() }
}

/**
 * 构建随 APK 发布的内置题库。
 *
 * 在线题库不可用（离线、拉取失败、校验不通过、版本更旧）时永远回落到这里，
 * 因此 App 在完全离线环境下与纯离线版本行为完全一致。
 */
fun buildBundledUnits(): List<Unit> = assemble(CLIMB_CURRICULUM, CLIMB_TRACK)

/** 全部轨道。目前只有高山攀登有内容，其余轨道数据层已就绪，等接入内容。 */
fun buildBundledTracks(): List<Track> {
    val climb = Track(
        id = CLIMB_TRACK,
        title = "高山攀登",
        subtitle = "5000 → 8000 米，四级递进",
        icon = "\uD83C\uDFD4\uFE0F",
        units = buildBundledUnits()
    )
    return listOf(climb)
}

/** 大纲引用了但尚未写出的课程 id —— 供校验工具统计进度。 */
fun missingCurriculumLessons(): List<String> =
    CLIMB_CURRICULUM.flatMap { it.lessonIds }.filter { it !in LESSON_POOL }

/** 大纲规划的课程总数。 */
val CURRICULUM_LESSON_COUNT: Int = CLIMB_CURRICULUM.sumOf { it.lessonIds.size }
