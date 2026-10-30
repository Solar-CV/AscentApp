package com.ascent.climb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 黏土配色。饱和色只用在"图标方块 + 数字"，其余一律中性，
 * 这样一屏里颜色是信息的索引而不是装饰。
 * 深色下整体提亮一档，避免在深底上发闷。
 */
data class ClayAccents(
    val streak: Color,
    val days: Color,
    val lessons: Color,
    val badges: Color,
    val warning: Color,
    val danger: Color,
    val heroTop: Color,
    val heroBottom: Color,
    /** 平涂用的 hero 底色：大色块不再做上下渐变，取中间调一档。 */
    val heroFlat: Color,
    val heroText: Color,
    val heroText2: Color,
    val label: Color,
    val value: Color,
    val sub: Color,
    val track: Color
)

@Composable
fun clayAccents(): ClayAccents = if (Ascent.IsDark) {
    ClayAccents(
        streak = Color(0xFFFFA347),
        days = Color(0xFF7FB2F7),
        lessons = Color(0xFF57A9FF),
        badges = Color(0xFFFFCB55),
        warning = Color(0xFFF5B84D),
        danger = Color(0xFFF07077),
        heroTop = Color(0xFF1E2126),
        heroBottom = Color(0xFF121417),
        heroFlat = Color(0xFF1B1E23),
        heroText = Color(0xFFF4F7FB),
        heroText2 = Color(0xFFA5B1C0),
        label = Color(0xFFC6D0DE),
        value = Color(0xFFF4F7FB),
        sub = Color(0xFF8D97A6),
        track = Color(0xFF2B2E33)
    )
} else {
    ClayAccents(
        streak = Color(0xFFF08519),
        days = Color(0xFF2F6FD0),
        lessons = Color(0xFF0F86F5),
        badges = Color(0xFFD99A00),
        warning = Color(0xFFE8A317),
        danger = Color(0xFFE5484D),
        heroTop = Color(0xFF28323F),
        heroBottom = Color(0xFF1B232D),
        heroFlat = Color(0xFF212B38),
        heroText = Color(0xFFFFFFFF),
        heroText2 = Color(0xFFA7B2C0),
        label = Color(0xFF4A5260),
        value = Color(0xFF18181B),
        sub = Color(0xFF8B8B93),
        track = Color(0xFFE7EAF0)
    )
}

/** 分区标题：小字号等宽，压深到中性墨色，比原来的浅灰更立得住。 */
@Composable
fun ClaySectionLabel(text: String, modifier: Modifier = Modifier) {
    val a = clayAccents()
    Text(
        text = text,
        color = a.label,
        fontSize = 10.sp,
        fontFamily = MonoNumbers,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = modifier
    )
}

/**
 * 一行数据：图标方块 + 标签 + 右对齐数值，可选带一条进度。
 * 高度 60（纯数值）/ 78（带进度），与设计稿一致。
 */
@Composable
fun ClayStatRow(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    ratio: Float? = null,
    barColor: Color = tint,
    emphasize: Boolean = false
) {
    val a = clayAccents()
    val withBar = ratio != null
    val fg = if (emphasize) a.heroText else a.value
    val labelColor = if (emphasize) a.heroText2 else a.label
    ClaySurface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (withBar) 78.dp else 60.dp),
        radius = 20.dp,
        elevation = 7.dp,
        fill = if (emphasize) SolidColor(a.heroFlat) else null,
        rim = if (emphasize) a.heroBottom else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = if (withBar) 13.dp else 0.dp),
            verticalArrangement = if (withBar) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClayChip(
                    color = if (emphasize) Color(0x33FFFFFF) else tint,
                    size = 30.dp,
                    radius = 11.dp
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = value,
                    color = fg,
                    fontSize = if (withBar) 15.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoNumbers
                )
                if (unit != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (withBar) {
                ClayProgress(ratio = ratio, color = barColor, track = a.track)
            }
        }
    }
}

/** 进度条。轨道本身略微内凹，填充用同色系实色（不做渐变，保持黏土的"实心"感）。 */
@Composable
fun ClayProgress(
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color? = null,
    height: Dp = 6.dp
) {
    val t = track ?: clayAccents().track
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(t)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/** 通用小圆点/徽章底：用于徽章墙的占位圆片。 */
@Composable
fun ClayPuck(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.08f)))),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * 黏土卡片（纵向内容）。参数与旧的 `OutlineCard` 对齐，替换时只改调用头、不动内部结构。
 * [dark] = true 时整块翻成墨黑，用作页面的"重量锥"（复习的排队卡、我的主页的海拔行）。
 */
@Composable
fun ClayColumn(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    elevation: Dp = 8.dp,
    padding: Dp = 16.dp,
    gap: Dp = 12.dp,
    dark: Boolean = false,
    fill: Brush? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val a = clayAccents()
    ClaySurface(
        modifier = modifier.fillMaxWidth(),
        radius = radius,
        elevation = elevation,
        fill = fill ?: if (dark) SolidColor(a.heroFlat) else null,
        rim = if (dark) a.heroBottom else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content
        )
    }
}
