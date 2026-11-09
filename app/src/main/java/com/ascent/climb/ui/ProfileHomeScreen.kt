package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.pluralStringResource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.activeTrackId
import com.ascent.climb.data.displaySummitOf
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.Account

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
    onOpenAllBadges: () -> Unit,
    onOpenBadge: (String) -> Unit = {},
    onEditProfile: () -> Unit = {},
    /** 本地没设过用户名时用它兜底：登录了就是账号昵称，没登录才落到「登山者」。 */
    fallbackName: String = "",
    // ---------------- 账号（v0.26.0 从「我的」搬过来，与编辑资料合并成一张卡） ----------------
    account: Account? = null,
    /** 上次同步时间 / 同步中 / 失败原因，由上层算好传进来。 */
    syncLine: String = "",
    syncing: Boolean = false,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSyncNow: () -> Unit = {}
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

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
        ) {
            item(key = "head") {
                Row(
                    modifier = Modifier
                        .clip(squircle(999.dp))
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

            // ---------------- 档案 + 账号 ----------------
            // 原来「我的」页单独有一张账号卡，身份信息在两处各说一遍。
            // 现在合成一张：上半是身份（点进编辑资料），下半是账号状态与动作。
            item(key = "id") {
                val a = clayAccents()
                val acc = account
                val loggedIn = acc != null
                val shownName = store.displayName.ifBlank {
                    fallbackName.ifBlank { stringResource(R.string.pf_default_name) }
                }
                ClayColumn(
                    modifier = Modifier.padding(bottom = 16.dp),
                    radius = 24.dp,
                    elevation = 9.dp,
                    padding = 16.dp,
                    gap = 12.dp
                ) {
                    // 身份行：整块可点，进「编辑资料」
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(squircle(16.dp))
                            .clickable { onEditProfile() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(version = store.avatarVersion, name = shownName, size = 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(shownName, color = a.value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (!loggedIn) {
                                    Spacer(Modifier.width(6.dp))
                                    SoftTag(stringResource(R.string.prof_not_logged_in), a.sub, Ascent.Surface2)
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            // 登录了显示邮箱，没登录直接说清楚「登录能得到什么」
                            Text(
                                text = acc?.email ?: stringResource(R.string.prof_login_cloud),
                                color = a.sub,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(
                                    R.string.home_meta,
                                    formatMeters(altitude),
                                    doneHere,
                                    ALL_LESSONS.size
                                ),
                                color = a.sub,
                                fontSize = 11.sp
                            )
                        }
                        Icon(Ic.ChevronRight, contentDescription = null, tint = a.sub, modifier = Modifier.size(16.dp))
                    }

                    // 同步状态只在登录后才有意义
                    if (loggedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Ic.Cloud, contentDescription = null, tint = a.sub, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = syncLine,
                                color = a.sub,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            SoftTag(
                                text = stringResource(if (syncing) R.string.sync_running else R.string.sync_now),
                                foreground = Ascent.OnAccent,
                                background = if (syncing) Ascent.Surface2 else Ascent.Accent,
                                modifier = Modifier
                                    .clip(squircle(999.dp))
                                    .clickable(enabled = !syncing) { onSyncNow() },
                                icon = if (syncing) null else Ic.RefreshCw
                            )
                        }
                    }

                    // 动作：未登录时把「登录 / 注册」放在最显眼的位置
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!loggedIn) {
                            PillButton(
                                text = stringResource(R.string.prof_login_register),
                                modifier = Modifier.weight(1.15f),
                                style = BtnStyle.Primary,
                                height = 44,
                                fontSize = 13.sp,
                                onClick = onLogin
                            )
                        }
                        PillButton(
                            text = stringResource(R.string.pf_title),
                            modifier = Modifier.weight(1f),
                            style = BtnStyle.Secondary,
                            height = 44,
                            fontSize = 13.sp,
                            onClick = onEditProfile
                        )
                        if (loggedIn) {
                            PillButton(
                                text = stringResource(R.string.prof_logout),
                                modifier = Modifier.weight(1f),
                                style = BtnStyle.Secondary,
                                height = 44,
                                fontSize = 13.sp,
                                onClick = onLogout
                            )
                        }
                    }
                }
            }

            // ---------------- 学习概览 ----------------
            // 原先 2×2 四宫格 + 两条进度。现在每项一行：图标色块给索引，数字同色加深，
            // 海拔那行翻成墨黑作为页面的重量锥（总进度已并进"完成课程"，同一个数）。
            item(key = "lbStat") {
                ClaySectionLabel(
                    stringResource(R.string.home_overview),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            item(key = "stat") {
                val a = clayAccents()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ClayStatRow(
                        label = stringResource(R.string.home_streak),
                        value = pluralStringResource(R.plurals.common_days, streak, streak),
                        icon = Ic.Flame,
                        tint = a.streak
                    )
                    ClayStatRow(
                        label = stringResource(R.string.home_active_days),
                        value = pluralStringResource(R.plurals.common_days, stats.activeDays, stats.activeDays),
                        icon = Ic.Calendar,
                        tint = a.days
                    )
                    ClayStatRow(
                        label = stringResource(R.string.home_lessons_done),
                        value = "$doneHere / ${ALL_LESSONS.size}",
                        icon = Ic.CircleCheck,
                        tint = a.lessons,
                        ratio = overall
                    )
                    ClayStatRow(
                        label = stringResource(R.string.home_badges),
                        value = "${earned.size} / ${ACHIEVEMENTS.size}",
                        icon = Ic.Award,
                        tint = a.badges,
                        ratio = if (ACHIEVEMENTS.isEmpty()) 0f
                        else earned.size.toFloat() / ACHIEVEMENTS.size
                    )
                    ClayStatRow(
                        label = stringResource(R.string.home_progress_altitude),
                        value = "${formatMeters(altitude)} / ${formatMeters(summitAlt)}",
                        icon = Ic.MountainSnow,
                        tint = a.lessons,
                        ratio = altFrac,
                        barColor = a.lessons,
                        emphasize = true
                    )
                }
                Spacer(Modifier.height(18.dp))
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
                            .clip(squircle(999.dp))
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
                ClaySurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    radius = 22.dp,
                    elevation = 8.dp
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                        BadgeGrid(items = shown, onOpen = onOpenBadge)
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

            // ---------------- 品牌尾标 ----------------
            // 固定英文，**不进多语言体系**：它是字标（logotype）+ 品牌口号，
            // 逐语言翻译会把语气打散（比如「探索更远。攀登更高。」读起来像说明书）。
            item(key = "brand") {
                BrandFooter()
            }
        }
    }
}

/**
 * 页脚品牌块：字标 / 口号 / 一句话定位，三段居中等宽字距。
 *
 * 只在「我的 · 个人主页」最下方出现；颜色全部取自黏土令牌，深色模式自动换。
 */
@Composable
private fun BrandFooter() {
    val a = clayAccents()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ASCENTROAM",
            color = a.value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            // 全大写字标：字号压到 22，字距放到 2.2，九个字符才不挤在一起
            letterSpacing = 2.2.sp
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "EXPLORE FURTHER. CLIMB HIGHER.",
            color = a.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = "MOUNTAIN LEARNING & TRAINING",
            color = a.sub,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 徽章网格（每行 4 枚）。点开看 3D 展示。 */
@Composable
fun BadgeGrid(
    items: List<com.ascent.climb.data.Achievement>,
    onOpen: (String) -> Unit = {}
) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(squircle(14.dp))
                    .clickable { onOpen(a.id) }
                    .padding(vertical = 4.dp)
            ) {
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
