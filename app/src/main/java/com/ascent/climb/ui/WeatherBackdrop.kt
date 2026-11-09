package com.ascent.climb.ui

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * 天气背景（只换背景，卡片、按钮、配色一律不动）。
 *
 * 晴 / 阴是静态：天空渐变 + 太阳（夜间换月亮与星）+ 卡通云。
 * 雨 / 雷雨 / 雪是动态：3 层粒子，雷雨另有一道随机闪烁的闪电与其辉光。
 *
 * 两条硬约束：
 *  1. 卡片是不透明的，背景只在顶部、侧边 20dp 与卡片缝隙里可见 —— 所有天气元素
 *     都按「顶部 30% 高度」构图，中间那段画了也看不见。
 *  2. 浅色天空配深色标题、深色天空配浅色标题：所以每套配色只在自己的明度区间里
 *     变动，不跨到对方那一半，否则标题会糊掉。
 */
enum class WeatherMood(val key: String) {
    CLEAR("clear"),
    OVERCAST("overcast"),
    RAIN("rain"),
    STORM("storm"),
    SNOW("snow");

    val animated: Boolean get() = this == RAIN || this == STORM || this == SNOW

    companion object {
        fun of(key: String): WeatherMood? = entries.firstOrNull { it.key == key }
    }
}

private data class Sky(
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val cloud: Color,
    val particle: Color,
    val bolt: Color
)

private fun skyOf(mood: WeatherMood, night: Boolean): Sky = when (mood) {
    WeatherMood.CLEAR -> if (night) Sky(
        top = Color(0xFF0A0B10), bottom = Color(0xFF151A23),
        glow = Color(0xFFE9ECF3), cloud = Color(0xFF232833),
        particle = Color(0xFFE9ECF3), bolt = Color(0xFFE9ECF3)
    ) else Sky(
        top = Color(0xFFFFFCF4), bottom = Color(0xFFF1F5FB),
        glow = Color(0xFFFFC96B), cloud = Color(0xFFFFFFFF),
        particle = Color(0xFFFFFFFF), bolt = Color(0xFFFFC96B)
    )
    WeatherMood.OVERCAST -> if (night) Sky(
        top = Color(0xFF0A0B0F), bottom = Color(0xFF171A1F),
        glow = Color(0xFF3A4150), cloud = Color(0xFF21252C),
        particle = Color(0xFF3A4150), bolt = Color(0xFF3A4150)
    ) else Sky(
        top = Color(0xFFF3F5F9), bottom = Color(0xFFE7ECF3),
        glow = Color(0xFFFFFFFF), cloud = Color(0xFFFFFFFF),
        particle = Color(0xFFC9D4E1), bolt = Color(0xFFFFFFFF)
    )
    WeatherMood.RAIN -> if (night) Sky(
        top = Color(0xFF090B10), bottom = Color(0xFF131A23),
        glow = Color(0xFF2A3444), cloud = Color(0xFF1B2029),
        particle = Color(0xFF7E95B2), bolt = Color(0xFF2A3444)
    ) else Sky(
        top = Color(0xFFEDF2F8), bottom = Color(0xFFDCE6F1),
        glow = Color(0xFFFFFFFF), cloud = Color(0xFFC7D3E2),
        particle = Color(0xFF6E86A6), bolt = Color(0xFFFFFFFF)
    )
    WeatherMood.STORM -> if (night) Sky(
        top = Color(0xFF07090D), bottom = Color(0xFF0F1620),
        glow = Color(0xFF6C82A0), cloud = Color(0xFF1A1F28),
        particle = Color(0xFF6C82A0), bolt = Color(0xFFFFF3B0)
    ) else Sky(
        top = Color(0xFFE8EEF6), bottom = Color(0xFFD2DDEA),
        glow = Color(0xFFFFFFFF), cloud = Color(0xFFBFCBDB),
        particle = Color(0xFF5F7695), bolt = Color(0xFFFFF6C0)
    )
    WeatherMood.SNOW -> if (night) Sky(
        top = Color(0xFF090B11), bottom = Color(0xFF141A23),
        glow = Color(0xFFDCE6F2), cloud = Color(0xFF212734),
        particle = Color(0xFFDCE6F2), bolt = Color(0xFFDCE6F2)
    ) else Sky(
        top = Color(0xFFF8FBFE), bottom = Color(0xFFE7F0FA),
        glow = Color(0xFFFFFFFF), cloud = Color(0xFFDCE8F6),
        particle = Color(0xFFFFFFFF), bolt = Color(0xFFFFFFFF)
    )
}

private class Drop(val x: Float, val y0: Float, val len: Float, val spd: Float, val alpha: Float)
private class Flake(val x: Float, val y0: Float, val r: Float, val spd: Float, val drift: Float, val alpha: Float)

/** 确定性的伪随机：同一个 index 永远得到同一个值，避免每帧抖动。 */
private fun hash(i: Int, salt: Int = 0): Float {
    val v = sin(i * 12.9898 + salt * 78.233) * 43758.5453
    return (v - kotlin.math.floor(v)).toFloat()
}

