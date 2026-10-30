# ASCENT —— R8 保留规则（release 构建）
#
# 背景：Filament / gltfio 的渲染内核是 native 代码，Java 侧通过两类方式被 native 反查：
#   1. env->FindClass("com/google/android/filament/...")  —— 按**类名**查找
#   2. env->RegisterNatives(...) / GetMethodID("...")     —— 按**方法名 + 签名**查找
# 类名一被混淆，native 侧就找不到，表现通常是 UnsatisfiedLinkError，
# 或者更隐蔽：渲染线程静默失败、徽章一片黑。
#
# 三个 Filament AAR 自带 proguard.txt（用 @UsedBy 注解标了被 native 引用的成员），
# 但那套规则只为「被注解的部分」兜底。这里再加一道包级保留，
# 代价是几百个类不参与混淆（对 40 MB 的包体可以忽略），换来的是不用逐个排查注解遗漏。

-keep class com.google.android.filament.** { *; }
-keep class com.google.android.gltfio.** { *; }
-dontwarn com.google.android.filament.**

# Filament 的 @UsedBy 注解本身要在 R8 处理完之后仍然存在
-keep @interface com.google.android.filament.proguard.UsedBy*
-keep @com.google.android.filament.proguard.UsedBy* class * { *; }
-keepclassmembers class * {
    @com.google.android.filament.proguard.UsedBy* *;
}

# ---- 项目自身 ----
# 成就 / 题库 / 多语言包是纯 Kotlin 静态数据，按引用即可，无需额外保留。
# 唯一需要留意的是「按字符串键反射读取」的场景目前不存在，
# 若以后引入（例如用类名动态加载页面），请在此处补 -keep。

-dontwarn org.jetbrains.annotations.**
