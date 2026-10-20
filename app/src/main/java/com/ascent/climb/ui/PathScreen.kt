package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.BASE_ALTITUDE
import com.ascent.climb.data.Lesson
import com.ascent.climb.data.SUMMIT_TARGET
import com.ascent.climb.data.Unit as ClimbUnit
import com.ascent.climb.data.altitudeFor
import com.ascent.climb.data.currentLessonId
import com.ascent.climb.data.isUnlocked
import kotlin.math.cos
import kotlin.math.sin

/** 单元营地名（用于单元横幅 kicker 与分区线）。 */
@Composable
private fun camps(): List<String> = listOf(
    stringResource(R.string.path_camp_base),
    stringResource(R.string.path_camp_c1),
    stringResource(R.string.path_camp_c2),
    stringResource(R.string.path_camp_c3),
    stringResource(R.string.path_camp_high)
)

/**
 * 单元配色（参照设计稿 §7.5 派生规则：面 = 亮色，唇 = 面加深约 20%）。
 * 绿 → 蓝 → 紫 → 橙 → 青绿，每个单元一个色。
 */
private val UNIT_HUES = listOf(
    Color(0xFF58CC02), // 1 · 草甸绿
    Color(0xFF1CB0F6), // 2 · 冰川蓝
    Color(0xFFCE82FF), // 3 · 高原紫
    Color(0xFFFF9600), // 4 · 岩橙
    Color(0xFF00CD9C)  // 5 · 雪青绿
)
private val UNIT_LIPS = listOf(
    Color(0xFF46A302),
    Color(0xFF1899D6),
    Color(0xFFA964E8),
    Color(0xFFCC7A00),
    Color(0xFF00B08A)
)

private fun unitHue(index: Int): Color = UNIT_HUES[(index - 1).mod(UNIT_HUES.size)]
private fun unitLip(index: Int): Color = UNIT_LIPS[(index - 1).mod(UNIT_LIPS.size)]

/** 横幅副标题用的浅绿。 */
private val GREEN_PALE = Color(0xFFD8F5B8)

/**
 * 真椭圆形状。
 * ⚠️ 不能用 CircleShape：它是 RoundedCornerShape(50%)，而百分比圆角是按
 * **短边**算半径的，非正方形盒子会得到「胶囊/圆角矩形」而不是椭圆。
 */
private val OvalShape: Shape = GenericShape { size, _ ->
    addOval(Rect(Offset.Zero, size))
}

private enum class NodeState { Done, Current, Available, Locked, Trophy }

private class PathEntry(val lesson: Lesson?, val state: NodeState, val stars: Int)

/**
 * S 形蜿蜒的横向偏移（dp，周期 6）：0 → 右 → 半右 → 0 → 左 → 半左。
 */
private val ZIGZAG = listOf(0.dp, 64.dp, 44.dp, 0.dp, -44.dp, -64.dp)
private val PATH_STEP = 185.dp
private val NODE_W = 79.dp
private val NODE_H = 58.dp
private val NODE_LIP = 11.dp
private val HALO_W = 104.dp
private val HALO_H = 83.dp
private val GLYPH = 34.dp

@Composable
fun PathScreen(
    altitude: Int,
    streak: Int,
    rope: Int,
    completed: Set<String>,
    starsOf: (String) -> Int,
    devUnlockAll: Boolean = false,
    onOpenLesson: (String) -> Unit
) {
    val currentId = currentLessonId(completed)
    val total = ALL_LESSONS.size
    val doneCount = completed.size
    val pct = if (total == 0) 0 else doneCount * 100 / total

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ascent.DuoBg),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "stats") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp)) {
                StatsRow(altitude = altitude, streak = streak, rope = rope)
            }
        }

        ALL_UNITS.forEachIndexed { ui, unit ->
            val startAlt = altitudeBefore(ui)
            val endAlt = altitudeAfter(ui)
            val unitDone = unit.lessons.count { it.id in completed }
            if (ui > 0) {
                item(key = "div_${unit.id}") {
                    CampDivider(
                        label = "${camps()[ui.mod(camps().size)]} · ${unitTitle(unit.id, unit.title)} · ${formatMeters(startAlt)} M"
                    )
                }
            }
            item(key = "u_${unit.id}") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    UnitBanner(
                        unit = unit,
                        done = unitDone,
                        startAlt = startAlt,
                        endAlt = endAlt,
                        hue = unitHue(unit.index),
                        lip = unitLip(unit.index)
                    )
                }
            }
            item(key = "p_${unit.id}") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    LessonPath(
                        unit = unit,
                        lessons = unit.lessons,
                        completed = completed,
                        currentId = currentId,
                        starsOf = starsOf,
                        devUnlockAll = devUnlockAll,
                        startAlt = startAlt,
                        endAlt = endAlt,
                        hue = unitHue(unit.index),
                        lip = unitLip(unit.index),
                        onOpenLesson = onOpenLesson
                    )
                }
            }
        }

        item(key = "summit") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                SummitBlock(
                    toSummit = (SUMMIT_TARGET - altitude).coerceAtLeast(0),
                    done = doneCount,
                    total = total,
                    pct = pct
                )
            }
        }
    }
}

