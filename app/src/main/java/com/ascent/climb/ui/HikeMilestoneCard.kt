package com.ascent.climb.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.R
import com.ascent.climb.data.HikeRegion
import com.ascent.climb.data.HikingRoute
import com.ascent.climb.data.HikingRoutes
import com.ascent.climb.data.RegionHint

/**
 * 徒步路线里程碑卡片（折叠式）。
 *
 * 与山峰卡同构：折叠态两行，展开才给海拔剖面与数据。
 * 没有实景图，也没有 DEM —— 路线的主体是「高度随距离怎么变」，
 * 所以视觉主体是一张按主要营地高程画的剖面线，而不是一张山体图。
 */
@Composable
fun HikeMilestoneCard(
    route: HikingRoute,
    currentAlt: Int,
    unlockedCount: Int,
    onOpenAtlas: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = squircle(18.dp)
    val remaining = route.milestoneAlt - currentAlt
    val reached = remaining <= 0
    val name = routeName(route)
    val separator = "  ·  "

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ascent.Surface)
            .border(1.dp, Ascent.Border, shape)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteGlyph(
                modifier = Modifier.size(18.dp),
                color = Ascent.Fg3
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Ascent.Fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${formatMeters(route.maxAlt)} m",
                        color = Ascent.Fg2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mtn_current, formatMeters(currentAlt)),
                        color = Ascent.Fg3,
                        fontSize = 10.5.sp
                    )
                    Text(
                        text = separator + if (reached) stringResource(R.string.mtn_unlocked)
                        else stringResource(R.string.mtn_remaining, formatMeters(remaining)),
                        color = if (reached) Ascent.Success else Ascent.Fg3,
                        fontSize = 10.5.sp,
                        fontWeight = if (reached) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Ic.ChevronDown,
                contentDescription = null,
                tint = Ascent.Fg3,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(if (expanded) 180f else 0f)
            )
        }

        if (expanded) {
            // 卡片上只放 brief（一句话）；长介绍留给百科详情，否则卡片会被撑得很长
            HikeDetailBody(route = route, brief = routeBrief(route))

            Spacer(Modifier.height(11.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ascent.Line)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(squircle(10.dp))
                    .clickable { onOpenAtlas() }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (unlockedCount > 0)
                        stringResource(R.string.hike_jump_count, unlockedCount)
                    else stringResource(R.string.hike_jump_none),
                    color = Ascent.Fg2,
                    fontSize = 11.5.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Ic.ChevronRight,
                    contentDescription = null,
                    tint = Ascent.Fg3,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** 卡片展开态与百科详情共用的正文。 */
@Composable
private fun HikeDetailBody(
    route: HikingRoute,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 146.dp,
    brief: String? = null
) {
    val name = routeName(route)
    val area = routeArea(route)

    val hasImage = route.drawable != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            // 有图时框高按图片比例算（1080:416），图片正好填满，不留白也不裁切；
            // 无图的剖面图仍用调用方给的 chartHeight。
            .then(
                if (hasImage) Modifier.aspectRatio(1080f / 416f)
                else Modifier.height(chartHeight)
            )
            .clip(squircle(14.dp))
            // 有图时用纯白：地图本身白底，才不会显出灰边框
            .background(if (hasImage) Color.White else Ascent.Surface2)
            .background(if (hasImage) Color.Transparent else Ascent.Fg3.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            Image(
                painter = painterResource(route.drawable!!),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                RouteProfileChart(
                    profile = route.profile,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        if (name != route.nameEn) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = route.nameEn,
                color = Ascent.Fg3,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.hike_high_line,
                formatMeters(route.maxAlt), area
            ),
            color = Ascent.Fg2,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(11.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ascent.Line))
        Spacer(Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HikeStat(
                stringResource(R.string.hike_metric_distance),
                "${route.distanceKm} km",
                Modifier.weight(1f)
            )
            HikeStat(
                stringResource(R.string.hike_metric_gain),
                "${formatMeters(route.gainM)} m",
                Modifier.weight(1f)
            )
            HikeStat(
                stringResource(R.string.hike_metric_days),
                pluralStringResource(R.plurals.hike_days, route.days, route.days),
                Modifier.weight(1f)
            )
        }

        if (!brief.isNullOrBlank()) {
            Spacer(Modifier.height(11.dp))
            Text(
                text = brief,
                color = Ascent.Fg2,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(Modifier.height(9.dp))
        Text(
            text = stringResource(
                if (route.drawable != null) R.string.hike_note_map else R.string.hike_note_profile
            ),
            color = Ascent.Fg3,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun HikeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Ascent.Fg3, fontSize = 9.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = Ascent.Fg2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 海拔剖面。
 *
 * 横轴是采样点的顺序（起点、主要营地、垭口、终点），不是等比例距离 ——
 * 数据层只存了高度，不存累计里程，硬画成等距会假装自己有精度。
 * 曲线本身是真实的：每一个采样点都对应一个真实营地或垭口。
 */
@Composable
private fun RouteProfileChart(profile: List<Int>, modifier: Modifier = Modifier) {
    if (profile.size < 2) return
    val grid = Ascent.Line.copy(alpha = 0.55f)
    val line = Ascent.Fg2
    val fillTop = Ascent.Fg3.copy(alpha = 0.30f)
    val fillBottom = Ascent.Fg3.copy(alpha = 0.02f)
    val dot = Ascent.Fg2

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = h * 0.08f
        val bottom = h * 0.92f
        val usable = bottom - top

        // 三条水平参考线：不标数值，只给高度感
        for (i in 1..3) {
            val y = top + usable * i / 4f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        val lo = (profile.minOrNull() ?: 0) - 60f
        val hi = (profile.maxOrNull() ?: 1) + 60f
        val span = (hi - lo).coerceAtLeast(1f)
        fun xAt(i: Int) = w * i / (profile.size - 1)
        fun yAt(v: Int) = bottom - (v - lo) / span * usable

        val curve = Path().apply {
            moveTo(xAt(0), yAt(profile[0]))
            for (i in 1 until profile.size) lineTo(xAt(i), yAt(profile[i]))
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(w, bottom)
            lineTo(0f, bottom)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(fillTop, fillBottom)))
        drawPath(curve, line, style = Stroke(width = 1.6.dp.toPx()))

        // 起点 / 终点
        drawCircle(dot, radius = 2.2.dp.toPx(), center = Offset(xAt(0), yAt(profile[0])))
        drawCircle(
            dot, radius = 2.2.dp.toPx(),
            center = Offset(xAt(profile.lastIndex), yAt(profile.last()))
        )

        // 最高点：一个小空心圈，对应卡片上写的最高海拔
        val peakIndex = profile.indices.maxByOrNull { profile[it] } ?: 0
        val peak = Offset(xAt(peakIndex), yAt(profile[peakIndex]))
        drawCircle(fillTop, radius = 5.dp.toPx(), center = peak)
        drawCircle(dot, radius = 5.dp.toPx(), center = peak, style = Stroke(1.4.dp.toPx()))
        drawCircle(dot, radius = 1.8.dp.toPx(), center = peak)
    }
}

/** 路线符号：一条虚线轨迹，末端一个落点。比通用箭头更像「走线」。 */
@Composable
private fun RouteGlyph(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.08f, h * 0.90f)
            cubicTo(w * 0.30f, h * 0.86f, w * 0.20f, h * 0.58f, w * 0.46f, h * 0.52f)
            cubicTo(w * 0.70f, h * 0.46f, w * 0.56f, h * 0.24f, w * 0.88f, h * 0.14f)
        }
        drawPath(
            p, color,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.2f, 2.6f))
            )
        )
        drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(w * 0.88f, h * 0.14f))
    }
}

