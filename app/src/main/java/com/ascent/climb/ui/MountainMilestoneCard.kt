package com.ascent.climb.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.R
import com.ascent.climb.data.Mountain
import com.ascent.climb.data.MountainMilestones
import com.ascent.climb.data.MountainRegion

/**
 * 山峰里程碑卡片（折叠式）。
 *
 * 折叠态占两行：山峰名 + 真实海拔，以及「当前海拔 · 还差多少」。
 * 点开才展开地形图与数据 —— 展开态有 130dp 的图，铺在列表里会把课程入口全挤下去。
 *
 * 三个内容设计：
 *  1. **统一标注**：所有山都用同一句「地形示意 · 依据公开高程数据绘制」。
 *     只给小山加说明等于给小山贴「次品」标签。
 *  2. **数据行代替辩解**：山体跨度写出来，用户自己就明白为什么有的尖、有的圆。
 *  3. **有图用图，无图用符号**：目前只有珠峰和马特洪峰出了地形渲染，
 *     其余用同一套线稿符号，风格统一，不显示「待补」这类半成品文案。
 */
@Composable
fun MountainMilestoneCard(
    m: Mountain,
    currentAlt: Int,
    unlockedCount: Int,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = squircle(18.dp)
    val remaining = m.milestoneAlt - currentAlt
    val reached = remaining <= 0
    val name = mountainName(m)
    val separator = "  ·  "

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ascent.Surface)
            .border(1.dp, Ascent.Border, shape)
            .animateContentSize()
    ) {
        // ---------------- 折叠态 ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeakGlyph(
                modifier = Modifier.size(17.dp),
                color = Ascent.Fg3
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Ascent.Fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${formatMeters(m.elevation)} m",
                        color = Ascent.Fg2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mtn_current, formatMeters(currentAlt)),
                        color = Ascent.Fg3,
                        fontSize = 10.5.sp
                    )
                    Text(
                        // 「已到达」只留给真的站上去了的那座；档次打开但山还在头顶，说「已解锁」
                        text = separator + when {
                            !reached -> stringResource(R.string.mtn_remaining, formatMeters(remaining))
                            currentAlt >= m.elevation -> stringResource(R.string.mtn_summit_reached)
                            else -> stringResource(R.string.mtn_unlocked)
                        },
                        color = if (reached) Ascent.Success else Ascent.Fg3,
                        fontSize = 10.5.sp,
                        fontWeight = if (reached) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Ic.ChevronDown,
                contentDescription = null,
                tint = Ascent.Fg3,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(if (expanded) 180f else 0f)
            )
        }

        // ---------------- 展开态 ----------------
        if (expanded) {
            MountainDetailBody(m = m)

            Spacer(Modifier.height(11.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ascent.Line)
            )
            // 历史记录：完整列表在个人主页，这里只放跳转
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(squircle(10.dp))
                    .clickable { onOpenHistory() }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (unlockedCount > 0)
                        stringResource(R.string.mtn_jump_count, unlockedCount)
                    else stringResource(R.string.mtn_jump_none),
                    color = Ascent.Fg2,
                    fontSize = 11.5.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Ic.ChevronRight,
                    contentDescription = null,
                    tint = Ascent.Fg3,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * 卡片展开态与「历史 → 山峰卡片」共用的正文。
 *
 * 两处不可能各写一套：数据口径、单位、标注文案只要有一边动了，另一边就会变成旧版。
 */
@Composable
private fun MountainDetailBody(
    m: Mountain,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 146.dp,
    imageTopPadding: Dp = 0.dp
) {
    val name = mountainName(m)
    val range = mountainRange(m)
    val rank = MountainMilestones.rank8000(m)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = imageTopPadding)
            .padding(horizontal = 12.dp)
            .height(imageHeight)
            .clip(squircle(14.dp))
            .background(Ascent.Surface2)
            // 再压一层极淡的前景色：雪面接近纯白，压在 Surface2 上会糊成一片
            .background(Ascent.Fg3.copy(alpha = 0.06f))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (m.drawable != null) {
            Image(
                painter = painterResource(m.drawable),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MountainSilhouette(
                m = m,
                modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 10.dp)
            )
        }
    }

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        // 中文界面把英文名当副标题；德语、日文界面主标题本身就是本地名字，
        // 再挂一行一模一样的英文就成了重复
        if (name != m.nameEn) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = m.nameEn,
                color = Ascent.Fg3,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (rank > 0)
                stringResource(R.string.mtn_milestone_rank, formatMeters(m.milestoneAlt), rank)
            else stringResource(R.string.mtn_milestone_range, formatMeters(m.milestoneAlt), range),
            color = Ascent.Fg2,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(11.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ascent.Line))
        Spacer(Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Stat(stringResource(R.string.mtn_metric_span), m.spanKm, Modifier.weight(1f))
            Stat(stringResource(R.string.mtn_metric_snowline), mountainSnowline(m.snowline), Modifier.weight(1f))
            Stat(stringResource(R.string.mtn_metric_range), range, Modifier.weight(1f))
        }

        Spacer(Modifier.height(9.dp))
        Text(
            // 等高线图和山体渲染是两套语言，说明也得跟着换，不然「非实景照片」说得没头没脑
            text = stringResource(
                if (m.contour) R.string.mtn_note_contour else R.string.mtn_note_render
            ),
            color = Ascent.Fg3,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Ascent.Fg3, fontSize = 9.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = Ascent.Fg2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    }
}

