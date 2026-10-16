import com.ascent.climb.data.Lesson
import com.ascent.climb.data.Peak
import com.ascent.climb.data.QType
import com.ascent.climb.data.Question
import com.ascent.climb.data.Track
import com.ascent.climb.data.Unit

/**
 * 题库 JSON 导出（仅用于生成在线题库文件，不参与 App 运行）。
 *
 * 手写序列化，避免让工具额外依赖任何 JSON 库。
 * 输出格式必须与 App 侧 data/bank/BankJson.kt 的解码逻辑一一对应（schema 2）。
 */
object BankExport {

    /** 与 App 侧 BankJson.SCHEMA 保持一致。 */
    const val SCHEMA = 2

    fun toJson(
        tracks: List<Track>,
        version: Int,
        updatedAt: String,
        minAppVersion: Int
    ): String {
        val sb = StringBuilder(1024 * 1024)
        sb.append("{\n")
        sb.append("  \"schema\": ").append(SCHEMA).append(",\n")
        sb.append("  \"version\": ").append(version).append(",\n")
        sb.append("  \"updatedAt\": \"").append(esc(updatedAt)).append("\",\n")
        sb.append("  \"minAppVersion\": ").append(minAppVersion).append(",\n")
        sb.append("  \"tracks\": [\n")
        tracks.forEachIndexed { ti, t ->
            sb.append("    {\n")
            sb.append("      \"id\": \"").append(esc(t.id)).append("\",\n")
            sb.append("      \"title\": \"").append(esc(t.title)).append("\",\n")
            sb.append("      \"subtitle\": \"").append(esc(t.subtitle)).append("\",\n")
            sb.append("      \"icon\": \"").append(esc(t.icon)).append("\",\n")
            sb.append("      \"units\": [\n")
            t.units.forEachIndexed { ui, u ->
                appendUnit(sb, u)
                sb.append(if (ui == t.units.lastIndex) "\n" else ",\n")
            }
            sb.append("      ]\n")
            sb.append("    }")
            sb.append(if (ti == tracks.lastIndex) "\n" else ",\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun appendUnit(sb: StringBuilder, u: Unit) {
        sb.append("        {\n")
        sb.append("          \"id\": \"").append(esc(u.id)).append("\",\n")
        sb.append("          \"index\": ").append(u.index).append(",\n")
        sb.append("          \"code\": \"").append(esc(u.code)).append("\",\n")
        sb.append("          \"title\": \"").append(esc(u.title)).append("\",\n")
        sb.append("          \"altitude\": ").append(u.altitude).append(",\n")
        sb.append("          \"track\": \"").append(esc(u.track)).append("\",\n")
        sb.append("          \"subtitle\": \"").append(esc(u.subtitle)).append("\",\n")
        sb.append("          \"peaks\": [")
        if (u.peaks.isEmpty()) {
            sb.append("],\n")
        } else {
            sb.append("\n")
            u.peaks.forEachIndexed { i, p ->
                appendPeak(sb, p)
                sb.append(if (i == u.peaks.lastIndex) "\n" else ",\n")
            }
            sb.append("          ],\n")
        }
        sb.append("          \"lessons\": [\n")
        u.lessons.forEachIndexed { li, l ->
            appendLesson(sb, l)
            sb.append(if (li == u.lessons.lastIndex) "\n" else ",\n")
        }
        sb.append("          ]\n")
        sb.append("        }")
    }

    private fun appendPeak(sb: StringBuilder, p: Peak) {
        sb.append("            {")
        sb.append("\"name\": \"").append(esc(p.name)).append("\", ")
        sb.append("\"altitude\": ").append(p.altitude).append(", ")
        sb.append("\"location\": \"").append(esc(p.location)).append("\", ")
        sb.append("\"style\": \"").append(esc(p.style)).append("\", ")
        sb.append("\"note\": \"").append(esc(p.note)).append("\"}")
    }

    private fun appendLesson(sb: StringBuilder, l: Lesson) {
        sb.append("            {\n")
        sb.append("              \"id\": \"").append(esc(l.id)).append("\",\n")
        sb.append("              \"title\": \"").append(esc(l.title)).append("\",\n")
        sb.append("              \"code\": \"").append(esc(l.code)).append("\",\n")
        sb.append("              \"icon\": \"").append(esc(l.icon)).append("\",\n")
        sb.append("              \"needsCoach\": ").append(l.needsCoach).append(",\n")
        sb.append("              \"brief\": [")
        l.brief.forEachIndexed { i, b ->
            if (i > 0) sb.append(", ")
            sb.append("\"").append(esc(b)).append("\"")
        }
        sb.append("],\n")
        sb.append("              \"questions\": [\n")
        l.questions.forEachIndexed { qi, q ->
            appendQuestion(sb, q)
            sb.append(if (qi == l.questions.lastIndex) "\n" else ",\n")
        }
        sb.append("              ]\n")
        sb.append("            }")
    }

    private fun appendQuestion(sb: StringBuilder, q: Question, pad: String = "                ") {
        sb.append(pad).append("{\n")
        sb.append(pad).append("  \"type\": \"").append(typeName(q.type)).append("\",\n")
        sb.append(pad).append("  \"prompt\": \"").append(esc(q.prompt)).append("\",\n")
        if (!q.context.isNullOrBlank()) {
            sb.append(pad).append("  \"context\": \"").append(esc(q.context)).append("\",\n")
        }
        if (!q.image.isNullOrBlank()) {
            sb.append(pad).append("  \"image\": \"").append(esc(q.image)).append("\",\n")
        }
        if (q.options.isNotEmpty()) {
            sb.append(pad).append("  \"options\": [")
            q.options.forEachIndexed { i, o ->
                if (i > 0) sb.append(", ")
                sb.append("\"").append(esc(o)).append("\"")
            }
            sb.append("],\n")
        }
        if (q.type == QType.Single || q.type == QType.Multi) {
            if (q.type == QType.Single) {
                sb.append(pad).append("  \"correct\": ").append(q.correct).append(",\n")
            }
        }
        if (q.correctSet.isNotEmpty()) {
            sb.append(pad).append("  \"correctSet\": ").append(q.correctSet.joinToString(",", "[", "]")).append(",\n")
        }
        if (q.correctOrder.isNotEmpty()) {
            sb.append(pad).append("  \"correctOrder\": ").append(q.correctOrder.joinToString(",", "[", "]")).append(",\n")
        }
        if (q.pairsLeft.isNotEmpty()) {
            sb.append(pad).append("  \"pairsLeft\": [")
            q.pairsLeft.forEachIndexed { i, s -> if (i > 0) sb.append(", "); sb.append("\"").append(esc(s)).append("\"") }
            sb.append("],\n")
            sb.append(pad).append("  \"pairsRight\": [")
            q.pairsRight.forEachIndexed { i, s -> if (i > 0) sb.append(", "); sb.append("\"").append(esc(s)).append("\"") }
            sb.append("],\n")
        }
        if (q.answers.isNotEmpty()) {
            sb.append(pad).append("  \"answers\": [")
            q.answers.forEachIndexed { i, s -> if (i > 0) sb.append(", "); sb.append("\"").append(esc(s)).append("\"") }
            sb.append("],\n")
        }
        if (q.tolerance != 0.0) {
            sb.append(pad).append("  \"tolerance\": ").append(q.tolerance).append(",\n")
        }
        if (!q.unit.isNullOrBlank()) {
            sb.append(pad).append("  \"unit\": \"").append(esc(q.unit)).append("\",\n")
        }
        if (q.hotspots.isNotEmpty()) {
            sb.append(pad).append("  \"hotspots\": [")
            q.hotspots.forEachIndexed { i, h ->
                if (i > 0) sb.append(", ")
                sb.append("{\"x\": ").append(h.x).append(", \"y\": ").append(h.y)
                sb.append(", \"r\": ").append(h.r)
                sb.append(", \"correct\": ").append(h.correct)
                if (h.label.isNotEmpty()) sb.append(", \"label\": \"").append(esc(h.label)).append("\"")
                sb.append("}")
            }
            sb.append("],\n")
        }
        if (q.steps.isNotEmpty()) {
            sb.append(pad).append("  \"steps\": [\n")
            q.steps.forEachIndexed { i, s ->
                appendQuestion(sb, s, "$pad    ")
                sb.append(if (i == q.steps.lastIndex) "\n" else ",\n")
            }
            sb.append(pad).append("  ],\n")
        }
        sb.append(pad).append("  \"explanation\": \"").append(esc(q.explanation)).append("\"")
        if (!q.source.isNullOrBlank()) {
            sb.append(",\n").append(pad).append("  \"source\": \"").append(esc(q.source)).append("\"")
        }
        if (q.safetyCritical) sb.append(",\n").append(pad).append("  \"safetyCritical\": true")
        if (q.review) sb.append(",\n").append(pad).append("  \"review\": true")
        sb.append("\n").append(pad).append("}")
    }

    private fun typeName(t: QType): String = when (t) {
        QType.Single -> "single"
        QType.Multi -> "multi"
        QType.Order -> "order"
        QType.Match -> "match"
        QType.Fill -> "fill"
        QType.Calc -> "calc"
        QType.Hotspot -> "hotspot"
        QType.Scenario -> "scenario"
    }

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') {
                    sb.append("\\u").append(String.format("%04x", ch.code))
                } else {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }
}
