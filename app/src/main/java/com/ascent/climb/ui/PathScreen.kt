package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.ascent.climb.data.ALL_TRACKS
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.CLIMB_TRACK
import com.ascent.climb.data.TREK_TRACK
import com.ascent.climb.data.altitudeAfterUnit
import com.ascent.climb.data.altitudeBeforeUnit
import com.ascent.climb.data.currentLessonIdIn
import com.ascent.climb.data.isUnlockedIn
import com.ascent.climb.data.lessonsOfTrack
import com.ascent.climb.data.summitOfTrack
import com.ascent.climb.data.unitsOfTrack
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PathScreen(
    trackId: String,
    altitude: Int,
    streak: Int,
    rope: Int,
    completed: Set<String>,
    starsOf: (String) -> Int,
    devUnlockAll: Boolean = false,
    onOpenLesson: (String) -> Unit,
    onSwitchTrack: (String) -> Unit = {}
) {
    val units = unitsOfTrack(trackId)
    val pages = remember(units) { units.chunked(2) }
    val pageCount = pages.size.coerceAtLeast(1)
    val pager = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    var sheetOpen by remember { mutableStateOf(false) }

    val totalLessons = units.sumOf { it.lessons.size }
    val doneCount = units.sumOf { u -> u.lessons.count { it.id in completed } }
    val pct = if (totalLessons == 0) 0 else doneCount * 100 / totalLessons
    val currentId = currentLessonIdIn(trackId, completed)

    // 切大类后回到第一页，避免停在旧轨道的页码上
    LaunchedEffect(trackId) { pager.scrollToPage(0) }

    Box(modifier = Modifier.fillMaxSize().background(Ascent.DuoBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                StatsRow(altitude = altitude, streak = streak, rope = rope)
            }
            TrackSwitchRow(
                trackId = trackId,
                onOpenSheet = { sheetOpen = true }
            )
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { pageIndex ->
                val pageUnits = pages.getOrNull(pageIndex).orEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                ) {
                    pageUnits.forEach { unit ->
                        val globalIndex = units.indexOfFirst { it.id == unit.id }
                        val startAlt = altitudeBeforeUnit(trackId, globalIndex)
                        val endAlt = altitudeAfterUnit(trackId, globalIndex)
                        val unitDone = unit.lessons.count { it.id in completed }
                        if (globalIndex > 0) {
                            item(key = "div_${unit.id}") {
                                CampDivider(
                                    label = "${camps()[(unit.index - 1).mod(camps().size)]} · " +
                                        "${unitTitle(unit.id, unit.title)} · ${formatMeters(startAlt)} M"
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
                                    trackId = trackId,
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
                    if (pageIndex == pages.lastIndex) {
                        item(key = "summit") {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                                SummitBlock(
                                    toSummit = (summitOfTrack(trackId) - altitude).coerceAtLeast(0),
                                    done = doneCount,
                                    total = totalLessons,
                                    pct = pct
                                )
                            }
                        }
                    }
                }
            }
            PageDots(
                count = pageCount,
                current = pager.currentPage,
                onSelect = { target -> scope.launch { pager.animateScrollToPage(target) } }
            )
        }
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            TrackSheet(
                currentTrackId = trackId,
                onPick = { id ->
                    sheetOpen = false
                    onSwitchTrack(id)
                }
            )
        }
    }
}

/** 顶部两个大类胶囊：当前项实心，点任一项开底部弹层。 */
@Composable
private fun TrackSwitchRow(trackId: String, onOpenSheet: () -> Unit) {
    Row(
        // 下方留 14dp：胶囊与下面单元横幅之间需要一道明确的空隙，
        // 否则两颗胶囊贴着色块，视觉上会连成一块。
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (t in ALL_TRACKS) {
            val active = t.id == trackId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Ascent.UGreen else Color.White)
                    .border(1.5.dp, if (active) Ascent.UGreen else Ascent.DuoBorder, RoundedCornerShape(999.dp))
                    .clickable { onOpenSheet() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = trackLabel(t.id),
                    color = if (active) Color.White else Ascent.DuoFg2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun trackLabel(id: String): String = when (id) {
    TREK_TRACK -> stringResource(R.string.path_track_trek)
    CLIMB_TRACK -> stringResource(R.string.path_track_climb)
    else -> id
}

/** 底部弹层：列出全部大类，带简介与当前标记。 */
@Composable
private fun TrackSheet(currentTrackId: String, onPick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.track_sheet_title),
            color = Ascent.DuoFg,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = stringResource(R.string.track_sheet_sub),
            color = Ascent.DuoFg2,
            fontSize = 12.sp
        )
        for (t in ALL_TRACKS) {
            val active = t.id == currentTrackId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) Color(0xFFF3FBE8) else Color.White)
                    .border(
                        if (active) 2.dp else 1.5.dp,
                        if (active) Ascent.UGreen else Ascent.DuoBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onPick(t.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Ascent.UGreen else Color(0xFFF2F2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    // 大类图标：实心卡通（雪山 / 登山靴），替代原来的 ▲ / ≡ 文本字符
                    Icon(
                        imageVector = if (t.id == TREK_TRACK) Ic.TrackTrek else Ic.TrackClimb,
                        contentDescription = null,
                        tint = if (active) Color.White else Ascent.DuoFg2,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trackLabel(t.id), color = Ascent.DuoFg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "${t.subtitle} · ${t.units.sumOf { it.lessons.size }} 课",
                        color = Ascent.DuoFg2,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (active) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Ascent.UGreen)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(stringResource(R.string.track_current), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(stringResource(R.string.track_switch), color = Ascent.UGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = stringResource(R.string.track_sheet_note),
            color = Ascent.DuoFg2,
            fontSize = 11.sp
        )
    }
}

/** 三页的分页指示器。 */
@Composable
private fun PageDots(count: Int, current: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val active = i == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Ascent.UGreen else Ascent.DuoBorder)
                    .clickable { onSelect(i) }
                    .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
            )
        }
    }
}

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
    trackId: String,
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
                isUnlockedIn(trackId, l.id, completed, devUnlockAll) -> NodeState.Available
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
