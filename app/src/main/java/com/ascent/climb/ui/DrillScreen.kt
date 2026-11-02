package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.data.ProgressStore
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos

/**
 * 训练项 id。
 *
 * **稳定字符串，不要用资源 id 或下标** —— 因为它们会随语言和列表顺序变化，
 * 而这两个值会写进 SharedPreferences（v0.23 的头衔要读）。
 */
object Drill {
    /** 4-4 呼吸节拍器，5 分钟。 */
    const val BREATH = "breath44"

    /** 冰爪步伐节奏预演，4 组 × 30 秒。 */
    const val STEP = "step"
}

// ---------------- 时长参数 ----------------
//
// 呼吸：吸 4 秒 / 呼 4 秒，一次完整循环 8 秒，5 分钟约 37 次循环。
// 步伐：每步 1.2 秒（约 50 步/分钟）—— 高山行走刻意放慢，这个节拍比平地散步慢，
//       练的是「不抢步」而不是速度。

private const val BREATH_IN_MS = 4_000L
private const val BREATH_CYCLE_MS = BREATH_IN_MS * 2
private const val BREATH_TOTAL_MS = 5 * 60 * 1000L

private const val STEP_INTERVAL_MS = 1_200L
private const val STEP_SET_MS = 30_000L
private const val STEP_SETS = 4
private const val STEP_TOTAL_MS = STEP_SET_MS * STEP_SETS

/** 少于这个时长不记账：误触进来看一眼就退，不该污染累计。 */
private const val MIN_RECORD_SECONDS = 15

/** 训练页：按 id 分发到具体训练。 */
@Composable
fun DrillScreen(drillId: String, store: ProgressStore, onBack: () -> Unit) {
    when (drillId) {
        Drill.BREATH -> BreathDrill(store, onBack)
        Drill.STEP -> StepDrill(store, onBack)
        else -> onBack()
    }
}

/**
 * 计时引擎。
 *
 * 用 `System.nanoTime()` 累计而不是「每帧 +33ms」，因为 delay 的实际间隔会被调度抖动，
 * 累加固定值会在 5 分钟里偏出好几秒。这里按真实流逝时间累加，误差只来自最后一次 tick。
 */
private class DrillClock(val totalMs: Long) {
    var elapsed by mutableLongStateOf(0L)
    var running by mutableStateOf(false)
    var finished by mutableStateOf(false)

    fun start() {
        if (finished) reset()
        running = true
    }

    fun pause() {
        running = false
    }

    fun reset() {
        elapsed = 0L
        running = false
        finished = false
    }

