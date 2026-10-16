package com.ascent.climb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// 旧组件（保留：既有界面仍在引用）
// ============================================================================

/** 等高线 + 星点背景（旧视觉，方向 E 的页面不再使用）。 */
@Composable
fun TopoBackground(modifier: Modifier = Modifier) {
    val lineColor = Ascent.TopoLine
    val starColor = Ascent.Star
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 7) {
            val baseY = h * (0.16f + i * 0.115f)
            val path = Path().apply {
                moveTo(-w * 0.05f, baseY)
                cubicTo(
                    w * 0.30f, baseY - h * 0.075f,
                    w * 0.62f, baseY + h * 0.055f,
                    w * 1.05f, baseY - h * 0.02f
                )
            }
            drawPath(path, lineColor, style = Stroke(width = 1.6f))
        }
        val stars = listOf(
            0.15f to 0.11f, 0.30f to 0.06f, 0.75f to 0.09f,
            0.88f to 0.15f, 0.50f to 0.05f, 0.22f to 0.18f
        )
        stars.forEachIndexed { idx, (fx, fy) ->
            drawCircle(
                color = starColor,
                radius = if (idx % 3 == 0) 2.6f else 1.8f,
                center = Offset(w * fx, h * fy)
            )
        }
    }
}

/** 小节标题（等宽字体 + 字距，用于分区）。 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 22.dp, bottom = 10.dp),
        color = Ascent.Mut,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.2.sp
    )
}

/** 圆角胶囊标签（旧）。 */
@Composable
fun Chip(
    text: String,
    color: Color = Ascent.Txt,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Ascent.Panel)
            .border(1.dp, Ascent.Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 自绘进度条（旧别名）。 */
@Composable
fun AscentBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Int = 8,
    fill: Color = Ascent.Ice
) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Ascent.Track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(f)
                .height(height.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(fill)
        )
    }
}

/** 标准面板卡片（旧：无内边距的 Box）。 */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    background: Color = Ascent.Panel,
    borderColor: Color = Ascent.Line,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        content()
    }
}

/** 主行动按钮（旧：方角 16dp）。方向 E 页面请用 [PillButton]。 */
@Composable
fun AscentButton(
    text: String,
    enabled: Boolean = true,
    gold: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = when {
        !enabled -> Ascent.NodeLock
        gold -> Ascent.Gold
        else -> Ascent.Ice
    }
    val fg = when {
        !enabled -> Ascent.Mut2
        gold -> Ascent.OnGold
        else -> Ascent.OnIce
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** 顶部状态栏（旧）。 */
@Composable
fun AscentHeader(
    altitude: Int,
    streak: Int,
    rope: Int,
    titleOverride: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = titleOverride ?: "当前海拔",
                color = Ascent.Mut,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (titleOverride == null) formatMeters(altitude) else "",
                    color = Ascent.IceSoft,
                    fontSize = if (titleOverride == null) 26.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (titleOverride == null) {
                    Text(
                        text = " m",
                        color = Ascent.Mut,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("\uD83D\uDD25 $streak", Ascent.Orange)
            Chip("\uD83E\uDEA2 $rope", Ascent.Ice)
        }
    }
}

fun formatMeters(v: Int): String {
    val s = v.toString()
    if (s.length <= 3) return s
    return s.dropLast(3) + "," + s.takeLast(3)
}

// ============================================================================
// 方向 E 组件（HeroUI × 攀登）
// ============================================================================

/** 按钮风格。 */
enum class BtnStyle { Primary, Secondary, Ghost, Danger }

/**
 * 胶囊按钮。全宽、圆角 999、居中内容。
 * @param icon 可选尾随图标（如 arrow-right）
 */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Primary,
    icon: ImageVector? = null,
    height: Int = 54,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val bg = when (style) {
        BtnStyle.Primary -> Ascent.Accent
        BtnStyle.Secondary -> Ascent.Surface
        BtnStyle.Ghost -> Color.Transparent
        BtnStyle.Danger -> Ascent.Danger
    }
    val fg = when (style) {
        BtnStyle.Primary, BtnStyle.Danger -> Ascent.OnAccent
        BtnStyle.Secondary -> Ascent.Fg
        BtnStyle.Ghost -> Ascent.Fg2
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (style == BtnStyle.Secondary) Modifier.border(1.dp, Ascent.Border, shape)
                else Modifier
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

/** 轮廓卡片：面色 + 1dp 描边 + 圆角 + 内边距，纵向排布。 */
@Composable
fun OutlineCard(
    modifier: Modifier = Modifier,
    background: Color = Ascent.Surface,
    borderColor: Color = Ascent.Border,
    radius: Int = 20,
    padding: Int = 16,
    gap: Int = 12,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(padding.dp),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
        content = content
    )
}

/** 柔和标签胶囊（可带前置图标）。 */
@Composable
fun SoftTag(
    text: String,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    mono: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (mono) MonoNumbers else SansBody
        )
    }
}

/** 等宽微标签（区段标题 / meta）。 */
@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color = Ascent.Fg3) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 11.sp,
        fontFamily = MonoNumbers,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.4.sp
    )
}

