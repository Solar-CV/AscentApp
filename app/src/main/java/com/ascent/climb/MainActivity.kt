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
import com.ascent.climb.ui.CapsuleTabBar
import com.ascent.climb.ui.ClaySplashOverlay
import com.ascent.climb.ui.HandbookScreen
import com.ascent.climb.ui.LessonScreen
import com.ascent.climb.ui.PathScreen
import com.ascent.climb.ui.ProfileScreen
import com.ascent.climb.ui.ReviewScreen
import com.ascent.climb.ui.TrainingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动瞬间用 Theme.Ascent.Launch（整屏开屏底色 + 透明图标），
        // 这里立刻换回正常主题，之后的窗口/状态栏颜色与以前一致。
        setTheme(R.style.Theme_Ascent)

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

            // Android 13+：同步到系统「应用语言」设置，保证系统设置页与本 App 一致。
            // 「跟随系统」时清空，交还给系统。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LaunchedEffect(languageMode) {
                    val lm = applicationContext.getSystemService(LocaleManager::class.java)
                        ?: return@LaunchedEffect
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

            CompositionLocalProvider(LocalContext provides localized) {
                AscentTheme(darkTheme = dark) {
                    // 开屏只盖在根界面之上，不改任何既有页面；序列走完自行淡出。
                    // rememberSaveable：旋转/重建不重播。
                    var splashDone by rememberSaveable { mutableStateOf(false) }
                    // 开屏这 1.7 秒里让系统栏也是开屏底色，否则顶部会多出一条灰带
                    LaunchedEffect(splashDone) {
                        val act = view.context as? Activity ?: return@LaunchedEffect
                        val bar = ContextCompat.getColor(
                            act,
                            if (splashDone) (if (dark) R.color.ascent_bg else R.color.ascent_bg_light)
                            else (if (dark) R.color.splash_bg_dark else R.color.splash_bg_light)
                        )
                        @Suppress("DEPRECATION")
                        act.window.statusBarColor = bar
                        @Suppress("DEPRECATION")
                        act.window.navigationBarColor = bar
                    }
                    Box(Modifier.fillMaxSize()) {
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
                        if (!splashDone) {
                            ClaySplashOverlay(dark = dark) { splashDone = true }
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
                                devUnlockAll = devUnlockAll
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
                                }
                            )
                        }
                    }
                }
            }
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
