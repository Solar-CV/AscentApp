package com.ascent.climb

import android.app.Activity
import android.content.Context
import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.Diagnostics
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.achievementById
import com.ascent.climb.data.bank.BankRepository
import com.ascent.climb.ui.bankMsgOf
import com.ascent.climb.ui.bankMsgText
import com.ascent.climb.i18n.AppLocale
import com.ascent.climb.data.bank.UpdateCheck
import com.ascent.climb.data.altitudeAwardForTrack
import com.ascent.climb.data.setActiveTrack
import com.ascent.climb.data.trackOfLesson
import com.ascent.climb.data.lessonById
import com.ascent.climb.ui.AchievementUnlockOverlay
import com.ascent.climb.ui.Ascent
import com.ascent.climb.ui.AscentTheme
import com.ascent.climb.ui.AvatarCropController
import com.ascent.climb.ui.AvatarCropOverlay
import com.ascent.climb.ui.Badge3DStageScreen
import com.ascent.climb.ui.CapsuleTabBar
import com.ascent.climb.ui.ClaySplashOverlay
import com.ascent.climb.ui.HandbookScreen
import com.ascent.climb.ui.LessonScreen
import com.ascent.climb.ui.LocalAvatarCrop
import com.ascent.climb.ui.PathScreen
import com.ascent.climb.ui.ProfileScreen
import com.ascent.climb.ui.OnboardingOverlay
import com.ascent.climb.ui.ReviewScreen
import com.ascent.climb.ui.TrainingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动瞬间用 Theme.Ascent.Launch（整屏开屏底色 + 透明图标），
        // 这里立刻换回正常主题，之后的窗口/状态栏颜色与以前一致。
        setTheme(R.style.Theme_Ascent)

        // targetSdk 35+ 起系统强制 edge-to-edge，`android:statusBarColor` 那套全部失效。
        // 与其让老系统和新系统走两条不同的布局路径，不如统一显式开启，
        // 再由根布局按 `safeDrawing` 留出系统栏高度（否则顶部「3,200 M」会被状态栏压住）。
        enableEdgeToEdge()

        // 题库必须在 setContent 之前完成载入：缓存副本 → 内置题库
        BankRepository.init(applicationContext)

        // 崩溃记录：只落文件、不拦截，反馈页可选择性附带
        Diagnostics.installCrashHandler(applicationContext)

        val store = ProgressStore(this)

        setContent {
            var themeMode by remember { mutableStateOf(store.themeMode) }
            var languageMode by remember { mutableStateOf(store.languageMode) }

            val dark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            SideEffect {
                val act = view.context as? Activity ?: return@SideEffect
                val controller = WindowCompat.getInsetsController(act.window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            // 生效语言：手动选择优先，否则按系统语言列表匹配，都不支持则回退英文。
            // 这里用 LocalConfiguration 而不是 LocaleList.getDefault()，
            // 这样系统语言变化（Activity 重建）后能拿到最新值。
            val systemLocales = LocalConfiguration.current.locales
            val locale = remember(languageMode, systemLocales) {
                AppLocale.resolve(languageMode, systemLocales)
            }
            val baseContext = LocalContext.current
            val localized = remember(locale) {
                AppLocale.localizedContext(baseContext, locale)
            }

            CompositionLocalProvider(LocalContext provides localized) {
                AscentTheme(darkTheme = dark) {
                    // 开屏只盖在根界面之上，不改任何既有页面；序列走完自行淡出。
                    // rememberSaveable：旋转/重建不重播。
                    var splashDone by rememberSaveable { mutableStateOf(false) }
                    // 头像裁剪：任意页面发起请求，这里统一渲染在全屏层（见 AvatarCropScreen.kt）
                    val avatarCrop = remember { AvatarCropController() }
                    // 系统栏一律透明，让界面自己决定那两条区域长什么样。
                    //
                    // 原来这里按开屏进度给状态栏/导航栏刷不透明色。targetSdk 35+ 强制
                    // edge-to-edge 之后这两个属性已经是 no-op；但在旧系统上它们仍然生效，
                    // 会把「故意画到系统栏下面」的内容挡出一条边 —— 表现是成就弹窗的暗色
                    // 遮罩上下各露一条白边、3D 徽章页的背景图顶部被切成直角。
                    // 开屏那 1.7 秒也不受影响：ClaySplashOverlay 自己铺满整屏。
                    LaunchedEffect(Unit) {
                        val act = view.context as? Activity ?: return@LaunchedEffect
                        @Suppress("DEPRECATION")
                        act.window.statusBarColor = android.graphics.Color.TRANSPARENT
                        @Suppress("DEPRECATION")
                        act.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    }
                    CompositionLocalProvider(LocalAvatarCrop provides avatarCrop) {
                    Box(Modifier.fillMaxSize()) {
                        // 底色铺满整屏（含状态栏与导航栏区域）。
                        // 安全区不在这里加：AscentRoot 里还有几层要**故意画到系统栏下面**
                        // （成就弹窗的暗色遮罩、3D 徽章页的背景图），在外层统一收边会把它们裁短，
                        // 上下露出两条底色。所以 insets 下沉到 AscentRoot 内部按层处理。
                        Box(Modifier.fillMaxSize().background(Ascent.Bg))
                        AscentRoot(
                            store = store,
                            themeMode = themeMode,
                            onThemeModeChange = {
                                store.themeMode = it
                                themeMode = it
                            },
                            languageMode = languageMode,
                            onLanguageModeChange = {
                                store.languageMode = it
                                languageMode = it
                            }
                        )
                        // 头像裁剪页：盖在整棵树之上（含底部导航栏），自己按 safeDrawing 留边
                        AvatarCropOverlay(avatarCrop)
                        if (!splashDone) {
                            ClaySplashOverlay(dark = dark) { splashDone = true }
                        }
                    }
                    }
                }
            }
        }
    }
}