    val progress: Float get() = if (totalMs <= 0) 0f else (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
}

@Composable
private fun rememberDrillClock(totalMs: Long): DrillClock {
    val clock = remember { DrillClock(totalMs) }
    LaunchedEffect(clock.running) {
        if (!clock.running) return@LaunchedEffect
        var last = System.nanoTime()
        while (clock.running) {
            delay(33)
            val now = System.nanoTime()
            clock.elapsed += (now - last) / 1_000_000
            last = now
            if (clock.elapsed >= clock.totalMs) {
                clock.elapsed = clock.totalMs
                clock.running = false
                clock.finished = true
            }
        }
    }
    return clock
}

// ---------------- 呼吸 ----------------

@Composable
private fun BreathDrill(store: ProgressStore, onBack: () -> Unit) {
    val clock = rememberDrillClock(BREATH_TOTAL_MS)
    var revision by remember { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(false) }

    // 完成时落库一次。放在 finished 的 LaunchedEffect 里，避免重组时重复写入。
    // 记**实际时长**而不是总时长：提前按「结束」只练了 40 秒，不该记成 5 分钟。
    // 不足 15 秒一律不记 —— 误触进来看一眼就退，不该污染累计。
    LaunchedEffect(clock.finished) {
        if (clock.finished) {
            val sec = (clock.elapsed / 1000).toInt()
            if (sec >= MIN_RECORD_SECONDS) {
                store.recordDrill(Drill.BREATH, sec)
                revision++
                saved = true
            }
        }
    }

    BackHandler { onBack() }

    val t = clock.elapsed % BREATH_CYCLE_MS
    val inhale = t < BREATH_IN_MS
    val phaseProgress = if (inhale) t.toFloat() / BREATH_IN_MS else (t - BREATH_IN_MS).toFloat() / BREATH_IN_MS
    // 余弦缓动：起止都慢，中间快，比线性更像真实呼吸
    val eased = ((1 - cos(phaseProgress * PI)) / 2).toFloat()
    val circleScale = if (inhale) 0.55f + 0.45f * eased else 1f - 0.45f * eased
    val phaseSecondsLeft = if (inhale) ((BREATH_IN_MS - t) / 1000) + 1 else ((BREATH_CYCLE_MS - t) / 1000) + 1
    val cycles = (clock.elapsed / BREATH_CYCLE_MS).toInt()

    DrillScaffold(
        title = stringResource(R.string.tr_item2_title),
        subtitle = stringResource(R.string.tr_item2_desc),
        onBack = onBack,
        clock = clock,
        stats = stringResource(
            R.string.drill_history,
            remember(revision) { store.drillCount(Drill.BREATH) },
            remember(revision) { (store.drillTotalSeconds(Drill.BREATH) + 30) / 60 }
        ),
        doneText = stringResource(R.string.drill_done_breath, cycles),
        saved = saved,
        centre = {
            // 呼吸圈：外圈固定轮廓，内圈随呼吸缩放
            Box(modifier = Modifier.size(232.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(Ascent.Accent.copy(alpha = 0.10f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(circleScale)
                        .background(Ascent.UGreen.copy(alpha = 0.30f), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            clock.finished -> stringResource(R.string.common_done)
                            clock.running -> stringResource(if (inhale) R.string.drill_inhale else R.string.drill_exhale)
                            else -> stringResource(R.string.common_start)
                        },
                        color = Ascent.Fg,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (clock.running) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = phaseSecondsLeft.toString(),
                            color = Ascent.Fg3,
                            fontSize = 14.sp,
                            fontFamily = MonoNumbers
                        )
                    }
                }
            }
        }
    )
}

// ---------------- 步伐节奏 ----------------

@Composable
private fun StepDrill(store: ProgressStore, onBack: () -> Unit) {
    val clock = rememberDrillClock(STEP_TOTAL_MS)
    var revision by remember { mutableIntStateOf(0) }
    // true = 德式（前齿），false = 法式（全脚掌）
    var front by remember { mutableStateOf(false) }

    val steps = (clock.elapsed / STEP_INTERVAL_MS).toInt()
    var saved by remember { mutableStateOf(false) }
    // 实际完成的组数：提前结束时按已走过的时长折算，不能一律报 4 组
    val setsDone = ((clock.elapsed + STEP_SET_MS - 1) / STEP_SET_MS).toInt().coerceIn(0, STEP_SETS)
    LaunchedEffect(clock.finished) {
        if (clock.finished) {
            val sec = (clock.elapsed / 1000).toInt()
            if (sec >= MIN_RECORD_SECONDS) {
                store.recordDrill(Drill.STEP, sec)
                revision++
                saved = true
            }
        }
    }

    BackHandler { onBack() }

    val left = steps % 2 == 0
    val beatProgress = (clock.elapsed % STEP_INTERVAL_MS).toFloat() / STEP_INTERVAL_MS
    // 每步收缩一下：踩下去的那一拍最容易看出节拍
    val pulse = 1f - 0.10f * (1f - beatProgress)
    val setIndex = ((clock.elapsed / STEP_SET_MS).toInt() + 1).coerceAtMost(STEP_SETS)

    DrillScaffold(
        title = stringResource(R.string.tr_item1_title),
        subtitle = stringResource(R.string.tr_item1_desc),
        onBack = onBack,
        clock = clock,
        stats = stringResource(
            R.string.drill_history,
            remember(revision) { store.drillCount(Drill.STEP) },
            remember(revision) { (store.drillTotalSeconds(Drill.STEP) + 30) / 60 }
        ),
        doneText = stringResource(R.string.drill_done_step, setsDone, steps),
        saved = saved,
        centre = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(232.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(pulse)
                        .background(Ascent.Accent.copy(alpha = 0.10f), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            clock.finished -> stringResource(R.string.common_done)
                            clock.elapsed == 0L -> stringResource(R.string.common_start)
                            else -> stringResource(if (left) R.string.drill_left else R.string.drill_right)
                        },
                        color = Ascent.Fg,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (clock.elapsed > 0 && !clock.finished) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.drill_set, setIndex, STEP_SETS),
                            color = Ascent.Fg3,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // 技法切换只影响脚下提示，不影响节拍 —— 节拍是共通的
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                TechChip(
                    text = stringResource(R.string.drill_tech_flat),
                    selected = !front,
                    onClick = { front = false }
                )
                TechChip(
                    text = stringResource(R.string.drill_tech_front),
                    selected = front,
                    onClick = { front = true }
                )
            }
            }
        }
    )
}

