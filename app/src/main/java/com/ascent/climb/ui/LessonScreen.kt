package com.ascent.climb.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascent.climb.data.Lesson
import com.ascent.climb.data.QType
import com.ascent.climb.data.Question

private enum class Phase { Brief, Quiz, Retry, Done }

/** 归一化填空答案：忽略大小写、空白与常见单位符号。 */
private fun norm(s: String): String =
    s.lowercase().replace(Regex("[\\s，,。.、；;：:℃°度%]"), "")

/** 计算题判定：按容差比较，并容忍浮点误差。 */
private fun calcMatches(input: String, q: Question): Boolean {
    val v = input.trim().toDoubleOrNull() ?: return false
    return q.answers.any { a ->
        val t = a.toDoubleOrNull() ?: return@any false
        kotlin.math.abs(v - t) <= q.tolerance + 1e-6
    }
}

@Composable
fun LessonScreen(
    lesson: Lesson,
    /** 首次通关可获得的海拔；重做同一课时传 0 */
    altitudeAward: Int = 0,
    devShowAnswers: Boolean = false,
    onClose: () -> Unit,
    onFinished: (stars: Int, wrongCount: Int, wrongKeys: List<String>) -> Unit
) {
    var phase by remember(lesson.id) { mutableStateOf(Phase.Brief) }
    var qIndex by remember(lesson.id) { mutableStateOf(0) }
    var graded by remember(lesson.id) { mutableStateOf(false) }
    var lastCorrect by remember(lesson.id) { mutableStateOf(false) }
    var correctCount by remember(lesson.id) { mutableStateOf(0) }
    var retryPos by remember(lesson.id) { mutableStateOf(0) }
    var retryResolved by remember(lesson.id) { mutableStateOf(0) }

    // 各题型的作答状态
    var pickIndex by remember(lesson.id) { mutableStateOf<Int?>(null) }
    val pickSet = remember(lesson.id) { mutableStateListOf<Int>() }
    val orderState = remember(lesson.id) { mutableStateListOf<Int>() }
    var fillText by remember(lesson.id) { mutableStateOf("") }
    val matchAssign = remember(lesson.id) { mutableStateListOf(-1, -1, -1, -1, -1, -1) }
    var matchSel by remember(lesson.id) { mutableStateOf<Int?>(null) }
    var calcText by remember(lesson.id) { mutableStateOf("") }
    var hotspotTap by remember(lesson.id) { mutableStateOf<Offset?>(null) }
    var hotspotHit by remember(lesson.id) { mutableStateOf(-1) }
    // 情景题
    var scenarioStep by remember(lesson.id) { mutableStateOf(0) }
    var scenarioAllOk by remember(lesson.id) { mutableStateOf(true) }
    var stepOk by remember(lesson.id) { mutableStateOf(false) }

    // 图片查看器（查看大图 / 双指缩放）：气象截图、等高线等需看图辨识的题目
    var viewerImage by remember(lesson.id) { mutableStateOf<String?>(null) }
    viewerImage?.let { name ->
        ImageViewerDialog(image = name, onClose = { viewerImage = null })
    }

    val firstWrong = remember(lesson.id) { mutableStateListOf<Int>() }
    val stillWrong = remember(lesson.id) { mutableStateListOf<Int>() }

    val total = lesson.questions.size
    val retryQueue = firstWrong
    val denom = total + retryQueue.size
    val isRetry = phase == Phase.Retry
    val activeIndex = if (isRetry) retryQueue.getOrNull(retryPos) else qIndex
    val question = activeIndex?.let { lesson.questions.getOrNull(it) }

    fun resetAnswer(q: Question?) {
        pickIndex = null
        pickSet.clear()
        orderState.clear()
        fillText = ""
        matchAssign.clear()
        repeat(6) { matchAssign.add(-1) }
        matchSel = null
        calcText = ""
        hotspotTap = null
        hotspotHit = -1
        scenarioStep = 0
        scenarioAllOk = true
        stepOk = false
        graded = false
        if (q != null && q.type == QType.Order) {
            orderState.addAll(shuffledOrderStart(q))
        }
    }

    /** 判断当前作答是否正确。 */
    fun evaluate(q: Question): Boolean = when (q.type) {
        QType.Single -> pickIndex == q.correct
        QType.Multi -> pickSet.toSet() == q.correctSet.toSet()
        QType.Order -> orderState.toList() == q.correctOrder
        QType.Fill -> q.answers.any { norm(it) == norm(fillText) }
        QType.Match -> q.pairsLeft.indices.all { matchAssign.getOrElse(it) { -1 } == it }
        QType.Calc -> calcMatches(calcText, q)
        QType.Hotspot -> hotspotHit >= 0 && q.hotspots.getOrNull(hotspotHit)?.correct == true
        QType.Scenario -> false // 情景题按步判定，见 evaluateStep
    }

    /** 按题型的作答进度（用于控制“检查”按钮可用性）。 */
    fun answered(q: Question): Boolean = when (q.type) {
        QType.Single -> pickIndex != null
        QType.Multi -> pickSet.isNotEmpty()
        QType.Order -> true
        QType.Fill -> fillText.isNotBlank()
        QType.Match -> q.pairsLeft.indices.all { matchAssign.getOrElse(it) { -1 } >= 0 }
        QType.Calc -> calcText.isNotBlank()
        QType.Hotspot -> hotspotHit >= 0
        QType.Scenario -> {
            val s = q.steps.getOrNull(scenarioStep)
            when (s?.type) {
                QType.Multi -> pickSet.isNotEmpty()
                QType.Single -> pickIndex != null
                else -> false
            }
        }
    }

    /** 情景题：判定当前这一步。 */
    fun evaluateStep(s: Question): Boolean = when (s.type) {
        QType.Multi -> pickSet.toSet() == s.correctSet.toSet()
        else -> pickIndex == s.correct
    }

    /** 记录整题结果（情景题在最后一步完成后调用）。 */
    fun recordResult(ok: Boolean) {
        val idx = activeIndex ?: 0
        lastCorrect = ok
        if (isRetry) {
            if (ok) retryResolved += 1 else stillWrong.add(idx)
        } else {
            if (ok) correctCount += 1 else firstWrong.add(idx)
        }
    }

    /** 推进到下一题 / 进入错题重做 / 结束。 */
    fun advance() {
        if (isRetry) {
            if (retryPos + 1 >= retryQueue.size) phase = Phase.Done
            else {
                retryPos += 1
                resetAnswer(lesson.questions.getOrNull(retryQueue.getOrNull(retryPos) ?: 0))
            }
        } else {
            if (qIndex + 1 >= total) {
                if (firstWrong.isEmpty()) phase = Phase.Done
                else {
                    phase = Phase.Retry
                    retryPos = 0
                    resetAnswer(lesson.questions.getOrNull(retryQueue.firstOrNull() ?: 0))
                }
            } else {
                qIndex += 1
                resetAnswer(lesson.questions.getOrNull(qIndex))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {

        // ---------- 顶部进度 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Ic.X, contentDescription = "关闭", tint = Ascent.Fg2, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            val frac = when (phase) {
                Phase.Brief -> 0f
                Phase.Done -> 1f
                Phase.Quiz -> if (denom == 0) 0f else qIndex.toFloat() / denom
                Phase.Retry -> if (denom == 0) 1f else (total + retryPos).toFloat() / denom
            }
            AscentBar(
                fraction = frac,
                modifier = Modifier.weight(1f),
                height = 8,
                fill = if (isRetry) Ascent.Warning else Ascent.Accent
            )
            Spacer(Modifier.width(14.dp))
            if (altitudeAward > 0) {
                Text(
                    "+${altitudeAward}m",
                    color = Ascent.Warning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MonoNumbers
                )
            } else {
                Text("重做", color = Ascent.Fg3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ---------- 主体 ----------
        Box(modifier = Modifier.weight(1f)) {
            when (phase) {
                Phase.Brief -> BriefView(lesson) {
                    phase = Phase.Quiz
                    resetAnswer(lesson.questions.firstOrNull())
                }

                Phase.Quiz, Phase.Retry -> question?.let { q ->
                    QuizView(
                        stepLabel = if (isRetry) "错题重做 ${retryPos + 1} / ${retryQueue.size}" else "${qIndex + 1} / $total",
                        isRetry = isRetry,
                        q = q,
                        graded = graded,
                        showAnswerKey = devShowAnswers,
                        pickIndex = pickIndex,
                        pickSet = pickSet,
                        orderState = orderState,
                        fillText = fillText,
                        calcText = calcText,
                        hotspotTap = hotspotTap,
                        hotspotHit = hotspotHit,
                        scenarioStep = scenarioStep,
                        scenarioAllOk = scenarioAllOk,
                        stepOk = stepOk,
                        onOpenImage = { viewerImage = it },
                        onCalc = { if (!graded) calcText = it },
                        onHotspotTap = { nx, ny, hit ->
                            if (!graded) {
                                hotspotTap = Offset(nx, ny)
                                hotspotHit = hit
                            }
                        },
                        matchAssign = matchAssign,
                        matchSel = matchSel,
                        onPick = { i -> if (!graded) pickIndex = i },
                        onTogglePick = { i ->
                            if (!graded) {
                                if (i in pickSet) pickSet.remove(i) else pickSet.add(i)
                            }
                        },
                        onMove = { from, delta ->
                            if (!graded) {
                                val to = from + delta
                                if (to in orderState.indices) {
                                    val v = orderState[from]
                                    orderState[from] = orderState[to]
                                    orderState[to] = v
                                }
                            }
                        },
                        onFill = { if (!graded) fillText = it },
                        onMatchSelect = { i -> if (!graded) matchSel = i },
                        onMatchAssign = { rightIdx ->
                            if (!graded) {
                                val sel = matchSel
                                if (sel != null) {
                                    // 已被占用的右侧项先释放
                                    for (k in matchAssign.indices) {
                                        if (k != sel && matchAssign[k] == rightIdx) matchAssign[k] = -1
                                    }
                                    if (sel < matchAssign.size) matchAssign[sel] = rightIdx
                                    matchSel = null
                                }
                            }
                        }
                    )
                }

                Phase.Done -> DoneView(
                    lesson = lesson,
                    gained = altitudeAward,
                    stars = stars(correctCount, total),
                    correct = correctCount,
                    total = total,
                    retried = firstWrong.size,
                    resolved = retryResolved,
                    remaining = stillWrong.size,
                    onContinue = {
                        onFinished(
                            stars(correctCount, total),
                            stillWrong.size,
                            stillWrong.map { "${lesson.id}#$it" }
                        )
                    }
                )
            }
        }

        // ---------- 底部操作 ----------
        if ((phase == Phase.Quiz || phase == Phase.Retry) && question != null) {
            val q = question
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ascent.Bg)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (graded && q.type != QType.Scenario) {
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(shape)
                            .background(if (lastCorrect) Ascent.SuccessSoft else Ascent.DangerSoft)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (lastCorrect) Ic.CircleCheck else Ic.CircleX,
                                contentDescription = null,
                                tint = if (lastCorrect) Ascent.Success else Ascent.Danger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    lastCorrect && isRetry -> "订正成功"
                                    lastCorrect -> "正确"
                                    q.safetyCritical -> "安全关键题答错 — 必须掌握"
                                    else -> "再想想"
                                },
                                color = if (lastCorrect) Ascent.Success else Ascent.Danger,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = q.explanation,
                            color = Ascent.Fg,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        if (!q.source.isNullOrBlank()) {
                            Spacer(Modifier.height(9.dp))
                            Text(
                                text = "来源 · ${q.source}",
                                color = Ascent.Fg3,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                PillButton(
                    text = when {
                        !graded -> if (isRetry) "重做检查" else "检查"
                        q.type == QType.Scenario ->
                            if (scenarioStep + 1 >= q.steps.size) "完成" else "下一步"
                        lastCorrect -> "继续"
                        else -> "理解了，继续"
                    },
                    style = if (q.type != QType.Scenario && graded && !lastCorrect && q.safetyCritical)
                        BtnStyle.Danger else BtnStyle.Primary,
                    icon = if (q.type == QType.Scenario) null else Ic.ArrowRight,
                    enabled = answered(q),
                    onClick = {
                        if (q.type == QType.Scenario) {
                            if (!graded) {
                                graded = true
                                val s = q.steps.getOrNull(scenarioStep)
                                val ok = s != null && evaluateStep(s)
                                stepOk = ok
                                if (!ok) scenarioAllOk = false
                            } else if (scenarioStep + 1 >= q.steps.size) {
                                graded = false
                                recordResult(scenarioAllOk)
                                advance()
                            } else {
                                scenarioStep += 1
                                graded = false
                                pickIndex = null
                                pickSet.clear()
                            }
                        } else {
                            if (!graded) {
                                graded = true
                                recordResult(evaluate(q))
                            } else {
                                advance()
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun stars(correct: Int, total: Int): Int = when {
    total == 0 -> 3
    correct >= total -> 3
    correct * 10 >= total * 6 -> 2
    else -> 1
}

/** 题型徽标：单选用「单选」，其余用带图标的软标签。 */
@Composable
private fun TypeBadge(q: Question) {
    val pair: Pair<String, ImageVector> = when (q.type) {
        QType.Single -> "单选" to Ic.CircleDot
        QType.Multi -> "多选 · 选出所有正确项" to Ic.CircleCheck
        QType.Order -> "排序 · 调整顺序" to Ic.SlidersHorizontal
        QType.Match -> "配对 · 先左后右" to Ic.Link
        QType.Fill -> "填空 · 关键数据" to Ic.Book
        QType.Calc -> "计算 · 输入数值" to Ic.Ruler
        QType.Hotspot -> "热点 · 在图上点出位置" to Ic.MapPin
        QType.Scenario -> "情景 · 共 ${q.steps.size} 步" to Ic.Brain
    }
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftTag(pair.first, Ascent.Accent, Ascent.AccentSoft, icon = pair.second)
        if (q.safetyCritical) {
            SoftTag("安全关键", Ascent.Warning, Ascent.WarningSoft, icon = Ic.ShieldAlert)
        }
    }
}

@Composable
private fun QuizView(
    stepLabel: String,
    isRetry: Boolean,
    q: Question,
    graded: Boolean,
    showAnswerKey: Boolean,
    pickIndex: Int?,
    pickSet: List<Int>,
    orderState: List<Int>,
    fillText: String,
    calcText: String,
    hotspotTap: Offset?,
    hotspotHit: Int,
    scenarioStep: Int,
    scenarioAllOk: Boolean,
    stepOk: Boolean,
    matchAssign: List<Int>,
    matchSel: Int?,
    onPick: (Int) -> Unit,
    onTogglePick: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onFill: (String) -> Unit,
    onCalc: (String) -> Unit,
    onHotspotTap: (Float, Float, Int) -> Unit,
    onOpenImage: (String) -> Unit,
    onMatchSelect: (Int) -> Unit,
    onMatchAssign: (Int) -> Unit
) {
    val letters = listOf("A", "B", "C", "D", "E", "F")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MonoLabel(stepLabel)
            if (isRetry) {
                SoftTag("错题重做", Ascent.Warning, Ascent.WarningSoft, icon = Ic.RotateCcw)
            }
        }
        Spacer(Modifier.height(12.dp))

        TypeBadge(q)

        Text(q.prompt, color = Ascent.Fg, fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        if (!q.context.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(q.context, color = Ascent.Fg2, fontSize = 13.sp, lineHeight = 20.sp)
        }

        // 非热点题如果配了图，在这里展示（热点题自带可点击图）
        if (!q.image.isNullOrBlank() && q.type != QType.Hotspot) {
            Spacer(Modifier.height(14.dp))
            val imgBmp = rememberAssetBitmap(q.image)
            val imgRatio = imgBmp?.let { it.width.toFloat() / it.height.toFloat() } ?: (4f / 3f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imgRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ascent.Surface2)
                    .border(1.dp, Ascent.Border, RoundedCornerShape(16.dp))
                    .clickable { onOpenImage(q.image) }
            ) {
                if (imgBmp != null) {
                    Image(
                        bitmap = imgBmp,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("题图缺失：${q.image}", color = Ascent.Fg3, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.clickable { onOpenImage(q.image) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.Maximize2, contentDescription = null, tint = Ascent.Accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("点击图片可查看大图（支持双指缩放）", color = Ascent.Fg3, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(18.dp))

        when (q.type) {
            QType.Single -> shuffledSingleDisplay(q).forEachIndexed { pos, i ->
                val isSel = pickIndex == i
                OptionRow(
                    key = letters.getOrElse(pos) { "?" },
                    text = q.options[i],
                    selected = isSel,
                    correct = graded && i == q.correct,
                    wrong = graded && isSel && i != q.correct,
                    showKey = showAnswerKey && !graded && i == q.correct,
                    onClick = { onPick(i) }
                )
            }

            QType.Multi -> q.options.forEachIndexed { i, opt ->
                val isSel = i in pickSet
                val isRight = i in q.correctSet
                OptionRow(
                    key = if (isSel) "\u2713" else letters.getOrElse(i) { "?" },
                    text = opt,
                    selected = isSel,
                    correct = graded && isRight,
                    wrong = graded && isSel && !isRight,
                    showKey = showAnswerKey && !graded && isRight,
                    onClick = { onTogglePick(i) }
                )
            }

            QType.Order -> orderState.forEachIndexed { pos, optIdx ->
                val correctHere = graded && q.correctOrder.getOrNull(pos) == optIdx
                val shape = RoundedCornerShape(14.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(shape)
                        .background(
                            when {
                                graded && correctHere -> Ascent.SuccessSoft
                                graded -> Ascent.DangerSoft
                                else -> Ascent.Surface
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                graded && correctHere -> Ascent.Success
                                graded -> Ascent.Danger
                                else -> Ascent.Border
                            },
                            shape
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Ascent.Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${pos + 1}", color = Ascent.Fg2, fontSize = 12.sp, fontFamily = MonoNumbers, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(q.options[optIdx], color = Ascent.Fg, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                    if (!graded) {
                        Column {
                            Box(
                                modifier = Modifier.size(30.dp).clickable { onMove(pos, -1) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Ic.ChevronUp, contentDescription = "上移", tint = Ascent.Accent, modifier = Modifier.size(18.dp)) }
                            Box(
                                modifier = Modifier.size(30.dp).clickable { onMove(pos, 1) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Ic.ChevronDown, contentDescription = "下移", tint = Ascent.Accent, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }

            QType.Fill -> {
                OutlinedTextField(
                    value = fillText,
                    onValueChange = onFill,
                    enabled = !graded,
                    singleLine = true,
                    placeholder = { Text("在此填写答案", fontSize = 14.sp) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (graded) {
                    Spacer(Modifier.height(12.dp))
                    AnswerLine("参考答案：${q.answers.joinToString(" / ")}")
                }
            }

            QType.Match -> {
                // 右侧备选：打乱顺序，避免与左侧天然对齐
                val rightOrder = remember(q.prompt) {
                    val n = q.pairsRight.size
                    (0 until n).sortedBy { (it * 7 + 3) % n }
                }
                q.pairsLeft.forEachIndexed { i, left ->
                    val assigned = matchAssign.getOrElse(i) { -1 }
                    val isRight = graded && assigned == i
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 9.dp)
                            .clip(shape)
                            .background(
                                when {
                                    graded && isRight -> Ascent.SuccessSoft
                                    graded -> Ascent.DangerSoft
                                    matchSel == i -> Ascent.AccentSoft
                                    else -> Ascent.Surface
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    graded && isRight -> Ascent.Success
                                    graded -> Ascent.Danger
                                    matchSel == i -> Ascent.Accent
                                    else -> Ascent.Border
                                },
                                shape
                            )
                            .clickable { onMatchSelect(i) }
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(left, color = Ascent.Fg, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = if (assigned >= 0) q.pairsRight.getOrElse(assigned) { "?" } else "点此选择",
                            color = if (assigned >= 0) Ascent.Accent else Ascent.Fg3,
                            fontSize = 13.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (graded && !isRight) {
                        Text(
                            text = "　正确答案：${q.pairsRight.getOrElse(i) { "?" }}",
                            color = Ascent.Fg3,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 9.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                MonoLabel("从下面选择（先点上方条目，再点这里）")
                Spacer(Modifier.height(8.dp))
                rightOrder.forEach { r ->
                    val used = matchAssign.take(q.pairsLeft.size).contains(r)
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(shape)
                            .background(if (used) Ascent.Surface2 else Ascent.Surface)
                            .border(1.dp, if (used) Ascent.Border else Ascent.Accent, shape)
                            .clickable(enabled = !graded) { onMatchAssign(r) }
                            .padding(12.dp)
                    ) {
                        Text(
                            q.pairsRight.getOrElse(r) { "?" },
                            color = if (used) Ascent.Fg3 else Ascent.Fg,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            QType.Calc -> {
                OutlinedTextField(
                    value = calcText,
                    onValueChange = onCalc,
                    enabled = !graded,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = {
                        Text(
                            if (q.unit != null) "填入数值（单位 ${q.unit}）" else "填入数值",
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (graded) {
                    Spacer(Modifier.height(12.dp))
                    val head = q.answers.firstOrNull().orEmpty()
                    val u = q.unit?.let { " $it" }.orEmpty()
                    val tol = if (q.tolerance > 0) "（容差 ±${q.tolerance}）" else ""
                    AnswerLine("参考答案：$head$u$tol")
                }
            }

            QType.Hotspot -> {
                val bmp = rememberAssetBitmap(q.image.orEmpty())
                val ratio = bmp?.let { it.width.toFloat() / it.height.toFloat() } ?: (4f / 3f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Ascent.Surface2)
                        .border(1.dp, Ascent.Border, RoundedCornerShape(16.dp))
                        .pointerInput(graded, q.prompt) {
                            if (!graded) {
                                detectTapGestures { off ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w > 0f && h > 0f) {
                                        val nx = off.x / w
                                        val ny = off.y / h
                                        var hit = -1
                                        q.hotspots.forEachIndexed { i, r ->
                                            if (hit < 0) {
                                                val dx = nx - r.x
                                                val dy = (ny - r.y) * (h / w)
                                                if (dx * dx + dy * dy <= r.r * r.r) hit = i
                                            }
                                        }
                                        onHotspotTap(nx, ny, hit)
                                    }
                                }
                            }
                        }
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("题图缺失：${q.image}", color = Ascent.Fg3, fontSize = 11.sp)
                        }
                    }
                    val hotspotAccent = Ascent.Accent
                    val hotspotDanger = Ascent.Danger
                    val hotspotSuccess = Ascent.Success
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tapOk = graded && hotspotHit >= 0 && q.hotspots.getOrNull(hotspotHit)?.correct == true
                        hotspotTap?.let { p ->
                            val c = Offset(p.x * size.width, p.y * size.height)
                            val col = if (graded && !tapOk) hotspotDanger else hotspotAccent
                            drawCircle(col.copy(alpha = 0.20f), radius = 22f, center = c)
                            drawCircle(col, radius = 22f, center = c, style = Stroke(3f))
                        }
                        if (graded) {
                            q.hotspots.filter { it.correct }.forEach { r ->
                                drawCircle(
                                    color = hotspotSuccess,
                                    radius = r.r * size.width,
                                    center = Offset(r.x * size.width, r.y * size.height),
                                    style = Stroke(3f)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (graded) "绿色圆环为正确区域" else "点击图中你认为正确的位置",
                    color = Ascent.Fg3,
                    fontSize = 11.sp
                )
                if (!isSummitSpotting(q) && !q.image.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Ascent.Surface)
                            .border(1.dp, Ascent.Border, RoundedCornerShape(12.dp))
                            .clickable { onOpenImage(q.image) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Ic.Maximize2, contentDescription = null, tint = Ascent.Accent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("查看大图（支持双指缩放）", color = Ascent.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            QType.Scenario -> {
                val step = q.steps.getOrNull(scenarioStep)
                val isLast = scenarioStep + 1 >= q.steps.size

                // 步骤进度点
                Row(
                    modifier = Modifier.padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    q.steps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == scenarioStep) 24.dp else 20.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    when {
                                        i < scenarioStep -> Ascent.Success
                                        i == scenarioStep -> Ascent.Accent
                                        else -> Ascent.Surface2
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${i + 1}",
                                color = if (i <= scenarioStep) Ascent.OnAccent else Ascent.Fg3,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (step != null) {
                    MonoLabel("第 ${scenarioStep + 1} 步 / 共 ${q.steps.size} 步")
                    Spacer(Modifier.height(10.dp))
                    ChoiceBlock(
                        step = step,
                        pickIndex = pickIndex,
                        pickSet = pickSet,
                        graded = graded,
                        showAnswerKey = showAnswerKey,
                        onPick = onPick,
                        onTogglePick = onTogglePick,
                        onOpenImage = onOpenImage
                    )

                    if (graded) {
                        Spacer(Modifier.height(10.dp))
                        val shape = RoundedCornerShape(16.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(if (stepOk) Ascent.SuccessSoft else Ascent.DangerSoft)
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (stepOk) Ic.CircleCheck else Ic.CircleX,
                                    contentDescription = null,
                                    tint = if (stepOk) Ascent.Success else Ascent.Danger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (stepOk) "这一步正确" else "这一步答错",
                                    color = if (stepOk) Ascent.Success else Ascent.Danger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(step.explanation, color = Ascent.Fg, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    if (isLast && graded) {
                        Spacer(Modifier.height(12.dp))
                        OutlineCard(gap = 8) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (scenarioAllOk) Ic.Trophy else Ic.Info,
                                    contentDescription = null,
                                    tint = if (scenarioAllOk) Ascent.Warning else Ascent.Fg2,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (scenarioAllOk) "情景通过 · 全部步骤正确"
                                    else "情景未通过 · 错题重做时会整段重来",
                                    color = if (scenarioAllOk) Ascent.Warning else Ascent.Fg2,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(q.explanation, color = Ascent.Fg2, fontSize = 13.sp, lineHeight = 20.sp)
                            if (!q.source.isNullOrBlank()) {
                                Text("来源 · ${q.source}", color = Ascent.Fg3, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** 参考答案行（填空 / 计算）。 */
@Composable
private fun AnswerLine(text: String) {
    Text(text, color = Ascent.Success, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ChoiceBlock(
    step: Question,
    pickIndex: Int?,
    pickSet: List<Int>,
    graded: Boolean,
    showAnswerKey: Boolean,
    onPick: (Int) -> Unit,
    onTogglePick: (Int) -> Unit,
    onOpenImage: (String) -> Unit
) {
    val letters = listOf("A", "B", "C", "D", "E", "F")

    // 子题也可以配图（例如先看预报图再判断）
    if (!step.image.isNullOrBlank()) {
        val bmp = rememberAssetBitmap(step.image)
        val ratio = bmp?.let { it.width.toFloat() / it.height.toFloat() } ?: (4f / 3f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp))
                .background(Ascent.Surface2)
                .border(1.dp, Ascent.Border, RoundedCornerShape(16.dp))
                .clickable { onOpenImage(step.image) }
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("题图缺失：${step.image}", color = Ascent.Fg3, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点击图片可查看大图（支持双指缩放）",
            color = Ascent.Fg3,
            fontSize = 11.sp,
            modifier = Modifier.clickable { onOpenImage(step.image) }
        )
        Spacer(Modifier.height(14.dp))
    }

    Text(step.prompt, color = Ascent.Fg, fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
    if (!step.context.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(step.context, color = Ascent.Fg2, fontSize = 13.sp, lineHeight = 20.sp)
    }
    Spacer(Modifier.height(16.dp))
    if (step.type == QType.Multi) {
        step.options.forEachIndexed { i, opt ->
            val isSel = i in pickSet
            val isRight = i in step.correctSet
            OptionRow(
                key = if (isSel) "\u2713" else letters.getOrElse(i) { "?" },
                text = opt,
                selected = isSel,
                correct = graded && isRight,
                wrong = graded && isSel && !isRight,
                showKey = showAnswerKey && !graded && isRight,
                onClick = { onTogglePick(i) }
            )
        }
    } else {
        shuffledSingleDisplay(step).forEachIndexed { pos, i ->
            OptionRow(
                key = letters.getOrElse(pos) { "?" },
                text = step.options[i],
                selected = pickIndex == i,
                correct = graded && i == step.correct,
                wrong = graded && pickIndex == i && i != step.correct,
                showKey = showAnswerKey && !graded && i == step.correct,
                onClick = { onPick(i) }
            )
        }
    }
}

@Composable
private fun OptionRow(
    key: String,
    text: String,
    selected: Boolean,
    correct: Boolean,
    wrong: Boolean,
    showKey: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(shape)
            .background(
                when {
                    correct -> Ascent.SuccessSoft
                    wrong -> Ascent.DangerSoft
                    selected -> Ascent.AccentSoft
                    else -> Ascent.Surface
                }
            )
            .border(
                1.dp,
                when {
                    correct -> Ascent.Success
                    wrong -> Ascent.Danger
                    selected -> Ascent.Accent
                    else -> Ascent.Border
                },
                shape
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    when {
                        correct -> Ascent.Success
                        wrong -> Ascent.Danger
                        selected -> Ascent.Accent
                        else -> Ascent.Surface2
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = when {
                    correct || wrong || selected -> Ascent.OnAccent
                    else -> Ascent.Fg2
                },
                fontSize = 12.sp,
                fontFamily = MonoNumbers,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = Ascent.Fg, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
        if (showKey) {
            Text("KEY", color = Ascent.Success, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)
        } else if (correct) {
            Icon(Ic.CircleCheck, contentDescription = null, tint = Ascent.Success, modifier = Modifier.size(20.dp))
        } else if (wrong) {
            Icon(Ic.CircleX, contentDescription = null, tint = Ascent.Danger, modifier = Modifier.size(20.dp))
        }
    }
}

/** 加载 assets/bank/img/ 下的题图；null 表示缺失。 */
@Composable
private fun rememberAssetBitmap(name: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(name) {
        runCatching {
            context.assets.open("bank/img/$name").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.asImageBitmap()
    }
}

/**
 * 从 assets/bank/img/ 加载题图。
 * 热点题的容器按图片**真实宽高比**布局（调用方用 rememberAssetBitmap 取比例），
 * 这样点击坐标的归一化映射才与题目里的 hotspots 完全一致。
 */
@Composable
private fun AssetImage(name: String, modifier: Modifier = Modifier) {
    val bmp = rememberAssetBitmap(name)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("题图缺失：$name", color = Ascent.Fg3, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BriefView(lesson: Lesson, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Ic.BookOpen, Ascent.Accent, Ascent.AccentSoft, diameter = 44, iconSize = 22)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(lesson.title, color = Ascent.Fg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                MonoLabel(lesson.code.uppercase())
            }
        }
        Spacer(Modifier.height(14.dp))
        val reviewCount = lesson.questions.count { it.review }
        val typeCount = lesson.questions.map { it.type }.distinct().size
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftTag("${lesson.questions.size} 题", Ascent.Fg2, Ascent.Surface2, icon = Ic.Book)
            SoftTag("约 ${lesson.questions.size * 40 / 60 + 1} 分钟", Ascent.Fg2, Ascent.Surface2, icon = Ic.Timer)
            if (typeCount > 1) SoftTag("$typeCount 种题型", Ascent.Fg2, Ascent.Surface2)
        }
        Spacer(Modifier.height(18.dp))

        MonoLabel("本课要点 · KEY POINTS", modifier = Modifier.padding(bottom = 10.dp))
        OutlineCard(gap = 0, padding = 0, modifier = Modifier.padding(bottom = 14.dp)) {
            lesson.brief.forEachIndexed { idx, point ->
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconBadge(Ic.CircleDot, Ascent.Accent, Ascent.AccentSoft, diameter = 28, iconSize = 14)
                    Spacer(Modifier.width(12.dp))
                    Text(point, color = Ascent.Fg, fontSize = 14.sp, lineHeight = 22.sp)
                }
                if (idx < lesson.brief.size - 1) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Ascent.Border))
                }
            }
        }

        if (lesson.needsCoach) {
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(shape)
                    .background(Ascent.WarningSoft)
                    .border(1.dp, Ascent.Border, shape)
                    .padding(16.dp)
            ) {
                Icon(Ic.ShieldAlert, contentDescription = null, tint = Ascent.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("实操技能", color = Ascent.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "本课内容必须由持证教练现场指导与评估后，才能在真实环境中使用。",
                        color = Ascent.Fg2,
                        fontSize = 12.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        PillButton(text = "开始测验 · ${lesson.questions.size} 题", icon = Ic.ArrowRight, onClick = onStart)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun DoneView(
    lesson: Lesson,
    gained: Int,
    stars: Int,
    correct: Int,
    total: Int,
    retried: Int,
    resolved: Int,
    remaining: Int,
    onContinue: () -> Unit
) {
    val perfect = total > 0 && correct >= total && remaining == 0
    val shaky = remaining > 0
    val badgeBg: Any = when {
        perfect -> Brush.linearGradient(listOf(Color(0xFF3AA3FF), Color(0xFF0267CE)))
        shaky -> Ascent.WarningSoft
        else -> Ascent.SuccessSoft
    }
    val badgeIcon = when {
        perfect -> Ic.Trophy
        shaky -> Ic.RotateCcw
        else -> Ic.CircleCheck
    }
    val badgeTint = when {
        perfect -> Color.White
        shaky -> Ascent.Warning
        else -> Ascent.Success
    }
    val title = when {
        perfect -> "满分通过"
        shaky -> "完成 · 需巩固"
        else -> "已完成"
    }
    val sub = when {
        perfect -> "PERFECT"
        shaky -> "PASSED · 有错题"
        else -> "PASSED"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))

        val shape = RoundedCornerShape(999.dp)
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(shape)
                .then(
                    if (perfect) Modifier.background(badgeBg as Brush)
                    else Modifier.background(badgeBg as Color)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(badgeIcon, contentDescription = null, tint = badgeTint, modifier = Modifier.size(42.dp))
        }

        Spacer(Modifier.height(14.dp))
        Text(title, color = Ascent.Fg, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        MonoLabel("$sub · $correct / $total")

        Spacer(Modifier.height(14.dp))
        StarRow(f = stars)

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                value = if (total == 0) "100%" else "${correct * 100 / total}%",
                label = "正确率",
                modifier = Modifier.weight(1f)
            )
            StatTile(value = "$correct/$total", label = "首轮答对", modifier = Modifier.weight(1f))
            StatTile(value = "+$gained m", label = "海拔", modifier = Modifier.weight(1f))
        }

        if (retried > 0 || shaky) {
            Spacer(Modifier.height(14.dp))
            val nShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(nShape)
                    .background(Ascent.Surface2)
                    .padding(14.dp)
            ) {
                Icon(Ic.Info, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        shaky -> "还有 $remaining 题未掌握，已进入复习队列——重做同一课不再获得海拔，但错题会回炉直到答对。"
                        retried > 0 -> "错题重做 $retried 题 · 订正成功 $resolved 题。"
                        else -> ""
                    },
                    color = Ascent.Fg2,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlineCard(gap = 8) {
            Text(
                text = "技能「${lesson.title}」理论部分已记录。",
                color = Ascent.Fg,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            if (lesson.needsCoach) {
                Text("本课为实操技能，仍需教练现场指导与评估。", color = Ascent.Warning, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        PillButton(
            text = "继续攀登",
            icon = Ic.ArrowRight,
            style = if (shaky) BtnStyle.Secondary else BtnStyle.Primary,
            onClick = onContinue
        )
        Spacer(Modifier.height(28.dp))
    }
}

// ---------- 展示顺序的稳定打乱（防止答案位置被背诵） ----------

/** 单选（含情景单选）的展示顺序：按题面哈希稳定打乱（单选不存在「不动即对」，不必避免与原顺序相同）。 */
private fun shuffledSingleDisplay(q: Question): List<Int> {
    val n = q.options.size
    if (n <= 1) return q.options.indices.toList()
    val rnd = java.util.Random(q.prompt.hashCode().toLong() * 1000003L xor 0x5DEECE66DL)
    val idx = q.options.indices.toMutableList()
    for (i in n - 1 downTo 1) {
        val j = rnd.nextInt(i + 1)
        val t = idx[i]; idx[i] = idx[j]; idx[j] = t
    }
    return idx
}

/** 排序题的初始顺序：稳定打乱，且保证与正确顺序不同（避免「原地不动即正确」）。 */
private fun shuffledOrderStart(q: Question): List<Int> {
    val n = q.options.size
    if (n <= 1) return q.options.indices.toList()
    val rnd = java.util.Random(q.prompt.hashCode().toLong() * 31L xor 0x2545F4914F6CDD1DL)
    val idx = q.options.indices.toMutableList()
    for (i in n - 1 downTo 1) {
        val j = rnd.nextInt(i + 1)
        val t = idx[i]; idx[i] = idx[j]; idx[j] = t
    }
    val correct = if (q.correctOrder.size == n) q.correctOrder else q.options.indices.toList()
    if (idx == correct) {
        val k = 1 + Math.floorMod(q.prompt.hashCode(), n - 1)
        repeat(k) { val f = idx.removeAt(0); idx.add(f) }
    }
    return idx
}

/** 「选出顶峰位置」类热点题不提供查看大图入口（点图即作答，加按钮会造成误导）。 */
private fun isSummitSpotting(q: Question): Boolean =
    q.hotspots.any { it.label.contains("峰顶") }

/**
 * 全屏图片查看器：双指缩放 / 拖动 / 双击放大还原。
 * 用于需要「看图辨识」的题目（气象截图、等高线等）。
 */
@Composable
private fun ImageViewerDialog(image: String, onClose: () -> Unit) {
    val bmp = rememberAssetBitmap(image)
    var scale by remember(image) { mutableStateOf(1f) }
    var offset by remember(image) { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0D))
                .pointerInput(image) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, 8f)
                        scale = next
                        offset = if (next <= 1f) Offset.Zero else offset + pan
                    }
                }
                .pointerInput(image) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1.01f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    })
                }
        ) {
            // 顶部：缩放值 + 复位 + 关闭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.X, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(24.dp)) }
                Text(
                    text = String.format(java.util.Locale.US, "%.1f×", scale),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MonoNumbers
                )
                Box(
                    modifier = Modifier.size(36.dp).clickable { scale = 1f; offset = Offset.Zero },
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.RotateCcw, contentDescription = "复位", tint = Color(0xB3FFFFFF), modifier = Modifier.size(20.dp)) }
            }

            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("题图缺失：$image", color = Color(0xFFA1A1AA), fontSize = 13.sp)
                }
            }

            // 底部：缩放档位 + 手势提示
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1f, 2f, 4f, 8f).forEach { z ->
                        val selected = kotlin.math.abs(scale - z) < 0.3f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) Ascent.Accent else Color.Transparent)
                                .clickable {
                                    scale = z
                                    if (z <= 1f) offset = Offset.Zero
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${z.toInt()}×",
                                color = if (selected) Color.White else Color(0x99FFFFFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = MonoNumbers
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.Maximize2, contentDescription = null, tint = Color(0x99FFFFFF), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("双指缩放 · 拖动查看 · 双击复位", color = Color(0x99FFFFFF), fontSize = 12.sp)
                }
            }
        }
    }
}
