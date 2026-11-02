package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.QType
import com.ascent.climb.data.Question
import kotlinx.coroutines.delay
import android.app.Activity
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
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

    /** 装备清单默写（题库取题）。 */
    const val GEAR = "gear"

    /** 天气判读 · 看图识云（题库取题）。 */
    const val WEATHER = "weather"
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
fun DrillScreen(
    drillId: String,
    store: ProgressStore,
    onBack: () -> Unit,
    devShowAnswers: Boolean = false
) {
    // 训练期间不熄屏：呼吸 5 分钟、默写一组 3 分钟，中途黑屏会打断节奏，
    // 而且这种「盯着看」的场景用户通常不会主动触碰屏幕。
    KeepScreenOn()

    when (drillId) {
        Drill.BREATH -> BreathDrill(store, onBack)
        Drill.STEP -> StepDrill(store, onBack)
        Drill.GEAR -> QuizDrill(
            store = store,
            drillId = Drill.GEAR,
            titleRes = R.string.tr_item3_title,
            subtitleRes = R.string.tr_item3_desc,
            lessonIds = GEAR_LESSONS,
            onBack = onBack,
            devShowAnswers = devShowAnswers
        )
        Drill.WEATHER -> QuizDrill(
            store = store,
            drillId = Drill.WEATHER,
            titleRes = R.string.tr_item4_title,
            subtitleRes = R.string.tr_item4_desc,
            lessonIds = WEATHER_LESSONS,
            onBack = onBack,
            devShowAnswers = devShowAnswers
        )
        else -> onBack()
    }
}

/**
 * 训练期间保持屏幕常亮。
 *
 * 用窗口 flag（FLAG_KEEP_SCREEN_ON）而不是 `view.keepScreenOn`：前者不依赖
 * 当前 View 是否可见，退出组合时精确清掉，也不会和别处对同一 flag 的用法打架。
 * 离开训练页（onBack / 返回手势）时组合销毁，onDispose 把 flag 撤掉。
 */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/** Compose 的 view.context 在部分场景下是 ContextWrapper 而非 Activity，逐层解开。 */