@Composable
private fun TechChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Ascent.UGreen.copy(alpha = 0.16f) else Ascent.Surface2)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Ascent.UGreen else Ascent.Fg3,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ---------------- 共用外壳 ----------------

@Composable
private fun DrillScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    clock: DrillClock,
    stats: String,
    doneText: String,
    saved: Boolean,
    centre: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ascent.Bg)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
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
        Spacer(Modifier.height(6.dp))
        Text(title, color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Ascent.Fg3, fontSize = 12.sp)

        Spacer(Modifier.weight(0.6f))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { centre() }
        Spacer(Modifier.weight(0.6f))

        // 进度条：一条细轨 + 已走部分，比数字更能看出还剩多久
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Ascent.Surface2)
                .border(1.dp, Ascent.DuoBorder, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clock.progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ascent.UGreen)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(formatClock(clock.elapsed), color = Ascent.Fg2, fontSize = 13.sp, fontFamily = MonoNumbers)
            Text(formatClock(clock.totalMs), color = Ascent.Fg3, fontSize = 13.sp, fontFamily = MonoNumbers)
        }
        Spacer(Modifier.height(10.dp))
        Text(stats, color = Ascent.Fg3, fontSize = 12.sp, textAlign = TextAlign.Start)

        Spacer(Modifier.height(16.dp))
        if (clock.finished) {
            Text(doneText, color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (saved) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.drill_recorded), color = Ascent.UGreen, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                PillButton(
                    text = stringResource(R.string.common_retry),
                    style = BtnStyle.Primary,
                    height = 46,
                    modifier = Modifier.weight(1f),
                    onClick = { clock.reset(); clock.start() }
                )
                PillButton(
                    text = stringResource(R.string.common_back),
                    style = BtnStyle.Secondary,
                    height = 46,
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                )
            }
        } else {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                PillButton(
                    text = if (clock.running) {
                        stringResource(R.string.drill_pause)
                    } else if (clock.elapsed > 0) {
                        stringResource(R.string.common_continue)
                    } else {
                        stringResource(R.string.common_start)
                    },
                    style = BtnStyle.Primary,
                    height = 46,
                    modifier = Modifier.weight(1f),
                    onClick = { if (clock.running) clock.pause() else clock.start() }
                )
                if (clock.elapsed > 0) {
                    PillButton(
                        text = stringResource(R.string.drill_finish),
                        style = BtnStyle.Secondary,
                        height = 46,
                        modifier = Modifier.weight(1f),
                        onClick = { clock.pause(); clock.finished = true }
                    )
                }
            }
        }
    }
}

/** mm:ss。 */
private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