/** 该单元之前的累计海拔（用于「3,200 – 4,800 M」范围与分区线）。 */
private fun altitudeBefore(unitIndex: Int): Int =
    altitudeFor(ALL_UNITS.take(unitIndex).flatMap { u -> u.lessons.map { it.id } }.toSet())

private fun altitudeAfter(unitIndex: Int): Int =
    altitudeFor(ALL_UNITS.take(unitIndex + 1).flatMap { u -> u.lessons.map { it.id } }.toSet())

// ============================================================
// 顶部数值条
// ============================================================

@Composable
private fun StatsRow(altitude: Int, streak: Int, rope: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.MountainSnow, contentDescription = null, tint = Ascent.UGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatMeters(altitude),
                color = Ascent.DuoFg,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MonoNumbers
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "M",
                color = Ascent.DuoFg2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoNumbers,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            StatChip(Ic.Flame, "$streak", Ascent.Streak)
            StatChip(Ic.Link, "$rope", Ascent.UBlue)
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = value,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = DisplayFont
        )
    }
}

// ============================================================
// 单元横幅 + 分区线
// ============================================================

@Composable
private fun UnitBanner(unit: ClimbUnit, done: Int, startAlt: Int, endAlt: Int, hue: Color, lip: Color) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(hue)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.path_unit_meta, unit.index, camps()[(unit.index - 1).mod(camps().size)]),
                color = GREEN_PALE,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(unitTitle(unit.id, unit.title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.path_lessons_range, unit.lessons.size, formatMeters(startAlt), formatMeters(endAlt)),
                color = GREEN_PALE,
                fontSize = 11.sp,
                fontFamily = MonoNumbers,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(lip),
            contentAlignment = Alignment.Center
        ) {
            Icon(Ic.BookOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

/** 营地区分线：—— 文本 —— */
@Composable
private fun CampDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(2.dp).background(Ascent.DuoBorder))
        Text(
            text = label,
            color = Ascent.DuoFg3,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(Modifier.weight(1f).height(2.dp).background(Ascent.DuoBorder))
    }
}

// ============================================================
// 课程入口（无高光椭圆盘）
// ============================================================

