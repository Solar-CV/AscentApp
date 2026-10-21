package com.ascent.climb.data

/**
 * 多轨道（课程大类）的运行时辅助函数。
 *
 * 两条轨道共用一套「完成课程 id」集合（id 全局唯一：攀登 l1/m01，徒步 t01），
 * 但各有自己的海拔曲线与解锁顺序：
 *   高山攀登  3200 → 4800 → 8848
 *   徒步       200 → 1200 → 5630
 *
 * 之所以单独成文件：Altitude.kt 是纯逻辑、被 tools/contentcheck 直接编译；
 * 这里依赖运行时题库（BankRepository），只给界面用。
 */

/** 一条轨道的海拔曲线参数。 */
data class TrackCurve(val base: Int, val firstTarget: Int, val summit: Int)

fun trackCurve(trackId: String): TrackCurve = when (trackId) {
    TREK_TRACK -> TrackCurve(base = 200, firstTarget = 1200, summit = 5630)
    else -> TrackCurve(base = BASE_ALTITUDE, firstTarget = FIRST_LESSON_TARGET, summit = SUMMIT_TARGET)
}

fun trackById(trackId: String): Track? = ALL_TRACKS.firstOrNull { it.id == trackId }

/** 某条轨道的单元（按路线顺序）。 */
fun unitsOfTrack(trackId: String): List<Unit> =
    ALL_TRACKS.firstOrNull { it.id == trackId }?.units ?: emptyList()

/** 某条轨道的课程（线性顺序）。 */
fun lessonsOfTrack(trackId: String): List<Lesson> = unitsOfTrack(trackId).flatMap { it.lessons }

/** 这一课属于哪条轨道；找不到时按攀登处理（老数据兜底）。 */
fun trackOfLesson(lessonId: String): String =
    ALL_TRACKS.firstOrNull { t -> t.units.any { u -> u.lessons.any { it.id == lessonId } } }?.id
        ?: CLIMB_TRACK

fun routeLengthOf(trackId: String): Int = lessonsOfTrack(trackId).size

/** 某一课在所属轨道里通关后的海拔增量。 */
fun altitudeAwardForTrack(trackId: String, lessonId: String): Int {
    val order = lessonsOfTrack(trackId).map { it.id }
    val n = order.size
    if (n == 0) return 0
    val i = order.indexOf(lessonId)
    if (i < 0) return 0
    val curve = trackCurve(trackId)
    if (i == 0) return curve.firstTarget - curve.base
    val slots = n - 1
    val remaining = curve.summit - curve.firstTarget
    val per = remaining / slots
    val carry = remaining - per * slots
    return if (i == n - 1) per + carry else per
}

/** 完成 [completed] 后，某条轨道上的海拔。 */
fun altitudeForTrack(trackId: String, completed: Set<String>): Int {
    var a = trackCurve(trackId).base
    for (lesson in lessonsOfTrack(trackId)) if (lesson.id in completed) a += altitudeAwardForTrack(trackId, lesson.id)
    return a
}

/** 某条轨道走完后能否到「峰顶」。 */
fun summitOfTrack(trackId: String): Int = trackCurve(trackId).summit

/**
 * 档案页展示用的目标刻度。
 *
 * 攀登沿用 8,000 这个对外说的整数目标（教学口径一直是「5000 → 8000」），
 * 徒步没有这个约定，直接用自己路线的最高点（冈仁波齐 5,630 m）。
 */
fun displaySummitOf(trackId: String): Int =
    if (trackId == CLIMB_TRACK) SUMMIT_ALTITUDE else summitOfTrack(trackId)

/** 轨道内的线性解锁：前面的课都完成，本节才开。 */
fun isUnlockedIn(trackId: String, lessonId: String, completed: Set<String>, unlockAll: Boolean = false): Boolean {
    if (unlockAll) return true
    val order = lessonsOfTrack(trackId).map { it.id }
    val i = order.indexOf(lessonId)
    if (i <= 0) return true
    return order.take(i).all { it in completed }
}

/** 轨道内当前该学的课。 */
fun currentLessonIdIn(trackId: String, completed: Set<String>): String? =
    lessonsOfTrack(trackId).firstOrNull { it.id !in completed }?.id

/** 轨道完成度（0–100）。 */
fun progressOfTrack(trackId: String, completed: Set<String>): Int {
    val total = routeLengthOf(trackId)
    if (total == 0) return 0
    val done = lessonsOfTrack(trackId).count { it.id in completed }
    return done * 100 / total
}

/** 某单元之前的累计海拔（用于横幅上的海拔区间）。 */
fun altitudeBeforeUnit(trackId: String, unitIndex: Int): Int {
    val units = unitsOfTrack(trackId)
    val ids = units.take(unitIndex).flatMap { u -> u.lessons.map { it.id } }.toSet()
    return altitudeForTrack(trackId, ids)
}

/** 某单元结束时的累计海拔。 */
fun altitudeAfterUnit(trackId: String, unitIndex: Int): Int {
    val units = unitsOfTrack(trackId)
    val ids = units.take(unitIndex + 1).flatMap { u -> u.lessons.map { it.id } }.toSet()
    return altitudeForTrack(trackId, ids)
}
