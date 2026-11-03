import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * 发布签名：密钥**不在仓库里**。
 *
 * 读取顺序：
 *   1. 环境变量 `ASCENT_KEYSTORE_PROPS` 指向的属性文件（CI 用，值从 secrets 落地到临时路径）
 *   2. `$HOME/AscentBackUp/keystore/keystore.properties`（本机默认）
 *
 * 找不到就跳过签名、打一行警告，release 构建会产出未签名包 ——
 * 目的是让「没有密钥的机器」（比如 CI 上只跑 debug 的场景）也能正常构建，不会因为签名缺失直接失败。
 */
val keystorePropsFile: File = System.getenv("ASCENT_KEYSTORE_PROPS")
    ?.takeIf { it.isNotBlank() }
    ?.let { File(it) }
    ?: File(System.getProperty("user.home"), "AscentBackUp/keystore/keystore.properties")

val keystoreProps = Properties().apply {
    if (keystorePropsFile.isFile) keystorePropsFile.inputStream().use { load(it) }
}

val releaseStoreFile: File? =
    keystoreProps.getProperty("storeFile")?.let { File(it) }?.takeIf { it.isFile }

val hasReleaseKey = releaseStoreFile != null
if (!hasReleaseKey) {
    logger.warn("[ascent] 未找到发布签名：${keystorePropsFile.absolutePath}（release 将产出未签名包）")
}

android {
    namespace = "com.ascent.climb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ascent.climb"
        minSdk = 26
        targetSdk = 36
        versionCode = 141
        versionName = "0.25.0"
        // 支持 10 种语言：简中（values 默认）+ 繁中 + 英日俄法瑞德西意。
        // 只打包这里列出的语言资源，避免把依赖库的其余语言一起带进来。
        resourceConfigurations += listOf(
            "zh", "zh-rCN", "zh-rTW", "en", "ja", "ru", "fr", "sv", "de", "es", "it"
        )
        // 3D 徽章 POC：Filament 体积几乎全在原生库上，先只打 64 位，省掉 x86/armeabi 约 6 MB。
        // 正式接入时改用 App Bundle 按 ABI 分发，或保留 armeabi-v7a 覆盖 32 位老机。
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // 与 release 版共存：同一 applicationId 在设备上只能装一个，
            // 加后缀后变成独立应用（独立的 prefs / 数据目录 / 桌面图标）。
            // 副作用：debug 与 release 的学习进度不互通，这是预期行为。
            applicationIdSuffix = ".debug"
            // 设置页与反馈邮件里显示 0.24.0-debug，便于区分是哪一路包在跑。
            // 本工程没有解析 VERSION_NAME 的逻辑，所以后缀不影响任何比较。
            versionNameSuffix = "-debug"
        }
        release {
            // 正式包开 R8 + 资源压缩。Filament / gltfio 走 JNI 按名查找，
            // 保留规则见 proguard-rules.pro。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // 16 KB 页兼容：`useLegacyPackaging = true`（旧默认）会按 4 KB 对齐 .so，
        // 在 16 KB 页设备上 dlopen 直接失败。显式关掉，让 AGP 走未压缩 + 16 KB 对齐
        // 的打包路径；Filament / gltfio 的 ELF 本身已经是 p_align=0x4000（链接期定死，
        // 打包期改不了），所以只差这一步。用 `tools/check_16k.py` 回归。
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // 3D 徽章：Filament（渲染）+ gltfio（glb 加载）+ utils（KTX 环境光加载）
    // 锁 1.75.0：1.76.0 起 AAR 声明 minCompileSdk=37，本工程 compileSdk 36 仍差一档
    //（要上 1.76 得先升到 API 37 + AGP 8.13+）。1.75.0 的四个 .so 已经是 16 KB 页对齐
    //（p_align=0x4000，见 tools/check_16k.py），所以这条依赖不阻塞 Play 的 16 KB 要求。
    val filament = "1.75.0"
    implementation("com.google.android.filament:filament-android:$filament")
    implementation("com.google.android.filament:gltfio-android:$filament")
    implementation("com.google.android.filament:filament-utils-android:$filament")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

/**
 * 简体中文的「精确」资源目录（values-zh-rCN）。
 *
 * 背景：`res/values/` 是简体中文，但它没有任何语言限定符，而 `values-zh-rTW` 有。
 * 系统语言是 zh-CN 时，Android 找不到 zh-rCN 桶，就退到「同语言变体」命中了
 * 繁体桶 —— 表现就是简体系统上界面全变繁体，手动切一次简体才正常
 *（手动选语言时框架会带上脚本子标签，请求变精确，才绕开繁体桶）。
 *
 * 修法是给 zh-CN 一个精确桶：构建时把 `values/strings.xml` 复制成
 * `values-zh-rCN/strings.xml`，精确匹配永远赢过繁体桶。
 *
 * 单一事实来源仍然是 `res/values/strings.xml`，**不要手改生成目录**。
 */
val zhCnResDir: Provider<Directory> = layout.buildDirectory.dir("generated/res/zh-cn")

val genZhCnRes = tasks.register<Copy>("genZhCnRes") {
    from(layout.projectDirectory.file("src/main/res/values/strings.xml"))
    into(zhCnResDir.map { it.dir("values-zh-rCN") })
}

android.sourceSets.getByName("main").res.srcDir(zhCnResDir)

// preBuild 可能晚于脚本执行才注册，用 configureEach 兜住两种时序
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(genZhCnRes) }
