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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.activeTrackId
import com.ascent.climb.data.displaySummitOf
import com.ascent.climb.data.ALL_LESSONS

/**
 * 个人主页（「我的」页点头像进入）。
 *
 * 结构：档案头 → 学习概览（连续天数 / 累计天数 / 完成课程 / 总进度）→ 已解锁徽章。
 * **只展示已解锁徽章**，未解锁的需进入「全部」页面预览，避免首页徽章过多。
 */
@Composable
fun ProfileHomeScreen(
    store: ProgressStore,
    altitude: Int,
    streak: Int,
    completed: Set<String>,
    achRevision: Int,
    onBack: () -> Unit,
    onOpenAllBadges: () -> Unit
) {
    val unlocked = remember(achRevision) { store.unlockedAchievements.toSet() }
    val stats = remember(achRevision) { store.achievementStats() }
    val earned = ACHIEVEMENTS.filter { it.id in unlocked }
    val shown = earned.take(8)
    // completed 是两条轨道共用的 id 集合，分母是当前轨道，分子必须也只数当前轨道，
    // 否则「攀登完成 68 课、切到徒步」会显示 68 / 21。
    val doneHere = ALL_LESSONS.count { it.id in completed }
    val overall = if (ALL_LESSONS.isEmpty()) 0f
    else doneHere.toFloat() / ALL_LESSONS.size.toFloat()
    val summitAlt = displaySummitOf(activeTrackId())
    val altFrac = if (summitAlt == 0) 0f else altitude.toFloat() / summitAlt.toFloat()

    // 系统返回 / 全面屏侧滑手势：回到「我的」，而不是退出 App
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
                    Text(stringResource(R.string.common_back), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(10.dp))
            }

            // ---------------- 档案头 ----------------
            item(key = "id") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Ascent.Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Ic.UserRound, contentDescription = null, tint = Ascent.OnAccent, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_title), color = Ascent.Fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.home_meta, formatMeters(altitude), doneHere, ALL_LESSONS.size),
                            color = Ascent.Fg3,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ---------------- 学习概览 ----------------
            item(key = "lbStat") { MonoLabel(stringResource(R.string.home_overview), modifier = Modifier.padding(bottom = 8.dp)) }
            item(key = "stat") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatTile(value = stringResource(R.string.common_days_n, streak), label = stringResource(R.string.home_streak))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatTile(value = stringResource(R.string.common_days_n, stats.activeDays), label = stringResource(R.string.home_active_days))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatTile(value = "$doneHere / ${ALL_LESSONS.size}", label = stringResource(R.string.home_lessons_done))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatTile(value = "${earned.size} / ${ACHIEVEMENTS.size}", label = stringResource(R.string.home_badges))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    MiniProgressRow(label = stringResource(R.string.home_progress_total), frac = overall)
                    Spacer(Modifier.height(8.dp))
                    MiniProgressRow(label = stringResource(R.string.home_progress_altitude), frac = altFrac)
                }
            }

            // ---------------- 已解锁徽章 ----------------
            item(key = "lbBadge") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLabel(stringResource(R.string.home_badges_unlocked, earned.size), modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onOpenAllBadges() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.hb_filter_all), color = Ascent.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Icon(
                            Ic.ChevronRight,
                            contentDescription = null,
                            tint = Ascent.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            item(key = "badges") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    if (earned.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_no_badges),
                            color = Ascent.Fg3,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        PillButton(
                            text = stringResource(R.string.home_view_all_badges),
                            style = BtnStyle.Secondary,
                            height = 42,
                            onClick = onOpenAllBadges
                        )
                    } else {
                        BadgeGrid(items = shown)
                        if (earned.size > shown.size) {
                            PillButton(
                                text = stringResource(R.string.home_view_all_badges, ACHIEVEMENTS.size),
                                style = BtnStyle.Secondary,
                                height = 42,
                                onClick = onOpenAllBadges
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 徽章网格（每行 4 枚）。 */
@Composable
fun BadgeGrid(items: List<com.ascent.climb.data.Achievement>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(((items.size + 3) / 4 * 92).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(items, key = { it.id }) { a ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AchievementBadge(id = a.id, unlocked = true, size = 56.dp)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = achievementTitle(a.id, a.title),
                    color = Ascent.Fg2,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MiniProgressRow(label: String, frac: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Ascent.Fg2, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                "${(frac * 100).toInt()}%",
                color = Ascent.Fg3,
                fontSize = 11.sp,
                fontFamily = MonoNumbers
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Ascent.Track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ascent.Accent)
            )
        }
    }
}
