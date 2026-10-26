package com.ascent.climb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ascent.climb.ui.AscentTheme
import com.ascent.climb.ui.Badge3DStageScreen

/**
 * 仅 debug 源集：直接拉起 3D 徽章 POC，跳过「我的 → 设置 → 开发者」的点选路径，
 * 方便模拟器 / adb 自动化排查。
 *
 *   adb shell am start -n com.ascent.climb/.Poc3DActivity
 */
class Poc3DActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 可指定任意成就：am start ... --es id fourteen_peaks
        // 加 --ez unlock true 直接弹解锁弹窗（验证 3D + 彩带）
        val id = intent?.getStringExtra("id") ?: "first_steps"
        val unlock = intent?.getBooleanExtra("unlock", false) == true
        setContent {
            AscentTheme(darkTheme = true) {
                if (unlock) {
                    val ach = com.ascent.climb.data.achievementById(id)
                    if (ach != null) {
                        com.ascent.climb.ui.AchievementUnlockOverlay(
                            achievement = ach,
                            onDismiss = { finish() }
                        )
                    } else {
                        Badge3DStageScreen(achievementId = id, onBack = { finish() })
                    }
                } else {
                    Badge3DStageScreen(achievementId = id, onBack = { finish() })
                }
            }
        }
    }
}
