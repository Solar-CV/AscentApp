package com.ascent.climb.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 开屏（黏土拟态）。
 *
 * 只覆盖在根界面之上，**不改动任何既有页面**：序列走完自行淡出。
 * 视觉参数与 pen.dev 画板「S1 · 开屏 · 黏土版」一致：
 * 盘面 184dp / 圆角 54dp / 1dp 内描边 / 外影收紧（blur 20 + 3dp 接触影）。
 */
@Composable
fun ClaySplashOverlay(dark: Boolean, onFinish: () -> Unit) {
    val c = if (dark) SplashColors.dark() else SplashColors.light()
    // 名言用原文，不随界面语言变化
    val quote = remember { SplashQuotes.pick() }

    // 一条时间轴驱动全部入场：0..1 约 1.35s，随后 0.22s 淡出。
    val t = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val animatorsOn = runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
        if (animatorsOn) {
            t.animateTo(1f, tween(1350))
            delay(180)
            exit.animateTo(1f, tween(220))
        } else {
            t.snapTo(1f)
            delay(420)
        }
        onFinish()
    }

    val overflow = CubicBezierEasing(0.22f, 1.38f, 0.36f, 1f)
    val puckIn = overflow.transform((t.value / 0.46f).coerceIn(0f, 1f))
    val markIn = ((t.value - 0.10f) / 0.26f).coerceIn(0f, 1f)
    val wordIn = ((t.value - 0.28f) / 0.24f).coerceIn(0f, 1f)
    val quoteIn = ((t.value - 0.44f) / 0.24f).coerceIn(0f, 1f)
    val barIn = ((t.value - 0.56f) / 0.34f).coerceIn(0f, 1f)

    // clickable 只为吞掉这 1.7 秒里的点击，避免误触底下的界面（无涟漪）
    val sink = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .graphicsLayer { alpha = 1f - exit.value }
            .clickable(interactionSource = sink, indication = null) {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ClayPuck(
                size = 184.dp,
                colors = c,
                markAlpha = markIn,
                modifier = Modifier.graphicsLayer {
                    val s = 0.88f + 0.12f * puckIn
                    scaleX = s
                    // 落下时略微压扁，落地回弹
                    scaleY = s * (1f - 0.05f * (1f - puckIn))
                    alpha = (puckIn * 2.2f).coerceIn(0f, 1f)
                }
            )

            Spacer(Modifier.height(30.dp))

            Text(
                text = "ASCENT",
                color = c.ink,
                fontSize = 33.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 7.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = wordIn
                    translationY = (1f - wordIn) * 14f
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = quote.text,
                color = c.fg2,
                fontSize = 12.5.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = quoteIn
                        translationY = (1f - quoteIn) * 10f
                    }
            )

            Spacer(Modifier.height(9.dp))

            Text(
                text = "— " + quote.author,
                color = c.fg3,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = quoteIn * 0.95f }
            )

            Spacer(Modifier.height(38.dp))

            ClayProgress(
                fraction = barIn,
                colors = c,
                modifier = Modifier.graphicsLayer { alpha = ((t.value - 0.5f) / 0.1f).coerceIn(0f, 1f) }
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 黏土盘面                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun ClayPuck(
    size: Dp,
    colors: SplashColors,
    markAlpha: Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.293f) // 184 → 54
    Box(
        modifier
            .size(size)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = colors.shadow,
                spotColor = colors.shadow
            )
            .shadow(
                elevation = 3.dp,
                shape = shape,
                clip = false,
                ambientColor = colors.shadowSoft,
                spotColor = colors.shadowSoft
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.puckTop, colors.puckMid, colors.puckBottom)))
    ) {
        // 内光影：上缘白光、下缘压暗，靠两圈描边实现（不用模糊，边缘才硬）
        Canvas(Modifier.fillMaxSize()) {
            val inset = 1.dp.toPx()
            val corner = CornerRadius(this.size.minDimension * 0.293f - inset)
            val innerTopLeft = Offset(inset, inset)
            val innerSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (colors.dark) 0.16f else 0.95f), Color.Transparent),
                    startY = 0f,
                    endY = this.size.height * 0.45f
                ),
                topLeft = innerTopLeft,
                size = innerSize,
                cornerRadius = corner,
                style = Stroke(width = 2.dp.toPx())
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, colors.innerShade),
                    startY = this.size.height * 0.55f,
                    endY = this.size.height
                ),
                topLeft = innerTopLeft,
                size = innerSize,
                cornerRadius = corner,
                style = Stroke(width = 3.dp.toPx())
            )
            // 1dp 描边定义边缘（白底上全靠它把轮廓“切”出来）
            drawRoundRect(
                color = colors.rim,
                topLeft = Offset(0.5f.dp.toPx(), 0.5f.dp.toPx()),
                size = Size(this.size.width - 1.dp.toPx(), this.size.height - 1.dp.toPx()),
                cornerRadius = CornerRadius(this.size.minDimension * 0.293f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        MountainMark(
            colors = colors,
            modifier = Modifier
                .size(size * 0.72f) // 184 → 132
                .align(Alignment.Center)
                .graphicsLayer { alpha = markAlpha }
        )
    }
}

