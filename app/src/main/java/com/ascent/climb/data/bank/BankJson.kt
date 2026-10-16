package com.ascent.climb.data.bank

import com.ascent.climb.data.CLIMB_TRACK
import com.ascent.climb.data.HotspotRegion
import com.ascent.climb.data.Lesson
import com.ascent.climb.data.Peak
import com.ascent.climb.data.QType
import com.ascent.climb.data.Question
import com.ascent.climb.data.Track
import com.ascent.climb.data.Unit
import org.json.JSONArray
import org.json.JSONObject

/** 一份题库载荷。 */
data class BankPayload(
    val schema: Int,
    val version: Int,
    val updatedAt: String,
    val minAppVersion: Int,
    val tracks: List<Track>
) {
    val units: List<Unit> get() = tracks.flatMap { it.units }
    val lessons: List<Lesson> get() = units.flatMap { it.lessons }
    val questionCount: Int get() = lessons.sumOf { it.questions.size }
}

/**
 * 题库 JSON 编解码。
 *
 * 使用 Android 内置的 org.json，不引入任何第三方依赖。
 * 解码遵循「宁可少收，不可崩溃」原则：未知字段忽略、单条格式错误只跳过该条。
 *
 * 兼容性：
 *  - 新格式顶层是 `tracks`（可容纳多个课程轨道）；
 *  - 旧格式顶层是 `units`，会被自动包装成「高山攀登」轨道，已发布的题库仍可用。
 */
object BankJson {

    /** 当前支持的 schema。 */
    const val SCHEMA = 2

