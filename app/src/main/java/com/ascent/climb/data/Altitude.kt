package com.ascent.climb.data

/**
 * 海拔曲线（纯逻辑，不依赖 Android，便于 tools/contentcheck 直接校验）。
 *
 * 设计意图：把「学完一门课」映射到「向上爬升一段」，
 * 整条路线从大本营一路到 8,848 m（世界之巅）。
 *
 * 曲线形状：
 *   起步 3,200 m
 *   → 第一课直接拉到 4,800 m（大本营到前进营地的跃升）
 *   → 之后每课均分剩余高度
 *   → 最后剩下的零头并入最后一课，让冲顶段天然给得多一点
 *
 * 调整曲线只需要改下面三个常量。
 * 由于海拔是由「已完成课程」实时累加得出的，改完之后所有旧存档会自动重算。
 */

/** 起步海拔（大本营）。 */
const val BASE_ALTITUDE = 3200

/** 通关第一课后的海拔。 */
const val FIRST_LESSON_TARGET = 4800

/** 通关最后一课后的海拔。 */
const val SUMMIT_TARGET = 8848

/** 路线顺序（课的 id 列表）。用内置大纲的顺序，与在线题库生成源保持一致。 */
private val ROUTE_ORDER: List<String> by lazy {
    buildBundledUnits().flatMap { u -> u.lessons.map { it.id } }
}

/** 某一课通关可获得的海拔；不在路线中则返回 0。 */
fun altitudeAwardFor(lessonId: String): Int {
    val n = ROUTE_ORDER.size
    if (n == 0) return 0
    val i = ROUTE_ORDER.indexOf(lessonId)
    if (i < 0) return 0
    if (i == 0) return FIRST_LESSON_TARGET - BASE_ALTITUDE
    val slots = n - 1
    val remaining = SUMMIT_TARGET - FIRST_LESSON_TARGET
    val per = remaining / slots
    val carry = remaining - per * slots
    return if (i == n - 1) per + carry else per
}

/** 完成 [completed] 这些课程后的海拔。 */
fun altitudeFor(completed: Set<String>): Int {
    var a = BASE_ALTITUDE
    for (id in ROUTE_ORDER) if (id in completed) a += altitudeAwardFor(id)
    return a
}

/** 路线长度（课程数）。 */
fun routeLength(): Int = ROUTE_ORDER.size
