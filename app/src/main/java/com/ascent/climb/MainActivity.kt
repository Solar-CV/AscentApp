package com.ascent.climb

import android.app.Activity
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ascent.climb.data.ALL_UNITS
import com.ascent.climb.data.ProgressStore
import com.ascent.climb.data.achievementById
import com.ascent.climb.data.bank.BankRepository
import com.ascent.climb.data.bank.UpdateCheck
import com.ascent.climb.data.altitudeAwardFor
import com.ascent.climb.data.lessonById
import com.ascent.climb.ui.AchievementUnlockOverlay
import com.ascent.climb.ui.Ascent
import com.ascent.climb.ui.AscentTheme
import com.ascent.climb.ui.CapsuleTabBar
import com.ascent.climb.ui.HandbookScreen
import com.ascent.climb.ui.LessonScreen
import com.ascent.climb.ui.PathScreen
import com.ascent.climb.ui.ProfileScreen
import com.ascent.climb.ui.ReviewScreen
import com.ascent.climb.ui.TrainingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 题库必须在 setContent 之前完成载入：缓存副本 → 内置题库
        BankRepository.init(applicationContext)

        val store = ProgressStore(this)

        setContent {
            var themeMode by remember { mutableStateOf(store.themeMode) }
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
            AscentTheme(darkTheme = dark) {
                AscentRoot(
                    store = store,
                    themeMode = themeMode,
                    onThemeModeChange = {
                        store.themeMode = it
                        themeMode = it
                    }
                )
            }
        }
    }
}

private fun describeCheck(r: UpdateCheck): String = when (r) {
    is UpdateCheck.Downloaded -> "题库已更新到 v${r.version}（${r.questionCount} 题）"
    is UpdateCheck.UpToDate -> "题库已是最新（v${r.version}）"
    is UpdateCheck.NeedAppUpgrade -> "在线题库需要 App v${r.requiredAppVersion} 以上"
    is UpdateCheck.Failed -> r.message
}

@Composable
fun AscentRoot(
    store: ProgressStore,
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current

    var altitude by remember { mutableStateOf(store.altitude) }
    var streak by remember { mutableStateOf(store.streak) }
    var rope by remember { mutableStateOf(store.rope) }
    var completed by remember { mutableStateOf(store.completed.toSet()) }
    var dueKeys by remember { mutableStateOf(store.dueReviewKeys()) }
    var reviewTotal by remember { mutableStateOf(store.reviewTotal()) }
    var tab by remember { mutableStateOf(0) }
    var openLessonId by remember { mutableStateOf<String?>(null) }
    var devUnlocked by remember { mutableStateOf(store.devUnlocked) }
    var devUnlockAll by remember { mutableStateOf(store.devUnlockAll) }
    var devShowAnswers by remember { mutableStateOf(store.devShowAnswers) }
    var pendingAchievements by remember { mutableStateOf(store.pendingAchievements.toList()) }
    var achRevision by remember { mutableStateOf(0) }

    /** 事件后统一做成就判定并把新解锁排进待播队列。 */
    fun refreshAchievements() {
        store.grantAchievements(ALL_UNITS)
        pendingAchievements = store.pendingAchievements.toList()
        achRevision += 1
    }

    // 题库状态
    var bankRevision by remember { mutableStateOf(0) }
    var bankChecking by remember { mutableStateOf(false) }
    var bankMessage by remember { mutableStateOf(BankRepository.lastResult) }
    var checkTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        store.touchDay()
        altitude = store.altitude
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
        bankMessage = describeCheck(result)
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
                altitudeAward = if (openLesson.id in completed) 0 else altitudeAwardFor(openLesson.id),
                devShowAnswers = devShowAnswers,
                onClose = { openLessonId = null },
                onFinished = { stars, wrongCount, wrongKeys ->
                    store.finishLesson(openLesson.id, stars)
                    repeat(wrongCount) { store.loseRope() }
                    wrongKeys.forEach { store.markWrong(it) }
                    altitude = store.altitude
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
                    when (tab) {
                        0 -> PathScreen(
                            altitude = altitude,
                            streak = streak,
                            rope = rope,
                            completed = completed,
                            starsOf = { store.starsFor(it) },
                            devUnlockAll = devUnlockAll,
                            onOpenLesson = { openLessonId = it }
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
                                bankMessage = BankRepository.lastResult
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