/** 极简山形：一个不规则的三角轮廓，比通用图标更贴合主题。 */
@Composable
private fun PeakGlyph(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.04f, h * 0.88f)
            lineTo(w * 0.40f, h * 0.14f)
            lineTo(w * 0.62f, h * 0.52f)
            lineTo(w * 0.78f, h * 0.30f)
            lineTo(w * 0.96f, h * 0.88f)
        }
        drawPath(path = p, color = color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

/**
 * 没有地形渲染时的山形符号。
 *
 * 不用「按海拔变形」的多边形 —— 试过，矮海拔会变成一个土丘，看着像画坏了。
 * 改成前后两座山的层次构图：后山淡、前山深，前山带一条雪线。
 * 这样四十几座山共用同一套符号也不会显得敷衍。
 */
@Composable
private fun MountainSilhouette(m: Mountain, modifier: Modifier = Modifier) {
    // 颜色必须在 Composable 上下文取，DrawScope 里读不到 CompositionLocal
    // 用 Fg3 调透明度而不是 Border：Border 本身就是浅灰，铺成面之后和底色分不开
    val backFill = Ascent.Fg3.copy(alpha = 0.14f)
    val frontFill = Ascent.Fg3.copy(alpha = 0.30f)
    val edge = Ascent.Fg3.copy(alpha = 0.70f)
    // 雪线要比山体亮：浅色模式下用面板白，深色模式下用前景近白
    val snow = if (Ascent.IsDark) Ascent.Fg.copy(alpha = 0.85f) else Ascent.Surface

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val baseY = h * 0.94f

        // 后山：偏右、略矮、淡
        val bx = w * 0.63f
        val bw = w * 0.34f
        val bh = h * 0.62f
        val back = Path().apply {
            moveTo(bx - bw, baseY)
            lineTo(bx - bw * 0.28f, baseY - bh * 0.62f)
            lineTo(bx, baseY - bh)
            lineTo(bx + bw * 0.34f, baseY - bh * 0.58f)
            lineTo(bx + bw, baseY)
            close()
        }
        drawPath(back, backFill)

        // 前山：偏左、更高
        val fx = w * 0.44f
        val fw = w * 0.40f
        val fh = h * 0.82f
        val front = Path().apply {
            moveTo(fx - fw, baseY)
            lineTo(fx - fw * 0.46f, baseY - fh * 0.44f)
            lineTo(fx - fw * 0.12f, baseY - fh * 0.88f)
            lineTo(fx, baseY - fh)
            lineTo(fx + fw * 0.26f, baseY - fh * 0.60f)
            lineTo(fx + fw * 0.62f, baseY - fh * 0.30f)
            lineTo(fx + fw, baseY)
            close()
        }
        drawPath(front, frontFill)
        drawPath(front, edge, style = Stroke(width = 1.3.dp.toPx()))

        // 雪线：一条横线，用山体裁切。按坡度算端点会算偏，露出山体外面像画歪了
        val snowY = baseY - fh * 0.68f
        clipPath(front) {
            drawLine(
                color = snow,
                start = Offset(fx - fw, snowY),
                end = Offset(fx + fw, snowY),
                strokeWidth = 1.6.dp.toPx()
            )
        }
    }
}

/**
 * 路径页顶部那一块：只显示「当前海拔最该看到的那一座」。
 *
 * 历史上解锁过的山不铺在路径页（八十多座会把课程挤没），
 * 从这里进底部面板看全量。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainMilestoneBlock(
    modifier: Modifier = Modifier,
    currentAlt: Int = 3200,
    region: MountainRegion? = null
) {
    var showHistory by remember { mutableStateOf(false) }
    val current = remember(currentAlt, region) { MountainMilestones.pick(currentAlt, region) }
    val unlocked = remember(currentAlt) { MountainMilestones.unlocked(currentAlt) }

    Column(modifier = modifier.fillMaxWidth()) {
        MountainMilestoneCard(
            m = current,
            currentAlt = currentAlt,
            unlockedCount = unlocked.size,
            onOpenHistory = { showHistory = true }
        )
        Spacer(Modifier.height(4.dp))
    }

    if (showHistory) {
        MountainHistorySheet(currentAlt = currentAlt, onDismiss = { showHistory = false })
    }
}

/**
 * 「我的 · 当前海拔」下面那一行入口。
 *
 * 完整历史不铺在路径页 —— 八十多座会把课程挤没；卡片展开态也只放跳转。
 */
