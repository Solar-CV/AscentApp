package com.ascent.climb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.R
import com.ascent.climb.data.ALL_TRACKS
import com.ascent.climb.data.CLIMB_TRACK
import com.ascent.climb.data.TREK_TRACK
import com.ascent.climb.i18n.AppLocale

/**
 * 首次启动的偏好引导：整屏遮罩、没有返回键，只问三件事——攀登 / 徒步 / 语言。
 *
 * 三个选项是严格按需求收的：不在这里问提醒、主题、单位，那些留给「设置」。
 * 语言默认「跟随系统」，并且**不预选系统当前语言**——否则用户会以为被写死了。
 *
 * 交互上的两个决定：
 *   - 语言是**即时生效**的：点完这一页的语言，整棵树（含本页文案）立刻切换。
 *     这不是炫技，是为了让用户在按下「继续」之前就能确认没选错。
 *   - 轨道只改本地状态，按「继续」才落到 store——中途退出不该留下半套偏好。
 */
@Composable
fun OnboardingOverlay(
    initialTrack: String,
    language: String,
    onLanguageChange: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    val a = clayAccents()
    val dark = Ascent.IsDark
    var track by rememberSaveable { mutableStateOf(initialTrack) }
    var sheetOpen by remember { mutableStateOf(false) }

    // 整屏遮罩期间吞掉系统返回：误触退出会重来一遍，不如干脆不给退
    BackHandler(enabled = true) {
        if (sheetOpen) sheetOpen = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Ic.MountainSnow,
                    contentDescription = null,
                    tint = a.lessons,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ASCENTROAM",
                    color = a.label,
                    fontSize = 10.sp,
                    fontFamily = MonoNumbers,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )
            }

            Spacer(Modifier.height(38.dp))
            Text(
                text = stringResource(R.string.ob_title),
                color = a.value,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.ob_sub),
                color = a.sub,
                fontSize = 12.5.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))
            TrackOption(
                selected = track == CLIMB_TRACK,
                icon = Ic.TrackClimb,
                tint = a.lessons,
                name = stringResource(R.string.path_track_climb),
                meta = stringResource(R.string.ob_climb_meta, lessonCount(CLIMB_TRACK)),
                note = stringResource(R.string.ob_climb_note),
                onClick = { track = CLIMB_TRACK }
            )
            Spacer(Modifier.height(14.dp))
            TrackOption(
                selected = track == TREK_TRACK,
                icon = Ic.TrackTrek,
                tint = Color(0xFF2E8B62),
                name = stringResource(R.string.path_track_trek),
                meta = stringResource(R.string.ob_trek_meta, lessonCount(TREK_TRACK)),
                note = stringResource(R.string.ob_trek_note),
                onClick = { track = TREK_TRACK }
            )

            Spacer(Modifier.height(14.dp))
            LanguageOption(
                value = languageLabel(language),
                onClick = { sheetOpen = true }
            )

            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.ob_hint),
                color = a.sub,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            ClaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onConfirm(track) },
                radius = 20.dp,
                elevation = 10.dp,
                fill = SolidColor(a.lessons),
                rim = Color(0xFF0B6FCB)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.ob_continue),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        if (sheetOpen) {
            // 深纱：轻一点，浅色主题下压太重会像对话框
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x331B2328))
                    .clickable { sheetOpen = false }
            )
            // Box 里的 Column 默认是顶部对齐，不显式下沉的话 sheet 会飘在屏幕上方
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                LanguageSheet(
                    current = language,
                    onPick = {
                        onLanguageChange(it)
                        sheetOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackOption(
    selected: Boolean,
    icon: ImageVector,
    tint: Color,
    name: String,
    meta: String,
    note: String,
    onClick: () -> Unit
) {
    val a = clayAccents()
    val dark = Ascent.IsDark
    ClaySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable(onClick = onClick),
        radius = 24.dp,
        elevation = if (selected) 10.dp else 8.dp,
        fill = if (selected) {
            SolidColor(if (dark) Color(0xFF14263A) else Color(0xFFF1F7FF))
        } else null,
        rim = if (selected) {
            if (dark) Color(0xFF2E6FB8) else Color(0xFFBFD9F8)
        } else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayChip(color = tint, size = 46.dp, radius = 16.dp) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = a.value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta,
                    color = a.sub,
                    fontSize = 9.5.sp,
                    fontFamily = MonoNumbers,
                    letterSpacing = 0.4.sp,
                    // 德语 / 俄语的这行比中文长得多，不限行会把卡片撑破
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note,
                    color = if (selected) a.lessons else a.sub,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            RadioDot(selected)
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val a = clayAccents()
    val dark = Ascent.IsDark
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> a.lessons
                    // 深色下纯白圆圈太扎眼，压成一级灰
                    dark -> Color(0xFF24272C)
                    else -> Color.White
                }
            )
            .border(
                1.dp,
                if (selected) Color(0xFF0B6FCB)
                else if (dark) Color(0xFF3A4048)
                else Color(0xFFD8DEE8),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Ic.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun LanguageOption(value: String, onClick: () -> Unit) {
    val a = clayAccents()
    ClaySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        radius = 22.dp,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayChip(color = Color(0xFF5B6B80), size = 40.dp, radius = 14.dp) {
                Icon(Ic.Globe, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lang_title),
                    color = a.value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.ob_lang_sub),
                    color = a.sub,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(value, color = a.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(Ic.ChevronRight, contentDescription = null, tint = a.sub, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LanguageSheet(current: String, onPick: (String) -> Unit) {
    val a = clayAccents()
    val dark = Ascent.IsDark
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(squircle(28.dp))
            .background(if (dark) Color(0xFF1B1E23) else Color.White)
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCE0E7))
        )
        Spacer(Modifier.height(14.dp))
        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(stringResource(R.string.lang_title), color = a.value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.ob_lang_sheet_sub), color = a.sub, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            LangRow(
                label = stringResource(R.string.lang_follow_system),
                desc = stringResource(R.string.ob_follow_desc),
                selected = current == AppLocale.AUTO,
                onClick = { onPick(AppLocale.AUTO) }
            )
            AppLocale.SUPPORTED.forEach { lang ->
                LangRow(
                    label = lang.nativeName,
                    desc = null,
                    selected = current == lang.tag,
                    onClick = { onPick(lang.tag) }
                )
            }
        }
    }
}

@Composable
private fun LangRow(label: String, desc: String?, selected: Boolean, onClick: () -> Unit) {
    val a = clayAccents()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(squircle(15.dp))
            .background(if (selected) (if (Ascent.IsDark) Color(0xFF152230) else Color(0xFFEAF3FF)) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF0B5FBF) else a.label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
        if (desc != null) {
            Spacer(Modifier.width(12.dp))
            Text(text = desc, color = a.sub, fontSize = 10.5.sp)
        }
        Spacer(Modifier.weight(1f))
        if (selected) {
            Icon(Ic.Check, contentDescription = null, tint = a.lessons, modifier = Modifier.size(16.dp))
        }
    }
}

/** 轨道课数：直接数数据层，避免在文案里写死 68 / 21。 */
private fun lessonCount(trackId: String): Int =
    ALL_TRACKS.firstOrNull { it.id == trackId }?.units?.sumOf { it.lessons.size } ?: 0

/** 当前语言的展示名；"auto" 时显示「跟随系统」。 */
@Composable
private fun languageLabel(mode: String): String =
    if (mode == AppLocale.AUTO) stringResource(R.string.lang_follow_system)
    else AppLocale.nativeName(mode).ifEmpty { mode }
