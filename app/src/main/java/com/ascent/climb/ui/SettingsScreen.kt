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
import com.ascent.climb.BuildConfig
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
    onBack: () -> Unit
) {
    var bankUrlDraft by remember { mutableStateOf(BankRepository.baseUrl) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmBankReset by remember { mutableStateOf(false) }
    // 隐藏入口：连点版本号 7 次
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }

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
                    Text("返回", color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text("设置", color = Ascent.Fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
            }

            // ---------------- 外观 ----------------
            item(key = "lbLook") { MonoLabel("外观", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
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
                            Text("主题", color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = when (themeMode) {
                                    "light" -> "始终浅色"
                                    "dark" -> "始终深色"
                                    else -> "跟随系统（当前${if (Ascent.IsDark) "深色" else "浅色"}）"
                                },
                                color = Ascent.Fg3,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("auto" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (v, label) ->
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

            // ---------------- 题库 ----------------
            item(key = "lbBank") { MonoLabel("题库", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "bank") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Ic.BookOpen, Ascent.Accent, Ascent.AccentSoft, diameter = 40, iconSize = 20)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "当前题库 v$BANK_VERSION",
                                color = Ascent.Fg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${BankRepository.sourceDescription} · ${ALL_LESSONS.size} 课 · $TOTAL_QUESTIONS 题",
                                color = Ascent.Fg3,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (bankMessage.isNotBlank()) {
                        Text(bankMessage, color = Ascent.Accent, fontSize = 11.sp, lineHeight = 17.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            PillButton(
                                text = if (bankChecking) "检查中…" else "检查更新",
                                style = BtnStyle.Primary,
                                height = 44,
                                enabled = !bankChecking,
                                onClick = onCheckBank
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PillButton(
                                text = if (confirmBankReset) "确认恢复？" else "恢复内置",
                                style = BtnStyle.Secondary,
                                height = 44,
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
                    }
                    Text(
                        text = "有网时自动检查更新；离线时始终使用本地题库，功能不受影响。",
                        color = Ascent.Fg3,
                        fontSize = 10.sp,
                        lineHeight = 16.sp
                    )
                    if (devUnlocked) {
                        Text("在线题库地址", color = Ascent.Fg3, fontSize = 11.sp)
                        OutlinedTextField(
                            value = bankUrlDraft,
                            onValueChange = { bankUrlDraft = it },
                            singleLine = true,
                            placeholder = { Text("https://…/ascent-bank", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        PillButton(
                            text = "保存并检查",
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
            item(key = "lbDev") { MonoLabel("开发者模式", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "dev") {
                OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                    if (!devUnlocked) {
                        Text(
                            "未开启 · 连点下方版本号 7 次开启",
                            color = Ascent.Fg,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "开发者模式仅用于测试：可解锁全部关卡、显示答案、进入成就测试区。",
                            color = Ascent.Fg3,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    } else {
                        Text("开发者模式已开启", color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("仅用于测试，不影响正式进度记录", color = Ascent.Fg3, fontSize = 11.sp)
                        SettingsToggle(
                            title = "解锁全部关卡",
                            subtitle = "跳过前置要求，所有课程可直接进入",
                            checked = devUnlockAll,
                            onChange = onDevUnlockAllChange
                        )
                        SettingsToggle(
                            title = "显示正确答案",
                            subtitle = "答题前在正确选项旁标记 KEY",
                            checked = devShowAnswers,
                            onChange = onDevShowAnswersChange
                        )
                        PillButton(
                            text = "成就测试区（${ACHIEVEMENTS.size} 项）",
                            style = BtnStyle.Secondary,
                            height = 44,
                            onClick = onOpenAchievements
                        )
                        PillButton(
                            text = "退出开发者模式",
                            style = BtnStyle.Secondary,
                            height = 42,
                            onClick = onDevExit
                        )
                    }
                }
            }

            // ---------------- 数据 ----------------
            item(key = "lbData") { MonoLabel("数据", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "data") {
                OutlineCard(gap = 10, modifier = Modifier.padding(bottom = 14.dp)) {
                    Text(
                        "重置学习进度",
                        color = Ascent.Fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "清空课程完成记录、星级、海拔与学习统计；成就记录也会一并清空。此操作不可撤销。",
                        color = Ascent.Fg3,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                    PillButton(
                        text = if (confirmReset) "确认重置？此操作不可撤销" else "重置学习进度",
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
            item(key = "lbAbout") { MonoLabel("关于", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) }
            item(key = "about") {
                OutlineCard(gap = 4, modifier = Modifier.padding(bottom = 14.dp)) {
                    Text(
                        text = "ASCENT 攀登之路",
                        color = Ascent.Fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "高海拔攀登 / 攀冰理论学习",
                        color = Ascent.Fg3,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "版本 v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
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
                            text = "再点击 ${7 - tapCount} 次解锁开发者模式",
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
