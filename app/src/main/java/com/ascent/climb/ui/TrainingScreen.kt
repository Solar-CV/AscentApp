package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.isUnlocked

@Composable
fun TrainingScreen(
    altitude: Int,
    completed: Set<String>,
    store: ProgressStore,
    devUnlockAll: Boolean = false,
    // 开发者模式「显示正确答案」：题库型训练（装备 / 天气）也认这条开关，
    // 否则开发者只能在一课一课的测验里看到答案，练题时看不到
    devShowAnswers: Boolean = false,
    onOpenLesson: (String) -> Unit = {}
) {
    // 打开的是哪个训练项；null 表示停在列表
    var openDrill by rememberSaveable { mutableStateOf<String?>(null) }
    // 列表与训练页互斥，切走时列表离开组合 —— 用状态槽保住滚动位置
    val pageStates = rememberSaveableStateHolder()

    if (openDrill != null) {
        DrillScreen(
            drillId = openDrill!!,
            store = store,
            onBack = { openDrill = null },
            devShowAnswers = devShowAnswers
        )
        return
    }

    val nextLesson = ALL_LESSONS.firstOrNull { it.id !in completed && isUnlocked(it.id, completed, devUnlockAll) }

    // 四项训练全部可进：前两条是计时节奏训练，后两条从题库抽单选默写
    val drills = listOf(
        DrillEntry(Drill.STEP, Ic.Footprints, stringResource(R.string.tr_item1_title), stringResource(R.string.tr_item1_desc)),
        DrillEntry(Drill.BREATH, Ic.Wind, stringResource(R.string.tr_item2_title), stringResource(R.string.tr_item2_desc)),
        DrillEntry(Drill.GEAR, Ic.Backpack, stringResource(R.string.tr_item3_title), stringResource(R.string.tr_item3_desc)),
        DrillEntry(Drill.WEATHER, Ic.CloudSnow, stringResource(R.string.tr_item4_title), stringResource(R.string.tr_item4_desc))
    )

    pageStates.SaveableStateProvider("tr-list") {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item(key = "title") {
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text(stringResource(R.string.tr_title), color = Ascent.Fg, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                MonoLabel(stringResource(R.string.tr_subtitle))
            }
        }

        // ---- 今日推荐 ----
        // 这张卡指向路线上的下一课，所以整块可点：跳路径页并直接打开那课。
        // 全部通关时下面的文案换成「绳结速度训练」，那个还没有内页，保持不可点。
        item(key = "today") {
            // 整块可点，但不用默认水波纹：ClaySurface 的圆角是画出来的、
            // 没有 clip（clip 会把外扩的投影一起裁掉），矩形水波纹会在四角露出直角。
            // 改成按下微缩，反馈足够，也不破坏黏土质感。
            val press = remember { MutableInteractionSource() }
            val pressed by press.collectIsPressedAsState()
            ClaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(if (pressed) 0.98f else 1f)
                    .clickable(
                        enabled = nextLesson != null,
                        interactionSource = press,
                        indication = null
                    ) { nextLesson?.let { onOpenLesson(it.id) } },
                radius = 24.dp,
                elevation = 9.dp,
                // 平涂：顶部大块不再做上下渐变，一块实色 + 原色描边
                fill = SolidColor(Color(0xFF0F86F5)),
                rim = Color(0xFF0B6FCB)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.Dumbbell, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.tr_today),
                        color = Color(0xB3FFFFFF),
                        fontSize = 10.sp,
                        fontFamily = MonoNumbers,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (nextLesson != null) stringResource(R.string.tr_preview, lessonTitle(nextLesson.id, nextLesson.title)) else stringResource(R.string.tr_knot_speed),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (nextLesson != null)
                            stringResource(R.string.tr_meta, nextLesson.questions.size, formatMeters(altitude))
                        else
                            stringResource(R.string.tr_knot_desc),
                        color = Color(0xCCFFFFFF),
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(squircle(999.dp))
                        .background(Color(0x2EFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Ic.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            }
        }

        item(key = "label") {
            ClaySectionLabel(stringResource(R.string.tr_recommend), modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        }

        items(drills.size, key = { "d$it" }) { i ->
            val d = drills[i]
            DrillRow(
                icon = d.icon,
                title = d.title,
                reason = d.reason,
                onClick = { openDrill = d.id }
            )
        }

        // ---- 安全边界 ----
        item(key = "safety") {
            Spacer(Modifier.height(18.dp))
            ClaySurface(
                modifier = Modifier.fillMaxWidth(),
                radius = 20.dp,
                elevation = 7.dp,
                fill = Brush.verticalGradient(listOf(Color(0xFFFFF8EC), Color(0xFFFDF2DF))),
                rim = Color(0xFFF2E2C4)
            ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Ic.TriangleAlert, contentDescription = null, tint = Ascent.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.tr_boundary), color = Ascent.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.tr_boundary_body),
                        color = Ascent.Fg2,
                        fontSize = 12.sp,
                        lineHeight = 19.sp
                    )
                }
            }
            }
        }
    }
    }
}

@Composable
private fun DrillRow(
    icon: ImageVector,
    title: String,
    reason: String,
    onClick: () -> Unit
) {
    ClaySurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        radius = 20.dp,
        elevation = 7.dp
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClayChip(color = Ascent.Accent, size = 36.dp, radius = 13.dp) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(reason, color = Ascent.Fg3, fontSize = 12.sp)
        }
        Icon(Ic.ChevronRight, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
    }
    }
}

/** 训练列表的一项。[id] 是写进存档的稳定字符串，见 [Drill]。 */
private data class DrillEntry(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val reason: String
)
