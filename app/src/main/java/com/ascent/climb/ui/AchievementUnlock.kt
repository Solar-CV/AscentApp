package com.ascent.climb.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * cardAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
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
                text = "成就解锁",
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
                Box(modifier = Modifier.scale(badgeScale)) {
                    AchievementBadge(
                        id = achievement.id,
                        unlocked = true,
                        size = 112.dp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = achievement.title,
                color = Ascent.Fg,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = achievement.desc,
                color = Ascent.Fg2,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            SoftTag(
                text = "${achievement.rarity.label} · ${achievement.category.label}",
                foreground = if (achievement.rarity == Rarity.Legendary) LEGENDARY_TEXT else colors.face,
                background = colors.face.copy(alpha = 0.14f)
            )
            Spacer(Modifier.height(18.dp))
            PillButton(
                text = "收下",
                style = BtnStyle.Primary,
                height = 46,
                onClick = onDismiss
            )
        }
    }
}

private val LEGENDARY_TEXT = Color(0xFF8A6400)