@Composable
fun WeatherBackdrop(mood: WeatherMood, modifier: Modifier = Modifier) {
    val night = Ascent.IsDark
    val sky = remember(mood, night) { skyOf(mood, night) }
    val context = LocalContext.current

    // 系统关掉动画（开发者选项 / 无障碍「减少动态效果」）时只画静态帧。
    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
    val animated = mood.animated && !reduceMotion

    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> clock.floatValue = (now - start) / 1_000_000_000f }
        }
    }

    val drops = remember(mood) {
        if (mood != WeatherMood.RAIN && mood != WeatherMood.STORM) emptyList()
        else List(3) { layer ->
            val count = if (mood == WeatherMood.STORM) 16 + layer * 6 else 14 + layer * 6
            List(count) { i ->
                val seed = layer * 100 + i
                Drop(
                    x = hash(seed, 1),
                    y0 = hash(seed, 2),
                    len = 0.018f + hash(seed, 3) * 0.030f,
                    spd = (if (mood == WeatherMood.STORM) 0.70f else 0.55f) + layer * 0.28f,
                    alpha = (if (mood == WeatherMood.STORM) 0.24f else 0.20f) - layer * 0.05f
                )
            }
        }
    }
    val flakes = remember(mood) {
        if (mood != WeatherMood.SNOW) emptyList()
        else List(3) { layer ->
            List(10 + layer * 4) { i ->
                val seed = layer * 200 + i
                Flake(
                    x = hash(seed, 4),
                    y0 = hash(seed, 5),
                    r = 1.4f + hash(seed, 6) * 2.6f + layer * 0.6f,
                    spd = 0.030f + layer * 0.022f,
                    drift = hash(seed, 7) * 6.283f,
                    alpha = 0.85f - layer * 0.18f
                )
            }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val t = clock.floatValue
        drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom)))
        when (mood) {
            WeatherMood.CLEAR -> {
                if (night) {
                    drawStars()
                    drawMoon(sky)
                } else {
                    drawSun(sky)
                }
                drawClouds(sky, night, t, animated, compact = false)
            }
            WeatherMood.OVERCAST -> drawClouds(sky, night, t, animated, compact = true)
            WeatherMood.RAIN -> drawRain(drops, sky, t)
            WeatherMood.STORM -> {
                drawRain(drops, sky, t)
                drawBolt(sky, t)
            }
            WeatherMood.SNOW -> drawSnow(flakes, sky, t)
        }
    }
}

// ---------------------------------------------------------------- 静态元素

private fun DrawScope.drawSun(sky: Sky) {
    val cx = size.width * 0.80f
    val cy = size.height * 0.085f
    val r = size.width * 0.085f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sky.glow.copy(alpha = 0.55f), sky.glow.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = r * 2.4f
        ),
        radius = r * 2.4f,
        center = Offset(cx, cy)
    )
    drawCircle(color = sky.glow.copy(alpha = 0.92f), radius = r, center = Offset(cx, cy))
}

/** 月亮用「圆减圆」的布尔运算切出缺口，比叠一个底色圆更干净。 */
private fun DrawScope.drawMoon(sky: Sky) {
    val cx = size.width * 0.80f
    val cy = size.height * 0.085f
    val r = size.width * 0.075f
    val body = Path().apply { addOval(Rect(center = Offset(cx, cy), radius = r)) }
    val bite = Path().apply {
        addOval(Rect(center = Offset(cx + r * 0.62f, cy - r * 0.30f), radius = r * 0.92f))
    }
    val crescent = Path().apply { op(body, bite, PathOperation.Difference) }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sky.glow.copy(alpha = 0.24f), sky.glow.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = r * 3f
        ),
        radius = r * 3f,
        center = Offset(cx, cy)
    )
    drawPath(crescent, sky.glow.copy(alpha = 0.95f))
}

private fun DrawScope.drawStars() {
    for (i in 0 until 26) {
        val x = hash(i, 11) * size.width
        val y = hash(i, 12) * size.height * 0.34f
        val r = 0.6f + hash(i, 13) * 1.3f
        drawCircle(Color.White.copy(alpha = 0.18f + hash(i, 14) * 0.45f), r, Offset(x, y))
    }
}

/**
 * 卡通云：三个圆团 + 一条平底。中间那团最大，否则三团一样高会摊平成一个白斑。
 * [compact] = 阴天：三朵错落；否则两朵（晴）。
 */
