package com.ascent.climb.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ascent.climb.R

/**
 * ASCENT 视觉语言 · 方向 E（HeroUI × 攀登）。
 *
 * 中性灰底 + 冰川蓝强调，全平面、1dp 细描边、12/16/20/24/999 圆角。
 * 新 token（Accent/Surface/Border/Fg…）为推荐名；旧 token（Ice/Panel/Line/Txt…）
 * 作为同义别名保留，避免既有界面代码失效。
 *
 * 主题跟随系统：isSystemInDarkTheme()。
 */
@Immutable
data class AscentColors(
    val isDark: Boolean,
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val ice: Color,
    val iceSoft: Color,
    val gold: Color,
    val orange: Color,
    val green: Color,
    val red: Color,
    val txt: Color,
    val mut: Color,
    val mut2: Color,
    /** 长段落正文颜色 */
    val body: Color,
    /** 进度条轨道 */
    val track: Color,
    /** 未解锁节点底色 */
    val nodeLock: Color,
    val navBg: Color,
    val topoLine: Color,
    val star: Color,
    val trail: Color,
    val gridLine: Color,
    /** 强调底上的前景色 */
    val onIce: Color,
    /** 金色底上的前景色 */
    val onGold: Color,
    val okText: Color,
    val warnText: Color,
    val errText: Color,
    /** 强调色的柔和底（约 14% alpha） */
    val accentSoft: Color,
    val successSoft: Color,
    val warningSoft: Color,
    val dangerSoft: Color,
    /** 弹窗遮罩 */
    val scrim: Color,

    // ---- 方向 V2（多邻国真实风路径页） ----
    val duoBg: Color,
    val duoFg: Color,
    val duoFg2: Color,
    val duoFg3: Color,
    val duoBorder: Color,
    val lockFace2: Color,
    val lockLip3: Color,
    val lockGlyph: Color,
    /** 品牌绿（单元主色） */
    val uGreen: Color,
    val uGreenLip: Color,
    /** 可挑战蓝 */
    val uBlue: Color,
    val uBlueLip: Color,
    /** 登顶金 */
    val uGold: Color,
    val uGoldLip: Color,
    val goldGlyph: Color,
    /** 连续天数橙 */
    val streak: Color
)

val DarkAscent = AscentColors(
    isDark = true,
    bg = Color(0xFF060607),
    panel = Color(0xFF141416),
    panel2 = Color(0xFF1F1F23),
    line = Color(0xFF2A2A2E),
    ice = Color(0xFF0485F7),
    iceSoft = Color(0xFF6FB6FB),
    gold = Color(0xFFF5A524),
    orange = Color(0xFFFB923C),
    green = Color(0xFF17C964),
    red = Color(0xFFFF383C),
    txt = Color(0xFFFAFAFA),
    mut = Color(0xFFA1A1AA),
    mut2 = Color(0xFF71717A),
    body = Color(0xFFD4D4D8),
    track = Color(0xFF1F1F23),
    nodeLock = Color(0xFF1F1F23),
    navBg = Color(0xFF141416),
    topoLine = Color(0x0AFFFFFF),
    star = Color(0xFF3F3F46),
    trail = Color(0x380485F7),
    gridLine = Color(0xFF2A2A2E),
    onIce = Color(0xFFFFFFFF),
    onGold = Color(0xFF2A1D00),
    okText = Color(0xFF6EE7A8),
    warnText = Color(0xFFFCD34D),
    errText = Color(0xFFFF8A97),
    accentSoft = Color(0x240485F7),
    successSoft = Color(0x2617C964),
    warningSoft = Color(0x26F5A524),
    dangerSoft = Color(0x26FF383C),
    scrim = Color(0x8A000000),
    duoBg = Color(0xFF060607),
    duoFg = Color(0xFFFAFAFA),
    duoFg2 = Color(0xFFA1A1AA),
    duoFg3 = Color(0xFF71717A),
    duoBorder = Color(0xFF2A2A2E),
    lockFace2 = Color(0xFF2C2C31),
    lockLip3 = Color(0xFF232327),
    lockGlyph = Color(0xFF71717A),
    uGreen = Color(0xFF58CC02),
    uGreenLip = Color(0xFF46A302),
    uBlue = Color(0xFF1CB0F6),
    uBlueLip = Color(0xFF1899D6),
    uGold = Color(0xFFFFC800),
    uGoldLip = Color(0xFFD19E00),
    goldGlyph = Color(0xFF4A3208),
    streak = Color(0xFFFF9600)
)