@Composable
fun MountainHistoryEntry(
    currentAlt: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 「我的」页那张海拔卡是深色底，文字颜色要跟着换。 */
    fg: Color? = null,
    sub: Color? = null
) {
    val unlocked = remember(currentAlt) { MountainMilestones.unlocked(currentAlt) }
    val fgColor = fg ?: Ascent.Fg
    val subColor = sub ?: Ascent.Fg3
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(squircle(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeakGlyph(modifier = Modifier.size(15.dp), color = subColor)
        Spacer(Modifier.width(11.dp))
        Text(
            text = stringResource(R.string.mtn_entry_title),
            color = fgColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (unlocked.isEmpty()) stringResource(R.string.mtn_entry_none)
            else stringResource(R.string.mtn_entry_progress, unlocked.size, MountainMilestones.ALL.size),
            color = subColor,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Ic.ChevronRight,
            contentDescription = null,
            tint = subColor,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * 山峰历史面板。路径页卡片和「我的」页共用同一份。
 *
 * 点开一座已解锁的山，面板内部换成那张山峰卡片（带返回），
 * 不再叠一层 ModalBottomSheet —— 两层 sheet 在返回手势下会互相抢 dismiss。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountainHistorySheet(currentAlt: Int, onDismiss: () -> Unit) {
    var detail by remember { mutableStateOf<Mountain?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ascent.Bg
    ) {
        val open = detail
        if (open == null) {
            MountainHistoryList(currentAlt = currentAlt, onOpen = { detail = it })
        } else {
            MountainHistoryDetail(m = open, currentAlt = currentAlt, onBack = { detail = null })
        }
    }
}

@Composable
private fun MountainHistoryList(currentAlt: Int, onOpen: (Mountain) -> Unit) {
    val unlocked = remember(currentAlt) { MountainMilestones.unlocked(currentAlt) }
    val history = remember(currentAlt) { MountainMilestones.history(currentAlt) }
    val eightTotal = remember { MountainMilestones.ALL.count { it.elevation >= 8000 } }
    val eightDone = remember(currentAlt) { MountainMilestones.eightKReached(currentAlt) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Text(
            text = stringResource(R.string.mtn_history_title),
            color = Ascent.Fg,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.mtn_history_summary,
                unlocked.size, MountainMilestones.ALL.size, eightDone, eightTotal
            ),
            color = Ascent.Fg3,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.height(14.dp))

        // 八十多座，必须懒加载；fill = false 让内容少时面板不会撑满屏
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(bottom = 26.dp)
        ) {
            items(history, key = { it.id }) { m ->
                val done = m.milestoneAlt <= currentAlt
                MountainHistoryRow(m = m, done = done, onClick = { if (done) onOpen(m) })
            }
        }
    }
}

@Composable
private fun MountainHistoryRow(m: Mountain, done: Boolean, onClick: () -> Unit) {
    val rank = MountainMilestones.rank8000(m)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(squircle(12.dp))
            // 未解锁的行不给点击：点进去只有一张写着「未到达」的卡片，等于白点
            .clickable(enabled = done) { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeakGlyph(
            modifier = Modifier.size(15.dp),
            color = if (done) Ascent.Fg2 else Ascent.Fg3.copy(alpha = 0.4f)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mountainName(m),
                color = if (done) Ascent.Fg else Ascent.Fg3.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (rank > 0)
                    stringResource(R.string.mtn_row_rank, rank, formatMeters(m.elevation))
                else stringResource(
                    R.string.mtn_row_milestone,
                    formatMeters(m.milestoneAlt), formatMeters(m.elevation)
                ),
                color = Ascent.Fg3,
                fontSize = 10.5.sp
            )
        }
        Text(
            text = stringResource(if (done) R.string.mtn_unlocked else R.string.mtn_row_locked),
            color = if (done) Ascent.Success else Ascent.Fg3.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        if (done) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Ic.ChevronRight,
                contentDescription = null,
                tint = Ascent.Fg3,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** 面板内的单座山峰卡片（次级页）。 */
@Composable
private fun MountainHistoryDetail(m: Mountain, currentAlt: Int, onBack: () -> Unit) {
    val reached = m.milestoneAlt <= currentAlt
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(squircle(10.dp))
                .clickable { onBack() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Ic.ChevronLeft,
                contentDescription = null,
                tint = Ascent.Fg3,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.common_back),
                color = Ascent.Fg2,
                fontSize = 12.5.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (reached) stringResource(R.string.mtn_unlocked)
                else stringResource(R.string.mtn_row_locked),
                color = if (reached) Ascent.Success else Ascent.Fg3,
                fontSize = 11.5.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = mountainName(m),
            color = Ascent.Fg,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(
                R.string.mtn_detail_progress,
                formatMeters(currentAlt), formatMeters(m.milestoneAlt)
            ),
            color = Ascent.Fg3,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.height(12.dp))

        // 面板被撑满时要能滚：200dp 的图 + 三列数据 + 标注，小屏正好超一屏
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            MountainDetailBody(m = m, imageHeight = 200.dp)
            Spacer(Modifier.height(20.dp))
        }
    }
}
