package com.ascent.climb.data.cloud

import com.ascent.climb.data.ReviewItem
import org.json.JSONArray
import org.json.JSONObject

/** 一条训练记录（呼吸 / 步伐 / 装备默写 / 天气判读）。 */
data class DrillRecord(
    val id: String,
    val count: Int,
    val seconds: Int,
    val best: Int,
    val last: Long
)

/**
 * 一份学习进度快照。**这就是「导出文件」与「云同步」共用的格式**，
 * 换机迁移不需要第二套逻辑（CLOUD-PLAN.md §5）。
 *
 * 刻意不包含：主题、界面语言、当前轨道、开发者开关、首启引导是否完成、
 * 待播成就队列。前四个是「这台设备怎么用」，后两个是设备本地状态，
 * 同步过去只会让两台设备的观感互相打架。
 */
data class SyncPayload(
    val completed: Set<String> = emptySet(),
    val stars: Map<String, Int> = emptyMap(),
    val reviews: List<ReviewItem> = emptyList(),
    val achievements: Set<String> = emptySet(),
    val lessonRuns: Int = 0,
    val reviewDone: Int = 0,
    val days: Set<String> = emptySet(),
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val streakBroken: Boolean = false,
    val lastDay: String = "",
    val drills: List<DrillRecord> = emptyList()
) {

    fun isEmpty(): Boolean =
        completed.isEmpty() && achievements.isEmpty() && reviews.isEmpty() && days.isEmpty()

    fun toJson(): JSONObject = JSONObject().apply {
        put("schema", SCHEMA)
        put("exportedAt", System.currentTimeMillis() / 1000)
        put("completed", JSONArray(completed.sorted()))
        put("stars", JSONObject().also { o -> stars.forEach { (k, v) -> o.put(k, v) } })
        put("reviews", JSONArray().also { arr ->
            reviews.forEach { arr.put(JSONObject().put("k", it.key).put("lvl", it.level).put("due", it.due)) }
        })
        put("achievements", JSONArray(achievements.sorted()))
        put("counters", JSONObject().put("lessonRuns", lessonRuns).put("reviewDone", reviewDone))
        put("days", JSONArray(days.sorted()))
        put("streak", JSONObject()
            .put("current", streak)
            .put("max", maxStreak)
            .put("broken", streakBroken)
            .put("lastDay", lastDay))
        put("drills", JSONArray().also { arr ->
            drills.forEach {
                arr.put(JSONObject()
                    .put("id", it.id)
                    .put("count", it.count)
                    .put("seconds", it.seconds)
                    .put("best", it.best)
                    .put("last", it.last))
            }
        })
    }

    /**
     * 合并两份快照。规则见 CLOUD-PLAN.md §5.1：一律「取并集 / 取更大」，
     * 因此合并是**幂等且单调**的 —— 409 重试一定会收敛。
     *
     * `stars` / `drills` 取 max 是安全的方向：星级和最好成绩只可能因为练得更多而变高。
     * 复习项同等级时取更晚的到期日，避免两台设备把同一道题重复推给用户。
     */
    fun merge(other: SyncPayload): SyncPayload {
        val mergedStars = stars.toMutableMap()
        other.stars.forEach { (k, v) -> mergedStars[k] = maxOf(mergedStars[k] ?: 0, v) }

        val mergedReviews = LinkedHashMap<String, ReviewItem>()
        (reviews + other.reviews).forEach { item ->
            val cur = mergedReviews[item.key]
            mergedReviews[item.key] = when {
                cur == null -> item
                item.level > cur.level -> item
                item.level < cur.level -> cur
                // 同等级：谁到期早就听谁（宁可多复习一次，也不要漏）
                else -> if (item.due < cur.due) item else cur
            }
        }

        val mergedDrills = LinkedHashMap<String, DrillRecord>()
        (drills + other.drills).forEach { d ->
            val cur = mergedDrills[d.id]
            mergedDrills[d.id] = if (cur == null) d else DrillRecord(
                id = d.id,
                count = maxOf(cur.count, d.count),
                seconds = maxOf(cur.seconds, d.seconds),
                best = maxOf(cur.best, d.best),
                last = maxOf(cur.last, d.last)
            )
        }

        return SyncPayload(
            completed = completed + other.completed,
            stars = mergedStars,
            reviews = mergedReviews.values.sortedBy { it.key },
            achievements = achievements + other.achievements,
            lessonRuns = maxOf(lessonRuns, other.lessonRuns),
            reviewDone = maxOf(reviewDone, other.reviewDone),
            days = days + other.days,
            streak = maxOf(streak, other.streak),
            maxStreak = maxOf(maxStreak, other.maxStreak),
            streakBroken = streakBroken || other.streakBroken,
            lastDay = if (lastDay >= other.lastDay) lastDay else other.lastDay,
            drills = mergedDrills.values.sortedBy { it.id }
        )
    }

    companion object {
        const val SCHEMA = 1

        fun fromJson(o: JSONObject): SyncPayload {
            val stars = HashMap<String, Int>()
            o.optJSONObject("stars")?.let { s ->
                s.keys().forEach { k -> stars[k] = s.optInt(k, 0) }
            }

            val reviews = ArrayList<ReviewItem>()
            o.optJSONArray("reviews")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i) ?: continue
                    val key = r.optString("k", "")
                    if (key.isNotEmpty()) {
                        reviews.add(ReviewItem(key, r.optInt("lvl", 0), r.optLong("due", 0L)))
                    }
                }
            }

            val drills = ArrayList<DrillRecord>()
            o.optJSONArray("drills")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val d = arr.optJSONObject(i) ?: continue
                    val id = d.optString("id", "")
                    if (id.isNotEmpty()) {
                        drills.add(DrillRecord(
                            id = id,
                            count = d.optInt("count", 0),
                            seconds = d.optInt("seconds", 0),
                            best = d.optInt("best", 0),
                            last = d.optLong("last", 0L)
                        ))
                    }
                }
            }

            val counters = o.optJSONObject("counters")
            val streak = o.optJSONObject("streak")

            return SyncPayload(
                completed = o.optJSONArray("completed").toStringSet(),
                stars = stars,
                reviews = reviews,
                achievements = o.optJSONArray("achievements").toStringSet(),
                lessonRuns = counters?.optInt("lessonRuns", 0) ?: 0,
                reviewDone = counters?.optInt("reviewDone", 0) ?: 0,
                days = o.optJSONArray("days").toStringSet(),
                streak = streak?.optInt("current", 0) ?: 0,
                maxStreak = streak?.optInt("max", 0) ?: 0,
                streakBroken = streak?.optBoolean("broken", false) ?: false,
                lastDay = streak?.optString("lastDay", "") ?: "",
                drills = drills
            )
        }

        private fun JSONArray?.toStringSet(): Set<String> {
            if (this == null) return emptySet()
            val out = HashSet<String>(length())
            for (i in 0 until length()) {
                val v = optString(i, "")
                if (v.isNotEmpty()) out.add(v)
            }
            return out
        }
    }
}