val LightAscent = AscentColors(
    isDark = false,
    bg = Color(0xFFF5F5F5),
    panel = Color(0xFFFFFFFF),
    panel2 = Color(0xFFF4F4F5),
    line = Color(0xFFE7E7EA),
    ice = Color(0xFF0485F7),
    iceSoft = Color(0xFF0369C9),
    gold = Color(0xFFD97706),
    orange = Color(0xFFEA580C),
    green = Color(0xFF15803D),
    red = Color(0xFFDC2626),
    txt = Color(0xFF18181B),
    mut = Color(0xFF71717A),
    mut2 = Color(0xFFA1A1AA),
    body = Color(0xFF3F3F46),
    track = Color(0xFFE4E4E7),
    nodeLock = Color(0xFFF4F4F5),
    navBg = Color(0xFFFFFFFF),
    topoLine = Color(0x0818181B),
    star = Color(0xFFE4E4E7),
    trail = Color(0x380485F7),
    gridLine = Color(0xFFE7E7EA),
    onIce = Color(0xFFFFFFFF),
    onGold = Color(0xFF3A2A05),
    okText = Color(0xFF0F7A3D),
    warnText = Color(0xFF92400E),
    errText = Color(0xFF9F1239),
    accentSoft = Color(0x240485F7),
    successSoft = Color(0x2417C964),
    warningSoft = Color(0x26F5A524),
    dangerSoft = Color(0x24FF383C),
    scrim = Color(0x8A000000),
    duoBg = Color(0xFFFFFFFF),
    duoFg = Color(0xFF3C3C3C),
    duoFg2 = Color(0xFF777777),
    duoFg3 = Color(0xFFAFAFAF),
    duoBorder = Color(0xFFE5E5E5),
    lockFace2 = Color(0xFFE5E5E5),
    lockLip3 = Color(0xFFC9C9C9),
    lockGlyph = Color(0xFFAFAFAF),
    uGreen = Color(0xFF58CC02),
    uGreenLip = Color(0xFF46A302),
    uBlue = Color(0xFF1CB0F6),
    uBlueLip = Color(0xFF1899D6),
    uGold = Color(0xFFFFC800),
    uGoldLip = Color(0xFFD19E00),
    goldGlyph = Color(0xFF4A3208),
    streak = Color(0xFFFF9600)
)

val LocalAscentColors = staticCompositionLocalOf { DarkAscent }

/**
 * 语义色入口。声明为 @Composable getter，因此所有调用点无需改动即可跟随主题。
 * 注意：在 Canvas 的绘制 lambda（非 composable 作用域）中需先取出到局部变量。
 */
object Ascent {
    // ---- 推荐名（方向 E） ----
    val IsDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.isDark
    val Bg: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.bg
    val Accent: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.ice
    val AccentText: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.iceSoft
    val OnAccent: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.onIce
    val AccentSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.accentSoft
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.panel
    val Surface2: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.panel2
    val Border: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.line
    val Fg: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.txt
    val Fg2: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.mut
    val Fg3: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.mut2
    val Success: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.green
    val SuccessSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.successSoft
    val Warning: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.gold
    val WarningSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.warningSoft
    val Danger: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.red
    val DangerSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.dangerSoft
    val Scrim: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.scrim

