package com.ascent.climb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.AchCategory
import com.ascent.climb.data.Achievement
import com.ascent.climb.data.ProgressStore

/**
 * 全部徽章墙：35 枚全量展示，**未解锁以灰度呈现**（可预览，但不是已获得状态）。
 * 隐藏成就在未解锁时不显示形象，只显示「?」占位。
 */
@Composable
fun BadgeWallScreen(
    store: ProgressStore,
    achRevision: Int,
    onBack: () -> Unit
) {
    val unlocked = remember(achRevision) { store.unlockedAchievements.toSet() }

    // 系统返回 / 全面屏侧滑手势：回到个人主页，而不是退出 App
    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxWidth().background(Ascent.Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
        ) {
            item(key = "head") {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onBack() }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.width(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("返回", color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text("全部徽章", color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "已解锁 ${unlocked.size} / ${ACHIEVEMENTS.size} · 灰色为未解锁（仅预览）",
                    color = Ascent.Fg3,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
            }

            AchCategory.entries.forEach { cat ->
                val group = ACHIEVEMENTS.filter { it.category == cat }
                if (group.isEmpty()) return@forEach
                item(key = "lb_$cat") {
                    MonoLabel(
                        "${cat.label} · ${group.count { it.id in unlocked }} / ${group.size}",
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                    )
                }
                item(key = "g_$cat") {
                    OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                        group.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { a ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        BadgeWallItem(a = a, unlocked = a.id in unlocked)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeWallItem(a: Achievement, unlocked: Boolean) {
    val concealed = a.hidden && !unlocked
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AchievementBadge(
            id = a.id,
            unlocked = unlocked,
            concealed = concealed,
            size = 56.dp
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (concealed) "隐藏成就" else a.title,
            color = if (unlocked) Ascent.Fg2 else Ascent.Fg3,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = a.rarity.label,
            color = if (unlocked) Ascent.Accent else Ascent.Fg3,
            fontSize = 9.sp
        )
    }
}