@Composable
private fun LessonPath(
    unit: ClimbUnit,
    lessons: List<Lesson>,
    completed: Set<String>,
    currentId: String?,
    starsOf: (String) -> Int,
    devUnlockAll: Boolean,
    startAlt: Int,
    endAlt: Int,
    hue: Color,
    lip: Color,
    onOpenLesson: (String) -> Unit
) {
    if (lessons.isEmpty()) return

    val entries = buildList {
        lessons.forEach { l ->
            val st = when {
                l.id in completed -> NodeState.Done
                l.id == currentId -> NodeState.Current
                isUnlocked(l.id, completed, devUnlockAll) -> NodeState.Available
                else -> NodeState.Locked
            }
            add(PathEntry(l, st, starsOf(l.id)))
        }
        if (lessons.all { it.id in completed }) add(PathEntry(null, NodeState.Trophy, 0))
    }
    val n = entries.size
    val boardH = PATH_STEP * n
    val lastLessonIdx = lessons.size - 1
    val peak = unit.peaks.firstOrNull()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(boardH)) {
        val wDp = maxWidth
        fun xDp(i: Int): Dp = wDp / 2 + ZIGZAG[i % ZIGZAG.size]
        fun yDp(i: Int): Dp = PATH_STEP / 2 + PATH_STEP * i

        // 起点海拔侧标（左上）
        Box(modifier = Modifier.offset(x = 2.dp, y = 34.dp).width(64.dp)) {
            Text(
                text = "${formatMeters(startAlt)} M",
                color = Ascent.DuoFg3,
                fontSize = 10.sp,
                fontFamily = MonoNumbers,
                fontWeight = FontWeight.Medium
            )
        }
        // 终点海拔侧标（终点节点右侧）
        val lastIdx = entries.lastIndex
        Box(
            modifier = Modifier
                .offset(x = wDp - 66.dp, y = yDp(lastIdx) - 8.dp)
                .width(64.dp)
        ) {
            Text(
                text = "${formatMeters(endAlt)} M",
                color = Ascent.DuoFg3,
                fontSize = 10.sp,
                fontFamily = MonoNumbers,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // 帐篷装饰
        Box(
            modifier = Modifier
                .offset(x = wDp - 24.dp - 26.dp, y = boardH * 0.66f)
                .size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Ic.Tent, contentDescription = null, tint = hue.copy(alpha = 0.38f), modifier = Modifier.size(24.dp))
        }

        entries.forEachIndexed { i, e ->
            val x = xDp(i)
            val y = yDp(i)
            val state = e.state
            val finale = i == lastLessonIdx

            val face = when {
                state == NodeState.Trophy -> Ascent.UGold
                state == NodeState.Locked -> Ascent.LockFace2
                finale -> Ascent.UGold
                else -> hue
            }
            val lipColor = when {
                state == NodeState.Trophy -> Ascent.UGoldLip
                state == NodeState.Locked -> Ascent.LockLip3
                finale -> Ascent.UGoldLip
                else -> lip
            }
            val glyphColor = when {
                state == NodeState.Locked -> Ascent.LockGlyph
                state == NodeState.Trophy || finale -> Ascent.GoldGlyph
                else -> Color.White
            }

            // 当前入口：椭圆环
            if (state == NodeState.Current) {
                Box(
                    modifier = Modifier
                        .offset(x = x - HALO_W / 2, y = y - HALO_H / 2)
                        .width(HALO_W)
                        .height(HALO_H)
                        .clip(OvalShape)
                        .background(face.copy(alpha = 0.14f))
                        .border(2.dp, face.copy(alpha = 0.55f), OvalShape)
                )
            }

            // 底座（下移 NODE_LIP，形成厚度）
            Box(
                modifier = Modifier
                    .offset(x = x - NODE_W / 2, y = y - NODE_H / 2 + NODE_LIP)
                    .width(NODE_W)
                    .height(NODE_H)
                    .clip(OvalShape)
                    .background(lipColor)
            )
            // 侧壁（直边）：夹在两个椭圆的圆心之间，提供圆柱侧面的竖直边
            // —— 缺这一件就会看成「两个重叠的椭圆」而不是一个立体盘
            Box(
                modifier = Modifier
                    .offset(x = x - NODE_W / 2, y = y)
                    .width(NODE_W)
                    .height(NODE_LIP)
                    .background(lipColor)
            )
            // 盘面
            Box(
                modifier = Modifier
                    .offset(x = x - NODE_W / 2, y = y - NODE_H / 2)
                    .width(NODE_W)
                    .height(NODE_H)
                    .clip(OvalShape)
                    .background(face)
                    .clickable(enabled = e.lesson != null && state != NodeState.Locked) {
                        e.lesson?.let { onOpenLesson(it.id) }
                    }
            )
            // 字形
            Box(
                modifier = Modifier
                    .offset(x = x - NODE_W / 2, y = y - NODE_H / 2)
                    .width(NODE_W)
                    .height(NODE_H),
                contentAlignment = Alignment.Center
            ) {
                NodeGlyph(state, glyphColor, GLYPH)
            }

            // 「开始」气泡（带尾巴）
            if (state == NodeState.Current) {
                val bubbleW = 120.dp
                Column(
                    modifier = Modifier
                        .offset(x = x - bubbleW / 2, y = y - NODE_H / 2 - 52.dp)
                        .width(bubbleW),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(face)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.common_start), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Canvas(modifier = Modifier.size(14.dp, 8.dp)) {
                        val p = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(p, face)
                    }
                }
            }

            // 星级（完成）
            if (state == NodeState.Done && e.stars > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = x - 84.dp, y = y + NODE_H / 2 + NODE_LIP + 6.dp)
                        .width(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(3) { k -> MiniStar(filled = k < e.stars, diameter = 13.dp, tint = Ascent.UGold) }
                    }
                }
            }

            // 课程名
            Box(
                modifier = Modifier
                    .offset(
                        x = x - 84.dp,
                        y = y + NODE_H / 2 + NODE_LIP + if (state == NodeState.Done && e.stars > 0) 24.dp else 6.dp
                    )
                    .width(168.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val label = when {
                    e.lesson != null -> lessonTitle(e.lesson.id, e.lesson.title)
                    peak != null -> stringResource(R.string.path_summit, peakName(peak.name), formatMeters(peak.altitude))
                    else -> stringResource(R.string.path_unit_clear)
                }
                Text(
                    text = label,
                    color = when {
                        e.lesson == null -> Ascent.UGoldLip
                        state == NodeState.Current -> lip
                        state == NodeState.Available -> lip
                        state == NodeState.Locked -> Ascent.DuoFg3
                        else -> Ascent.DuoFg3
                    },
                    fontSize = 11.sp,
                    fontWeight = if (state == NodeState.Current || e.lesson == null) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 手绘卡通字形：对勾 / 播放三角 / 星 / 挂锁。
 * 统一做法：先填充，再叠一层同形状的粗圆角描边 → 尖角圆润、造型敦实。
 */
@Composable
private fun NodeGlyph(state: NodeState, color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val round = w * 0.17f
        val pad = round / 2f
        val iw = w - round
        val ih = h - round

        fun plump(p: Path) {
            drawPath(p, color)
            drawPath(p, color, style = Stroke(width = round, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        when (state) {
            NodeState.Current -> {
                val p = Path().apply {
                    moveTo(pad + iw * 0.22f, pad + ih * 0.05f)
                    lineTo(pad + iw * 0.22f, pad + ih * 0.95f)
                    lineTo(pad + iw * 0.95f, pad + ih * 0.50f)
                    close()
                }
                plump(p)
            }
            NodeState.Trophy, NodeState.Available, NodeState.Locked -> plump(starPath(pad, pad, iw, ih, 5))
            else -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.52f)
                    lineTo(w * 0.41f, h * 0.75f)
                    lineTo(w * 0.82f, h * 0.26f)
                }
                drawPath(
                    p,
                    color,
                    style = Stroke(width = w * 0.20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/** 实心小星（星级评分）。 */
@Composable
private fun MiniStar(filled: Boolean, diameter: Dp, tint: Color) {
    val c = if (filled) tint else Ascent.DuoBorder
    Canvas(modifier = Modifier.size(diameter)) {
        drawPath(starPath(0f, 0f, this.size.width, this.size.height, 5), c)
    }
}

/** 五角星路径（内接于给定矩形；x0/y0 为左上偏移）。 */
private fun starPath(x0: Float, y0: Float, w: Float, h: Float, points: Int): Path {
    val cx = x0 + w / 2f
    val cy = y0 + h / 2f
    val outer = minOf(w, h) / 2f
    val inner = outer * 0.48f
    val p = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = (-Math.PI / 2.0) + i * Math.PI / points
        val px = cx + (cos(a) * r).toFloat()
        val py = cy + (sin(a) * r).toFloat()
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
    return p
}

// ============================================================
// 底部：距顶峰插画块
// ============================================================

@Composable
private fun SummitBlock(toSummit: Int, done: Int, total: Int, pct: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(96.dp)) {
            Icon(
                Ic.Mountain,
                contentDescription = null,
                tint = Ascent.DuoFg3,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .offset(x = 62.dp, y = 4.dp)
                    .size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Ic.Flag, contentDescription = null, tint = Ascent.UGold, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.path_to_summit, formatMeters(toSummit)),
            color = Ascent.DuoFg2,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoNumbers
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.path_done_pct, done, total, pct),
            color = Ascent.DuoFg3,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Ascent.DuoBorder)
        ) {
            if (pct > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ascent.UGreen)
                )
            }
        }
    }
}