    // ---- 方向 V2 ----
    val DuoBg: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.duoBg
    val DuoFg: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.duoFg
    val DuoFg2: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.duoFg2
    val DuoFg3: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.duoFg3
    val DuoBorder: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.duoBorder
    val LockFace2: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.lockFace2
    val LockLip3: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.lockLip3
    val LockGlyph: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.lockGlyph
    val UGreen: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.uGreen
    val UGreenLip: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.uGreenLip
    val UBlue: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.uBlue
    val UBlueLip: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.uBlueLip
    val UGold: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.uGold
    val UGoldLip: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.uGoldLip
    val GoldGlyph: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.goldGlyph
    val Streak: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.streak

    // ---- 兼容别名（旧界面沿用） ----
    val Panel: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.panel
    val Panel2: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.panel2
    val Line: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.line
    val Ice: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.ice
    val IceSoft: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.iceSoft
    val Gold: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.gold
    val Orange: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.orange
    val Green: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.green
    val Red: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.red
    val Txt: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.txt
    val Mut: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.mut
    val Mut2: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.mut2
    val Body: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.body
    val Track: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.track
    val NodeLock: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.nodeLock
    val NavBg: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.navBg
    val TopoLine: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.topoLine
    val Star: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.star
    val Trail: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.trail
    val GridLine: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.gridLine
    val OnIce: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.onIce
    val OnGold: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.onGold
    val OkText: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.okText
    val WarnText: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.warnText
    val ErrText: Color
        @Composable @ReadOnlyComposable get() = LocalAscentColors.current.errText
}

/**
 * 内嵌字体（子集化，见 HANDOFF §11）：
 *  - [SansBody]：Noto Sans SC（≈思源黑体）400 / 600，覆盖中文 + 拉丁 + 数字；
 *  - [MonoNumbers]：JetBrains Mono 400 / 600，用于海拔、坐标、编号、meta 微标签。
 * 仅声明 400 与 600 两个字重，Medium/SemiBold/Bold 均指向 600 文件（避免系统合成假粗体）。
 */
val SansBody = FontFamily(
    Font(R.font.ascent_sans_regular, FontWeight.Normal),
    Font(R.font.ascent_sans_semibold, FontWeight.Medium),
    Font(R.font.ascent_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ascent_sans_semibold, FontWeight.Bold)
)

val MonoNumbers = FontFamily(
    Font(R.font.ascent_mono_regular, FontWeight.Normal),
    Font(R.font.ascent_mono_semibold, FontWeight.Medium),
    Font(R.font.ascent_mono_semibold, FontWeight.SemiBold),
    Font(R.font.ascent_mono_semibold, FontWeight.Bold)
)

/** 数字/拉丁展示字体：Nunito ExtraBold（多邻国同款）。用于顶部数值、chips 数字等。 */
val DisplayFont = FontFamily(
    Font(R.font.ascent_display_bold, FontWeight.Bold),
    Font(R.font.ascent_display_bold, FontWeight.ExtraBold)
)

private val ascentTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SansBody,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MonoNumbers,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun AscentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val c = if (darkTheme) DarkAscent else LightAscent

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.ice,
            onPrimary = c.onIce,
            secondary = c.gold,
            onSecondary = c.onGold,
            background = c.bg,
            onBackground = c.txt,
            surface = c.panel,
            onSurface = c.txt,
            surfaceVariant = c.panel2,
            onSurfaceVariant = c.mut,
            outline = c.line,
            error = c.red
        )
    } else {
        lightColorScheme(
            primary = c.ice,
            onPrimary = c.onIce,
            secondary = c.gold,
            onSecondary = c.onGold,
            background = c.bg,
            onBackground = c.txt,
            surface = c.panel,
            onSurface = c.txt,
            surfaceVariant = c.panel2,
            onSurfaceVariant = c.mut,
            outline = c.line,
            error = c.red
        )
    }

    CompositionLocalProvider(LocalAscentColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ascentTypography,
            content = content
        )
    }
}
