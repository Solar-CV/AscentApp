package com.ascent.climb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.Lesson
import com.ascent.climb.data.lessonById

@Composable
fun ReviewScreen(
    completed: Set<String>,
    dueKeys: List<String>,
    reviewTotal: Int,
    starsOf: (String) -> Int,
    onOpenLesson: (String) -> Unit,
    onGradeReview: (String, Boolean) -> Unit = { _, _ -> }
) {
    val safetyCriticalMisses = dueKeys.mapNotNull { key ->
        val id = key.substringBefore("#")
        val idx = key.substringAfter("#").toIntOrNull()
        val lesson = lessonById(id)
        val q = idx?.let { lesson?.questions?.getOrNull(it) }
        if (lesson != null && q != null && q.safetyCritical) key to lesson else null
    }
    val otherMisses = dueKeys.mapNotNull { key ->
        val id = key.substringBefore("#")
        val idx = key.substringAfter("#").toIntOrNull()
        val lesson = lessonById(id)
        val q = idx?.let { lesson?.questions?.getOrNull(it) }
        if (lesson != null && q != null && !q.safetyCritical) key to lesson else null
    }
    val pendingCoach = ALL_LESSONS.filter { it.needsCoach && it.id in completed }

    val totalStars = ALL_LESSONS.sumOf { starsOf(it.id) }
    val maxStars = ALL_LESSONS.size * 3
    val accuracy = if (maxStars == 0) 0 else (totalStars * 100 / maxStars)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        // ---- 标题 ----
        item(key = "title") {
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text("复习", color = Ascent.Fg, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MonoLabel("间隔重复 · SPACED REPETITION")
            }
        }

        // ---- 队列 ----
        item(key = "queue") {
            OutlineCard(gap = 14) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${dueKeys.size}",
                            color = Ascent.Fg,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MonoNumbers
                        )
                        Text(
                            text = "今日待复习",
                            color = Ascent.Fg2,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (safetyCriticalMisses.isNotEmpty()) {
                            SoftTag(
                                text = "${safetyCriticalMisses.size} 道安全关键",
                                foreground = Ascent.Warning,
                                background = Ascent.WarningSoft,
                                icon = Ic.ShieldAlert
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        MonoLabel("队列 $reviewTotal · 熟练度 $accuracy%")
                    }
                }
                val cells = 8
                val filled = dueKeys.size.coerceAtMost(cells)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(cells) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (i < filled) Ascent.Accent else Ascent.Track)
                        )
                    }
                }
            }
        }

        // ---- 空态 ----
        if (dueKeys.isEmpty()) {
            item(key = "empty") {
                Spacer(Modifier.height(14.dp))
                OutlineCard(gap = 10) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            IconBadge(Ic.CircleCheck, Ascent.Fg3, Ascent.Surface2, diameter = 56, iconSize = 26)
                            Spacer(Modifier.height(12.dp))
                            Text("今天的复习已完成", color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = if (reviewTotal > 0)
                                    "队列里还有 $reviewTotal 题，到期后会自动出现在这里。"
                                else
                                    "答错的题会自动进入队列，按间隔重复排期（1 / 3 / 7 / 16 / 35 天）。",
                                color = Ascent.Fg3,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "cta") {
                Spacer(Modifier.height(14.dp))
                PillButton(
                    text = "开始复习 · ${dueKeys.size} 题",
                    icon = Ic.ArrowRight,
                    onClick = { onOpenLesson(dueKeys.first().substringBefore("#")) }
                )
            }
        }

        // ---- 安全关键 ----
        if (safetyCriticalMisses.isNotEmpty()) {
            item(key = "sec_safety") {
                MonoLabel("队列 · 安全关键优先", modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            }
            items(safetyCriticalMisses.size, key = { "sf_$it" }) { i ->
                val (_, lesson) = safetyCriticalMisses[i]
                ReviewRow(
                    lesson = lesson,
                    sub = "安全关键 · 权重 ×2",
                    icon = Ic.ShieldAlert,
                    tint = Ascent.Danger,
                    bg = Ascent.DangerSoft,
                    onClick = { onOpenLesson(lesson.id) }
                )
            }
        }

        // ---- 其他错题 ----
        if (otherMisses.isNotEmpty()) {
            item(key = "sec_other") {
                MonoLabel("其他错题", modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            }
            items(otherMisses.size, key = { "ot_$it" }) { i ->
                val (_, lesson) = otherMisses[i]
                ReviewRow(
                    lesson = lesson,
                    sub = lesson.code,
                    icon = Ic.RotateCcw,
                    tint = Ascent.Accent,
                    bg = Ascent.AccentSoft,
                    onClick = { onOpenLesson(lesson.id) }
                )
            }
        }

        // ---- 待教练评估 ----
        if (pendingCoach.isNotEmpty()) {
            item(key = "sec_coach") {
                MonoLabel("待教练现场评估", modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            }
            items(pendingCoach.size, key = { "co_$it" }) { i ->
                val lesson = pendingCoach[i]
                ReviewRow(
                    lesson = lesson,
                    sub = "理论已记录 · 需现场确认",
                    icon = Ic.Users,
                    tint = Ascent.Warning,
                    bg = Ascent.WarningSoft,
                    onClick = { onOpenLesson(lesson.id) }
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(
    lesson: Lesson,
    sub: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(Ascent.Surface)
            .border(1.dp, Ascent.Border, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tint, bg, diameter = 30, iconSize = 15)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(lesson.title, color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(sub, color = Ascent.Fg3, fontSize = 10.sp, fontFamily = MonoNumbers, letterSpacing = 0.3.sp)
        }
        Icon(Ic.ChevronRight, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
    }
}