private fun android.content.Context.findActivity(): Activity? {
    var c: android.content.Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
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
        DrillTopBar(title, subtitle, onBack)

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

/** 页头：返回 + 标题 + 副标题（计时训练与题库训练共用）。 */
@Composable
private fun DrillTopBar(title: String, subtitle: String, onBack: () -> Unit) {
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
}

// ---------------- 题库型训练：装备默写 / 天气判读 ----------------
//
// 这两个训练不新增任何题目内容，而是从现有课的**单选**里随机抽背：
// 题干、选项、解析、课程名都已经有十种语言的译文，训练页不会在某个语言下退回中文。
// 代价是以后加了装备 / 气象类的新课，要把课 id 补进下面两个集合（见 HANDOFF）。

/** 装备类取题课：攀登的器材 / 穿着 / 背负 / 睡眠 / 急救包 + 徒步的鞋袜 / 背包 / 维护。 */
private val GEAR_LESSONS = setOf(
    "l2", "l21", "l3", "l23", "l24", "l22", "l30",
    "t02", "t03", "t20"
)

/** 天气类取题课：天气与导航、山地气象、云图判读、徒步天气应对与雷暴，外加两节识云课。 */
private val WEATHER_LESSONS = setOf(
    "m04", "m10", "m15", "m26",
    "t08", "t21", "t22"
)

/** 一组题数。10 题约 3 分钟，再长就变成刷题而不是训练。 */
private const val QUIZ_SIZE = 10

/** 选项字母。超过 6 个选项的题在题库里没有，但留好兜底。 */
private const val OPTION_LETTERS = "ABCDEFGH"

/** 一道待答题：原题 + 打乱后的选项顺序（元素是原 options 的下标）。 */
private data class QuizItem(
    val q: Question,
    /** 显示顺序 → 原下标。答对判定要走这一层，不能直接比显示位置。 */
    val order: List<Int>,
    val lessonId: String,
    val lessonOriginal: String
)

/**
 * 从指定课里随机抽一组单选。
 *
 * 只取单选：多选 / 排序 / 热点在训练页要各自一套交互，而这里的目标是
 * 「快速回忆 + 立刻看解析」，单选够用，题量也稳定。
 * 选项每次重排 —— 否则第二组开始记的就是位置而不是内容。
 */
private fun buildQuiz(lessonIds: Set<String>, size: Int): List<QuizItem> {
    val pool = ALL_UNITS.asSequence()
        .flatMap { it.lessons.asSequence() }
        .filter { it.id in lessonIds }
        .flatMap { l ->
            l.questions.asSequence()
                // 复习副本（review = true）是别的课的题，且与原题 id 重复：
                // 不排除的话训练池会被掺进无关题，同一道题还可能在一组里出现两次
                .filter { !it.review }
                .filter { it.type == QType.Single && it.correct in it.options.indices }
                .map { Triple(l.id, l.title, it) }
        }
        .toList()
    if (pool.isEmpty()) return emptyList()
    return pool.shuffled().take(size).map { (lid, ltitle, q) ->
        QuizItem(q, q.options.indices.shuffled(), lid, ltitle)
    }
}

/** 一组题的状态。与 [DrillClock] 同思路：可变状态收进一个对象，不散成一堆 remember。 */
private class QuizRun(val items: List<QuizItem>) {
    var index by mutableIntStateOf(0)

    /** 已选的**显示序号**；-1 表示本题还没作答。 */
    var picked by mutableIntStateOf(-1)
    var correct by mutableIntStateOf(0)
    var done by mutableStateOf(false)
    private val startedAt = System.nanoTime()

    val total: Int get() = items.size
    val current: QuizItem get() = items[index]
    val answered: Boolean get() = picked >= 0
    val isLast: Boolean get() = index + 1 >= items.size

    fun pick(pos: Int) {
        if (answered || pos !in current.order.indices) return
        picked = pos
        if (current.order[pos] == current.q.correct) correct++
    }

    fun next() {
        if (isLast) done = true else { index++; picked = -1 }
    }

    fun seconds(): Int = ((System.nanoTime() - startedAt) / 1_000_000_000L).toInt().coerceAtLeast(0)
}

@Composable
private fun QuizDrill(
    store: ProgressStore,
    drillId: String,
    titleRes: Int,
    subtitleRes: Int,
    lessonIds: Set<String>,
    onBack: () -> Unit,
    devShowAnswers: Boolean = false
) {
    var revision by remember { mutableIntStateOf(0) }
    var run by remember { mutableStateOf<QuizRun?>(null) }
    var saved by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    val title = stringResource(titleRes)
    val subtitle = stringResource(subtitleRes)
    val poolSize = remember(lessonIds) { buildQuiz(lessonIds, Int.MAX_VALUE).size }

    // 收尾落库：一组记一次，存实际用时与答对数。
    // 不足 15 秒说明是进来点了两下就退，不记账（与计时训练同一口径）。
    LaunchedEffect(run, run?.done) {
        val r = run ?: return@LaunchedEffect
        if (!r.done) return@LaunchedEffect
        val sec = r.seconds()
        if (sec >= MIN_RECORD_SECONDS) {
            store.recordDrillScore(drillId, sec, r.correct, r.total)
            revision++
            saved = true
        }
    }

    val r = run
    val attempts = remember(revision) { store.drillCount(drillId) }
    val minutes = remember(revision) { (store.drillTotalSeconds(drillId) + 30) / 60 }
    val best = remember(revision) { store.drillBest(drillId) }

    when {
        r == null -> QuizIntro(
            title = title,
            subtitle = subtitle,
            poolSize = poolSize,
            count = minOf(QUIZ_SIZE, poolSize),
            attempts = attempts,
            minutes = minutes,
            best = best,
            onBack = onBack,
            onStart = {
                // 题库里没有可用单选时保持原样（不构造空的一组，否则读 items[0] 会崩）
                val items = buildQuiz(lessonIds, QUIZ_SIZE)
                if (items.isNotEmpty()) run = QuizRun(items)
            }
        )
        !r.done -> QuizPlay(run = r, title = title, onBack = onBack, devShowAnswers = devShowAnswers)
        else -> QuizDone(
            run = r,
            title = title,
            best = best,
            saved = saved,
            attempts = attempts,
            onBack = onBack,
            onAgain = {
                val items = buildQuiz(lessonIds, QUIZ_SIZE)
                if (items.isNotEmpty()) {
                    saved = false
                    run = QuizRun(items)
                }
            }
        )
    }
}

@Composable
private fun QuizIntro(
    title: String,
    subtitle: String,
    poolSize: Int,
    count: Int,
    attempts: Int,
    minutes: Int,
    best: Int,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ascent.Bg)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
        DrillTopBar(title, subtitle, onBack)

        Spacer(Modifier.weight(0.4f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(Ic.Brain, Ascent.Accent, Ascent.AccentSoft, diameter = 64, iconSize = 30)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.drill_quiz_intro),
                color = Ascent.Fg2,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftTag(stringResource(R.string.drill_quiz_pool, poolSize, count), Ascent.Fg2, Ascent.Surface2)
                if (attempts > 0) {
                    SoftTag(stringResource(R.string.drill_quiz_best, best), Ascent.UGreen, Ascent.SuccessSoft)
                }
            }
            if (attempts > 0) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.drill_history, attempts, minutes), color = Ascent.Fg3, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.weight(0.6f))

        PillButton(
            text = stringResource(R.string.drill_quiz_start, count),
            icon = Ic.Play,
            onClick = onStart
        )
    }
}

