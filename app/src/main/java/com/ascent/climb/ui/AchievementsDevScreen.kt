package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
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
import com.ascent.climb.data.ALL_UNITS_EVERY_TRACK
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
    // 3D 展示：null 表示不在展示页。成就测试区是唯一入口（设置页的旧入口已移除）。
    var stageId by remember { mutableStateOf<String?>(null) }

    stageId?.let { id ->
        Badge3DStageScreen(achievementId = id, onBack = { stageId = null })
        return
    }

    val unlocked = remember(revision) { store.unlockedAchievements.toSet() }
    val stats = remember(revision) { store.achievementStats() }
    // 校验用全轨道的课程/单元 id：成就条件里会引用徒步的 t01、t1–t4，
    // 只用当前轨道会让这些引用在攀登轨道上被误报为「不存在」。
    val problems = remember { validateAchievements(
        validLessonIds = ALL_UNITS_EVERY_TRACK.flatMap { it.lessons }.map { it.id }.toSet(),
        validUnitIds = ALL_UNITS_EVERY_TRACK.map { it.id }.toSet(),
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
                    Text(stringResource(R.string.dev_back_to), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.dev_title), color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dev_unlocked_meta, unlocked.size, ACHIEVEMENTS.size),
                    color = Ascent.Fg3,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
            }

            // ---------------- 统计快照 ----------------
            item(key = "stats") {
                OutlineCard(gap = 8, modifier = Modifier.padding(bottom = 14.dp)) {
                    MonoLabel(stringResource(R.string.dev_snapshot))
                    Text(
                        stringResource(R.string.dev_stats_detail, stats.maxStreak, stats.activeDays, stats.lessonRuns, stats.reviewDone, stats.reviewQueueSize, stats.altitude) + " · streakBroken=${stats.streakBroken}",
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
                    MonoLabel(stringResource(R.string.dev_validity))
                    if (problems.isEmpty()) {
                        Text(
                            "✓ " + stringResource(R.string.dev_valid_ok, ACHIEVEMENTS.size),
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
                    MonoLabel(stringResource(R.string.dev_batch))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            PillButton(
                                text = stringResource(R.string.dev_batch_play),
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
                                text = if (confirmResetAll) stringResource(R.string.dev_reset_all_confirm) else stringResource(R.string.dev_reset_all),
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
                        text = if (queued.isEmpty()) stringResource(R.string.dev_queue_empty) else stringResource(R.string.dev_queue_clear, queued.size),
                        style = BtnStyle.Secondary,
                        height = 38,
                        onClick = {
                            store.clearPendingAchievements()
                            changed()
                        }
                    )
                    Text(
                        stringResource(R.string.dev_note_1) +
                            stringResource(R.string.dev_note_2),
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
                        "${achCategoryName(cat)} · ${group.count { it.id in unlocked }} / ${group.size}",
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                    )
                }
                items(group, key = { it.id }) { a ->
                    val isUnlocked = a.id in unlocked
                    val concealed = a.hidden && !isUnlocked
                    val cur = a.progress(stats).coerceIn(0, a.target)
                    OutlineCard(gap = 8, padding = 12, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 点徽章直接看 3D 展示（真实模型或图片假 3D）
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { stageId = a.id }
                            ) {
                                AchievementBadge(
                                    id = a.id,
                                    unlocked = isUnlocked,
                                    concealed = concealed,
                                    size = 48.dp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (concealed) "？？？" else achievementTitle(a.id, a.title),
                                        color = if (isUnlocked) Ascent.Fg else Ascent.Fg2,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    SoftTag(
                                        text = rarityName(a.rarity),
                                        foreground = if (isUnlocked) rarityColors(a.rarity).face else Ascent.Fg3,
                                        background = if (isUnlocked) rarityColors(a.rarity).face.copy(alpha = 0.14f) else Ascent.Surface2
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(a.id, color = Ascent.Fg3, fontSize = 10.sp, fontFamily = MonoNumbers)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (concealed) stringResource(R.string.badge_hidden) else a.conditionText,
                                    color = Ascent.Fg3,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            SoftTag(
                                text = if (isUnlocked) stringResource(R.string.dev_unlocked) else stringResource(R.string.dev_locked),
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
                                    text = stringResource(R.string.dev_preview),
                                    style = BtnStyle.Secondary,
                                    height = 36,
                                    onClick = { previewQueue = listOf(a) }
                                )
                            }
                            Box(modifier = Modifier.weight(0.72f)) {
                                PillButton(
                                    text = "3D",
                                    style = BtnStyle.Secondary,
                                    height = 36,
                                    onClick = { stageId = a.id }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (isUnlocked) {
                                    PillButton(
                                        text = stringResource(R.string.dev_reset_item),
                                        style = BtnStyle.Secondary,
                                        height = 36,
                                        onClick = {
                                            store.resetAchievement(a.id)
                                            changed()
                                        }
                                    )
                                } else {
                                    PillButton(
                                        text = stringResource(R.string.dev_force_unlock),
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
