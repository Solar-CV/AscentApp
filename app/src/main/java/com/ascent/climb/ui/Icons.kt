package com.ascent.climb.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 线型图标集（源自 lucide-static v1.52.0，ISC License）。
 * 24×24 网格 · stroke 2 · round cap/join。用 Icon(Ic.Xxx, tint = ...) 着色。
 * 纯 Compose 生成，不引入任何第三方图标库。
 */
object Ic {
    val Activity: ImageVector by lazy { icon("activity", "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2") }
    val ArrowLeft: ImageVector by lazy { icon("arrow-left", "m12 19-7-7 7-7", "M19 12H5") }
    val ArrowRight: ImageVector by lazy { icon("arrow-right", "M5 12h14", "m12 5 7 7-7 7") }
    val Award: ImageVector by lazy { icon("award", "m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526", "M6 8 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z") }
    val Backpack: ImageVector by lazy { icon("backpack", "M4 10a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z", "M8 10h8", "M8 18h8", "M8 22v-6a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v6", "M9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2") }
    val BatteryFull: ImageVector by lazy { icon("battery-full", "M10 10v4", "M14 10v4", "M22 14v-4", "M6 10v4", "M4 6 H16 A2 2 0 0 1 18 8 V16 A2 2 0 0 1 16 18 H4 A2 2 0 0 1 2 16 V8 A2 2 0 0 1 4 6 Z") }
    val Bell: ImageVector by lazy { icon("bell", "M10.268 21a2 2 0 0 0 3.464 0", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326") }
    val BookOpen: ImageVector by lazy { icon("book-open", "M12 5v16", "M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z") }
    val Book: ImageVector by lazy { icon("book", "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20") }
    val Bookmark: ImageVector by lazy { icon("bookmark", "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z") }
    val Brain: ImageVector by lazy { icon("brain", "M12 18V5", "M15 13a4.17 4.17 0 0 1-3-4 4.17 4.17 0 0 1-3 4", "M17.598 6.5A3 3 0 1 0 12 5a3 3 0 1 0-5.598 1.5", "M17.997 5.125a4 4 0 0 1 2.526 5.77", "M18 18a4 4 0 0 0 2-7.464", "M19.967 17.483A4 4 0 1 1 12 18a4 4 0 1 1-7.967-.517", "M6 18a4 4 0 0 1-2-7.464", "M6.003 5.125a4 4 0 0 0-2.526 5.77") }
    val Calendar: ImageVector by lazy { icon("calendar", "M8 2v3", "M16 2v3", "M5 3 H19 A2 2 0 0 1 21 5 V19 A2 2 0 0 1 19 21 H5 A2 2 0 0 1 3 19 V5 A2 2 0 0 1 5 3 Z", "M3 9h18") }
    val CheckSquare: ImageVector by lazy { icon("check-square", "M21 10.656V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h12.344", "m9 11 3 3L22 4") }
    val Check: ImageVector by lazy { icon("check", "M20 6 9 17l-5-5") }
    val ChevronDown: ImageVector by lazy { icon("chevron-down", "m6 9 6 6 6-6") }
    val ChevronLeft: ImageVector by lazy { icon("chevron-left", "m15 18-6-6 6-6") }
    val ChevronRight: ImageVector by lazy { icon("chevron-right", "m9 18 6-6-6-6") }
    val Globe: ImageVector by lazy {
        icon(
            "globe",
            "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z",
            "M2 12h20",
            "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"
        )
    }
    val ChevronUp: ImageVector by lazy { icon("chevron-up", "m18 15-6-6-6 6") }
    val CircleCheck: ImageVector by lazy { icon("circle-check", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", "m16 9-5.5 5.5L8 12") }
    val CircleDot: ImageVector by lazy { icon("circle-dot", "M11 12 a1 1 0 1 0 2 0 a1 1 0 1 0 -2 0 Z", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z") }
    val CircleX: ImageVector by lazy { icon("circle-x", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", "m15 9-6 6", "m9 9 6 6") }
    val Clock: ImageVector by lazy { icon("clock", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", "M12 6v6l4 2") }
    val CloudSnow: ImageVector by lazy { icon("cloud-snow", "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242", "M8 15h.01", "M8 19h.01", "M12 17h.01", "M12 21h.01", "M16 15h.01", "M16 19h.01") }
    val Cloud: ImageVector by lazy { icon("cloud", "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z") }
    val Compass: ImageVector by lazy { icon("compass", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", "m16.24 7.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z") }
    val Database: ImageVector by lazy { icon("database", "M3 5 a9 3 0 1 0 18 0 a9 3 0 1 0 -18 0 Z", "M3 5V19A9 3 0 0 0 21 19V5", "M3 12A9 3 0 0 0 21 12") }
    val Download: ImageVector by lazy { icon("download", "M12 15V3", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5") }
    val Dumbbell: ImageVector by lazy { icon("dumbbell", "M17.596 12.768a2 2 0 1 0 2.829-2.829l-1.768-1.767a2 2 0 0 0 2.828-2.829l-2.828-2.828a2 2 0 0 0-2.829 2.828l-1.767-1.768a2 2 0 1 0-2.829 2.829z", "m2.5 21.5 1.4-1.4", "m20.1 3.9 1.4-1.4", "M5.343 21.485a2 2 0 1 0 2.829-2.828l1.767 1.768a2 2 0 1 0 2.829-2.829l-6.364-6.364a2 2 0 1 0-2.829 2.829l1.768 1.767a2 2 0 0 0-2.828 2.829z", "m9.6 14.4 4.8-4.8") }
    val Ellipsis: ImageVector by lazy { icon("ellipsis", "M11 12 a1 1 0 1 0 2 0 a1 1 0 1 0 -2 0 Z", "M18 12 a1 1 0 1 0 2 0 a1 1 0 1 0 -2 0 Z", "M4 12 a1 1 0 1 0 2 0 a1 1 0 1 0 -2 0 Z") }
    val ExternalLink: ImageVector by lazy { icon("external-link", "M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6") }
    val Flag: ImageVector by lazy { icon("flag", "M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 7.333 2q2 0 3.067-.8A1 1 0 0 1 20 4v10a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.528") }
    val Flame: ImageVector by lazy { icon("flame", "M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4") }
    val Folder: ImageVector by lazy { icon("folder", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z") }
    val Footprints: ImageVector by lazy { icon("footprints", "M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16a2 2 0 1 1-4 0Z", "M20 20v-2.38c0-2.12 1.03-3.12 1-5.62-.03-2.72-1.49-6-4.5-6C14.63 6 14 7.8 14 9.5c0 3.11 2 5.66 2 8.68V20a2 2 0 1 0 4 0Z", "M16 17h4", "M4 13h4") }
    val GraduationCap: ImageVector by lazy { icon("graduation-cap", "M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z", "M22 10v6", "M6 12.5V16a6 3 0 0 0 12 0v-3.5") }
    val HeartPulse: ImageVector by lazy { icon("heart-pulse", "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5", "M3.22 13H9.5l.5-1 2 4.5 2-7 1.5 3.5h5.27") }
    val Info: ImageVector by lazy { icon("info", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", "M12 16v-4", "M12 8h.01") }
    val Lightbulb: ImageVector by lazy { icon("lightbulb", "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5", "M9 18h6", "M10 22h4") }
    val Link: ImageVector by lazy { icon("link", "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71", "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71") }
    val Lock: ImageVector by lazy { icon("lock", "M5 11 H19 A2 2 0 0 1 21 13 V20 A2 2 0 0 1 19 22 H5 A2 2 0 0 1 3 20 V13 A2 2 0 0 1 5 11 Z", "M7 11V7a5 5 0 0 1 10 0v4") }
    val LogOut: ImageVector by lazy { icon("log-out", "m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4") }
    val MapPin: ImageVector by lazy { icon("map-pin", "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0", "M9 10 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z") }
    val Map: ImageVector by lazy { icon("map", "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z", "M15 5.764v15", "M9 3.236v15") }
    val MessageSquare: ImageVector by lazy { icon("message-square", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z") }
    val Maximize2: ImageVector by lazy { icon("maximize-2", "M15 3h6v6", "m21 3-7 7", "m3 21 7-7", "M9 21H3v-6") }
    val Medal: ImageVector by lazy { icon("medal", "M7.21 15 2.66 7.14a2 2 0 0 1 .13-2.2L4.4 2.8A2 2 0 0 1 6 2h12a2 2 0 0 1 1.6.8l1.6 2.14a2 2 0 0 1 .14 2.2L16.79 15", "M11 12 5.12 2.2", "m13 12 5.88-9.8", "M8 7h8", "M7 17 a5 5 0 1 0 10 0 a5 5 0 1 0 -10 0 Z", "M12 18v-2h-.5") }
    val Minus: ImageVector by lazy { icon("minus", "M5 12h14") }
    val Moon: ImageVector by lazy { icon("moon", "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401") }
    val MountainSnow: ImageVector by lazy { icon("mountain-snow", "m8 3 4 8 5-5 5 15H2L8 3z", "M4.14 15.08c2.62-1.57 5.24-1.43 7.86.42 2.74 1.94 5.49 2 8.23.19") }
    val Mountain: ImageVector by lazy { icon("mountain", "m8 3 4 8 5-5 5 15H2L8 3z") }
    val Newspaper: ImageVector by lazy { icon("newspaper", "M15 18h-5", "M18 14h-8", "M4 22h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-4 0v-9a2 2 0 0 1 2-2h2", "M11 6 H17 A1 1 0 0 1 18 7 V9 A1 1 0 0 1 17 10 H11 A1 1 0 0 1 10 9 V7 A1 1 0 0 1 11 6 Z") }
    val Play: ImageVector by lazy { icon("play", "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z") }
    val Plus: ImageVector by lazy { icon("plus", "M5 12h14", "M12 5v14") }
    val RefreshCw: ImageVector by lazy { icon("refresh-cw", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5") }
    val RotateCcw: ImageVector by lazy { icon("rotate-ccw", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5") }
    val Ruler: ImageVector by lazy { icon("ruler", "M21.3 15.3a2.4 2.4 0 0 1 0 3.4l-2.6 2.6a2.4 2.4 0 0 1-3.4 0L2.7 8.7a2.41 2.41 0 0 1 0-3.4l2.6-2.6a2.41 2.41 0 0 1 3.4 0Z", "m14.5 12.5 2-2", "m11.5 9.5 2-2", "m8.5 6.5 2-2", "m17.5 15.5 2-2") }
    val Search: ImageVector by lazy { icon("search", "m21 21-4.34-4.34", "M3 11 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 Z") }
    val Settings: ImageVector by lazy { icon("settings", "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915", "M9 12 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z") }
    val Share2: ImageVector by lazy { icon("share-2", "M15 5 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z", "M3 12 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z", "M15 19 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0 Z", "M8.59 13.51 L15.42 17.49", "M15.41 6.51 L8.59 10.49") }
    val ShieldAlert: ImageVector by lazy { icon("shield-alert", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "M12 8v4", "M12 16h.01") }
    val Signal: ImageVector by lazy { icon("signal", "M2 20h.01", "M7 20v-4", "M12 20v-8", "M17 20V8", "M22 4v16") }
    val SlidersHorizontal: ImageVector by lazy { icon("sliders-horizontal", "M10 5H3", "M12 19H3", "M14 3v4", "M16 17v4", "M21 12h-9", "M21 19h-5", "M21 5h-7", "M8 10v4", "M8 12H3") }
    val Snowflake: ImageVector by lazy { icon("snowflake", "m10 20-1.25-2.5L6 18", "M10 4 8.75 6.5 6 6", "m14 20 1.25-2.5L18 18", "m14 4 1.25 2.5L18 6", "m17 21-3-6h-4", "m17 3-3 6 1.5 3", "M2 12h6.5L10 9", "m20 10-1.5 2 1.5 2", "M22 12h-6.5L14 15", "m4 10 1.5 2L4 14", "m7 21 3-6-1.5-3", "m7 3 3 6h4") }
    val Sparkles: ImageVector by lazy { icon("sparkles", "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z", "M20 2v4", "M22 4h-4", "M2 20 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0 Z") }
    val SquareCheckBig: ImageVector by lazy { icon("square-check-big", "M21 10.656V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h12.344", "m9 11 3 3L22 4") }
    val Star: ImageVector by lazy { icon("star", "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z") }
    val Sun: ImageVector by lazy { icon("sun", "M8 12 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41") }
    val Target: ImageVector by lazy { icon("target", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 Z", "M6 12 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 Z", "M10 12 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0 Z") }
    val Tent: ImageVector by lazy { icon("tent", "M3.5 21 14 3", "M20.5 21 10 3", "M15.5 21 12 15l-3.5 6", "M2 21h20") }
    val Timer: ImageVector by lazy { icon("timer", "M10 2 L14 2", "M12 14 L15 11", "M4 14 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 Z") }
    val TriangleAlert: ImageVector by lazy { icon("triangle-alert", "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01") }
    val Trophy: ImageVector by lazy { icon("trophy", "M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2", "M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2", "M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3", "M4 22h16", "M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z", "M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3") }
    val Upload: ImageVector by lazy { icon("upload", "M12 3v12", "m17 8-5-5-5 5", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4") }
    val UserRound: ImageVector by lazy { icon("user-round", "M7 8 a5 5 0 1 0 10 0 a5 5 0 1 0 -10 0 Z", "M20 21a8 8 0 0 0-16 0") }
    val User: ImageVector by lazy { icon("user", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M8 7 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z") }
    val Users: ImageVector by lazy { icon("users", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M16 3.128a4 4 0 0 1 0 7.744", "M22 21v-2a4 4 0 0 0-3-3.87", "M5 7 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z") }
    val Wifi: ImageVector by lazy { icon("wifi", "M12 20h.01", "M2 8.82a15 15 0 0 1 20 0", "M5 12.859a10 10 0 0 1 14 0", "M8.5 16.429a5 5 0 0 1 7 0") }
    val Wind: ImageVector by lazy { icon("wind", "M12.8 19.6A2 2 0 1 0 14 16H2", "M17.5 8a2.5 2.5 0 1 1 2 4H2", "M9.8 4.4A2 2 0 1 1 11 8H2") }
    val X: ImageVector by lazy { icon("x", "M18 6 6 18", "m6 6 12 12") }
    val ZoomIn: ImageVector by lazy { icon("zoom-in", "M3 11 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 Z", "M21 21 L16.65 16.65", "M11 8 L11 14", "M8 11 L14 11") }

    // ---------------- 课程大类图标（实心卡通，v0.19.28） ----------------
    //
    // 这两个不是线型图标，而是实心剪影：24 网格、evenodd 挖孔。
    // 用 filledIcon 生成后仍然可以用 Icon(tint = ...) 整体着色，
    // 所以选中态（绿底白图标）与未选中态（灰底灰图标）共用同一份路径。
    //
    // 雪山：三角山体 + 一条锯齿雪线（挖空）。雪山本身比「▲」多一层信息，
    // 用户一眼能看出是「高山」而不是任意三角形。
    val TrackClimb: ImageVector by lazy {
        filledIcon(
            "track-climb",
            "M3.6 19.9 L10.3 6.6 Q12 3.5 13.7 6.6 L20.4 19.9 Q20.9 21 19.7 21 H4.3 Q3.2 21 3.6 19.9 Z" +
                " M7.95 12.2 Q9.3 13.1 10 14.3 Q10.9 12.5 12 11.4 Q13.1 12.5 14 14.3 Q14.7 13.1 16.05 12.2" +
                " L16.95 13.8 Q15.4 14.6 14.2 16 Q13.2 14.3 12 13.1 Q10.8 14.3 9.8 16 Q8.6 14.6 7.05 13.8 Z"
        )
    }

    // 徒步：登山靴（鞋帮 + 带齿鞋底 + 两道鞋带）。原来的「≡」三条横杠
    // 完全看不出含义；靴子在 44dp 下轮廓清楚，也不与雪山混淆。
    val TrackTrek: ImageVector by lazy {
        filledIcon(
            "track-trek",
            "M7.4 6.2 H12.2 Q13 6.2 13 7 V11 L18.9 14.3 Q19.9 14.9 19.9 15.9 V16.4 H7.4 Q6.6 16.4 6.6 15.6 V7 Q6.6 6.2 7.4 6.2 Z" +
                " M5.9 16.9 H20.3 Q21.1 16.9 21.1 17.7 V19 Q21.1 19.8 20.3 19.8 H5.9 Q5.1 19.8 5.1 19 V17.7 Q5.1 16.9 5.9 16.9 Z" +
                " M7.8 19.1 H9.4 V19.9 H7.8 Z M11.6 19.1 H13.2 V19.9 H11.6 Z M15.4 19.1 H17 V19.9 H15.4 Z" +
                " M8 8 H11.2 V8.9 H8 Z M8 10 H11.2 V10.9 H8 Z"
        )
    }
}

private fun filledIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        for (d in paths) {
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.Black)
            )
        }
    }.build()

private fun icon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        for (d in paths) {
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
