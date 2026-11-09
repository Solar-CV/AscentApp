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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascent.climb.BuildConfig
import com.ascent.climb.R
import com.ascent.climb.i18n.AppLocale
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.BANK_VERSION
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.TOTAL_QUESTIONS
import com.ascent.climb.data.bank.BankRepository
import com.ascent.climb.data.ALL_LESSONS
import kotlinx.coroutines.launch

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
    weatherAuto: Boolean,
    onWeatherAutoChange: (Boolean) -> Unit,
    devUnlocked: Boolean,
    devUnlockAll: Boolean,
    devShowAnswers: Boolean,
    onDevUnlocked: () -> Unit,
    onDevExit: () -> Unit,
    onDevUnlockAllChange: (Boolean) -> Unit,
    onDevShowAnswersChange: (Boolean) -> Unit,
    onOpenAchievements: () -> Unit,
    bankChecking: Boolean,
    bankMessage: String,
    onCheckBank: () -> Unit,
    onResetBank: () -> Unit,
    onBankUrlChange: (String) -> Unit,
    onReset: () -> Unit,
    /** 当前登录账号的邮箱；null 表示未登录，此时不显示账号分区。 */
    accountEmail: String?,
    /** 打开应用内隐私政策。 */
    onOpenPrivacy: () -> Unit,
    /** 打开「已登录设备」页。 */
    onOpenDevices: () -> Unit,
    /** 执行删除账号（含本机数据），返回是否成功。 */
    onDeleteAccount: suspend () -> Boolean,
    onBack: () -> Unit
) {
    var bankUrlDraft by remember { mutableStateOf(BankRepository.baseUrl) }
    // 两个危险操作都走同一个确认弹框组件；重置是同步的，删除要等网络/落盘
    var showReset by remember { mutableStateOf(false) }
    var confirmBankReset by remember { mutableStateOf(false) }
    // 删除账号：弹确认框，删除中禁掉两个按钮，失败留在框里可重试
    var showDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteFailed by remember { mutableStateOf(false) }
    // 语言选择页（独立二级页面）
    var showLanguage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 必须放在 showLanguage 的提前返回之前：进语言页时这一层列表会离开组合，
    // 状态留在这里才不会跟着丢，返回时还在原位。
    val listState = rememberLazyListState()
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

    if (showReset) {
        DangerConfirmDialog(
            title = stringResource(R.string.set_reset_title),
            body = stringResource(R.string.set_reset_desc),
            confirmText = stringResource(R.string.set_reset_action),
            onCancel = { showReset = false },
            onConfirm = {
                showReset = false
                onReset()
            }
        )
    }

    if (showDelete) {
        DangerConfirmDialog(
            title = stringResource(R.string.acc_delete_title),
            body = stringResource(R.string.acc_delete_body, accountEmail ?: ""),
            confirmText = stringResource(R.string.acc_delete_action),
            busy = deleting,
            busyText = stringResource(R.string.auth_wait),
            error = if (deleteFailed) stringResource(R.string.acc_delete_failed) else null,
            onCancel = {
                if (!deleting) {
                    showDelete = false
                    deleteFailed = false
                }
            },
            onConfirm = {
                scope.launch {
                    deleting = true
                    deleteFailed = false
                    val ok = onDeleteAccount()
                    deleting = false
                    if (ok) showDelete = false else deleteFailed = true
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth().background(Ascent.Bg)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
        ) {
            // ---------------- 头部 ----------------
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

            // ---------------- 天气背景 ----------------
            item(key = "weather") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    SettingsToggle(
                        title = stringResource(R.string.set_weather_title),
                        subtitle = if (weatherAuto) {
                            stringResource(R.string.set_weather_auto_on)
                        } else {
                            stringResource(R.string.set_weather_auto_off)
                        },
                        checked = weatherAuto,
                        onChange = onWeatherAutoChange
                    )
                    // CC BY 4.0 要求署名，删掉这行就违反了 Open-Meteo 的许可。
                    Text(
                        text = "Weather data by Open-Meteo.com",
                        color = Ascent.Fg3,
                        fontSize = 10.sp
                    )
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
                            shape = squircle(12.dp),
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
            // 未解锁时整块不渲染（连标题一起藏）：这个分区只对已解锁的人有意义，
            // 留着「未开启 / 连点版本号 7 次」等于把隐藏入口写在明面上。
            if (devUnlocked) {
                item(key = "lbDev") { MonoLabel(stringResource(R.string.set_section_dev), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
                item(key = "dev") {
                    OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
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
                        text = stringResource(R.string.set_reset),
                        style = BtnStyle.Danger,
                        height = 44,
                        onClick = { showReset = true }
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
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(squircle(10.dp))
                            .clickable { onOpenPrivacy() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.set_privacy),
                            color = Ascent.Fg2,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Ic.ChevronRight,
                            contentDescription = null,
                            tint = Ascent.Fg3,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                }
            }

            // ---------------- 账号 ----------------
            // 放最底部：破坏性操作不该在进设置的第一屏就能点到。
            // 未登录时整块不渲染 —— 没有账号可删，留着只会让人以为要先登录再删。
            if (accountEmail != null) {
                item(key = "lbAccount") {
                    MonoLabel(
                        stringResource(R.string.set_section_account),
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                    )
                }
                item(key = "devices") {
                    OutlineCard(
                        gap = 0,
                        modifier = Modifier.padding(bottom = 10.dp),
                        onClick = onOpenDevices
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(Ic.Signal, Ascent.Accent, Ascent.AccentSoft, diameter = 40, iconSize = 20)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                // 设备管理这一行的文案在德语 / 俄语 / 西语下比中文长一倍多，
                                // 不限行会把卡片撑成三四行高，和上下几行完全不在一个节奏上。
                                // 标题与副标都锁一行、超出省略；同时把几门语言的副标改短（见 strings）。
                                Text(
                                    stringResource(R.string.dv_title),
                                    color = Ascent.Fg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    stringResource(R.string.set_devices_hint),
                                    color = Ascent.Fg3,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                item(key = "account") {
                    OutlineCard(
                        gap = 0,
                        modifier = Modifier.padding(bottom = 14.dp),
                        onClick = { showDelete = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(Ic.TriangleAlert, Ascent.Danger, Ascent.DangerSoft, diameter = 40, iconSize = 20)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.acc_delete),
                                    color = Ascent.Danger,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(accountEmail, color = Ascent.Fg3, fontSize = 11.sp)
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
            }
        }
    }
}

/**
 * 危险操作确认框（重置进度 / 删除账号共用）。
 *
 * 用 Dialog 而不是行内「点一次变红再点一次」：这两件事都不可撤销，需要一次明确的打断
 * （遮罩 + 两个按钮 + 逐条列出后果），也避免误触背后的那一行。
 *
 * @param busy 执行中：两个按钮禁用，确认键显示 [busyText]
 * @param error 失败提示（为空不占位）
 */
@Composable
internal fun DangerConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    busy: Boolean = false,
    busyText: String? = null,
    error: String? = null
) {
    Dialog(
        onDismissRequest = { if (!busy) onCancel() },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(squircle(24.dp))
                .background(Ascent.Surface)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Ic.TriangleAlert, Ascent.Danger, Ascent.DangerSoft, diameter = 36, iconSize = 18)
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    color = Ascent.Fg,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                color = Ascent.Fg2,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = Ascent.Danger, fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PillButton(
                        text = stringResource(R.string.common_cancel),
                        style = BtnStyle.Secondary,
                        height = 46,
                        enabled = !busy,
                        onClick = onCancel
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PillButton(
                        text = if (busy && busyText != null) busyText else confirmText,
                        style = BtnStyle.Danger,
                        height = 46,
                        enabled = !busy,
                        onClick = onConfirm
                    )
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