/** 圆形图标徽章。 */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    background: Color,
    diameter: Int = 32,
    iconSize: Int = 16,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize.dp))
    }
}

/** 数据小方块：数值 + 说明。 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Ascent.Surface,
    borderColor: Color = Ascent.Border
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Ascent.Fg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = MonoNumbers)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Ascent.Fg3, fontSize = 11.sp)
    }
}

/** 星级（3 星，f 为点亮数）。 */
@Composable
fun StarRow(
    f: Int,
    modifier: Modifier = Modifier,
    size: Int = 32,
    spacing: Int = 8
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            Icon(
                imageVector = Ic.Star,
                contentDescription = null,
                tint = if (i < f) Ascent.Warning else Ascent.Star,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

/** 海拔剖面图：网格 + 渐变面积 + 折线 + 节点。codes 与 altitudes 等长。 */
@Composable
fun ElevationProfile(
    altitudes: List<Int>,
    codes: List<String>,
    currentIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    if (altitudes.size < 2) return
    val n = altitudes.size
    val minAlt = 3000f
    val maxAlt = 9000f
    val lineColor = Ascent.Accent
    val surfaceColor = Ascent.Surface
    val gridColor = Ascent.Border
    val softColor = Ascent.AccentSoft
    val fgColor = Ascent.Fg
    val fg3 = Ascent.Fg3
    val pillBg = Ascent.Accent
    val pillFg = Ascent.OnAccent

    val inset = 22.dp
    val bottomPad = 26.dp
    val topPad = 22.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(178.dp)) {
        val density = LocalDensity.current
        val wDp = maxWidth
        val hDp = maxHeight
        val stepDp: Dp = (wDp - inset * 2) / (n - 1)

        fun xDp(i: Int): Dp = inset + stepDp * i
        fun yDp(a: Int): Dp {
            val frac = ((a - minAlt) / (maxAlt - minAlt)).coerceIn(0f, 1f)
            return (hDp - bottomPad) - (hDp - bottomPad - topPad) * frac
        }

        val xs = List(n) { with(density) { xDp(it).toPx() } }
        val ys = List(n) { with(density) { yDp(altitudes[it]).toPx() } }
        val gridYs = listOf(9000, 7000, 5000, 3000).map { with(density) { yDp(it).toPx() } }
        val baseY = with(density) { (hDp - bottomPad).toPx() }

        Canvas(Modifier.fillMaxSize()) {
            for (gy in gridYs) {
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            val line = Path().apply {
                moveTo(xs[0], ys[0])
                for (i in 1 until n) lineTo(xs[i], ys[i])
            }
            val area = Path().apply {
                moveTo(xs[0], ys[0])
                for (i in 1 until n) lineTo(xs[i], ys[i])
                lineTo(xs[n - 1], baseY)
                lineTo(xs[0], baseY)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(softColor, Color.Transparent)))
            drawPath(line, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            for (i in 0 until n) {
                val c = Offset(xs[i], ys[i])
                if (i == n - 1) {
                    drawCircle(lineColor, radius = 7f, center = c)
                } else {
                    drawCircle(surfaceColor, radius = 5.5f, center = c)
                    drawCircle(lineColor, radius = 5.5f, center = c, style = Stroke(width = 2.5f))
                }
            }
        }

        // 轴标签
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            codes.forEachIndexed { i, c ->
                Text(
                    text = if (i == n - 1) "TOP" else c,
                    color = fg3,
                    fontSize = 9.sp,
                    fontFamily = MonoNumbers,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 顶峰海拔
        Text(
            text = formatMeters(altitudes.last()),
            color = fgColor,
            fontSize = 10.sp,
            fontFamily = MonoNumbers,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopStart).offset(x = xDp(n - 1) - 56.dp)
        )

        // 你在这里
        val ci = currentIndex.coerceIn(0, n - 1)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = xDp(ci) - 18.dp, y = yDp(altitudes[ci]) - 30.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(pillBg)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Ic.MapPin, contentDescription = null, tint = pillFg, modifier = Modifier.size(11.dp))
            Text("你在这里", color = pillFg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 底栏条目定义。 */
data class TabItem(val icon: ImageVector, val label: String)

val ASCENT_TABS = listOf(
    TabItem(Ic.Mountain, "路径"),
    TabItem(Ic.BookOpen, "技能书"),
    TabItem(Ic.Dumbbell, "训练"),
    TabItem(Ic.RotateCcw, "复习"),
    TabItem(Ic.User, "我的")
)

/** 浮起胶囊底栏（V2：当前项为绿色实心胶囊）。 */
@Composable
fun CapsuleTabBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        val shape = RoundedCornerShape(36.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(8.dp, shape, clip = false)
                .clip(shape)
                .background(Ascent.Surface)
                .border(1.dp, Ascent.DuoBorder, shape)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ASCENT_TABS.forEachIndexed { i, tab ->
                val on = i == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (on) Ascent.UGreen else Color.Transparent)
                        .clickable { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (on) Color.White else Ascent.DuoFg3,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        color = if (on) Color.White else Ascent.DuoFg3,
                        fontSize = 10.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
