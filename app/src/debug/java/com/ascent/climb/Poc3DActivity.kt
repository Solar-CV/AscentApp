package com.ascent.climb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ascent.climb.ui.AscentTheme
import com.ascent.climb.ui.Badge3DPocScreen

/**
 * 仅 debug 源集：直接拉起 3D 徽章 POC，跳过「我的 → 设置 → 开发者」的点选路径，
 * 方便模拟器 / adb 自动化排查。
 *
 *   adb shell am start -n com.ascent.climb/.Poc3DActivity
 */
class Poc3DActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AscentTheme(darkTheme = true) {
                Badge3DPocScreen(onBack = { finish() })
            }
        }
    }
}
