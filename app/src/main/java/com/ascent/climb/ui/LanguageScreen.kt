package com.ascent.climb.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.R
import com.ascent.climb.i18n.AppLocale
import java.util.Locale

/**
 * 语言选择页（二级页面，从设置进入）。
 *
 * 结构：「跟随系统」+ 全部 10 种语言，每种语言用它自身的文字显示。
 * 选择后立即写回偏好，由 MainActivity 重组生效，无需重启。
 */
@Composable
fun LanguageScreen(
    languageMode: String,
    onLanguageModeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // 「跟随系统」当前会落到哪种语言：用于副标题，让用户知道自动匹配的结果
    val autoLocale = AppLocale.resolve(AppLocale.AUTO)
    val systemLabel = AppLocale.SUPPORTED
        .firstOrNull { it.tag == autoLocale.toLanguageTag() }
        ?.nativeName
        ?: stringResource(R.string.lang_system_unknown)

    Box(modifier = Modifier.fillMaxWidth().background(Ascent.Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
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
                    Icon(
                        Ic.ChevronLeft,
                        contentDescription = null,
                        tint = Ascent.Fg2,
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.common_back),
                        color = Ascent.Fg2,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.lang_title),
                    color = Ascent.Fg,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.lang_note),
                    color = Ascent.Fg3,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // ---------------- 跟随系统 ----------------
            item(key = "lbSys") {
                MonoLabel(
                    stringResource(R.string.lang_section),
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )
            }
            item(key = "sys") {
                LanguageRow(
                    title = stringResource(R.string.lang_follow_system),
                    subtitle = stringResource(R.string.lang_follow_system_desc, systemLabel),
                    selected = languageMode == AppLocale.AUTO,
                    onClick = { onLanguageModeChange(AppLocale.AUTO) }
                )
            }

            // ---------------- 全部语言 ----------------
            item(key = "lbAll") {
                MonoLabel(
                    stringResource(R.string.lang_section_all),
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )
            }
            items(AppLocale.SUPPORTED, key = { it.tag }) { lang ->
                LanguageRow(
                    title = lang.nativeName,
                    subtitle = null,
                    selected = languageMode == lang.tag,
                    onClick = { onLanguageModeChange(lang.tag) }
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlineCard(
        modifier = Modifier.padding(bottom = 8.dp),
        gap = 0,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Ascent.Fg,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = Ascent.Fg3, fontSize = 12.sp)
                }
            }
            if (selected) {
                Icon(
                    Ic.CircleCheck,
                    contentDescription = null,
                    tint = Ascent.Accent,
                    modifier = Modifier.width(20.dp)
                )
            }
        }
    }
}
