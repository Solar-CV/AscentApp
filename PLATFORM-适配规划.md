# ASCENT 平台适配规划（iOS / 鸿蒙）

> 2026-09-30 起草。对象：ASCENT 攀登 App（Android 原生，Kotlin 2.0.21 + Jetpack Compose，无第三方依赖，AGP 8.5.2）
> 背景：内容与逻辑已基本与 UI 解耦——题库是 JSON（bank.json）、海拔曲线/复习/解锁是纯 Kotlin 函数（contentcheck 已能在纯 JVM 跑），这对多端复用非常有利。

---

## 0. 结论速览

| 平台 | 推荐方案 | 理由 | 工作量估计 |
|---|---|---|---|
| **iOS** | **Kotlin Multiplatform + Compose Multiplatform**（复用同一套 Compose UI） | iOS 端自 CMP 1.8.0（2025-05）起 **正式稳定**，1.11（2026-05）持续完善；本工程 UI 全是 Compose，迁移 = 换壳 + 平台能力接口化 | 2–5 个专注日（需 macOS + Xcode） |
| **鸿蒙 HarmonyOS NEXT** | **ArkTS / ArkUI 原生重写**（官方唯一路径）；备选：腾讯 **Kuikly** 一码多端 | NEXT 已剥离 AOSP，APK 无法直接运行；新应用必须编译为 HAP + ArkTS/ArkUI | 1–2 周业余（复用全部内容 JSON 与规则逻辑） |
| （可选）Web | CMP Web 目标 / 静态站 | 可作鸿蒙过渡入口与推广页 | 1–3 天 |

---

## 1. iOS 适配（走 Compose Multiplatform）

### 1.1 为什么可行
- Compose Multiplatform for iOS 已在 **1.8.0（2025-05）达到 Stable / Production-ready**；当前 1.11.0（2026-05）继续改进 iOS 与 Web。
- 本 App 的 UI 是纯 Compose（无 Android View 混排、无第三方 UI 库），共享比例可以做到 ~90–95%。
- 数据层是纯 Kotlin（无 Android 依赖）：`Curriculum / Model / Altitude / BundledBank` 可直接放 commonMain。

### 1.2 改造清单（从当前工程出发）
1. **工程结构**：改为 `shared`（commonMain + androidMain/iosMain）+ `androidApp` + `iosApp`（Xcode 壳工程）三模块；Gradle 用 Kotlin Multiplatform 插件，Kotlin 升到 2.1+ 以配合新版 CMP。
2. **平台能力接口化（expect/actual）**：
   - 进度存储：`SharedPreferences` → **androidx DataStore-Preferences 多平台版**（或自写 NSUserDefaults expect/actual）。
   - 网络层：`HttpURLConnection`（BankRemote）→ **Ktor Client**（Darwin 引擎）/ NSURLSession；接口不变，只换实现。
   - 题图资源：assets 读取（BitmapFactory）→ **Compose Multiplatform Resources**（`composeResources`）或 `NSBundle` 读取；`LessonScreen.rememberAssetBitmap()` 是唯一改动点。
   - 时钟/时间格式、日志等小接口补 expect/actual。
3. **UI 细节**：
   - 中文字体：iOS 上用系统 PingFang SC（可 `systemFont`），或打包思源黑体保证与 Android 一致。
   - 安全区域（刘海/Home Indicator）：Compose iOS 需要 `WindowInsets` 适配。
   - 键盘行为（填空/计算题输入）、滚动回弹差异，逐页过一遍。
4. **发布侧**：
   - 需要 macOS + Xcode（本机 Mac mini M2 可以）；Apple 开发者账号 $99/年（真机长期安装与 App Store 必需；纯自测可临时签名 7 天）。
   - App Store 审核：教育/安全类内容需保留免责声明（App 内「课程安全声明」已具备），隐私政策页（目前 App 无数据收集，比较容易）。
   - 题库在线更新地址照旧 HTTPS 即可（无需 Android 的明文 HTTP 特例）。

### 1.3 风险与对策
- Compose iOS 的文本输入框、长列表性能细节需要真机验证；建议第一期先做「可跑通主线（路径→课程→测验→复习）」，UI 细节二期打磨。
- CMP 版本与 Kotlin 版本要配对升降（CMP 1.11 ↔ Kotlin 2.2.x 左右，落地时查官方兼容表）。

---

## 2. 鸿蒙适配（HarmonyOS NEXT）

