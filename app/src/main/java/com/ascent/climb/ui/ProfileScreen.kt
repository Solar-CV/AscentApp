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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.BuildConfig
import com.ascent.climb.data.ACHIEVEMENTS
import com.ascent.climb.data.AchCategory
import com.ascent.climb.data.Account
import com.ascent.climb.data.BANK_VERSION
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.TOTAL_QUESTIONS
import com.ascent.climb.data.bank.BankRepository
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.AuthResult
import com.ascent.climb.data.MockAuthApi
import com.ascent.climb.data.SUMMIT_ALTITUDE
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    altitude: Int,
    streak: Int,
    completed: Set<String>,
    store: ProgressStore,
    achRevision: Int,
    devUnlocked: Boolean,
    devUnlockAll: Boolean,
    devShowAnswers: Boolean,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onDevUnlocked: () -> Unit,
    onDevUnlockAllChange: (Boolean) -> Unit,
    onDevShowAnswersChange: (Boolean) -> Unit,
    onDevExit: () -> Unit,
    bankChecking: Boolean,
    bankMessage: String,
    onCheckBank: () -> Unit,
    onResetBank: () -> Unit,
    onBankUrlChange: (String) -> Unit,
    onReset: () -> Unit,
    onAchChanged: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { MockAuthApi(context) }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(auth.currentAccount()) }
    var showAuth by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showProfileHome by remember { mutableStateOf(false) }
    var showBadgeWall by remember { mutableStateOf(false) }

    if (showBadgeWall) {
        BadgeWallScreen(
            store = store,
            achRevision = achRevision,
            onBack = { showBadgeWall = false }
        )
    } else if (showProfileHome) {
        ProfileHomeScreen(
            store = store,
            altitude = altitude,
            streak = streak,
            completed = completed,
            achRevision = achRevision,
            onBack = { showProfileHome = false },
            onOpenAllBadges = { showBadgeWall = true }
        )
    } else if (showSettings) {
        SettingsScreen(
            store = store,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            devUnlocked = devUnlocked,
            devUnlockAll = devUnlockAll,
            devShowAnswers = devShowAnswers,
            onDevUnlocked = onDevUnlocked,
            onDevExit = onDevExit,
            onDevUnlockAllChange = onDevUnlockAllChange,
            onDevShowAnswersChange = onDevShowAnswersChange,
            onOpenAchievements = { showAchievements = true },
            bankChecking = bankChecking,
            bankMessage = bankMessage,
            onCheckBank = onCheckBank,
            onResetBank = onResetBank,
            onBankUrlChange = onBankUrlChange,
            onReset = onReset,
            onBack = { showSettings = false }
        )
    } else if (showAchievements) {
        AchievementsDevScreen(
            store = store,
            onBack = { showAchievements = false },
            onChanged = onAchChanged
        )
    } else if (showAuth) {
        AuthScreen(
            auth = auth,
            onBack = { showAuth = false },
            onSuccess = {
                account = it
                showAuth = false
            }
        )
    } else {
        ProfileBody(
            altitude = altitude,
            streak = streak,
            completed = completed,
            account = account,
            store = store,
            achRevision = achRevision,
            devUnlocked = devUnlocked,
            devUnlockAll = devUnlockAll,
            devShowAnswers = devShowAnswers,
            onDevUnlocked = onDevUnlocked,
            onDevUnlockAllChange = onDevUnlockAllChange,
            onDevShowAnswersChange = onDevShowAnswersChange,
            onDevExit = onDevExit,
            bankChecking = bankChecking,
            bankMessage = bankMessage,
            onCheckBank = onCheckBank,
            onResetBank = onResetBank,
            onBankUrlChange = onBankUrlChange,
            onLoginClick = { showAuth = true },
            onLogout = {
                scope.launch {
                    auth.logout()
                    account = null
                }
            },
            onOpenSettings = { showSettings = true },
            onOpenProfileHome = { showProfileHome = true },
            onReset = onReset
        )
    }
}

