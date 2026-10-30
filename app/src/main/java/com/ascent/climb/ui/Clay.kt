package com.ascent.climb.ui

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 黏土（Claymorphism）材质。
 *
 * 难点只有一个：柔和的双向投影。Compose 到 API 31 才有 `Modifier.blur()`，
 * 而本工程 minSdk 26；`Modifier.shadow()` 又只能给"正下方"一层、颜色也不可控。
 * 所以这里用离屏 Bitmap + `BlurMaskFilter` 把阴影**预渲染**出来，按
 * 「尺寸 / 圆角 / 偏移 / 模糊 / 颜色」缓存，绘制时只是一次 `drawImage`。
 * 一屏十来张卡片、滚动时也不会掉帧。
 *
 * 材质由四层构成（缺一层就变回普通的圆角白卡）：
 *   1. 外投影：右下方的暖灰，做"压在背景上"的重量
 *   2. 外光斑：左上方纯白，做"被顶光照到"的接触面
 *   3. 内顶高光：形状内上沿一条白色渐隐，做圆润的厚度
 *   4. 内底压暗：形状内下沿一条灰蓝渐隐，做转折
 */

/** 阴影位图 + 四周留白（留白用于容纳模糊半径）。 */
private class ShadowImage(val image: ImageBitmap, val pad: Int)

private object ClayShadow {
    // LRU，太多不同尺寸会把内存吃掉
    private val cache = object : LinkedHashMap<String, ShadowImage>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ShadowImage>?) =
            size > 24
    }

    fun of(w: Int, h: Int, radius: Float, dx: Float, dy: Float, blur: Float, argb: Int): ShadowImage {
        val key = "$w:$h:$radius:$dx:$dy:$blur:$argb"
        cache[key]?.let { return it }

        val pad = ceil(blur * 1.6f + max(abs(dx), abs(dy))).toInt().coerceAtLeast(1)
        val bw = (w + pad * 2).coerceAtLeast(1)
        val bh = (h + pad * 2).coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            pad + dx, pad + dy, pad + dx + w, pad + dy + h, radius, radius, paint
        )
        val result = ShadowImage(bmp.asImageBitmap(), pad)
        cache[key] = result
        return result
    }
}

private fun DrawScope.drawShadow(img: ShadowImage) {
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawBitmap(
            img.image.asAndroidBitmap(),
            -img.pad.toFloat(),
            -img.pad.toFloat(),
            null
        )
    }
}

/** 黏土表面的配色：随深浅色主题切换。 */
private data class ClayTone(
    val top: Color,
    val bottom: Color,
    val rim: Color,
    val darkShadow: Int,
    val lightShadow: Int,
    val innerTop: Float,
    val innerBottom: Color
)

@Composable
private fun clayTone(dark: Boolean): ClayTone = if (dark) {
    ClayTone(
        top = Color(0xFF232528),
        bottom = Color(0xFF191A1D),
        rim = Color(0xFF3A4048),
        darkShadow = 0x99000000.toInt(),
        lightShadow = 0x1FFFFFFF.toInt(),
        innerTop = 0.10f,
        innerBottom = Color(0x59000000)
    )
} else {
    ClayTone(
        top = Color(0xFFFFFFFF),
        bottom = Color(0xFFFAFBFD),
        // 底是 #F5F5F5、卡是 #FFF，两者只差 9 个色阶，全靠这圈描边与投影撑住边界。
        // 描边给到 216 左右才看得见；早先的 #E8EAEF（232）在同色底上等于没有。
        rim = Color(0xFFD8DEE8),
        darkShadow = 0x668C94A3.toInt(),
        lightShadow = 0x40FFFFFF.toInt(),
        innerTop = 0.30f,
        innerBottom = Color(0x3BC4CBD8)
    )
}

/**
 * 一块黏土。内容是叠在材质之上的普通 Compose 内容。
 *
 * [elevation] 控制投影的距离与模糊：卡片一般 8–10dp，小图标 4–5dp。
 * 注意：阴影画在控件边界**之外**，所以外层留白交给自己控制，不要太挤。
 */