/** 作答页。答完一题立刻出解析，不攒到最后一题才给反馈。 */
@Composable
private fun QuizPlay(
    run: QuizRun,
    title: String,
    onBack: () -> Unit,
    devShowAnswers: Boolean = false
) {
    val item = run.current
    val q = item.q
    val answered = run.answered
    val right = answered && item.order[run.picked] == q.correct
    val scroll = rememberScrollState()
    // 题图放大：识云要看细节（云的边缘、砧状、袋状），小图常常分不清
    var viewer by remember { mutableStateOf<String?>(null) }
    viewer?.let { ImageViewerDialog(image = it, onClose = { viewer = null }) }

    // 答完滚到解析卡顶部：解析在选项下面，长选项时一屏放不下。
    // 不能直接滚到 maxValue —— 有题图时那会把图整块滚出屏幕，
    // 而判读题恰恰需要对照图片看解析。滚到解析卡顶端两头都能兼顾。
    var explainTop by remember { mutableStateOf(0f) }
    LaunchedEffect(run.picked) {
        if (run.picked >= 0) {
            delay(90)
            val target = if (explainTop > 0f) {
                minOf(scroll.maxValue, (explainTop - 8f).toInt().coerceAtLeast(0))
            } else {
                scroll.maxValue
            }
            scroll.animateScrollTo(target)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ascent.Bg)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
        // 作答页用紧凑页头：进度比标题更有用，标题留一组的小字
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.drill_quiz_progress, run.index + 1, run.total),
                color = Ascent.Fg3,
                fontSize = 12.sp,
                fontFamily = MonoNumbers
            )
        }
        Spacer(Modifier.height(6.dp))
        // 进度轨：已答题数，不含当前题的猜测结果
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Ascent.Surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((run.index + if (answered) 1 else 0).toFloat() / run.total)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ascent.UGreen)
            )
        }
        Text(
            text = title,
            color = Ascent.Fg3,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
        ) {
            Spacer(Modifier.height(16.dp))
            // 题图：识云题必须看图，按图片真实宽高比铺满，点开可放大
            if (!q.image.isNullOrBlank()) {
                val bmp = rememberAssetBitmap(q.image)
                val ratio = bmp?.let { it.width.toFloat() / it.height.toFloat() } ?: 1.5f
                // 宽高比差别很大：云图是 3:2 的横图，气象截图是接近 1:2 的竖图。
                // 统一按宽度铺满会让竖图撑满整屏、把题干和选项全推到屏幕外，
                // 所以横图铺满宽度、竖图按高度封顶后居中，两边都不裁切。
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cap = 320.dp
                    val rawHeight = maxWidth / ratio
                    val boxWidth = if (rawHeight > cap) cap * ratio else maxWidth
                    val boxHeight = if (rawHeight > cap) cap else rawHeight
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(boxWidth)
                            .height(boxHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Ascent.Surface2)
                            .border(1.dp, Ascent.Border, RoundedCornerShape(14.dp))
                            .clickable { viewer = q.image }
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.lesson_img_missing, q.image.orEmpty()),
                                    color = Ascent.Fg3,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = qPrompt(q),
                color = Ascent.Fg,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item.order.forEachIndexed { pos, orig ->
                    val state = when {
                        !answered -> OptState.Idle
                        orig == q.correct -> OptState.Correct
                        pos == run.picked -> OptState.Wrong
                        else -> OptState.Muted
                    }
                    QuizOptionRow(
                        letter = OPTION_LETTERS.getOrElse(pos) { ' ' }.toString(),
                        text = qOption(q, orig),
                        state = state,
                        // 开发者模式：作答前把正确项标出来，答完就不再标（那时本来也会高亮）
                        showKey = devShowAnswers && !answered && orig == q.correct,
                        onClick = { run.pick(pos) }
                    )
                }
            }

            if (answered) {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.onGloballyPositioned { explainTop = it.positionInParent().y }) {
                OutlineCard(
                    background = if (right) Ascent.SuccessSoft else Ascent.DangerSoft,
                    borderColor = if (right) Ascent.Success else Ascent.Danger,
                    gap = 8
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (right) Ic.Check else Ic.CircleX,
                            contentDescription = null,
                            tint = if (right) Ascent.Success else Ascent.Danger,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(if (right) R.string.drill_quiz_right else R.string.drill_quiz_wrong),
                            color = if (right) Ascent.Success else Ascent.Danger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    MonoLabel(stringResource(R.string.drill_quiz_explain))
                    Text(qExplanation(q), color = Ascent.Fg, fontSize = 13.sp, lineHeight = 21.sp)
                    MonoLabel(
                        stringResource(R.string.drill_quiz_from, lessonTitle(item.lessonId, item.lessonOriginal))
                    )
                }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 未作答时留出同样的高度，避免按钮出现时页面跳一下
        Box(modifier = Modifier.fillMaxWidth().height(46.dp)) {
            if (answered) {
                PillButton(
                    text = stringResource(if (run.isLast) R.string.drill_quiz_see else R.string.drill_quiz_next),
                    icon = if (run.isLast) Ic.CircleCheck else Ic.ArrowRight,
                    height = 46,
                    onClick = { run.next() }
                )
            }
        }
    }
}