@Composable
private fun ProfileBody(
    altitude: Int,
    streak: Int,
    completed: Set<String>,
    account: Account?,
    store: ProgressStore,
    achRevision: Int,
    devUnlocked: Boolean,
    devUnlockAll: Boolean,
    devShowAnswers: Boolean,
    onDevUnlocked: () -> Unit,
    onDevUnlockAllChange: (Boolean) -> Unit,
    onDevShowAnswersChange: (Boolean) -> Unit,
    onDevExit: () -> Unit,
    bankChecking: Boolean,
    bankMessage: String,
    onCheckBank: () -> Unit,
    onResetBank: () -> Unit,
    onBankUrlChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfileHome: () -> Unit,
    onReset: () -> Unit
) {
    var bankUrlDraft by remember { mutableStateOf(BankRepository.baseUrl) }
    // 隐藏入口：连点版本号 7 次
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val tapHint = if (!devUnlocked && tapCount >= 3) {
        "再点击 ${7 - tapCount} 次解锁开发者模式"
    } else {
        null
    }
    val onVersionTap: () -> Unit = {
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

    fun unitFraction(unitId: String): Float {
        val u = ALL_UNITS.firstOrNull { it.id == unitId } ?: return 0f
        if (u.lessons.isEmpty()) return 0f
        return u.lessons.count { it.id in completed }.toFloat() / u.lessons.size.toFloat()
    }

    val overall = if (ALL_LESSONS.isEmpty()) 0f
    else completed.size.toFloat() / ALL_LESSONS.size.toFloat()

    val caps = listOf(
        "5000 入门" to unitFraction("m5i"),
        "5000 进阶" to unitFraction("m5a"),
        "6000 米" to unitFraction("m6"),
        "7000 米" to unitFraction("m7"),
        "8000 米" to unitFraction("m8")
    )

    val unlockedAch = remember(achRevision) { store.unlockedAchievements.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        // ---------------- 档案头 ----------------
        item(key = "head") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onOpenProfileHome() }
                ) {
                    IconBadge(
                        icon = Ic.UserRound,
                        tint = Ascent.Accent,
                        background = Ascent.AccentSoft,
                        diameter = 56,
                        iconSize = 28
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account?.nickname ?: "未登录",
                        color = Ascent.Fg,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "理论路线 · ${completed.size} / ${ALL_LESSONS.size} 课完成",
                        color = Ascent.Fg3,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Ic.Settings, contentDescription = "设置", tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ---------------- 海拔 ----------------
        item(key = "alt") {
            val frac = if (SUMMIT_ALTITUDE == 0) 0f else altitude.toFloat() / SUMMIT_ALTITUDE.toFloat()
            OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("当前海拔", color = Ascent.Fg3, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatMeters(altitude),
                                color = Ascent.Fg,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = MonoNumbers
                            )
                            Text(
                                text = "M / ${formatMeters(SUMMIT_ALTITUDE)}",
                                color = Ascent.Fg3,
                                fontSize = 13.sp,
                                fontFamily = MonoNumbers,
                                modifier = Modifier.padding(start = 5.dp, bottom = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "距顶峰 ${formatMeters((SUMMIT_ALTITUDE - altitude).coerceAtLeast(0))} m",
                        color = Ascent.Fg2,
                        fontSize = 12.sp
                    )
                }
                AscentBar(fraction = frac, height = 8, fill = Ascent.Accent)
            }
        }

        // ---------------- 能力概览 ----------------
        item(key = "caps") {
            OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                Text("能力概览", color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                caps.forEach { (label, v) ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = Ascent.Fg2, fontSize = 12.sp)
                            Text(
                                text = "${(v * 100).toInt()}%",
                                color = Ascent.Fg3,
                                fontSize = 11.sp,
                                fontFamily = MonoNumbers
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        AscentBar(fraction = v, height = 6, fill = Ascent.Accent)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = "技术维度需教练现场评估，不计入自动评分。",
                    color = Ascent.Fg3,
                    fontSize = 10.sp
                )
            }
        }

        // ---------------- 成就徽章（紧凑入口；详情在个人主页 / 全部徽章） --------
        item(key = "badges") {
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(shape)
                    .background(Ascent.Surface)
                    .border(1.dp, Ascent.Border, shape)
                    .clickable { onOpenProfileHome() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(Ic.Award, Ascent.Accent, Ascent.AccentSoft, diameter = 40, iconSize = 20)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "成就徽章 · ${ACHIEVEMENTS.count { it.id in unlockedAch }} / ${ACHIEVEMENTS.size}",
                        color = Ascent.Fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text("点头像进入个人主页查看已解锁徽章", color = Ascent.Fg3, fontSize = 11.sp)
                }
                Icon(Ic.ChevronRight, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
            }
        }

        // ---------------- 账号 ----------------
        item(key = "account") {
            MonoLabel("账号", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
        }
        item(key = "accountCard") {
            if (account == null) {
                val shape = RoundedCornerShape(20.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Ascent.Surface)
                        .border(1.dp, Ascent.Border, shape)
                        .clickable { onLoginClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Ic.UserRound, Ascent.Fg2, Ascent.Surface2, diameter = 46, iconSize = 22)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("未登录", color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text("登录后可同步学习进度与徽章", color = Ascent.Fg3, fontSize = 12.sp)
                    }
                    SoftTag("登录 / 注册", Ascent.OnAccent, Ascent.Accent)
                }
                Spacer(Modifier.height(14.dp))
            } else {
                OutlineCard(gap = 14, modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Ascent.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account.nickname.take(1).uppercase(),
                                color = Ascent.OnAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.nickname, color = Ascent.Fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Text(account.email, color = Ascent.Fg3, fontSize = 12.sp)
                        }
                    }
                    PillButton(text = "退出登录", style = BtnStyle.Secondary, height = 44, onClick = onLogout)
                }
            }
        }

        // ---------------- 声明 ----------------
        item(key = "disc") {
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
                Icon(Ic.TriangleAlert, contentDescription = null, tint = Ascent.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("理论掌握不等于实际操作能力", color = Ascent.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "本应用记录的是理论学习进度。冰雪、绳索、攀冰、救援等实践技能，" +
                            "必须由持证教练现场评估后，才能在真实环境中使用。涉及药物与急救的决策须咨询专业医生。",
                        color = Ascent.Fg2,
                        fontSize = 12.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // ---------------- 重置 + 版本 ----------------
        item(key = "reset") {
            Spacer(Modifier.height(8.dp))
            val shape = RoundedCornerShape(999.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Ascent.DangerSoft)
                    .clickable { onReset() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("重置全部学习进度", color = Ascent.Danger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        item(key = "version") {
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVersionTap() }
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ASCENT 攀登 · v${BuildConfig.VERSION_NAME}", color = Ascent.Fg3, fontSize = 11.sp)
                if (tapHint != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(tapHint, color = Ascent.Warning, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun DevToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ascent.Fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Ascent.Fg3, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ==================== 登录 / 注册 ====================

private enum class AuthMode { Login, Register }

@Composable
private fun AuthScreen(
    auth: MockAuthApi,
    onBack: () -> Unit,
    onSuccess: (Account) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 系统返回 / 全面屏侧滑手势：关掉登录页，而不是退出 App
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { onBack() }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("返回", color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(26.dp))
        Icon(Ic.MountainSnow, contentDescription = null, tint = Ascent.Accent, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (mode == AuthMode.Login) "登录 ASCENT" else "创建账号",
            color = Ascent.Fg,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (mode == AuthMode.Login) {
                "同步你的海拔、徽章与学习记录"
            } else {
                "注册后可在多设备间同步进度"
            },
            color = Ascent.Fg3,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        // 模式切换（分段控件）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Ascent.Surface2)
                .padding(4.dp)
        ) {
            listOf(AuthMode.Login to "登录", AuthMode.Register to "注册").forEach { (m, label) ->
                val on = mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) Ascent.Surface else Ascent.Surface2)
                        .clickable {
                            mode = m
                            error = null
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (on) Ascent.Fg else Ascent.Fg2,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (mode == AuthMode.Register) {
            AuthField(
                value = nickname,
                onChange = { nickname = it; error = null },
                label = "昵称",
                keyboardType = KeyboardType.Text
            )
            Spacer(Modifier.height(12.dp))
        }

        AuthField(
            value = email,
            onChange = { email = it; error = null },
            label = "邮箱",
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))

        AuthField(
            value = password,
            onChange = { password = it; error = null },
            label = "密码（至少 6 位）",
            keyboardType = KeyboardType.Password,
            isPassword = true
        )

        if (error != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ascent.DangerSoft)
                    .padding(13.dp)
            ) {
                Text(text = error!!, color = Ascent.Danger, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        PillButton(
            text = when {
                loading -> "请稍候…"
                mode == AuthMode.Login -> "登录"
                else -> "注册并登录"
            },
            enabled = !loading,
            onClick = {
                if (loading) return@PillButton
                loading = true
                error = null
                scope.launch {
                    val result = if (mode == AuthMode.Login) {
                        auth.login(email, password)
                    } else {
                        auth.register(email, password, nickname)
                    }
                    loading = false
                    when (result) {
                        is AuthResult.Success -> onSuccess(result.account)
                        is AuthResult.Failure -> error = result.message
                    }
                }
            }
        )

        Spacer(Modifier.height(18.dp))
        val shape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Ascent.AccentSoft)
                .padding(13.dp)
        ) {
            Text(
                text = "当前为本地模拟登录接口（MockAuthApi），不会联网、不上传任何数据。" +
                    "后端就绪后替换 AuthApi 的实现即可，界面无需改动。",
                color = Ascent.Fg2,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}
