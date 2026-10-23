package com.ascent.climb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ascent.climb.BuildConfig
import com.ascent.climb.R
import com.ascent.climb.i18n.AppLocale
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.BANK_VERSION
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.TOTAL_QUESTIONS
import com.ascent.climb.data.bank.BankRepository
import com.ascent.climb.data.ALL_LESSONS

/**
 * 设置页（「我的」页右上角齿轮进入）。
 *
 * 收纳原本散落在「我的」页的项目：外观、题库、开发者模式、数据、关于。
 * 危险操作（重置进度 / 恢复内置题库）均需二次确认。
 */
@Composable
fun SettingsScreen(
    store: ProgressStore,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    languageMode: String,
    onLanguageModeChange: (String) -> Unit,
    devUnlocked: Boolean,
    devUnlockAll: Boolean,
    devShowAnswers: Boolean,
    onDevUnlocked: () -> Unit,
    onDevExit: () -> Unit,
    onDevUnlockAllChange: (Boolean) -> Unit,
    onDevShowAnswersChange: (Boolean) -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenBadge3D: () -> Unit,
    bankChecking: Boolean,
    bankMessage: String,
    onCheckBank: () -> Unit,
    onResetBank: () -> Unit,
    onBankUrlChange: (String) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    var bankUrlDraft by remember { mutableStateOf(BankRepository.baseUrl) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmBankReset by remember { mutableStateOf(false) }
    // 语言选择页（独立二级页面）
    var showLanguage by remember { mutableStateOf(false) }
    // 隐藏入口：连点版本号 7 次
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }

    // 系统语言自动匹配到的语言（用于「跟随系统」的副标题）
    val autoLocale = AppLocale.resolve(AppLocale.AUTO)
    val autoLabel = AppLocale.SUPPORTED
        .firstOrNull { it.tag == autoLocale.toLanguageTag() }
        ?.nativeName
        ?: stringResource(R.string.lang_system_unknown)

    if (showLanguage) {
        LanguageScreen(
            languageMode = languageMode,
            onLanguageModeChange = onLanguageModeChange,
            onBack = { showLanguage = false }
        )
        return
    }

    // 系统返回 / 全面屏侧滑手势：回到上一个界面，而不是退出 App
    BackHandler { onBack() }

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
                    Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.width(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.common_back), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.prof_settings), color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
            }

            // ---------------- 外观 ----------------
            item(key = "lbLook") { MonoLabel(stringResource(R.string.set_section_appearance), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "look") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (Ascent.IsDark) Ic.Moon else Ic.Sun,
                            contentDescription = null,
                            tint = Ascent.Fg2,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.set_theme), color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = when (themeMode) {
                                    "light" -> stringResource(R.string.set_theme_desc_light)
                                    "dark" -> stringResource(R.string.set_theme_desc_dark)
                                    else -> stringResource(R.string.set_theme_system_current, stringResource(if (Ascent.IsDark) R.string.set_theme_dark else R.string.set_theme_light))
                                },
                                color = Ascent.Fg3,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("auto" to stringResource(R.string.set_theme_system), "light" to stringResource(R.string.set_theme_light), "dark" to stringResource(R.string.set_theme_dark)).forEach { (v, label) ->
                            Box(modifier = Modifier.weight(1f)) {
                                PillButton(
                                    text = label,
                                    style = if (themeMode == v) BtnStyle.Primary else BtnStyle.Secondary,
                                    height = 40,
                                    onClick = { onThemeModeChange(v) }
                                )
                            }
                        }
                    }
                }
            }

            // ---------------- 语言 ----------------
            item(key = "lang") {
                OutlineCard(
                    modifier = Modifier.padding(bottom = 14.dp),
                    gap = 0,
                    onClick = { showLanguage = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(Ic.Globe, Ascent.Accent, Ascent.AccentSoft, diameter = 40, iconSize = 20)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.set_language),
                                color = Ascent.Fg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (languageMode == AppLocale.AUTO) {
                                    stringResource(R.string.set_language_desc_system, autoLabel)
                                } else {
                                    AppLocale.nativeName(languageMode)
                                },
                                color = Ascent.Fg3,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            Ic.ChevronRight,
                            contentDescription = null,
                            tint = Ascent.Fg3,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ---------------- 题库 ----------------
            item(key = "lbBank") { MonoLabel(stringResource(R.string.set_section_bank), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "bank") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Ic.BookOpen, Ascent.Accent, Ascent.AccentSoft, diameter = 40, iconSize = 20)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.set_bank_current, BANK_VERSION),
                                color = Ascent.Fg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.set_bank_meta, bankSourceText(BankRepository.sourceKind, BankRepository.bankVersion), ALL_LESSONS.size, TOTAL_QUESTIONS),
                                color = Ascent.Fg3,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (bankMessage.isNotBlank()) {
                        Text(bankMessage, color = Ascent.Accent, fontSize = 11.sp, lineHeight = 17.sp)
                    }
                    // 两个按钮竖排：俄/法/德/西/意 的按钮文案较长，
                    // 并排在窄屏上会换行挤爆 44dp 的高度。
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PillButton(
                            text = if (bankChecking) stringResource(R.string.set_bank_checking) else stringResource(R.string.set_bank_check),
                            style = BtnStyle.Primary,
                            height = 46,
                            enabled = !bankChecking,
                            onClick = onCheckBank
                        )
                        PillButton(
                            text = if (confirmBankReset) stringResource(R.string.set_bank_confirm_restore) else stringResource(R.string.set_bank_restore),
                            style = BtnStyle.Secondary,
                            height = 46,
                            onClick = {
                                if (confirmBankReset) {
                                    onResetBank()
                                    confirmBankReset = false
                                } else {
                                    confirmBankReset = true
                                }
                            }
                        )
                    }
                    Text(
                        text = stringResource(R.string.set_bank_note),
                        color = Ascent.Fg3,
                        fontSize = 10.sp,
                        lineHeight = 16.sp
                    )
                    if (devUnlocked) {
                        Text(stringResource(R.string.set_bank_url), color = Ascent.Fg3, fontSize = 11.sp)
                        OutlinedTextField(
                            value = bankUrlDraft,
                            onValueChange = { bankUrlDraft = it },
                            singleLine = true,
                            placeholder = { Text("https://…/ascent-bank", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        PillButton(
                            text = stringResource(R.string.set_bank_save_check),
                            style = BtnStyle.Secondary,
                            height = 42,
                            onClick = {
                                onBankUrlChange(bankUrlDraft)
                                onCheckBank()
                            }
                        )
                    }
                }
            }

            // ---------------- 开发者模式 ----------------
            item(key = "lbDev") { MonoLabel(stringResource(R.string.set_section_dev), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "dev") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    if (!devUnlocked) {
                        Text(
                            stringResource(R.string.set_dev_locked),
                            color = Ascent.Fg,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.set_dev_hint),
                            color = Ascent.Fg3,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    } else {
                        Text(stringResource(R.string.set_dev_on), color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.set_dev_on_desc), color = Ascent.Fg3, fontSize = 11.sp)
                        SettingsToggle(
                            title = stringResource(R.string.set_dev_unlock_all),
                            subtitle = stringResource(R.string.set_dev_unlock_all_desc),
                            checked = devUnlockAll,
                            onChange = onDevUnlockAllChange
                        )
                        SettingsToggle(
                            title = stringResource(R.string.set_dev_show_answers),
                            subtitle = stringResource(R.string.set_dev_show_answers_desc),
                            checked = devShowAnswers,
                            onChange = onDevShowAnswersChange
                        )
                        PillButton(
                            text = stringResource(R.string.set_dev_achievements, ACHIEVEMENTS.size),
                            style = BtnStyle.Secondary,
                            height = 44,
                            onClick = onOpenAchievements
                        )
                        PillButton(
                            text = "3D 徽章 POC（first_step）",
                            style = BtnStyle.Secondary,
                            height = 44,
                            onClick = onOpenBadge3D
                        )
                        PillButton(
                            text = stringResource(R.string.set_dev_exit),
                            style = BtnStyle.Secondary,
                            height = 42,
                            onClick = onDevExit
                        )
                    }
                }
            }

            // ---------------- 数据 ----------------
            item(key = "lbData") { MonoLabel(stringResource(R.string.set_section_data), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "data") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    Text(
                        stringResource(R.string.set_reset),
                        color = Ascent.Fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.set_reset_desc),
                        color = Ascent.Fg3,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                    PillButton(
                        text = if (confirmReset) stringResource(R.string.set_reset_confirm) else stringResource(R.string.set_reset),
                        style = BtnStyle.Danger,
                        height = 44,
                        onClick = {
                            if (confirmReset) {
                                onReset()
                                confirmReset = false
                            } else {
                                confirmReset = true
                            }
                        }
                    )
                }
            }

            // ---------------- 关于 ----------------
            item(key = "lbAbout") { MonoLabel(stringResource(R.string.set_section_about), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "about") {
                OutlineCard(gap = 4, modifier = Modifier.padding(bottom = 14.dp)) {
                    Text(
                        text = stringResource(R.string.set_about_name),
                        color = Ascent.Fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.set_about_desc),
                        color = Ascent.Fg3,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.set_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        color = Ascent.Fg2,
                        fontSize = 12.sp,
                        fontFamily = MonoNumbers,
                        modifier = Modifier.clickable {
                            if (!devUnlocked) {
                                val now = System.currentTimeMillis()
                                tapCount = if (now - lastTapAt > 1600L) 1 else tapCount + 1
                                lastTapAt = now
                                if (tapCount >= 7) {
                                    tapCount = 0
                                    onDevUnlocked()
                                }
                            }
                        }
                    )
                    if (!devUnlocked && tapCount >= 3) {
                        Text(
                            text = stringResource(R.string.dev_tap_hint, 7 - tapCount),
                            color = Ascent.Accent,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Ascent.Fg3, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
