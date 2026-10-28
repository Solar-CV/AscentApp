package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.Achievement
import com.ascent.climb.data.Rarity

/** 是否应减少动效（尊重系统「移除动画」设置）。 */
@Composable
private fun reduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * 成就解锁弹窗（单条）。队列由调用方维护：每次只展示一条，关闭后播下一条。
 * 动画：卡片淡入上移 + 徽章弹性放大；传说级附加金色光环脉冲。
 */
@Composable
fun AchievementUnlockOverlay(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    val reduce = reduceMotion()
    val colors = rarityColors(achievement.rarity)

    // 系统返回 / 全面屏侧滑手势：先关掉弹窗，而不是退出 App
    BackHandler { onDismiss() }

    // 入场：徽章弹性放大
    var appeared by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { appeared = true }
    val badgeScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.4f,
        animationSpec = if (reduce) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "badgeScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = if (reduce) tween(0) else tween(220),
        label = "cardAlpha"
    )

    // 传说级：光环脉冲
    val legendaryGlow = if (!reduce && achievement.rarity == Rarity.Legendary) {
        val t = rememberInfiniteTransition(label = "glow")
        t.animateFloat(
            initialValue = 1f,
            targetValue = 1.22f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "glowScale"
        ).value
    } else 1f

    // 防误触：遮罩（卡片以外的边框区域）第一次点击只「上膛」，第二次才关。
    // 弹窗往往在某次点击后立刻出现，手指可能还在屏幕上，一次点击就关掉等于白得一枚徽章。
    var scrimArmed by remember(achievement.id) { mutableStateOf(false) }
    // 上膛后淡入一行小字，免得用户以为点了没反应
    val hintAlpha by animateFloatAsState(
        targetValue = if (scrimArmed) 1f else 0f,
        animationSpec = if (reduce) tween(0) else tween(180),
        label = "hintAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * cardAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (scrimArmed) onDismiss() else scrimArmed = true },
        contentAlignment = Alignment.Center
    ) {
        if (!reduce) {
            ConfettiBurst(colors = listOf(colors.face, colors.lip), modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .scale(cardAlpha)
                .clip(RoundedCornerShape(24.dp))
                .background(Ascent.Surface)
                .border(2.dp, colors.face.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 卡片本体不拦截关闭误触 */ }
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.ach_unlocked),
                color = colors.face,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                if (achievement.rarity == Rarity.Legendary) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(legendaryGlow)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.face.copy(alpha = 0.25f))
                    )
                }
                Box(
                    modifier = Modifier.size(132.dp).scale(badgeScale),
                    contentAlignment = Alignment.Center
                ) {
                    // 浅色底盘只给真 3D：弹窗卡片是深色，透明底的金属模型直接贴上去
                    // 会糊成一团黑；图片假 3D 自带深色底，再套圆底反而像方卡嵌在圆盘里
                    if (badgeHasModel(achievement.id)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFF4F8F9), Color(0xFFD5E1E3))
                                    )
                                )
                        )
                    }
                    // 有 glb 走实时 3D；首帧到达前先用定稿图顶着，避免弹窗出现空白
                    var badgeReady by remember(achievement.id) { mutableStateOf(false) }
                    Badge3DAuto(
                        achievementId = achievement.id,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { badgeReady = true }
                    )
                    if (!badgeReady) {
                        AchievementBadge(id = achievement.id, unlocked = true, size = 112.dp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = achievementTitle(achievement.id, achievement.title),
                color = Ascent.Fg,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = achievementDesc(achievement.id, achievement.desc),
                color = Ascent.Fg2,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            SoftTag(
                text = "${rarityName(achievement.rarity)} · ${achCategoryName(achievement.category)}",
                foreground = if (achievement.rarity == Rarity.Legendary) LEGENDARY_TEXT else colors.face,
                background = colors.face.copy(alpha = 0.14f)
            )
            Spacer(Modifier.height(18.dp))
            PillButton(
                text = stringResource(R.string.ach_accept),
                style = BtnStyle.Primary,
                height = 46,
                onClick = onDismiss
            )
        }

        if (hintAlpha > 0f) {
            Text(
                text = stringResource(R.string.ach_tap_again),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 46.dp)
                    .alpha(hintAlpha)
            )
        }
    }
}

private val LEGENDARY_TEXT = Color(0xFF8A6400)

private class ConfettiPiece(
    val fromRight: Boolean,
    val yFraction: Float,
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val w: Float,
    val h: Float,
    val color: Color
)

/**
 * 解锁彩带：从左右两侧向屏幕中央喷射，受重力下坠并自转，2.8 秒后消失。
 *
 * 位置由时间解析式算出来（p = p0 + v·t + ½g·t²），不累积状态，
 * 因此每帧只读一个 elapsed，不会产生大量临时对象。
 */
@Composable
private fun ConfettiBurst(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val palette = remember(colors) {
        colors + listOf(Color(0xFFFFE9A8), Color(0xFFFFFFFF), Color(0xFFFFC800))
    }
    val pieces = remember(palette) {
        val rnd = java.util.Random(20261026L)
        List(56) { i ->
            ConfettiPiece(
                fromRight = i % 2 == 1,
                yFraction = 0.12f + rnd.nextFloat() * 0.52f,
                vx = (if (i % 2 == 1) -1f else 1f) * (430f + rnd.nextFloat() * 640f),
                vy = -(560f + rnd.nextFloat() * 820f),
                spin = (rnd.nextFloat() - 0.5f) * 1000f,
                w = 6f + rnd.nextFloat() * 7f,
                h = 13f + rnd.nextFloat() * 13f,
                color = palette[rnd.nextInt(palette.size)]
            )
        }
    }

    var elapsed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            elapsed = (now - start) / 1_000_000_000f
            if (elapsed > 2.8f) break
        }
    }

    Canvas(modifier = modifier) {
        if (elapsed <= 0f) return@Canvas
        val gravity = 1500f
        val fade = (1f - ((elapsed - 2.1f) / 0.7f)).coerceIn(0f, 1f)
        pieces.forEach { piece ->
            val x = (if (piece.fromRight) size.width + 28f else -28f) + piece.vx * elapsed
            val y = size.height * piece.yFraction + piece.vy * elapsed +
                0.5f * gravity * elapsed * elapsed
            if (y > size.height + 80f) return@forEach
            rotate(piece.spin * elapsed, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = piece.color.copy(alpha = fade),
                    topLeft = Offset(x - piece.w / 2f, y - piece.h / 2f),
                    size = Size(piece.w, piece.h),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}