/** 路径页顶部：当前该看到的那一条。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeMilestoneBlock(
    modifier: Modifier = Modifier,
    currentAlt: Int = 200,
    region: HikeRegion? = null
) {
    var showAtlas by remember { mutableStateOf(false) }
    val current = remember(currentAlt, region) { HikingRoutes.pick(currentAlt, region) }
    val unlocked = remember(currentAlt) { HikingRoutes.unlocked(currentAlt) }

    Column(modifier = modifier.fillMaxWidth()) {
        HikeMilestoneCard(
            route = current,
            currentAlt = currentAlt,
            unlockedCount = unlocked.size,
            onOpenAtlas = { showAtlas = true }
        )
        Spacer(Modifier.height(4.dp))
    }

    if (showAtlas) {
        HikeAtlasSheet(currentAlt = currentAlt, onDismiss = { showAtlas = false })
    }
}

/** 「我的 · 当前海拔」下面那一行入口。 */
@Composable
fun HikeAtlasEntry(
    currentAlt: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fg: Color? = null,
    sub: Color? = null
) {
    val unlocked = remember(currentAlt) { HikingRoutes.unlocked(currentAlt) }
    val fgColor = fg ?: Ascent.Fg
    val subColor = sub ?: Ascent.Fg3
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(squircle(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RouteGlyph(modifier = Modifier.size(16.dp), color = subColor)
        Spacer(Modifier.width(11.dp))
        Text(
            text = stringResource(R.string.hike_entry_title),
            color = fgColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (unlocked.isEmpty()) stringResource(R.string.hike_entry_none)
            else stringResource(R.string.hike_entry_progress, unlocked.size, HikingRoutes.ALL.size),
            color = subColor,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Ic.ChevronRight,
            contentDescription = null,
            tint = subColor,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * 徒步百科面板。与山峰百科同一套交互：面板内部切换列表/详情，
 * 滚动位置挂在 sheet 这一层，返回不弹顶。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeAtlasSheet(currentAlt: Int, onDismiss: () -> Unit) {
    var detail by remember { mutableStateOf<HikingRoute?>(null) }
    val listState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ascent.Bg
    ) {
        val open = detail
        if (open == null) {
            HikeAtlasList(
                currentAlt = currentAlt,
                listState = listState,
                onOpen = { detail = it }
            )
        } else {
            HikeAtlasDetail(route = open, currentAlt = currentAlt, onBack = { detail = null })
        }
    }
}

@Composable
private fun HikeAtlasList(
    currentAlt: Int,
    listState: LazyListState,
    onOpen: (HikingRoute) -> Unit
) {
    val unlocked = remember(currentAlt) { HikingRoutes.unlocked(currentAlt) }
    // 按界面语言推断所在大区，只影响未解锁线路的排序（本区优先），不做过滤
    val langTag = LocalContext.current.resources.configuration.locales
        .let { if (it.isEmpty) "" else it[0].toLanguageTag() }
    val history = remember(currentAlt, langTag) {
        HikingRoutes.history(currentAlt, RegionHint.forTag(langTag))
    }
    val highest = remember(unlocked) { unlocked.maxOfOrNull { it.maxAlt } ?: 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Text(
            text = stringResource(R.string.hike_history_title),
            color = Ascent.Fg,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.hike_history_summary,
                unlocked.size, HikingRoutes.ALL.size,
                if (highest > 0) formatMeters(highest) else "—"
            ),
            color = Ascent.Fg3,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            state = listState,
            contentPadding = PaddingValues(bottom = 26.dp)
        ) {
            items(history, key = { it.id }) { route ->
                HikeAtlasRow(
                    route = route,
                    currentAlt = currentAlt,
                    done = route.milestoneAlt <= currentAlt,
                    onClick = { onOpen(route) }
                )
            }
        }
    }
}

@Composable
private fun HikeAtlasRow(
    route: HikingRoute,
    currentAlt: Int,
    done: Boolean,
    onClick: () -> Unit
) {
    val remaining = route.milestoneAlt - currentAlt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(squircle(12.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(17.dp), contentAlignment = Alignment.Center) {
            if (done) {
                Icon(
                    Ic.CircleCheck,
                    contentDescription = null,
                    tint = Ascent.Success,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                RouteGlyph(
                    modifier = Modifier.size(15.dp),
                    color = Ascent.Fg3.copy(alpha = 0.45f)
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = routeName(route),
                color = if (done) Ascent.Fg else Ascent.Fg2,
                fontSize = 14.sp,
                fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.hike_row_milestone,
                    formatMeters(route.milestoneAlt), formatMeters(route.maxAlt)
                ),
                color = Ascent.Fg3,
                fontSize = 10.5.sp
            )
        }
        Text(
            text = if (done) stringResource(R.string.mtn_unlocked)
            else stringResource(R.string.mtn_remaining, formatMeters(remaining)),
            color = if (done) Ascent.Success else Ascent.Fg3.copy(alpha = 0.85f),
            fontSize = 11.sp
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Ic.ChevronRight,
            contentDescription = null,
            tint = Ascent.Fg3.copy(alpha = if (done) 1f else 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

/** 面板内的单条路线详情。未解锁也能进。 */
@Composable
private fun HikeAtlasDetail(route: HikingRoute, currentAlt: Int, onBack: () -> Unit) {
    val reached = route.milestoneAlt <= currentAlt
    val remaining = route.milestoneAlt - currentAlt
    // 详情页优先显示长介绍；没写过的线回退到 brief（一句话）
    val body = routeIntro(route) ?: routeBrief(route)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(squircle(10.dp))
                .clickable { onBack() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Ic.ChevronLeft,
                contentDescription = null,
                tint = Ascent.Fg3,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.common_back),
                color = Ascent.Fg2,
                fontSize = 12.5.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (reached) stringResource(R.string.mtn_unlocked)
                else stringResource(R.string.mtn_remaining, formatMeters(remaining)),
                color = if (reached) Ascent.Success else Ascent.Fg3,
                fontSize = 11.5.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = routeName(route),
            color = Ascent.Fg,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(
                R.string.hike_detail_progress,
                formatMeters(currentAlt), formatMeters(route.milestoneAlt)
            ),
            color = Ascent.Fg3,
            fontSize = 11.5.sp
        )
        if (!reached) {
            Spacer(Modifier.height(8.dp))
            val progress = (currentAlt.toFloat() / route.milestoneAlt).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(squircle(2.dp))
                    .background(Ascent.Line)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(Ascent.Fg3)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            HikeDetailBody(route = route, chartHeight = 180.dp)

            if (body.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Ascent.Line)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.hike_intro_title),
                    color = Ascent.Fg2,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = body,
                    color = Ascent.Fg2,
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
