package com.ascent.climb.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * 连续曲率圆角，也就是常说的 squircle。
 *
 * 普通 [androidx.compose.foundation.shape.RoundedCornerShape] 的四角是四分之一圆：
 * 直线接圆弧的地方曲率会从 0 突然跳到 1/r。半径小的时候看不出来，到了底栏
 * 这种 20dp 以上的尺寸，接缝会显得“顿”一下。这里用超椭圆
 * |x|^n + |y|^n = 1（n = 2.2）代替圆角，曲率从直边到转角连续变化，
 * 视觉上更接近 iOS 那种“没开始、也没结束”的圆角。
 *
 * 不是所有尺寸都适合超椭圆：胶囊按钮（999dp）和正圆图标会退化成普通胶囊/圆，
 * 否则超椭圆会在短边中点挤出一个尖。半径也会夹到短边的一半，避免负空间。
 */
@Immutable
data class SquircleShape(
    val radius: Dp,
    val exponent: Float = 2.2f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height
        // 首次布局时个别容器会拿到 NaN 尺寸（例如还没测量完的 bottomBar）。
        // Border 的通用路径会用 Path.bounds 去建一张 Alpha8 位图，NaN 会被
        // ceil 成 0，随后 Bitmap.createBitmap 直接抛 "width and height must be > 0"。
        // 这里把所有非有限/零尺寸都退成空矩形，让 border 走普通 Rect 分支。
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
            val safeWidth = if (width.isFinite()) width.coerceAtLeast(0f) else 0f
            val safeHeight = if (height.isFinite()) height.coerceAtLeast(0f) else 0f
            return Outline.Rectangle(Rect(0f, 0f, safeWidth, safeHeight))
        }

        val limit = minOf(width, height) / 2f
        val r = with(density) { radius.toPx() }.coerceIn(0f, limit)
        if (r <= 0.01f) {
            return Outline.Rectangle(Rect(0f, 0f, width, height))
        }

        // 半径已经达到短边一半：这里本来就没有直边了，超椭圆会在端点收成一个尖。
        // 胶囊按钮、圆形头像和圆点都走这一支，保持原来的几何。
        if (r >= limit - 0.5f) {
            return Outline.Rounded(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = width,
                    bottom = height,
                    cornerRadius = CornerRadius(limit, limit)
                )
            )
        }

        return Outline.Generic(
            squirclePath(
                width = width,
                height = height,
                radius = r,
                exponent = exponent.coerceIn(2.1f, 12f)
            )
        )
    }
}

/** 连续曲率圆角的简写。 */
fun squircle(radius: Dp, exponent: Float = 2.2f): SquircleShape =
    SquircleShape(radius = radius, exponent = exponent)

/**
 * 生成超椭圆圆角矩形路径。Clay 材质要自己画阴影和描边，所以路径单独暴露出来，
 * 避免它继续用 [androidx.compose.ui.geometry.RoundRect] 画出两种不同的角。
 */
internal fun squirclePath(
    width: Float,
    height: Float,
    radius: Float,
    exponent: Float = 2.2f,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path {
    val path = Path()
    if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
        val safeWidth = if (width.isFinite()) width.coerceAtLeast(0f) else 0f
        val safeHeight = if (height.isFinite()) height.coerceAtLeast(0f) else 0f
        path.addRect(Rect(offsetX, offsetY, offsetX + safeWidth, offsetY + safeHeight))
        return path
    }

    val limit = minOf(width, height) / 2f
    val r = radius.coerceIn(0f, limit)
    if (r <= 0.01f) {
        path.addRect(Rect(offsetX, offsetY, offsetX + width, offsetY + height))
        return path
    }
    if (r >= limit - 0.5f) {
        path.addRoundRect(
            RoundRect(
                left = offsetX,
                top = offsetY,
                right = offsetX + width,
                bottom = offsetY + height,
                cornerRadius = CornerRadius(limit, limit)
            )
        )
        return path
    }

    val p = 2f / exponent.coerceIn(2.1f, 12f)
    // 点数跟着半径走：小标签 12 段就够，底栏这种大圆角给到 40 段，
    // 1dp 描边在 3x 屏上也看不到折线。
    val segments = (r / 2f).coerceIn(12f, 40f).toInt()

    // cos(π/2) 在 Float 下可能是 -4e-8；负数开小数次方会得到 NaN，
    // 整条 Path 的 bounds 会变成空，Border 再拿它建 0×0 位图就崩。
    fun sx(t: Float): Float {
        val s = sin(t).toDouble().coerceAtLeast(0.0)
        return s.pow(p.toDouble()).toFloat() * r
    }

    fun sy(t: Float): Float {
        val c = cos(t).toDouble().coerceAtLeast(0.0)
        return c.pow(p.toDouble()).toFloat() * r
    }
    fun step(i: Int): Float = (PI * 0.5 * i / segments).toFloat()

    path.moveTo(offsetX + r, offsetY)
    path.lineTo(offsetX + width - r, offsetY)

    // 右上角：从顶边切点走到右边切点
    for (i in 1..segments) {
        val t = step(i)
        path.lineTo(
            offsetX + width - r + sx(t),
            offsetY + r - sy(t)
        )
    }

    path.lineTo(offsetX + width, offsetY + height - r)

    // 右下角：从右边切点绕到下面切点
    for (i in 1..segments) {
        val t = step(i)
        path.lineTo(
            offsetX + width - r + sy(t),
            offsetY + height - r + sx(t)
        )
    }

    path.lineTo(offsetX + r, offsetY + height)

    // 左下角
    for (i in 1..segments) {
        val t = step(i)
        path.lineTo(
            offsetX + r - sx(t),
            offsetY + height - r + sy(t)
        )
    }

    path.lineTo(offsetX, offsetY + r)

    // 左上角：从左边切点绕到上面切点
    for (i in 1..segments) {
        val t = step(i)
        path.lineTo(
            offsetX + r - sy(t),
            offsetY + r - sx(t)
        )
    }

    path.close()
    return path
}
