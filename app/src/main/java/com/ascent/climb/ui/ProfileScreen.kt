package com.ascent.climb.ui

import com.ascent.climb.R
import androidx.compose.ui.res.stringResource
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
import com.ascent.climb.data.ALL_TRACKS
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.AuthResult
import com.ascent.climb.data.CLIMB_TRACK
import com.ascent.climb.data.TREK_TRACK
import com.ascent.climb.data.MockAuthApi
import com.ascent.climb.data.activeTrackId
import com.ascent.climb.data.displaySummitOf
import kotlinx.coroutines.launch

@Composable
private fun trackLabelOf(trackId: String): String = when (trackId) {
    TREK_TRACK -> stringResource(R.string.path_track_trek)
    CLIMB_TRACK -> stringResource(R.string.path_track_climb)
    else -> trackId
}

/** 单元名：攀登是 5000 入门 / 6000 m 一类，徒步是一日徒步 / 高原徒步一类。 */
@Composable
private fun unitLabelOf(unitId: String, fallback: String): String = when (unitId) {
    "m5i" -> stringResource(R.string.cap_m5i)
    "m5a" -> stringResource(R.string.cap_m5a)
    "m6" -> stringResource(R.string.cap_m6)
    "m8" -> stringResource(R.string.cap_m8)
    "t1" -> stringResource(R.string.cap_t1)
    "t2" -> stringResource(R.string.cap_t2)
    "t3" -> stringResource(R.string.cap_t3)
    "t4" -> stringResource(R.string.cap_t4)
    else -> fallback
}

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
    languageMode: String,
    onLanguageModeChange: (String) -> Unit,
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
    // 注意分支顺序：成就测试页是从设置页里点开的，必须排在 showSettings 之前判断。
    // 否则 showSettings 仍为 true，先命中设置页，点了按钮界面纹丝不动
    //（曾经就是这个顺序导致「成就测试点不进去」）。
    } else if (showAchievements) {
        AchievementsDevScreen(
            store = store,
            onBack = { showAchievements = false },
            onChanged = onAchChanged
        )
    } else if (showSettings) {
        SettingsScreen(
            store = store,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            languageMode = languageMode,
            onLanguageModeChange = onLanguageModeChange,
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
        stringResource(R.string.dev_tap_hint, 7 - tapCount)
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

    fun unitFraction(total: Int, done: Int): Float = if (total == 0) 0f else done.toFloat() / total

    val overall = if (ALL_LESSONS.isEmpty()) 0f
    else completed.size.toFloat() / ALL_LESSONS.size.toFloat()

    // 能力概览按课程大类分组：两条轨道的单元都列出来，各自的进度独立计算。
    val capsByTrack = ALL_TRACKS.map { track ->
        track to track.units.map { u ->
            unitLabelOf(u.id, u.title) to unitFraction(u.lessons.size, u.lessons.count { it.id in completed })
        }
    }

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
                        text = account?.nickname ?: stringResource(R.string.prof_not_logged_in),
                        color = Ascent.Fg,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.prof_route_meta, completed.size, ALL_LESSONS.size),
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
                    Icon(Ic.Settings, contentDescription = stringResource(R.string.prof_settings), tint = Ascent.Fg2, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ---------------- 海拔 ----------------
        item(key = "alt") {
            // 目标刻度跟着当前大类走：攀登 8,000，徒步用自己路线的最高点
            val summitAlt = displaySummitOf(activeTrackId())
            val frac = if (summitAlt == 0) 0f else altitude.toFloat() / summitAlt.toFloat()
            OutlineCard(gap = 12, modifier = Modifier.padding(bottom = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.prof_current_altitude), color = Ascent.Fg3, fontSize = 12.sp)
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
                                text = "M / ${formatMeters(summitAlt)}",
                                color = Ascent.Fg3,
                                fontSize = 13.sp,
                                fontFamily = MonoNumbers,
                                modifier = Modifier.padding(start = 5.dp, bottom = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.path_to_summit, formatMeters((summitAlt - altitude).coerceAtLeast(0))),
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
                Text(stringResource(R.string.prof_abilities), color = Ascent.Fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                capsByTrack.forEachIndexed { trackIndex, (track, bars) ->
                    Column(
                        modifier = Modifier.padding(top = if (trackIndex == 0) 0.dp else 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 大类小标题：说明下面这几条属于哪套课程
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trackLabelOf(track.id),
                                color = Ascent.Accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val tTotal = track.units.sumOf { it.lessons.size }
                            val tDone = track.units.sumOf { u -> u.lessons.count { it.id in completed } }
                            Text(
                                text = "${if (tTotal == 0) 0 else tDone * 100 / tTotal}% · $tDone/$tTotal",
                                color = Ascent.Fg3,
                                fontSize = 10.sp,
                                fontFamily = MonoNumbers
                            )
                        }
                        bars.forEach { (label, v) ->
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
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.prof_ability_note),
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
                        stringResource(R.string.prof_badges_meta, ACHIEVEMENTS.count { it.id in unlockedAch }, ACHIEVEMENTS.size),
                        color = Ascent.Fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(stringResource(R.string.prof_badges_hint), color = Ascent.Fg3, fontSize = 11.sp)
                }
                Icon(Ic.ChevronRight, contentDescription = null, tint = Ascent.Fg3, modifier = Modifier.size(16.dp))
            }
        }

        // ---------------- 账号 ----------------
        item(key = "account") {
            MonoLabel(stringResource(R.string.prof_section_account), modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
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
                        Text(stringResource(R.string.prof_not_logged_in), color = Ascent.Fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(stringResource(R.string.prof_login_mock), color = Ascent.Fg3, fontSize = 12.sp)
                    }
                    SoftTag(stringResource(R.string.prof_login_register), Ascent.OnAccent, Ascent.Accent)
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
                    PillButton(text = stringResource(R.string.prof_logout), style = BtnStyle.Secondary, height = 44, onClick = onLogout)
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
                    Text(stringResource(R.string.prof_disclaimer_title), color = Ascent.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = stringResource(R.string.prof_disclaimer_body),
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
                Text(stringResource(R.string.prof_reset_all), color = Ascent.Danger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                Text(stringResource(R.string.prof_version, BuildConfig.VERSION_NAME), color = Ascent.Fg3, fontSize = 11.sp)
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
            Text(stringResource(R.string.common_back), color = Ascent.Fg2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(26.dp))
        Icon(Ic.MountainSnow, contentDescription = null, tint = Ascent.Accent, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (mode == AuthMode.Login) stringResource(R.string.auth_login_title) else stringResource(R.string.auth_register_title),
            color = Ascent.Fg,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (mode == AuthMode.Login) {
                stringResource(R.string.auth_login_note)
            } else {
                stringResource(R.string.auth_register_note)
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
            listOf(AuthMode.Login to stringResource(R.string.auth_login_action), AuthMode.Register to stringResource(R.string.auth_register)).forEach { (m, label) ->
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
                label = stringResource(R.string.auth_nickname),
                keyboardType = KeyboardType.Text
            )
            Spacer(Modifier.height(12.dp))
        }

        AuthField(
            value = email,
            onChange = { email = it; error = null },
            label = stringResource(R.string.auth_email),
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))

        AuthField(
            value = password,
            onChange = { password = it; error = null },
            label = stringResource(R.string.auth_password),
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
                loading -> stringResource(R.string.auth_wait)
                mode == AuthMode.Login -> stringResource(R.string.auth_login_action)
                else -> stringResource(R.string.auth_register_action)
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
                text = stringResource(R.string.auth_mock_note) +
                    stringResource(R.string.auth_backend_note),
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
