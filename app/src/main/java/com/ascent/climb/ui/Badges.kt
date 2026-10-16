package com.ascent.climb.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ACHIEVEMENT_GLYPHS
import com.ascent.climb.data.Rarity

/** 稀有度配色（与单元色同族；唇色 = 面色加深）。 */
data class RarityColors(val face: Color, val lip: Color)

private val COMMON_C = RarityColors(Color(0xFF58CC02), Color(0xFF46A302))
private val RARE_C = RarityColors(Color(0xFF1CB0F6), Color(0xFF1899D6))
private val EPIC_C = RarityColors(Color(0xFFCE82FF), Color(0xFFA964E8))
private val LEGENDARY_C = RarityColors(Color(0xFFFFC800), Color(0xFFD19E00))

fun rarityColors(r: Rarity): RarityColors = when (r) {
    Rarity.Common -> COMMON_C
    Rarity.Rare -> RARE_C
    Rarity.Epic -> EPIC_C
    Rarity.Legendary -> LEGENDARY_C
}

private val LEGENDARY_GLYPH = Color(0xFF4A3208)

/** 成就校验用的合法字形键集合（以数据层 ACHIEVEMENT_GLYPHS 为准）。 */
val BADGE_GLYPH_KEYS: Set<String> = ACHIEVEMENT_GLYPHS

/** 未解锁（含隐藏）时的去饱和滤镜：同一张位图直接转灰，不出第二套资源。 */
private val LOCKED_SATURATION = ColorMatrix().apply { setToSaturation(0f) }

/**
 * 成就徽章（AI 渲染位图版）。
 *
 * - 已解锁：原图全彩；
 * - 未解锁：同一张图去饱和 + 62% 不透明度（保持轮廓可辨，统一灰调）；
 * - 隐藏成就未解锁 / 无资源：灰盘 + 「?」占位（不泄露隐藏成就的形象）。
 *
 * @param id 成就 id（对应 [badgeRes] 映射）
 */
@Composable
fun AchievementBadge(
    id: String,
    unlocked: Boolean,
    concealed: Boolean = false,
    size: Dp = 52.dp
) {
    val res = badgeRes(id)
    if (res != null && !(concealed && !unlocked)) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .then(if (unlocked) Modifier else Modifier.alpha(0.62f)),
            colorFilter = if (unlocked) null else ColorFilter.colorMatrix(LOCKED_SATURATION)
        )
        return
    }
    // 占位：隐藏成就未解锁 / 资源缺失
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Ascent.LockFace2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            color = Ascent.LockGlyph,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