@Composable
fun ClaySurface(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    elevation: Dp = 8.dp,
    fill: Brush? = null,
    rim: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tone = clayTone(Ascent.IsDark)
    val brush = fill ?: Brush.verticalGradient(listOf(tone.top, tone.bottom))

    Box(
        modifier = modifier.drawBehind {
            val r = radius.toPx()
            val e = elevation.toPx()
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@drawBehind

            // 1) 外投影（右下），两层：
            //    贴边的一层收紧做接触影（卡片"坐"在底色上），远的一层做体积。
            //    只有一层时怎么调都发虚——边缘的落差全在接触影上。
            drawShadow(
                ClayShadow.of(w.toInt(), h.toInt(), r, 0f, e * 0.92f, e * 1.5f, tone.darkShadow)
            )
            drawShadow(
                ClayShadow.of(
                    w.toInt(), h.toInt(), r, 0f, e * 0.3f, e * 0.5f,
                    (tone.darkShadow and 0x00FFFFFF) or 0x40000000
                )
            )
            // 2) 左上外光：贴边一圈，模糊小才不会在浅底上糊成一团白气
            drawShadow(
                ClayShadow.of(w.toInt(), h.toInt(), r, 0f, -e * 0.5f, e * 0.85f, tone.lightShadow)
            )

            // 3) 本体
            drawRoundRect(brush = brush, cornerRadius = CornerRadius(r), size = size)
            // 描边按 dp 走：原来写死 1f = 1 物理像素，在 2.75 倍屏上只有 0.36dp，
            // 抗锯齿一平均就没了——这正是"边缘不明显"的另一半原因。
            drawRoundRect(
                color = rim ?: tone.rim,
                cornerRadius = CornerRadius(r),
                size = size,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4) 内高光 / 内压暗：贴边的一道唇，高度封顶，不随卡片变高而越铺越白。
            // 早先是「85% 白铺到 0.45 高度」，彩色卡上半块直接被漂白，看着像半透明。
            val lipTop = minOf(h * 0.4f, 20.dp.toPx())
            val lipBottom = minOf(h * 0.45f, 24.dp.toPx())
            val path = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        0f, 0f, w, h, CornerRadius(r)
                    )
                )
            }
            clipPath(path) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = tone.innerTop),
                        0.45f to Color.White.copy(alpha = tone.innerTop * 0.22f),
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = lipTop
                    ),
                    size = Size(w, h)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to tone.innerBottom,
                        startY = h - lipBottom,
                        endY = h
                    ),
                    size = Size(w, h)
                )
            }
        },
        content = content
    )
}

/** 图标方块：饱和色**平涂**小块 + 同色投影。上亮下暗与唇光都去掉——色块要看着是压扁的，不是一颗糖。 */
@Composable
fun ClayChip(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    radius: Dp = 11.dp,
    glyph: String? = null,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val dark = Ascent.IsDark
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val r = radius.toPx()
                val e = size.toPx() * 0.12f
                val w = this.size.width
                val h = this.size.height
                if (w <= 0f) return@drawBehind
                drawShadow(
                    ClayShadow.of(
                        w.toInt(), h.toInt(), r, 0f, e * 0.8f, e * 1.1f,
                        // 0.42 的同色投影在白色卡片上会晕成一圈发光，看着像霓虹；
                        // 压到 0.30 才只是"压在卡上"的影子
                        color.copy(alpha = if (dark) 0.26f else 0.30f).toArgb()
                    )
                )
                // 平涂：一块实色，不做垂直渐变、不做顶部唇光。
                // 早先是「上亮下暗 + 18% 白唇」，读起来像糖块／塑料件，
                // 和整页「压实的一片泥」不是一个语言。
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(r),
                    size = this.size
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (content != null) content()
        else if (glyph != null) {
            androidx.compose.material3.Text(
                text = glyph,
                color = Color.White,
                fontSize = (size.value * 0.46f).sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
        }
    }
}

/**
 * 内凹（按下去）的表面：用于搜索框、进度槽这类"被压进背景"的元素。
 * 与外凸相反，暗线在上、亮线在下。
 */
@Composable
fun Modifier.clayInset(radius: Dp = 16.dp): Modifier {
    val dark = Ascent.IsDark
    return drawBehind {
        val r = radius.toPx()
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@drawBehind
        val path = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, CornerRadius(r)))
        }
        clipPath(path) {
            // 上沿暗：光从左上来，凹槽的里面那面背光
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = if (dark) 0.35f else 0.12f),
                    1f to Color.Transparent,
                    startY = 0f,
                    endY = minOf(h * 0.35f, 14.dp.toPx())
                ),
                size = Size(w, h)
            )
            // 下沿亮：凹槽底面朝向光。原来给到 85% 白，等于在底边贴了一条白条，
            // 输入框看着像被切掉了下缘；收到 55% 才是"磨圆的槽底"。
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.White.copy(alpha = if (dark) 0.06f else 0.55f),
                    startY = h - minOf(h * 0.4f, 16.dp.toPx()),
                    endY = h
                ),
                size = Size(w, h)
            )
        }
        drawRoundRect(
            color = if (dark) Color(0xFF111214) else Color(0xFFDCE1E9),
            cornerRadius = CornerRadius(r),
            size = size,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun Color.darken(f: Float): Color = Color(
    red = red * f,
    green = green * f,
    blue = blue * f,
    alpha = alpha
)