    private fun strList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "")
            if (s.isNotEmpty()) out.add(s)
        }
        return out
    }

    private fun intList(arr: JSONArray?): List<Int> {
        if (arr == null) return emptyList()
        val out = ArrayList<Int>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.optInt(i, -1))
        return out
    }

    fun decode(text: String): BankPayload? = try {
        val root = JSONObject(text)
        val schema = root.optInt("schema", SCHEMA)
        val version = root.optInt("version", 0)
        val updatedAt = root.optString("updatedAt", "")
        val minApp = root.optInt("minAppVersion", 0)

        val tracksArr = root.optJSONArray("tracks")
        val tracks = ArrayList<Track>()
        if (tracksArr != null) {
            for (i in 0 until tracksArr.length()) {
                val t = tracksArr.optJSONObject(i) ?: continue
                decodeTrack(t)?.let { tracks.add(it) }
            }
        } else {
            // 兼容旧格式：只有 units
            val unitsArr = root.optJSONArray("units")
            if (unitsArr != null) {
                val units = parseUnits(unitsArr)
                if (units.isNotEmpty()) {
                    tracks.add(
                        Track(
                            id = CLIMB_TRACK,
                            title = "高山攀登",
                            subtitle = "",
                            icon = "\uD83C\uDFD4\uFE0F",
                            units = units
                        )
                    )
                }
            }
        }
        if (tracks.isEmpty()) null
        else BankPayload(schema, version, updatedAt, minApp, tracks)
    } catch (e: Exception) {
        null
    }

    private fun decodeTrack(o: JSONObject): Track? {
        val id = o.optString("id", "")
        if (id.isEmpty()) return null
        val unitsArr = o.optJSONArray("units") ?: return null
        val units = parseUnits(unitsArr)
        if (units.isEmpty()) return null
        return Track(
            id = id,
            title = o.optString("title", id),
            subtitle = o.optString("subtitle", ""),
            icon = o.optString("icon", "\uD83C\uDFD4\uFE0F"),
            units = units
        )
    }

    private fun parseUnits(arr: JSONArray): List<Unit> {
        val out = ArrayList<Unit>(arr.length())
        for (i in 0 until arr.length()) {
            val u = arr.optJSONObject(i) ?: continue
            decodeUnit(u)?.let { out.add(it) }
        }
        return out
    }

    private fun decodeUnit(o: JSONObject): Unit? {
        val id = o.optString("id", "")
        if (id.isEmpty()) return null
        val lessonsArr = o.optJSONArray("lessons") ?: JSONArray()
        val lessons = ArrayList<Lesson>(lessonsArr.length())
        for (i in 0 until lessonsArr.length()) {
            val l = lessonsArr.optJSONObject(i) ?: continue
            decodeLesson(l)?.let { lessons.add(it) }
        }
        if (lessons.isEmpty()) return null

        val peaksArr = o.optJSONArray("peaks")
        val peaks = ArrayList<Peak>(peaksArr?.length() ?: 0)
        if (peaksArr != null) {
            for (i in 0 until peaksArr.length()) {
                val p = peaksArr.optJSONObject(i) ?: continue
                val name = p.optString("name", "")
                if (name.isEmpty()) continue
                peaks.add(
                    Peak(
                        name = name,
                        altitude = p.optInt("altitude", 0),
                        location = p.optString("location", ""),
                        style = p.optString("style", ""),
                        note = p.optString("note", "")
                    )
                )
            }
        }

        return Unit(
            id = id,
            index = o.optInt("index", 0),
            code = o.optString("code", ""),
            title = o.optString("title", id),
            altitude = o.optInt("altitude", 0),
            lessons = lessons,
            track = o.optString("track", CLIMB_TRACK),
            subtitle = o.optString("subtitle", ""),
            peaks = peaks
        )
    }

    private fun decodeLesson(o: JSONObject): Lesson? {
        val id = o.optString("id", "")
        if (id.isEmpty()) return null
        val questionsArr = o.optJSONArray("questions") ?: JSONArray()
        val questions = ArrayList<Question>(questionsArr.length())
        for (i in 0 until questionsArr.length()) {
            val q = questionsArr.optJSONObject(i) ?: continue
            decodeQuestion(q)?.let { questions.add(it) }
        }
        if (questions.isEmpty()) return null

        return Lesson(
            id = id,
            title = o.optString("title", id),
            code = o.optString("code", ""),
            icon = o.optString("icon", "\uD83C\uDFD4\uFE0F"),
            brief = strList(o.optJSONArray("brief")),
            questions = questions,
            needsCoach = o.optBoolean("needsCoach", false)
        )
    }

    private fun decodeQuestion(o: JSONObject, depth: Int = 0): Question? {
        val prompt = o.optString("prompt", "")
        if (prompt.isEmpty()) return null

        val type = when (o.optString("type", "single").lowercase()) {
            "multi" -> QType.Multi
            "order" -> QType.Order
            "match" -> QType.Match
            "fill" -> QType.Fill
            "calc" -> QType.Calc
            "hotspot" -> QType.Hotspot
            "scenario" -> QType.Scenario
            else -> QType.Single
        }
        val options = strList(o.optJSONArray("options"))
        val correct = o.optInt("correct", -1)
        val correctSet = intList(o.optJSONArray("correctSet"))
        val correctOrder = intList(o.optJSONArray("correctOrder"))
        val pairsLeft = strList(o.optJSONArray("pairsLeft"))
        val pairsRight = strList(o.optJSONArray("pairsRight"))
        val answers = strList(o.optJSONArray("answers"))
        val image = o.optString("image", "").ifEmpty { null }
        val tolerance = o.optDouble("tolerance", 0.0)
        val unit = o.optString("unit", "").ifEmpty { null }

        val hotspotsArr = o.optJSONArray("hotspots")
        val hotspots = ArrayList<HotspotRegion>(hotspotsArr?.length() ?: 0)
        if (hotspotsArr != null) {
            for (i in 0 until hotspotsArr.length()) {
                val h = hotspotsArr.optJSONObject(i) ?: continue
                hotspots.add(
                    HotspotRegion(
                        x = h.optDouble("x", 0.0).toFloat(),
                        y = h.optDouble("y", 0.0).toFloat(),
                        r = h.optDouble("r", 0.1).toFloat(),
                        correct = h.optBoolean("correct", false),
                        label = h.optString("label", "")
                    )
                )
            }
        }

        // 情景题的子问题（只接受选择型，最多嵌套两层防死循环）
        val steps = ArrayList<Question>()
        if (type == QType.Scenario && depth < 2) {
            val arr = o.optJSONArray("steps")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val s = arr.optJSONObject(i) ?: continue
                    val d = decodeQuestion(s, depth + 1) ?: continue
                    if (d.type == QType.Single || d.type == QType.Multi) steps.add(d)
                }
            }
        }

        // 按题型校验必要字段，不合法就丢弃这一条
        when (type) {
            QType.Single -> if (options.size < 2 || correct !in options.indices) return null
            QType.Multi -> if (options.size < 2 || correctSet.isEmpty() ||
                correctSet.any { it !in options.indices }) return null
            QType.Order -> if (options.size < 2 || correctOrder.size != options.size ||
                correctOrder.toSet().size != options.size ||
                correctOrder.any { it !in options.indices }) return null
            QType.Match -> if (pairsLeft.size < 2 || pairsLeft.size != pairsRight.size) return null
            QType.Fill -> if (answers.isEmpty()) return null
            QType.Calc -> if (answers.isEmpty() || answers.none { it.toDoubleOrNull() != null }) return null
            QType.Hotspot -> if (image.isNullOrBlank() || hotspots.isEmpty() ||
                hotspots.none { it.correct }) return null
            QType.Scenario -> if (steps.size < 2) return null
        }

        return Question(
            prompt = prompt,
            context = o.optString("context", "").ifEmpty { null },
            options = options,
            correct = correct,
            explanation = o.optString("explanation", ""),
            safetyCritical = o.optBoolean("safetyCritical", false),
            review = o.optBoolean("review", false),
            source = o.optString("source", "").ifEmpty { null },
            type = type,
            correctSet = correctSet,
            correctOrder = correctOrder,
            pairsLeft = pairsLeft,
            pairsRight = pairsRight,
            answers = answers,
            image = image,
            tolerance = tolerance,
            unit = unit,
            hotspots = hotspots,
            steps = steps
        )
    }
}