### 2.1 现实约束（已核实）
- HarmonyOS NEXT（5.x 起）**完全剥离 AOSP**：Android APK 不能直接安装运行；仅华为应用市场对部分存量应用提供「兼容模式」，新应用必须编译为 **HAP**、以 **ArkTS/ArkUI** 开发。
- JetBrains 官方 **没有** Compose Multiplatform for HarmonyOS 支持（compose-multiplatform issue #4155 已关闭、无排期）。

### 2.2 路线 A（推荐）：ArkTS/ArkUI 原生重写
- **可 100% 复用**：`bank.json` 题库 + 全部课程文案（含技能书）、海拔曲线/复习间隔/解锁规则（都是几百行纯函数，翻译成 ArkTS 很容易）。
- **需要重写**：7 个页面 UI（Path / Lesson / Review / Training / Handbook / Profile / Main 壳）——ArkUI 也是声明式 UI（ArkTS + @Component），与 Compose 的心智模型接近。
- **平台能力**：进度用 `Preferences`（键值）或 `RelationalStore`；题库下载用 `@ohos.net.http` 或 rcp。
- **工具链**：DevEco Studio（IntelliJ 系，容易上手）；华为提供「**迁移评估工具**」可先扫 APK 生成 API 适配报告作为起点。
- **工作量**：约 1–2 周业余（内容与设计已冻结，主要是翻译式编码）；发布需华为开发者账号 + 实名，国内上架另有软著/备案等要求。

### 2.3 路线 B（备选）：腾讯 Kuikly（KMP 系跨端）
- 腾讯开源的 KMP 跨端框架，**一套 Kotlin 代码覆盖 Android / iOS / 鸿蒙 / Web / 小程序**；2025 年完成鸿蒙支持开源、鸿蒙 5 全面适配；**Compose DSL** 将于 2026 年正式发布（API 向官方 Compose 对齐）。
- 优势：如果决定「长期只用一套代码」，Kuikly 可同时解决 iOS + 鸿蒙（甚至 Web）；语言仍是 Kotlin。
- 代价：引入第三方框架（当前工程零依赖），生态较新；UI 仍需改写（Kuikly DSL 或其 Compose DSL 尚在推进）。
- 适用判断：如果 iOS 与鸿蒙都是硬需求、且愿意接受一个较新框架，可以评估「整体迁移 Kuikly」；否则更稳妥的是 **iOS 用 CMP、鸿蒙用 ArkTS**。

### 2.4 路线 C：等待官方 CMP 鸿蒙支持 —— 不建议（目前无排期）。

### 2.5 路线 D（过渡）：Web 版
- 用 CMP 的 Web 目标（或把内容做成静态 Web App）先提供浏览器可用的「看题/复习」轻量版；鸿蒙手机通过浏览器即可访问，也方便分享传播。
- 定位为过渡与推广，不是正式替代。

---

## 3. 架构准备建议（无论走哪条路线都值得做）

1. **抽出纯 Kotlin 核心模块**：`data/`（除 ProgressStore/Auth 的 Android 部分）已接近纯 Kotlin，正式建 `:core` 模块，KMP 与 ArkTS 侧都以「题库 JSON + 规则函数」为边界。
2. **内容接口稳定化**：`bank.json` 的 schema=2 已经是事实标准；鸿蒙/iOS/Web 都读同一份 JSON，后续改题只改一处。
3. **平台能力清单化**：把「存储 / 网络 / 资源 / 时钟」四件事写成小接口，iOS 走 expect/actual、鸿蒙走 ArkTS 实现。
4. **版本策略**：Android 主线（0.9.x）不受影响；iOS 分支 `ascent-ios`、鸿蒙分支 `ascent-ohos` 分别演进；题库版本（BUNDLED_VERSION）三端共享同一编号。

---

## 4. 建议执行顺序

1. （Android 基线）先按《REVIEW-711-通读校对报告》修完题（内容冻结后多端才有意义）。
2. （iOS）做 CMP 迁移，跑通主线 → TestFlight 内测。
3. （鸿蒙）用迁移评估工具扫一遍 → ArkTS 重写主线页面 → 华为内测（AppGallery Connect）。
4. （可选）Web 版 + 推广页。

> 提示：所有平台的题图（4.4 MB）与题库（547 KB）都已自包含，三端均可离线运行；在线更新走 HTTPS，
> iOS/鸿蒙都无需特殊配置。发布材料（免责声明、隐私说明）iOS 与华为市场均可复用同一套文案。

---

（本文件与 HANDOFF.md 一并维护；有新的适配决策请更新「建议执行顺序」与结论表。）
