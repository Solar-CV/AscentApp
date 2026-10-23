plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ascent.climb"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ascent.climb"
        minSdk = 26
        targetSdk = 34
        versionCode = 101
        versionName = "0.19.26"
        // 支持 10 种语言：简中（values 默认）+ 繁中 + 英日俄法瑞德西意。
        // 只打包这里列出的语言资源，避免把依赖库的其余语言一起带进来。
        resourceConfigurations += listOf(
            "zh", "zh-rCN", "zh-rTW", "en", "ja", "ru", "fr", "sv", "de", "es", "it"
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
