package com.ascent.climb.ui

import android.content.pm.ApplicationInfo
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascent.climb.BuildConfig
import com.ascent.climb.data.AchCategory
import com.ascent.climb.data.Account
import com.ascent.climb.data.BANK_VERSION
import com.ascent.climb.data.PrivacyPolicy
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.TOTAL_QUESTIONS
import com.ascent.climb.data.bank.BankRepository
import com.ascent.climb.data.ALL_LESSONS
import com.ascent.climb.data.ALL_TRACKS
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.AuthResult
import com.ascent.climb.data.CLIMB_TRACK
import com.ascent.climb.data.CodePurpose
import com.ascent.climb.data.TREK_TRACK
import com.ascent.climb.data.MockAuthApi
import com.ascent.climb.data.AuthApi
import com.ascent.climb.data.cloud.CloudAuthApi
import com.ascent.climb.i18n.AppLocale
import com.ascent.climb.data.cloud.CloudConfig
import com.ascent.climb.data.cloud.SyncEngine
import com.ascent.climb.data.cloud.SyncOutcome
import com.ascent.climb.data.cloud.TokenStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.ascent.climb.data.activeTrackId
import com.ascent.climb.data.displaySummitOf
import kotlinx.coroutines.delay
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
    // 7000 米在 v0.19 拆成 C3 技术营与 C4 高营两个单元，这里当初漏了分支，
    // 结果英文界面直接显示中文单元名（fallback 是 Curriculum 里的中文标题）
    "m7" -> stringResource(R.string.cap_m7)
    "m7h" -> stringResource(R.string.cap_m7h)
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
    onAchChanged: () -> Unit,
    /** 让上层重读进度/海拔（删号后、云同步拉回数据后都要走）。不切 tab，用户还停在「我的」页。 */
    onStoreReloaded: () -> Unit = {},
    onOpenBadge: (String) -> Unit = {}
) {
    val context = LocalContext.current
    // 配了后端就走真实登录，没配就退回本地模拟 —— 界面不需要知道区别
    val auth: AuthApi = remember {
        if (CloudConfig.enabled) CloudAuthApi(context) else MockAuthApi(context)
    }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(auth.currentAccount()) }
    // 老版本升上来时还没有 progressOwner：用本地账号镜像补一次。
    // 不补的话，升级后第一次换号会被当成「首次登录」而合并进度，正是要避免的那种串号。
    LaunchedEffect(Unit) {
        val signedIn = account ?: return@LaunchedEffect
        if (store.progressOwner.isEmpty()) store.progressOwner = signedIn.id
    }
    var showAuth by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var showProfileHome by remember { mutableStateOf(false) }
    var showBadgeWall by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    // 同步状态：「上次同步」要能随同步完成而刷新，所以用一个计数器把重算顶起来
    var syncing by remember { mutableStateOf(false) }
    var syncTick by remember { mutableIntStateOf(0) }
    var syncError by remember { mutableStateOf<String?>(null) }

    val neverText = stringResource(R.string.sync_never)
    val offlineText = stringResource(R.string.sync_offline)
    val failedText = stringResource(R.string.sync_failed)
    val lastSyncAt = remember(syncTick) { store.lastSyncAt }
    val syncLine = syncError ?: when {
        syncing -> stringResource(R.string.sync_running)
        lastSyncAt == 0L -> neverText
        else -> stringResource(R.string.sync_last, formatSyncTime(lastSyncAt))
    }

    /** 手动同步：进度 + 头像一起拉齐，完了让上层重读进度。 */
    fun runSyncNow() {
        if (syncing) return
        syncing = true
        syncError = null
        scope.launch {
            val outcome = SyncEngine(context, store).sync()
            if (outcome is SyncOutcome.Synced) AvatarSync.reconcile(context, store)
            syncing = false
            syncTick += 1
            when (outcome) {
                is SyncOutcome.Synced -> {
                    syncError = null
                    onStoreReloaded()
                }
                SyncOutcome.Offline -> syncError = offlineText
                SyncOutcome.NotLoggedIn -> syncError = failedText
                is SyncOutcome.Failed -> syncError = failedText
            }
        }
    }

    // 列表滚动位置提到这里：切到设置/成就/反馈等二级页时，ProfileBody 会离开组合，
    // 状态若留在它内部就会被丢掉，返回时弹回顶部。
    val listState = rememberLazyListState()

    // 个人主页 ↔ 全部徽章墙 也是互斥分支，回来时同样要保住列表位置
    val branchStates = rememberSaveableStateHolder()

    // 隐私政策做成覆盖层而不是 else-if 分支：分支切换会把登录页从组合里摘掉，
    // 用户点「查看」再返回时，已填的邮箱/密码和勾选状态会全部丢失。
    Box(modifier = Modifier.fillMaxSize()) {
        if (showAuth) {
            AuthScreen(
                auth = auth,
                onBack = { showAuth = false },
                onOpenPrivacy = { showPrivacy = true },
                onSuccess = { newAccount, fromRegister ->
                    account = newAccount
                    showAuth = false
                    // 账号名是权威：登录/注册后都覆盖本地显示名，避免「账号 Solar、界面 NewName」。
                    store.displayName = newAccount.nickname
                    store.syncedNickname = newAccount.nickname
                    // 头像也属于账号，但规则不同：
                    //  - 换号（本机头像记在别的账号名下）→ 直接丢，绝不能让它跟着过去；
                    //  - 登录既有账号 → 以云端为准，本机那张离线时设的不要顶上去；
                    //  - 注册 → 是新建身份，本机已有的头像跟着走（下次同步会推上去）。
                    val ownedByOther = store.avatarOwner.isNotEmpty() && store.avatarOwner != newAccount.id
                    val localOnly = store.avatarOwner.isEmpty()
                    if (ownedByOther || (localOnly && !fromRegister)) {
                        AvatarSync.forgetLocal(context, store)
                    }
                    store.avatarOwner = newAccount.id
                    // 学习进度也属于账号，但规则不同：换人才覆盖。
                    // 第一次登录（本机还没归属过任何账号）保持合并，老用户升级后不会丢离线进度。
                    if (store.progressOwner.isNotEmpty() && store.progressOwner != newAccount.id) {
                        store.pendingCloudReplace = true
                    }
                    store.progressOwner = newAccount.id
                    // 登录成功立刻同步一次：本机进度推上去，云端已有的拉回来。
                    // 走 runSyncNow 是为了顺带清掉上一次的「同步失败」提示，并刷「上次同步」时间。
                    runSyncNow()
                }
            )
        } else if (showBadgeWall) {
            branchStates.SaveableStateProvider("badgeWall") {
                BadgeWallScreen(
                    store = store,
                    achRevision = achRevision,
                    onBack = { showBadgeWall = false },
                    onOpenBadge = onOpenBadge
                )
            }
        } else if (showEditProfile) {
            branchStates.SaveableStateProvider("editProfile") {
                EditProfileScreen(
                    store = store,
                    onBack = { showEditProfile = false },
                    // 登录状态下把名字推到账号；这里发起是因为 ProfileScreen 的 scope 活得更久，
                    // 编辑页一退就会被回收，放在那边发请求会被取消。
                    onNameCommitted = { name ->
                        if (CloudConfig.enabled && TokenStore.hasSession(context)) {
                            scope.launch {
                                // 服务端返回新的 user，顺手刷新账号卡上的显示名
                                CloudAuthApi(context).updateNickname(name)?.let {
                                    account = it
                                    store.syncedNickname = it.nickname
                                }
                            }
                        }
                    },
                    onAvatarChanged = { hasAvatar ->
                        if (hasAvatar) {
                            // 先记脏再推：没网也不会丢，下次同步补上
                            store.avatarDirty = true
                            scope.launch { AvatarSync.reconcile(context, store) }
                        } else {
                            scope.launch {
                                AvatarSync.removeRemote(context, store)
                                store.avatarDirty = false
                            }
                        }
                    }
                )
            }
        } else if (showProfileHome) {
            branchStates.SaveableStateProvider("home") {
                ProfileHomeScreen(
                    store = store,
                    altitude = altitude,
                    streak = streak,
                    completed = completed,
                    achRevision = achRevision,
                    onBack = { showProfileHome = false },
                    onOpenAllBadges = { showBadgeWall = true },
                    onOpenBadge = onOpenBadge,
                    onEditProfile = { showEditProfile = true },
                    fallbackName = account?.nickname ?: "",
                    // 账号已整块搬到这一页：登录态、同步状态与动作都从这里下发
                    account = account,
                    syncLine = syncLine,
                    syncing = syncing,
                    onLogin = { showAuth = true },
                    onLogout = {
                        scope.launch {
                            auth.logout()
                            // 头像属于账号：退出后必须连本机文件一起清掉，
                            // 否则下一个人打开「我的」看到的是上一个账号的脸。
                            AvatarSync.forgetLocal(context, store)
                            account = null
                        }
                    },
                    onSyncNow = { runSyncNow() }
                )
            }
        // 注意分支顺序：成就测试页是从设置页里点开的，必须排在 showSettings 之前判断。
        // 否则 showSettings 仍为 true，先命中设置页，点了按钮界面纹丝不动
        //（曾经就是这个顺序导致「成就测试点不进去」）。
        } else if (showAchievements) {
            branchStates.SaveableStateProvider("achievements") {
                AchievementsDevScreen(
                    store = store,
                    onBack = { showAchievements = false },
                    onChanged = onAchChanged,
                    onOpenBadge = onOpenBadge
                )
            }
        } else if (showFeedback) {
            branchStates.SaveableStateProvider("feedback") {
                FeedbackScreen(store = store, onBack = { showFeedback = false })
            }
        } else if (showDevices) {
            branchStates.SaveableStateProvider("devices") {
                DevicesScreen(
                    onBack = { showDevices = false },
                    onSignedOut = {
                        // 撤销的是当前设备：服务端已把 access/refresh 一起作废，
                        // 本地必须跟着退出，否则下一次请求只会收到 401。
                        scope.launch {
                            runCatching { auth.logout() }
                            AvatarSync.forgetLocal(context, store)
                            account = null
                            showDevices = false
                            showSettings = false
                            onStoreReloaded()
                        }
                    }
                )
            }
        } else if (showSettings) {
            branchStates.SaveableStateProvider("settings") {
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
                    accountEmail = account?.email,
                    onOpenPrivacy = { showPrivacy = true },
                    onOpenDevices = { showDevices = true },
                    onDeleteAccount = {
                        // 删账号 = 账号记录 + 本机数据一起清（Play 的账号删除要求：
                        // 只删登录态、把数据留着是不合格的）。头像文件不在 prefs 里，单独删。
                        val ok = auth.deleteAccount()
                        if (ok) {
                            AvatarStore.remove(context)
                            store.wipeUserData()
                            account = null
                            showSettings = false
                            onStoreReloaded()
                        }
                        ok
                    },
                    onBack = { showSettings = false }
                )
            }
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
                onOpenSettings = { showSettings = true },
                onOpenProfileHome = { showProfileHome = true },
                onOpenFeedback = { showFeedback = true },
                listState = listState
            )
        }

        // 隐私政策：盖在任何一层之上；底层页面保持组合，返回后输入与勾选都还在
        if (showPrivacy) {
            PrivacyScreen(
                doc = PrivacyPolicy.of(AppLocale.resolve(languageMode).toLanguageTag()),
                onBack = { showPrivacy = false }
            )
        }
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
    onOpenSettings: () -> Unit,
    onOpenProfileHome: () -> Unit,
    onOpenFeedback: () -> Unit,
    listState: LazyListState
) {
    var bankUrlDraft by remember { mutableStateOf(BankRepository.baseUrl) }
    // 隐藏入口：连点版本号 7 次。不显示任何提示文案 —— 连点本身就是约定俗成的入口，
    // 提示会把它暴露给所有用户。
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
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

    // completed 覆盖两条轨道，分母是当前轨道的课数，分子也必须只数当前轨道
    val doneHere = ALL_LESSONS.count { it.id in completed }
    val overall = if (ALL_LESSONS.isEmpty()) 0f
    else doneHere.toFloat() / ALL_LESSONS.size.toFloat()

    // 能力概览按课程大类分组：两条轨道的单元都列出来，各自的进度独立计算。
    val capsByTrack = ALL_TRACKS.map { track ->
        track to track.units.map { u ->
            unitLabelOf(u.id, u.title) to unitFraction(u.lessons.size, u.lessons.count { it.id in completed })
        }
    }


    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        // ---------------- 档案头 ----------------
        item(key = "head") {
            val a = clayAccents()
            // 显示名优先本地资料，其次账号昵称，都没有才用默认称呼。
            // 本地资料在「编辑资料」里改，与登录无关（登录还是模拟态，等后端）。
            val defaultName = stringResource(R.string.pf_default_name)
            val shownName = store.displayName.ifBlank { account?.nickname ?: defaultName }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    version = store.avatarVersion,
                    name = shownName,
                    size = 56.dp,
                    modifier = Modifier.clickable { onOpenProfileHome() }
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shownName,
                        color = a.value,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.prof_route_meta, doneHere, ALL_LESSONS.size),
                        color = a.sub,
                        fontSize = 12.sp
                    )
                }
                ClaySurface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onOpenSettings() },
                    radius = 14.dp,
                    elevation = 5.dp
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Ic.Settings,
                            contentDescription = stringResource(R.string.prof_settings),
                            tint = a.label,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // ---------------- 海拔 ----------------
        item(key = "alt") {
            // 目标刻度跟着当前大类走：攀登 8,000，徒步用自己路线的最高点
            val summitAlt = displaySummitOf(activeTrackId())
            val frac = if (summitAlt == 0) 0f else (altitude.toFloat() / summitAlt.toFloat()).coerceIn(0f, 1f)
            val a = clayAccents()
            ClayColumn(
                modifier = Modifier.padding(bottom = 14.dp),
                radius = 24.dp,
                elevation = 9.dp,
                padding = 18.dp,
                gap = 12.dp,
                dark = true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.prof_current_altitude), color = a.heroText2, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatMeters(altitude),
                                color = a.heroText,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MonoNumbers
                            )
                            Text(
                                text = "M / ${formatMeters(summitAlt)}",
                                color = a.heroText2,
                                fontSize = 13.sp,
                                fontFamily = MonoNumbers,
                                modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.path_to_summit, formatMeters((summitAlt - altitude).coerceAtLeast(0))),
                        color = a.heroText2,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                ClayProgress(
                    ratio = frac,
                    color = a.lessons,
                    track = Color(0x33FFFFFF),
                    height = 8.dp
                )
            }
        }

        // ---------------- 能力概览 ----------------
        // 每个单元一行：名称 — 细条 — 百分比。原先每条占两行（标签行 + 条），
        // 十条堆下来把这张卡撑到近 400dp，且和上面五行统计的节奏冲突。
        item(key = "lbCaps") {
            ClaySectionLabel(
                stringResource(R.string.prof_abilities),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        item(key = "caps") {
            val a = clayAccents()
            ClayColumn(
                modifier = Modifier.padding(bottom = 14.dp),
                radius = 22.dp,
                elevation = 8.dp,
                padding = 16.dp,
                gap = 14.dp
            ) {
                capsByTrack.forEach { (track, bars) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 大类小标题：说明下面这几条属于哪套课程
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trackLabelOf(track.id),
                                color = a.lessons,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                            val tTotal = track.units.sumOf { it.lessons.size }
                            val tDone = track.units.sumOf { u -> u.lessons.count { it.id in completed } }
                            Text(
                                text = "${if (tTotal == 0) 0 else tDone * 100 / tTotal}% · $tDone/$tTotal",
                                color = a.sub,
                                fontSize = 10.sp,
                                fontFamily = MonoNumbers
                            )
                        }
                        bars.forEach { (label, v) -> UnitProgressRow(label, v, a) }
                    }
                }
                Text(
                    text = stringResource(R.string.prof_ability_note),
                    color = a.sub,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // ---------------- 反馈与建议 ----------------
        // 位置按用户要求：账号下方、风险声明上方
        item(key = "feedback") {
            FeedbackEntryCard(
                onOpen = onOpenFeedback,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        // ---------------- 声明 ----------------
        item(key = "disc") {
            ClaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                radius = 20.dp,
                elevation = 7.dp,
                fill = Brush.verticalGradient(listOf(Color(0xFFFBF4E6), Color(0xFFF7EEDD))),
                rim = Color(0xFFF0E3C9)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5A524)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Ic.TriangleAlert, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(stringResource(R.string.prof_disclaimer_title), color = Color(0xFF8A5A00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = stringResource(R.string.prof_disclaimer_body),
                            color = Color(0xFF8C7351),
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        // ---------------- 版本 ----------------
        // 重置学习进度已收进设置页（数据分区，带确认弹框）。这里原来是个一点就清空的
        // 按钮，误触代价太大，不再保留第二入口。
        item(key = "version") {
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVersionTap() }
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.prof_version, BuildConfig.VERSION_NAME), color = clayAccents().sub, fontSize = 11.sp)
            }
        }
    }
}

/**
 * 单元进度：一行内「名称 — 细条 — 百分比」。
 * 原来标签和条分两行（12sp 标签 + 6dp 条 + 间距），十条叠起来近 400dp，
 * 而且和「我的主页」那五行统计的节奏打架。压成一行后整块只有原来的一半高。
 */
@Composable
private fun UnitProgressRow(label: String, value: Float, a: ClayAccents) {
    Row(
        modifier = Modifier.fillMaxWidth().height(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = a.label,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(104.dp)
        )
        Box(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            ClayProgress(ratio = value, color = a.lessons, height = 5.dp)
        }
        Text(
            text = "${(value * 100).toInt()}%",
            color = a.sub,
            fontSize = 10.sp,
            fontFamily = MonoNumbers,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )
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

private enum class AuthMode { Login, Register, Reset }

@Composable
private fun AuthScreen(
    auth: AuthApi,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    /** [isRegister] 为 true 表示刚注册：此时昵称是用户当场输入的，应当覆盖本地旧名。 */
    onSuccess: (Account, Boolean) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    // 仅 debug 包展示的现场信息（包名 / 本机签名 / GMS 版本）。
    // Google 登录的失败原因在 Credential Manager 里高度歧义，这一行能把排查从「猜」变成「看」。
    var errorDetail by remember { mutableStateOf<String?>(null) }
    // 成功提示与错误分开：验证码发出去了要告诉用户，但那不是错误
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    // 注册前置：必须勾选同意隐私政策（登录 / 找回密码不拦）
    var agreedPrivacy by remember { mutableStateOf(false) }
    val authScroll = rememberScrollState()
    var sending by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    // Credential Manager 要 Activity，而这里的 LocalContext 是 ContextImpl（被替换过），
    // 拿不到 Activity —— 改用 View 的 context，它挂在 Activity 上（编辑资料页也是这么做的）。
    val view = LocalView.current

    // Google 登录：一步拿到 ID token 再交给服务端验票。
    // 「取消」不算错误 —— 用户点了返回不该看到红条。
    fun startGoogleSignIn() {
        if (loading) return
        loading = true
        error = null
        errorDetail = null
        notice = null
        scope.launch {
            when (val r = GoogleSignIn.signIn(view.context)) {
                is GoogleSignIn.Result.Success -> when (val res = auth.loginWithGoogle(r.idToken)) {
                    is AuthResult.Success -> {
                        loading = false
                        onSuccess(res.account, false)
                    }
                    is AuthResult.Failure -> {
                        loading = false
                        error = res.message
                        // 走到这里说明 ID token 已经拿到了、是本服务拒绝了它。
                        // 带上服务端的错误码，才能区分 aud 不匹配 / 连不上 Google / 其它。
                        errorDetail = "server: " + res.code.ifBlank { "unknown" }
                    }
                }
                GoogleSignIn.Result.Cancelled -> loading = false
                is GoogleSignIn.Result.NoAccount -> {
                    loading = false
                    error = ctx.getString(R.string.cloud_err_google_no_account)
                    errorDetail = r.detail
                }
                is GoogleSignIn.Result.NoPlayServices -> {
                    loading = false
                    error = ctx.getString(R.string.cloud_err_google_no_gms)
                    errorDetail = r.detail
                }
                is GoogleSignIn.Result.Failed -> {
                    loading = false
                    error = ctx.getString(R.string.cloud_err_google_failed)
                    errorDetail = r.detail
                }
            }
        }
    }

    // 报错时把页面滚到底：错误框与主按钮都在下方，不滚用户只看到输入框没反应
    LaunchedEffect(error) {
        if (error != null) authScroll.animateScrollTo(authScroll.maxValue)
    }

    // 重发倒计时。countdown 变化会重启这个 effect，减到 0 自然停下，不会空转。
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    /** 发码。注册与找回走同一条通道，只是 purpose 不同。 */
    fun requestCode(purpose: CodePurpose) {
        if (sending || countdown > 0) return
        sending = true
        error = null
        notice = null
        scope.launch {
            val failure = auth.sendCode(email, purpose)
            sending = false
            if (failure == null) {
                countdown = 60
                notice = ctx.getString(R.string.auth_code_sent)
            } else {
                error = failure
            }
        }
    }

    // 系统返回 / 全面屏侧滑手势：关掉登录页，而不是退出 App
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(authScroll)
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
            text = when (mode) {
                AuthMode.Login -> stringResource(R.string.auth_login_title)
                AuthMode.Register -> stringResource(R.string.auth_register_title)
                AuthMode.Reset -> stringResource(R.string.auth_reset_title)
            },
            color = Ascent.Fg,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        // 登录/注册共用这一句：云端已上线，不再按模式或 CloudConfig 分支显示「模拟登录」
        Text(
            text = if (mode == AuthMode.Reset) {
                stringResource(R.string.auth_reset_note)
            } else {
                stringResource(R.string.auth_note_cloud)
            },
            color = Ascent.Fg3,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        // 模式切换（分段控件）。找回密码是从登录进去的子流程，不显示分段器 ——
        // 三个选项挤一排会让人以为「重置」也是一种注册方式。
        if (mode != AuthMode.Reset) {
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
                                notice = null
                                code = ""
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
        } else {
            // 重置模式给一条明确回路，免得用户以为退不出去
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        mode = AuthMode.Login
                        error = null
                        notice = null
                        code = ""
                        password = ""
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.ChevronLeft, contentDescription = null, tint = Ascent.Fg2, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.auth_back_to_login), color = Ascent.Fg2, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Google 登录放在表单之前：它是一步完成，邮箱密码要三步。
        // 找回密码不需要它，所以只在登录/注册下出现。
        if (mode != AuthMode.Reset && CloudConfig.enabled) {
            PillButton(
                text = stringResource(R.string.auth_google_continue),
                style = BtnStyle.Secondary,
                enabled = !loading,
                onClick = { startGoogleSignIn() }
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Ascent.Line)
                )
                Text(
                    text = stringResource(R.string.auth_or),
                    color = Ascent.Fg3,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Ascent.Line)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

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

        // 验证码：注册与找回密码都要。登录不需要，所以只在两种模式出现。
        if (mode != AuthMode.Login) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AuthField(
                        value = code,
                        // 只收数字并截到 6 位：验证码是定长的，多余的字符只会让用户困惑
                        onChange = { input -> code = input.filter { it.isDigit() }.take(6); error = null },
                        label = stringResource(R.string.auth_code),
                        keyboardType = KeyboardType.Number
                    )
                }
                Spacer(Modifier.width(10.dp))
                PillButton(
                    text = when {
                        countdown > 0 -> stringResource(R.string.auth_resend_in, countdown)
                        sending -> stringResource(R.string.auth_wait)
                        else -> stringResource(R.string.auth_send_code)
                    },
                    modifier = Modifier.weight(0.62f),
                    style = BtnStyle.Secondary,
                    height = 54,
                    fontSize = 13.sp,
                    enabled = !sending && countdown == 0,
                    onClick = {
                        requestCode(
                            if (mode == AuthMode.Reset) CodePurpose.Reset else CodePurpose.Register
                        )
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        AuthField(
            value = password,
            onChange = { password = it; error = null },
            label = stringResource(
                when {
                    mode == AuthMode.Reset -> R.string.auth_new_password
                    CloudConfig.enabled -> R.string.auth_password_cloud
                    else -> R.string.auth_password
                }
            ),
            keyboardType = KeyboardType.Password,
            isPassword = true
        )

        // 忘记密码入口只在登录模式出现；找回密码本身不需要登录态
        if (mode == AuthMode.Login && CloudConfig.enabled) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.auth_forgot),
                color = Ascent.Fg2,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        mode = AuthMode.Reset
                        error = null
                        notice = null
                        code = ""
                        password = ""
                    }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            )
        }

        if (mode == AuthMode.Register) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedPrivacy,
                    onCheckedChange = {
                        agreedPrivacy = it
                        if (it) error = null
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Ascent.Accent,
                        uncheckedColor = Ascent.Fg3,
                        checkmarkColor = Ascent.OnAccent
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auth_privacy_agree),
                    color = Ascent.Fg2,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            agreedPrivacy = !agreedPrivacy
                            if (agreedPrivacy) error = null
                        }
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.auth_privacy_view),
                    color = Ascent.Accent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onOpenPrivacy() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        if (notice != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ascent.AccentSoft)
                    .padding(13.dp)
            ) {
                Text(text = notice!!, color = Ascent.Fg2, fontSize = 12.sp)
            }
        }

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
            // 只有 debug 包会显示：release 里这些信息只进 logcat，不给用户看。
            val debuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (debuggable && errorDetail != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = errorDetail!!,
                    color = Ascent.Fg3,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        PillButton(
            text = when {
                loading -> stringResource(R.string.auth_wait)
                mode == AuthMode.Login -> stringResource(R.string.auth_login_action)
                mode == AuthMode.Register -> stringResource(R.string.auth_register_action)
                else -> stringResource(R.string.auth_reset_action)
            },
            enabled = !loading,
            onClick = {
                if (loading) return@PillButton
                if (mode == AuthMode.Register && !agreedPrivacy) {
                    error = ctx.getString(R.string.auth_privacy_required)
                    return@PillButton
                }
                loading = true
                error = null
                notice = null
                scope.launch {
                    if (mode == AuthMode.Reset) {
                        val failure = auth.resetPassword(email, code, password)
                        loading = false
                        if (failure == null) {
                            // 服务端把所有会话都作废了，这里必须回到登录，不能假装已登录
                            mode = AuthMode.Login
                            password = ""
                            code = ""
                            notice = ctx.getString(R.string.auth_reset_done)
                        } else {
                            error = failure
                        }
                    } else {
                        val result = if (mode == AuthMode.Login) {
                            auth.login(email, password)
                        } else {
                            auth.register(email, password, nickname, code)
                        }
                        loading = false
                        when (result) {
                            is AuthResult.Success -> onSuccess(result.account, mode == AuthMode.Register)
                            is AuthResult.Failure -> error = result.message
                        }
                    }
                }
            }
        )

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

/**
 * 「上次同步」的时间文案。今天只显示时:分，跨天补上日期 ——
 * 同步是高频动作，写完整日期反而看不出「是不是刚同步过」。
 */
private fun formatSyncTime(ts: Long): String {
    val at = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
    val pattern = if (at.toLocalDate() == LocalDate.now()) "HH:mm" else "MM-dd HH:mm"
    return at.format(DateTimeFormatter.ofPattern(pattern))
}
