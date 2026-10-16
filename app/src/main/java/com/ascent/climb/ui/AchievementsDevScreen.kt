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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.AchCategory
import com.ascent.climb.data.Achievement
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.achievementById
import com.ascent.climb.data.validateAchievements

/**
 * 开发者模式 · 成就测试区。
 * 「模拟 / 预览」与「真实解锁」明确区分：预览弹窗与预览徽章均不写入进度。
 * 重置操作只清成就字段，绝不动课程进度。
 */
@Composable
fun AchievementsDevScreen(
    store: ProgressStore,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    var revision by remember { mutableIntStateOf(0) }
    // 预览队列：队首即当前弹窗项，关闭 = 弹出队首。
    // 旧实现用「过滤后再 drop(1) 取首个」推进，因预览不入库导致集合恒定不变，
    // 会永远算出同一项而卡死（表现为「收下」无反应）。
    var previewQueue by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var confirmResetAll by remember { mutableStateOf(false) }

    val unlocked = remember(revision) { store.unlockedAchievements.toSet() }
    val stats = remember(revision) { store.achievementStats(ALL_UNITS) }
    val problems = remember { validateAchievements(
        validLessonIds = ALL_LESSONS.map { it.id }.toSet(),
        validUnitIds = ALL_UNITS.map { it.id }.toSet(),
        glyphKeys = BADGE_GLYPH_KEYS
    ) }

    fun changed() {
        revision += 1
        onChanged()
    }

    // 系统返回 / 全面屏侧滑手势：回到设置页，而不是退出 App
    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
        ) {
            // ---------------- 头部 ----------------
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
                    Text("返回开发者模式", color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text("成就测试区", color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "已解锁 ${unlocked.size} / ${ACHIEVEMENTS.size} · 预览与模拟均不影响真实进度",
                    color = Ascent.Fg3,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
            }

            // ---------------- 统计快照 ----------------
            item(key = "stats") {
                OutlineCard(gap = 8, modifier = Modifier.padding(bottom = 14.dp)) {
                    MonoLabel("判定数据快照")
                    Text(
                        "maxStreak ${stats.maxStreak} · 累计活跃 ${stats.activeDays} 天 · 课次 ${stats.lessonRuns} · 复习答对 ${stats.reviewDone} · 队列 ${stats.reviewQueueSize} · 海拔 ${stats.altitude} · streakBroken=${stats.streakBroken}",
                        color = Ascent.Fg2,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        fontFamily = MonoNumbers
                    )
                }
            }

            // ---------------- 有效性校验 ----------------
            item(key = "validate") {
                OutlineCard(gap = 8, modifier = Modifier.padding(bottom = 14.dp)) {
                    MonoLabel("定义有效性校验")
                    if (problems.isEmpty()) {
                        Text(
                            "✓ ${ACHIEVEMENTS.size} 项全部通过：无重复 ID、徽章字形齐全、条件引用有效",
                            color = Ascent.Success,
                            fontSize = 12.sp
                        )
                    } else {
                        problems.forEach { p ->
                            Text("✗ $p", color = Ascent.Danger, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            // ---------------- 批量操作 ----------------
            item(key = "batch") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    MonoLabel("批量测试")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            PillButton(
                                text = "批量播放动画 ×3",
                                style = BtnStyle.Secondary,
                                height = 42,
                                onClick = {
                                    val pool = ACHIEVEMENTS.filter { it.id !in unlocked }
                                    previewQueue = if (pool.isEmpty()) listOf(ACHIEVEMENTS.last())
                                    else pool.take(3)
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PillButton(
                                text = if (confirmResetAll) "确认重置全部成就？" else "重置全部成就",
                                style = BtnStyle.Secondary,
                                height = 42,
                                onClick = {
                                    if (confirmResetAll) {
                                        store.resetAllAchievements()
                                        confirmResetAll = false
                                        changed()
                                    } else {
                                        confirmResetAll = true
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val queued = remember(revision) { store.pendingAchievements.toList() }
                    PillButton(
                        text = if (queued.isEmpty()) "待播队列：空" else "清空待播队列（${queued.size}）",
                        style = BtnStyle.Secondary,
                        height = 38,
                        onClick = {
                            store.clearPendingAchievements()
                            changed()
                        }
                    )
                    Text(
                        "「批量播放」仅预览弹窗动画，不入库；「重置全部」只清成就字段，不动课程进度；" +
                            "弹窗异常残留时用「清空待播队列」解卡。",
                        color = Ascent.Fg3,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // ---------------- 分组总览 ----------------
            AchCategory.entries.forEach { cat ->
                val group = ACHIEVEMENTS.filter { it.category == cat }
                if (group.isEmpty()) return@forEach
                item(key = "cat_$cat") {
                    MonoLabel(
                        "${cat.label} · ${group.count { it.id in unlocked }} / ${group.size}",
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                    )
                }
                items(group, key = { it.id }) { a ->
                    val isUnlocked = a.id in unlocked
                    val concealed = a.hidden && !isUnlocked
                    val cur = a.progress(stats).coerceIn(0, a.target)
                    OutlineCard(gap = 8, padding = 12, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AchievementBadge(
                                id = a.id,
                                unlocked = isUnlocked,
                                concealed = concealed,
                                size = 48.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (concealed) "？？？" else a.title,
                                        color = if (isUnlocked) Ascent.Fg else Ascent.Fg2,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    SoftTag(
                                        text = a.rarity.label,
                                        foreground = if (isUnlocked) rarityColors(a.rarity).face else Ascent.Fg3,
                                        background = if (isUnlocked) rarityColors(a.rarity).face.copy(alpha = 0.14f) else Ascent.Surface2
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(a.id, color = Ascent.Fg3, fontSize = 10.sp, fontFamily = MonoNumbers)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (concealed) "隐藏成就" else a.conditionText,
                                    color = Ascent.Fg3,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            SoftTag(
                                text = if (isUnlocked) "已解锁" else "未解锁",
                                foreground = if (isUnlocked) Ascent.Success else Ascent.Fg3,
                                background = if (isUnlocked) Ascent.SuccessSoft else Ascent.Surface2
                            )
                        }
                        if (a.target > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AscentBar(
                                        fraction = cur.toFloat() / a.target.toFloat(),
                                        height = 5,
                                        fill = if (isUnlocked) Ascent.Success else Ascent.Accent
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("$cur / ${a.target}", color = Ascent.Fg3, fontSize = 10.sp, fontFamily = MonoNumbers)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PillButton(
                                    text = "预览弹窗",
                                    style = BtnStyle.Secondary,
                                    height = 36,
                                    onClick = { previewQueue = listOf(a) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (isUnlocked) {
                                    PillButton(
                                        text = "重置此项",
                                        style = BtnStyle.Secondary,
                                        height = 36,
                                        onClick = {
                                            store.resetAchievement(a.id)
                                            changed()
                                        }
                                    )
                                } else {
                                    PillButton(
                                        text = "强制解锁",
                                        style = BtnStyle.Primary,
                                        height = 36,
                                        onClick = {
                                            store.forceUnlockAchievement(a.id)
                                            changed()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 预览弹窗（本地预览，不入库、不排队）：队首展示，关闭即出队
        previewQueue.firstOrNull()?.let { a ->
            AchievementUnlockOverlay(
                achievement = a,
                onDismiss = { previewQueue = previewQueue.drop(1) }
            )
        }
    }
}