private fun DrawScope.drawClouds(
    sky: Sky,
    night: Boolean,
    t: Float,
    animated: Boolean,
    compact: Boolean
) {
    val base = size.width * 0.34f
    val drift = if (animated) sin(t * 0.25f) * size.width * 0.012f else 0f
    val spots = if (compact) {
        listOf(
            Triple(0.36f, 0.10f, 1.05f),
            Triple(0.06f, 0.19f, 0.72f),
            Triple(0.72f, 0.26f, 0.62f)
        )
    } else {
        listOf(
            Triple(0.38f, 0.105f, 0.88f),
            Triple(0.80f, 0.215f, 0.55f)
        )
    }
    spots.forEachIndexed { index, (fx, fy, scale) ->
        val w = base * scale
        val cx = size.width * fx + drift * (index + 1) * 0.4f
        val cy = size.height * fy
        val alpha = if (night) 0.55f else 0.92f
        val c = sky.cloud.copy(alpha = alpha)
        drawCircle(c, w * 0.26f, Offset(cx - w * 0.30f, cy + w * 0.02f))
        drawCircle(c, w * 0.375f, Offset(cx, cy - w * 0.10f))
        drawCircle(c, w * 0.24f, Offset(cx + w * 0.32f, cy + w * 0.04f))
        drawRoundRect(
            color = c,
            topLeft = Offset(cx - w * 0.52f, cy + w * 0.04f),
            size = Size(w * 1.04f, w * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f)
        )
    }
}

// ---------------------------------------------------------------- 动态元素

private fun DrawScope.drawRain(drops: List<List<Drop>>, sky: Sky, t: Float) {
    drops.forEachIndexed { layer, list ->
        list.forEach { d ->
            val y = ((d.y0 + t * d.spd) % 1.15f) - 0.10f
            val x = d.x * size.width - y * size.width * 0.04f
            drawLine(
                color = sky.particle.copy(alpha = d.alpha),
                start = Offset(x, y * size.height),
                end = Offset(x - size.width * 0.006f, (y + d.len) * size.height),
                strokeWidth = (1.1f + layer * 0.3f).dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawSnow(flakes: List<List<Flake>>, sky: Sky, t: Float) {
    flakes.forEach { list ->
        list.forEach { f ->
            val y = ((f.y0 + t * f.spd) % 1.15f) - 0.08f
            val x = (f.x + sin(t * 0.5f + f.drift) * 0.012f) * size.width
            drawCircle(
                color = sky.particle.copy(alpha = f.alpha),
                radius = f.r.dp.toPx() * 0.55f,
                center = Offset(x, y * size.height)
            )
        }
    }
    // 近处的六角雪花：画 3 条交叉线，比圆点更像雪
    val arms = listOf(
        Triple(0.24f, 0.13f, 7f),
        Triple(0.62f, 0.085f, 9f),
        Triple(0.40f, 0.22f, 6f),
        Triple(0.84f, 0.175f, 8f)
    )
    arms.forEachIndexed { i, (fx, fy, len) ->
        val cx = size.width * fx
        val cy = size.height * fy + sin(t * 0.4f + i) * size.height * 0.004f
        val l = len.dp.toPx()
        val c = sky.particle.copy(alpha = 0.85f)
        for (k in 0 until 3) {
            val a = (k * 60f) * (Math.PI / 180f).toFloat()
            val dx = cos(a) * l
            val dy = sin(a) * l
            drawLine(c, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), 1.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

/** 闪电：本体按随机周期闪，[drawBolt] 只负责画，辉光把邻近天空点亮。 */
private fun flashAt(t: Float): Float {
    val cycle = 2.6f
    val idx = (t / cycle).toInt()
    val ph = (t / cycle) - idx
    if ((idx * 7919) % 3 == 1) return 0f
    return (exp(-ph * 9f) + 0.5f * exp(-abs(ph - 0.1f) * 22f)).toFloat()
}

private fun DrawScope.drawBolt(sky: Sky, t: Float) {
    val flash = flashAt(t)
    val pts = listOf(
        Offset(0.809f, 0.070f),
        Offset(0.722f, 0.152f),
        Offset(0.802f, 0.140f),
        Offset(0.702f, 0.258f)
    ).map { Offset(it.x * size.width, it.y * size.height) }

    val cx = size.width * 0.76f
    val cy = size.height * 0.16f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                sky.bolt.copy(alpha = 0.10f + flash * 0.45f),
                sky.bolt.copy(alpha = 0f)
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.55f
        ),
        radius = size.width * 0.55f,
        center = Offset(cx, cy)
    )

    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    }
    drawPath(
        path = path,
        color = sky.bolt.copy(alpha = 0.42f + flash * 0.58f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

// ---------------------------------------------------------------- 调试开关

/**
 * 调试用的天气切换：只在 debug 包里出现。
 *
 * - 手动档：点一下循环到下一档，写进 `weatherOverride`。
 * - 自动档（[auto] 为真）：显示的是实时取到的天气（不是手动设的那档），点击会立刻重取一次。
 *
 * 文案写死中文，不进十语言表 —— 这是排查工具，不是产品界面。
 */
@Composable
fun WeatherDebugChip(
    current: WeatherMood,
    auto: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(squircle(8.dp))
            .background(if (auto) Color(0xCC14532D) else Color(0xCC000000))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (auto) "天气 ${current.key} · auto" else "天气 ${current.key}",
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