@Composable
fun AscentRoot(
    store: ProgressStore,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    languageMode: String,
    onLanguageModeChange: (String) -> Unit
) {
    val context = LocalContext.current

    var trackId by remember { mutableStateOf(store.trackId) }
    // 首启引导：只在「真·新装」时出现；老用户升级上来直接跳过（ProgressStore.needsOnboarding）
    // rememberSaveable：换语言会触发系统重建（见下面的 LocaleManager 同步），状态不能丢
    var onboardingDone by rememberSaveable { mutableStateOf(!store.needsOnboarding) }

    // Android 13+：把「应用语言」同步给系统设置页。
    // 只在引导**完成后**才同步——引导途中同步会立刻触发 Activity 重建（黑屏一下），
    // 用户正在选语言时被打断，体验很差。
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(languageMode, onboardingDone) {
            if (!onboardingDone) return@LaunchedEffect
            val lm = context.getSystemService(LocaleManager::class.java) ?: return@LaunchedEffect
            val target = if (languageMode == AppLocale.AUTO) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(languageMode)
            }
            if (lm.applicationLocales != target) {
                runCatching { lm.applicationLocales = target }
            }
        }
    }
    // 聚合数据（ALL_UNITS / ALL_LESSONS）跟随当前大类，供训练页与我的页使用；
    // 成就判定不用它们，走 ProgressStore 内部的全轨道统计
    setActiveTrack(trackId)
    var altitude by remember { mutableStateOf(store.altitudeOn(store.trackId)) }
    var streak by remember { mutableStateOf(store.streak) }
    var rope by remember { mutableStateOf(store.rope) }
    var completed by remember { mutableStateOf(store.completed.toSet()) }
    var dueKeys by remember { mutableStateOf(store.dueReviewKeys()) }
    var reviewTotal by remember { mutableStateOf(store.reviewTotal()) }
    var tab by remember { mutableStateOf(0) }
    // 每个 tab 一份可保存状态（LazyListState / PagerState 都靠 rememberSaveable）。
    // 切 tab 或进课程再退出来，列表停在原来的位置，而不是弹回顶部。
    val tabStates = rememberSaveableStateHolder()
    var openLessonId by remember { mutableStateOf<String?>(null) }
    // 徽章 3D 展示页：从个人主页、徽章墙、成就测试区都能打开。
    // 放在最外层（和课程同一层）渲染，底部导航栏才不会压在这一页上面。
    var badgeStageId by remember { mutableStateOf<String?>(null) }
    var devUnlocked by remember { mutableStateOf(store.devUnlocked) }
    var devUnlockAll by remember { mutableStateOf(store.devUnlockAll) }
    var devShowAnswers by remember { mutableStateOf(store.devShowAnswers) }
    var pendingAchievements by remember { mutableStateOf(store.pendingAchievements.toList()) }
    var achRevision by remember { mutableStateOf(0) }

    /** 事件后统一做成就判定并把新解锁排进待播队列。 */
    fun refreshAchievements() {
        store.grantAchievements()
        pendingAchievements = store.pendingAchievements.toList()
        achRevision += 1
    }

    // 题库状态
    var bankRevision by remember { mutableStateOf(0) }
    var bankChecking by remember { mutableStateOf(false) }
    var bankMsg by remember { mutableStateOf(BankRepository.lastResult) }
    // 文案在组合期解析，语言切换后会自动跟着变
    val bankMessage = bankMsgText(bankMsg)
    var checkTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        store.touchDay()
        altitude = store.altitudeOn(trackId)
        streak = store.streak
        rope = store.rope
        completed = store.completed.toSet()
        dueKeys = store.dueReviewKeys()
        reviewTotal = store.reviewTotal()
        devUnlocked = store.devUnlocked
        devUnlockAll = store.devUnlockAll
        devShowAnswers = store.devShowAnswers
        refreshAchievements()
    }

    // 启动后静默检查题库更新；离线或未配置地址时静默跳过
    LaunchedEffect(checkTrigger) {
        val silent = checkTrigger == 0
        if (silent && (!BankRepository.hasRemoteConfigured || !BankRepository.shouldAutoCheck())) return@LaunchedEffect
        bankChecking = true
        val result = BankRepository.checkForUpdate()
        bankMsg = bankMsgOf(result)
        bankChecking = false
        if (result is UpdateCheck.Downloaded) {
            // 让界面重新读取新的题库
            bankRevision += 1
        }
    }

    val openLesson = openLessonId?.let { lessonById(it) }

    Box(modifier = Modifier.fillMaxSize()) {
    // ── 内容层 ──
    // 这一层只负责避让系统栏。缩进故意不跟着加：整块原本就写在一个 Box 里，
    // 外层再套一层是为了让**下面那几个全屏层**（3D 徽章背景、成就遮罩）能画到状态栏
    // 和手势条下面去；若在更外层统一收边，它们的背景会被裁短、上下露出底色。
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
    if (openLesson != null) {
        BackHandler { openLessonId = null }
        Box(modifier = Modifier.fillMaxSize().background(Ascent.Bg)) {
            LessonScreen(
                lesson = openLesson,
                altitudeAward = if (openLesson.id in completed) 0 else
                    altitudeAwardForTrack(trackOfLesson(openLesson.id), openLesson.id),
                devShowAnswers = devShowAnswers,
                onClose = { openLessonId = null },
                onFinished = { stars, wrongCount, wrongKeys ->
                    store.finishLesson(openLesson.id, stars)
                    repeat(wrongCount) { store.loseRope() }
                    wrongKeys.forEach { store.markWrong(it) }
                    altitude = store.altitudeOn(trackId)
                    rope = store.rope
                    completed = store.completed.toSet()
                    dueKeys = store.dueReviewKeys()
                    reviewTotal = store.reviewTotal()
                    refreshAchievements()
                    openLessonId = null
                }
            )
        }
    } else {
        Scaffold(
            containerColor = Ascent.Bg,
            bottomBar = {
                CapsuleTabBar(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Ascent.Bg)
            ) {
                // 题库更新后重建整棵子树，确保各页面读到新数据
                key(bankRevision) {
                    tabStates.SaveableStateProvider("tab-$tab") {
                        when (tab) {
                            0 -> PathScreen(
                                trackId = trackId,
                                altitude = altitude,
                                streak = streak,
                                rope = rope,
                                completed = completed,
                                starsOf = { store.starsFor(it) },
                                devUnlockAll = devUnlockAll,
                                onOpenLesson = { openLessonId = it },
                                onSwitchTrack = { id ->
                                    if (id != trackId) {
                                        trackId = id
                                        store.trackId = id
                                        setActiveTrack(id)
                                        altitude = store.altitudeOn(id)
                                    }
                                }
                            )
                            1 -> HandbookScreen()
                            2 -> TrainingScreen(
                                altitude = altitude,
                                completed = completed,
                                store = store,
                                devUnlockAll = devUnlockAll,
                                devShowAnswers = devShowAnswers,
                                // 「今日推荐」指向路线上的下一课：跳到路径页并直接把那课打开。
                                // 先切 tab 再置 id —— 顺序无所谓（同一次快照），但 tab 必须是 0，
                                // 否则关闭课程后会回到训练页，跟用户「跳过去」的预期不符。
                                onOpenLesson = { id ->
                                    tab = 0
                                    openLessonId = id
                                }
                            )
                            3 -> ReviewScreen(
                                completed = completed,
                                dueKeys = dueKeys,
                                reviewTotal = reviewTotal,
                                starsOf = { store.starsFor(it) },
                                onOpenLesson = { openLessonId = it },
                                onGradeReview = { key, ok ->
                                    store.gradeReview(key, ok)
                                    dueKeys = store.dueReviewKeys()
                                    reviewTotal = store.reviewTotal()
                                    refreshAchievements()
                                }
                            )
                            else -> ProfileScreen(
                                altitude = altitude,
                                streak = streak,
                                completed = completed,
                                store = store,
                                achRevision = achRevision,
                                devUnlocked = devUnlocked,
                                devUnlockAll = devUnlockAll,
                                devShowAnswers = devShowAnswers,
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                languageMode = languageMode,
                                onLanguageModeChange = onLanguageModeChange,
                                onDevUnlocked = {
                                    store.devUnlocked = true
                                    devUnlocked = true
                                    refreshAchievements()
                                },
                                onDevUnlockAllChange = {
                                    store.devUnlockAll = it
                                    devUnlockAll = it
                                },
                                onDevShowAnswersChange = {
                                    store.devShowAnswers = it
                                    devShowAnswers = it
                                },
                                onDevExit = {
                                    store.devUnlocked = false
                                    store.devUnlockAll = false
                                    store.devShowAnswers = false
                                    devUnlocked = false
                                    devUnlockAll = false
                                    devShowAnswers = false
                                },
                                bankChecking = bankChecking,
                                bankMessage = bankMessage,
                                onCheckBank = {
                                    checkTrigger += 1
                                },
                                onResetBank = {
                                    BankRepository.resetToBundled()
                                    bankMsg = BankRepository.lastResult
                                    bankRevision += 1
                                },
                                onBankUrlChange = { url ->
                                    BankRepository.setBaseUrl(url)
                                },
                                onReset = {
                                    store.reset()
                                    altitude = store.altitude
                                    streak = store.streak
                                    rope = store.rope
                                    completed = store.completed.toSet()
                                    dueKeys = store.dueReviewKeys()
                                    reviewTotal = store.reviewTotal()
                                    achRevision += 1
                                    tab = 0
                                },
                                onAchChanged = {
                                    pendingAchievements = store.pendingAchievements.toList()
                                    achRevision += 1
                                },
                                onOpenBadge = { badgeStageId = it }
                            )
                        }
                    }
                }
            }
        }
    }

    }

    // 徽章 3D 展示页：**盖在整棵树之上**而不是替换它。
    // 替换的话，个人主页/徽章墙会离开组合，showProfileHome 这类 remember 状态全丢，
    // 返回时会跳到「我的」根页面。盖一层就没有这个问题，也能盖住底部导航栏。
    val badgeStage = badgeStageId
    if (badgeStage != null) {
        Badge3DStageScreen(achievementId = badgeStage, onBack = { badgeStageId = null })
    }

    // 首次启动偏好：整屏盖在最上层，选完才进入主界面
    if (!onboardingDone) {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        OnboardingOverlay(
            initialTrack = trackId,
            language = languageMode,
            onLanguageChange = { mode ->
                // 语言即时生效：整棵树（含引导页自身）立刻切换，用户按下继续前就能确认
                onLanguageModeChange(mode)
            },
            onConfirm = { picked ->
                if (picked != trackId) {
                    trackId = picked
                    store.trackId = picked
                    setActiveTrack(picked)
                    altitude = store.altitudeOn(picked)
                }
                // 轨道变更会改变题库/技能书/徽章的可见集合，刷一次让各页重算
                achRevision += 1
                store.onboardingDone = true
                onboardingDone = true
            }
        )
        }
    }

    // 成就解锁弹窗：每次一条，关闭后播下一条（队列持久化，重启不丢）
    val currentAchievement = pendingAchievements.firstOrNull()?.let { achievementById(it) }
    if (currentAchievement != null) {
        AchievementUnlockOverlay(
            achievement = currentAchievement,
            onDismiss = {
                store.dequeuePendingAchievement(currentAchievement.id)
                pendingAchievements = store.pendingAchievements.toList()
            }
        )
    }
    }
}