/** 山体：单峰 + 侧面暗部 + 锯齿雪线 + 方块旗。全部直线折线，小尺寸不糊。 */
@Composable
private fun MountainMark(colors: SplashColors, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.width / 120f // 设计稿按 120 网格

        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        // 盘面上的接触影，让山“坐”在盘上
        drawOval(
            color = colors.markShadow,
            topLeft = p(20f, 98f),
            size = Size(80f * u, 10f * u)
        )

        // 山体正面
        val peak = Path().apply {
            moveTo(22f * u, 104f * u)
            lineTo(58f * u, 36f * u)
            lineTo(94f * u, 104f * u)
            close()
        }
        drawPath(
            peak,
            brush = Brush.linearGradient(
                colors = listOf(colors.peakTop, colors.peakMid, colors.peakBottom),
                start = p(30f, 36f),
                end = p(70f, 104f)
            )
        )

        // 右侧暗面：一个明确的三角，读成受光/背光两个面
        val shade = Path().apply {
            moveTo(58f * u, 36f * u)
            lineTo(94f * u, 104f * u)
            lineTo(58f * u, 104f * u)
            close()
        }
        drawPath(shade, color = colors.peakShade)

        // 雪帽：照 ic_splash_mark 的「两瓣、中间上凹」轮廓，不做锯齿
        val cap = Path().apply {
            moveTo(58f * u, 36f * u)
            lineTo(71f * u, 64f * u)
            lineTo(58f * u, 56f * u)
            lineTo(45f * u, 64f * u)
            close()
        }
        drawPath(cap, color = colors.snow)

        // 旗杆 + 方块旗，杆从峰顶往上，旗贴杆顶
        drawRoundRect(
            color = colors.pole,
            topLeft = p(55.4f, 16f),
            size = Size(4.4f * u, 21f * u),
            cornerRadius = CornerRadius(1.6f * u)
        )
        val flag = Path().apply {
            moveTo(59.8f * u, 16f)
            lineTo(79f * u, 22.5f * u)
            lineTo(59.8f * u, 29f * u)
            close()
        }
        drawPath(flag, color = colors.flag)
    }
}

/* ------------------------------------------------------------------ */
/* 进度条                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun ClayProgress(fraction: Float, colors: SplashColors, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(132.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(colors.track)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = colors.trackShade,
                cornerRadius = CornerRadius(size.height / 2),
                style = Stroke(width = 1.5f.dp.toPx())
            )
        }
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(14.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(colors.accent)
        )
    }
}

/* ------------------------------------------------------------------ */
/* 配色                                                                */
/* ------------------------------------------------------------------ */

/** 开屏专用色板：不接 AscentTheme，避免把既有语义色拖进这次改动。 */
data class SplashColors(
    val dark: Boolean,
    val bg: Color,
    val puckTop: Color,
    val puckMid: Color,
    val puckBottom: Color,
    val rim: Color,
    val innerShade: Color,
    val shadow: Color,
    val shadowSoft: Color,
    val ink: Color,
    val fg2: Color,
    val fg3: Color,
    val accent: Color,
    val track: Color,
    val trackShade: Color,
    val snow: Color,
    val flag: Color,
    val pole: Color,
    val peakTop: Color,
    val peakMid: Color,
    val peakBottom: Color,
    val peakShade: Color,
    val markShadow: Color
) {
    companion object {
        fun light() = SplashColors(
            dark = false,
            bg = Color(0xFFFFFFFF),
            puckTop = Color(0xFFFFFFFF),
            puckMid = Color(0xFFF8F9FB),
            puckBottom = Color(0xFFEDEFF3),
            rim = Color(0xFFE4E6EA),
            innerShade = Color(0xF2A6AFBD),
            shadow = Color(0x448C94A1),
            shadowSoft = Color(0x338C94A1),
            ink = Color(0xFF18181B),
            fg2 = Color(0xFF71717A),
            fg3 = Color(0xFFA1A1AA),
            accent = Color(0xFF0485F7),
            track = Color(0xFFEDEEF1),
            trackShade = Color(0x66B4BAC4),
            snow = Color(0xFFFFFFFF),
            flag = Color(0xFFFFC800),
            pole = Color(0xFFF5B942),
            peakTop = Color(0xFF3FA2FF),
            peakMid = Color(0xFF0F86F5),
            peakBottom = Color(0xFF0B6AD1),
            peakShade = Color(0x400B5DBD),
            markShadow = Color(0x1F6E7A88)
        )

        fun dark() = SplashColors(
            dark = true,
            bg = Color(0xFF0B0B0D),
            puckTop = Color(0xFF1C1D21),
            puckMid = Color(0xFF17181B),
            puckBottom = Color(0xFF111214),
            rim = Color(0xFF2C2D32),
            innerShade = Color(0xAA000000),
            shadow = Color(0x66000000),
            shadowSoft = Color(0x44000000),
            ink = Color(0xFFFAFAFA),
            fg2 = Color(0xFFA1A1AA),
            fg3 = Color(0xFF71717A),
            accent = Color(0xFF3E9BFF),
            track = Color(0xFF24252A),
            trackShade = Color(0xFF34363C),
            snow = Color(0xFFF7FAFF),
            flag = Color(0xFFFFC800),
            pole = Color(0xFFF5B942),
            peakTop = Color(0xFF4FAEFF),
            peakMid = Color(0xFF1E90FF),
            peakBottom = Color(0xFF0B6AD1),
            peakShade = Color(0x3D000000),
            markShadow = Color(0x66000000)
        )
    }
}