private enum class OptState { Idle, Correct, Wrong, Muted }

@Composable
private fun QuizOptionRow(
    letter: String,
    text: String,
    state: OptState,
    showKey: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = when (state) {
        OptState.Correct -> Ascent.SuccessSoft
        OptState.Wrong -> Ascent.DangerSoft
        else -> Ascent.Surface
    }
    val border = when (state) {
        OptState.Correct -> Ascent.Success
        OptState.Wrong -> Ascent.Danger
        else -> Ascent.Border
    }
    val accent = when (state) {
        OptState.Correct -> Ascent.Success
        OptState.Wrong -> Ascent.Danger
        else -> Ascent.Fg3
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(enabled = state == OptState.Idle) { onClick() }
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(999.dp))
                // 对错时行底色本身就是淡绿 / 淡红，字母再铺同色会糊掉，改用卡面白
                .background(if (state == OptState.Correct || state == OptState.Wrong) Ascent.Surface else Ascent.Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = MonoNumbers)
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = text,
            color = if (state == OptState.Muted) Ascent.Fg3 else Ascent.Fg,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
        when (state) {
            OptState.Correct -> {
                Spacer(Modifier.width(8.dp))
                Icon(Ic.Check, contentDescription = null, tint = Ascent.Success, modifier = Modifier.size(16.dp))
            }
            OptState.Wrong -> {
                Spacer(Modifier.width(8.dp))
                Icon(Ic.CircleX, contentDescription = null, tint = Ascent.Danger, modifier = Modifier.size(16.dp))
            }
            // 与课程测验里的开发者标记同一套写法（绿色的 KEY 小字），不另造视觉
            OptState.Idle -> if (showKey) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "KEY",
                    color = Ascent.Success,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun QuizDone(
    run: QuizRun,
    title: String,
    best: Int,
    saved: Boolean,
    attempts: Int,
    onBack: () -> Unit,
    onAgain: () -> Unit
) {
    val pct = if (run.total > 0) run.correct * 100 / run.total else 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ascent.Bg)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
        DrillTopBar(
            title = stringResource(R.string.common_done),
            subtitle = title,
            onBack = onBack
        )

        Spacer(Modifier.weight(0.4f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$pct%",
                color = Ascent.Fg,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoNumbers
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.drill_quiz_score, run.correct, run.total), color = Ascent.Fg3, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 百分比已经是大字，这里只留「最好成绩」与「练过几次」两条
                if (best > 0) SoftTag(stringResource(R.string.drill_quiz_best, best), Ascent.UGreen, Ascent.SuccessSoft)
                if (attempts > 0) SoftTag(stringResource(R.string.drill_history_short, attempts), Ascent.Fg2, Ascent.Surface2)
            }
            if (saved) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.drill_recorded), color = Ascent.UGreen, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.weight(0.6f))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                text = stringResource(R.string.drill_quiz_again),
                style = BtnStyle.Primary,
                height = 46,
                modifier = Modifier.weight(1f),
                onClick = onAgain
            )
            PillButton(
                text = stringResource(R.string.common_back),
                style = BtnStyle.Secondary,
                height = 46,
                modifier = Modifier.weight(1f),
                onClick = onBack
            )
        }
    }
}

/** mm:ss。 */
private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
