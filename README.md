# ASCENT · 攀登之路 — Android 测试版

高海拔攀登 / 攀冰培训 App，采用多邻国式游戏化学习路径。

**核心机制：你的学习进度就是海拔。** 每通过一课向上攀升若干米，路线由 5 个海拔单元组成：
5000M 入门 → 5000M 进阶 → 6000M → 7000M → 8000M，终点 8,000 m。

当前版本：**v0.15.0**（versionCode 32）

---

## 一、安装

1. 把 `ascent-v0.15.0-debug.apk` 传到 Android 手机
2. 手机上打开「允许安装未知来源应用」
3. 点击 APK 安装，打开即可

- 包名：`com.ascent.climb` · 版本 0.15.0
- 最低系统：Android 8.0（API 26）
- 体积：约 16.3 MB
- 权限：联网（仅用于拉取题库更新）；离线时功能完全不受影响

---

## 二、功能

| 模块 | 内容 |
|---|---|
| 路径 | 5 海拔单元 / **47 课**，线性解锁，蜿蜒绳索路径，完成度以海拔呈现 |
| 技能书 | **31 篇**专题文章（向导资料 + 中文登山书籍整理），每篇 4–8 节 |
| 测验 | **711 道题**，题量随海拔递增（平均 11 → 12 → 14 → 20 → 19 题/课），含安全关键题（加权 + 强制理解后继续） |
| 复习题 | 单元 2 起每课自动穿插前面单元的内容重答（间隔重复），共 82 道 |
| 错题重做 | 每课答题结束后自动拉出错题再答一遍，答对 +20m；仍错的进入复习中心 |
| 训练 | 目标峰海拔、单元阶段进度、准备度百分比 |
| 复习 | 错题队列（安全关键题置顶）、待教练评估清单 |
| 我的 | 档案头、海拔、能力概览、**徽章入口**、账号、声明 |
| 个人主页 | 点头像进入：学习概览（连续 / 累计天数、完成课程、徽章数）+ 已解锁徽章 |
| 徽章墙 | **35 枚**成就徽章，按分类分组，未解锁灰度预览，隐藏成就显示「?」 |
| 设置 | **齿轮入口**：外观（跟随系统 / 浅色 / 深色）、题库、开发者模式、数据重置、关于 |
| 外观 | 跟随系统 **或手动指定** 浅色 / 深色，偏好持久化（含状态栏配色） |

游戏化元素：🔥 连续天数、🪢 安全绳余量（答错扣减，每日重置）、⭐ 课程星级（全对 3 星 / ≥60% 2 星）、海拔累积。

### 近期更新

**v0.15.0** — 技能书补充中文书籍篇：新增 9 篇（h23–h31），总数 22 → **31 篇**，新增「户外领导力」分类
（领导力定义与软技能 / 制定最佳决策 / 领队责任与冲突 / 山区导航定向 / 营地与 LNT 环保 /
迷路与求救 / 脱水与体温过高 / 安全原则与风险管理 / 营养与能量策略）。
出处展示规则确定：仅显示 SEVEN SUMMIT TREKS 来源，中文书籍篇不显示来源区块。

**v0.14.0** — 技能书新增 6 篇 SST 向导内容（睡眠系统 / 背包收纳 / 个人护理 / 探险医疗 /
高原病风险人群 / 探险规定与登顶认证），16 → 22 篇。

**v0.13.1** — 次级页面（设置 / 个人主页 / 徽章墙 / 成就测试区 / 登录 / 手册长文 / 解锁弹窗）
接入返回手势拦截，全面屏侧滑回到上一界面而非直接退出 App。

**v0.13.0** — 新增设置页（外观 / 题库 / 开发者模式 / 数据 / 关于）、个人主页、全部徽章墙；
「我的」页 35 枚徽章墙收敛为紧凑入口卡；主题偏好持久化。

**v0.12.0** — 35 枚成就徽章全部重绘为 AI 渲染收藏品质感（按成就语义分配材质与边框类型），
作为 256px WebP 位图资源集成；`ui/BadgeArt.kt` 做 id → 资源映射。

**v0.11.0** — 成就系统落地：35 枚成就定义 + 解锁判定 + 弹窗 + 开发者测试区。

---

### v0.5.0 更新

- **题目溯源**：每道题新增 `source` 字段，答题后显示「来源 · 书名 页码」，为教练/医生逐条审核提供依据
- **实体书入库流水线**：`import/*.md`（Markdown）→ 一条命令编译成题库
- **内容溯源原则**：只取书中的知识事实、用自己的话重写题干与解析，并标注出处

### v0.4.0 更新

- **题库区块化**：新增 `data/bank/` 包，题库的载入/缓存/更新统一收口在 `BankRepository`
- **在线题库**：JSON 作为在线题库，启动时自动比对版本，比本地新则下载校验后覆盖本地缓存
- **离线兜底**：拉取失败 / 校验不通过 / 版本更旧，一律保持现状；内置题库始终可用
- 「我的」页新增题库区：当前版本、来源、检查更新、恢复内置题库
- 开发者模式新增「在线题库地址」输入框，可不重新编译就切换服务器
- 内容校验工具升级为 `tools/contentcheck/run.sh`，一条命令完成校验 + 导出在线题库

### v0.3.0 更新

- **课程长度随难度递增**：大本营 5 题/课 → C1 8 题 → C2 9 题 → 高营 10 题 → 顶峰 11 题（总题量 110 → 182）
- **自动插入复习题**：营地 2 起每课额外抽 2 道前面单元的题重答，答题界面标记「复习题 · 来自前面单元」
- **隐藏开发者模式**：在「我的」页底部连点版本号 7 次解锁，可一键解锁全部关卡、显示正确答案
- 新增内容校验工具 `tools/contentcheck`（纯 JVM，直接跑真实数据验证）

### 隐藏开发者模式

在「我的」页最底部连点版本号 **7 次**（1.6 秒内连续点击）即可解锁。解锁后「我的」页会出现开发者选项：

| 选项 | 作用 |
|---|---|
| 解锁全部关卡 | 跳过前置要求，22 门课程全部可进入（节点显示为「可挑战」） |
| 显示正确答案 | 答题前在正确选项旁标记 `✓ KEY` |
| 关闭开发者模式 | 关闭并清除以上开关（不会动学习进度） |

开发者模式状态存在本机，且「重置学习进度」不会清除它。

### v0.2.0 更新

- 日间 / 夜间主题跟随系统自动切换（新增浅色配色：冰川白 + 深青 + 深金）
- 每关结束后新增「错题重做」环节
- 课程题量从每题 3 题扩充到 5 题（60 → 110 题），新增 2 门装备课程
- 「我的」新增登录 / 注册界面（`AuthApi` 接口 + 本地模拟实现）
- 整合用户知识库中的三份向导资料，新增 4 篇技能书文章

---

## 三、课程内容体系

| 营地 | 海拔 | 课程 |
|---|---|---|
| 单元 | 海拔 | 课程数 | 题量 | 平均每课 |
|---|---|---|---|---|
| 5000M · 入门 | 5,000 m | 12 | 135 | 11 题 |
| 5000M · 进阶 | 5,000 m | 9 | 112 | 12 题 |
| 6000M | 6,000 m | 10 | 143 | 14 题 |
| 7000M | 7,000 m | 8 | 163 | 20 题 |
| 8000M | 8,000 m | 8 | 158 | 19 题 |

大纲规划 47 课、已实现 47 课、待补齐 0。详细课程清单见 `HANDOFF.md`。

### 内容来源

**公共权威资料**

| 领域 | 主要依据 |
|---|---|
| 高海拔医学 | Wilderness Medical Society《急性高原病临床实践指南》2024 更新版；CDC Yellow Book；StatPearls 流行病学数据 |
| 冰雪技术 | American Alpine Institute（自制动 / 冰爪技术）；American Alpine Club《雪坡攀登原理》；REI Expert Advice；BMC 冬季技能 |
| 攀冰分级 | UIAA《攀登难度分级体系》；Alpinist 分级对照表 |
| 绳索与救援 | AMGA / NOLS 绳索系统框架；Alpine Savvy 现代冰川救援；Cascade Mountain Ascents |
| 雪崩 | avalanche.org 雪崩百科（96% 发生在 30°–50°，多数集中在 34°–45°） |
| 装备标准 | UIAA / CE（EN 13089 冰镐、EN 893 冰爪、UIAA 105 安全带、EN 12492 头盔） |

**用户知识库（SEVEN SUMMIT TREKS 向导资料）**

源文件位于 `~/技能书/`：

| 文件 | 已整合为 |
|---|---|
| `了解高原病.docx` | 《高原病处置速查（向导版）》+ 营地 4 与营地 1 的相关题目（地塞米松 8–10mg 负荷剂量、HAPE 并发 HACE 13%–20%、4000m 以上发病率 50%–65% 等） |
| `装备指南.docx` | 《6000–8000 米装备清单（一）穿着与鞋类》《（二）技术器材与通讯》+ 新增 2 门课程（冰镐按身高选长、12 齿钢制冰爪、可拆卸内胆登顶靴等） |
| `探险活动通用指南.docx` | 《探险活动行前须知》（保险、航班取消、行李与挑夫、营地纪律、登顶证书） |

---

## 四、安全边界（已内置在 App 文案中）

- 本 App 只覆盖**理论与认知准备**，冰雪、绳索、攀冰、救援等实践技能必须由持证教练现场指导与评估
- 内容**不构成医疗建议**，涉及药物与急救的决策须咨询专业医生
- 不承诺登顶，也不保证高海拔适应结果
- 标注 `需教练评估` 的课程（自我制动、攀冰）在完成理论后会明确提示实操验证要求

**建议在上线前请持证教练与医生逐条审核**，重点是药物剂量与急救流程。

---

## 五、重新构建

工具链在本机 `~/android-toolchain/`（无需 Homebrew / Android Studio）：

- `jdk/` — Temurin OpenJDK 17.0.20.1（清华 Adoptium 镜像）
- `sdk/` — Android SDK：platforms;android-34、build-tools;34.0.0、platform-tools
- `gradle-8.9/` — Gradle 8.9（腾讯镜像）

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd ~/AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

依赖镜像已在 `settings.gradle.kts` 配置阿里云优先。
技术栈：Kotlin + Jetpack Compose（Material 3）· AGP 8.5.2 / Kotlin 2.0.21 / Gradle 8.9。

---

## 六、项目结构

```
~/AscentApp/
├── import/                      题库编写（Markdown，实体书整理）
│   └── README.md                格式说明与录入原则
├── bank/                        在线题库产物（bank.json + manifest.json，传到服务器即可）
├── tools/
│   ├── contentcheck/            内容校验 + 题库导出（纯 JVM）
│   │   ├── run.sh               一条命令：同步 → 校验 → 导出
│   │   ├── Check.kt
│   │   └── BankExport.kt
│   └── qbank/build.mjs          Markdown → 题库编译器
└── app/src/main/
    ├── AndroidManifest.xml
    ├── res/
    │   ├── values/            日间主题 + 颜色 + 字符串
    │   ├── values-night/      夜间主题（系统深色时生效）
    │   ├── drawable/          矢量启动图标（山峰 + 登顶旗）
    │   └── mipmap-anydpi-v26/ 自适应图标
    └── java/com/ascent/climb/
        ├── MainActivity.kt        底部导航 + 全屏课程 + 题库检查触发
        ├── data/
        │   ├── Model.kt           数据模型
        │   ├── Unit1~5.kt         主课程内容
        │   ├── Expansion.kt       扩充包一：补充题目 + 2 门装备课程
        │   ├── Expansion2.kt      扩充包二：进阶单元加题（难度递增）
        │   ├── BundledBank.kt     内置题库构建（纯 Kotlin，工具也复用这个文件）
        │   ├── BookContent.kt     ← 由 import/*.md 自动生成，勿手改
        │   ├── Content.kt         界面统一数据入口 + 解锁规则
        │   ├── Auth.kt            AuthApi 登录接口 + MockAuthApi 本地实现
        │   ├── ProgressStore.kt   本地进度持久化
        │   ├── bank/              ← 题库区块
        │   │   ├── BankConfig.kt      配置常量（版本号、超时、体积上限）
        │   │   ├── BankJson.kt        JSON 解码（org.json，零依赖）
        │   │   ├── BankRemote.kt      网络层（HttpURLConnection，零依赖）
        │   │   └── BankRepository.kt  统一入口：缓存 → 内置 + 在线更新
        │   ├── HandbookA/B.kt     技能书（公共资料）
        │   ├── HandbookC/D.kt     技能书（SEVEN SUMMIT TREKS 向导资料）
        │   ├── HandbookE.kt       技能书（中文登山书籍整理，来源不展示）
        │   └── Achievements.kt    35 枚成就定义（id / 条件 / 稀有度）
        └── ui/
            ├── Theme.kt           双主题配色 + LocalAscentColors
            ├── Components.kt      等高线背景、卡片、进度条、按钮
            ├── PathScreen.kt      攀登路径
            ├── LessonScreen.kt    技能要点 → 测验 → 错题重做 → 结算
            ├── HandbookScreen.kt  技能书
            ├── TrainingScreen.kt  训练计划
            ├── ReviewScreen.kt    复习中心
            ├── ProfileScreen.kt   我的（档案头 / 海拔 / 能力概览 / 徽章入口 / 账号）
            ├── SettingsScreen.kt  设置页
            ├── ProfileHomeScreen.kt 个人主页
            ├── BadgeWallScreen.kt 全部徽章墙
            ├── Badges.kt          徽章渲染（位图 + 三态）
            ├── BadgeArt.kt        成就 id → 徽章资源映射
            ├── AchievementsDevScreen.kt 成就测试区
            └── AchievementUnlock.kt     解锁弹窗
```

### 扩展内容的方式

新增题目或课程**不需要改动已审核的 Unit1~5**，只需在 `data/Expansion.kt` 或 `data/Expansion2.kt` 里：

```kotlin
// 给已有课程加题
val EXTRA_QUESTIONS = mapOf("l5" to listOf(Question(...)))

// 给营地加新课程
val EXTRA_LESSONS = mapOf("u1" to listOf(myNewLesson))
```

`Content.kt` 会自动合并，并按全局课序号插入复习题。

### 内容校验与题库导出

```bash
bash ~/AscentApp/tools/contentcheck/run.sh
```

这一个命令会：把 App 的内容源同步进纯 JVM 工程 → 跑完整校验（题量、难度递增、答案越界、空题、解锁逻辑）→ 导出 `bank/bank.json` 与 `bank/manifest.json`。

改动内容后请同步把 `bank/BankConfig.BUNDLED_VERSION` +1，否则客户端不会认为有新版本。

---

## 七、登录接口说明

界面只依赖 `AuthApi` 这一层接口：

```kotlin
interface AuthApi {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String, nickname: String): AuthResult
    suspend fun logout()
}
```

当前实现是 `MockAuthApi`：不联网，只做格式校验（邮箱格式、密码 ≥6 位），账号存在本机 SharedPreferences。
**后端就绪后只需替换实现**（改为 HTTP 请求），UI 无需任何改动。建议的接口：

```
POST /auth/login    { email, password }            -> { token, user }
POST /auth/register { email, password, nickname }  -> { token, user }
POST /auth/logout
```

---

## 八、题库架构与在线更新

### 三层来源

```
① 本地缓存副本  ← 之前从在线题库下载并校验通过的 JSON（filesDir/bank_cache.json）
② 内置题库      ← 随 APK 编译进来（buildBundledUnits），永远可用
③ 在线题库      ← 有网时检查，比本地新才下载覆盖 ①
```

实际取用顺序是 **① → ②**。在线题库只做「增量更新」，因此：

- 飞机上 / 高山无信号 → 直接用 ②，功能与纯离线 App 完全一致
- 服务端挂了 / JSON 格式错 / sha256 不匹配 / 版本更旧 → 全部保留现状，静默失败

### 自动更新流程

```
启动 → 载入 ① 或 ②
      ↓ 距上次检查超过 6 小时、且已配置地址
取 manifest.json（小文件）→ 比版本
      ↓ 有新版本
下载 bank.json → 校验 sha256 → 解析校验 → 写缓存 → 立即生效（界面自动重建）
```

首次启动不会弹窗阻塞：所有网络动作都在协程里静默进行，失败只记录一条状态文案。

### 发布一份新题库

1. 改内容（改 `Unit*.kt` / `Expansion*.kt`）
2. 把 `data/bank/BankConfig.kt` 里的 `BUNDLED_VERSION` +1
3. 跑 `bash tools/contentcheck/run.sh` —— 会生成新的 `bank/bank.json` + `bank/manifest.json`
4. 把 `bank/` 目录下两个文件传到你的服务器（或 CDN / GitHub raw）
5. 完成。用户下次打开 App 自动更新，**不用发版**

旧的 App 不会因新题库而崩：`manifest.json` 里的 `minAppVersion` 会拦住版本不足的客户端，只提示已是最新；未知字段直接忽略；单条题目格式错误只会跳过那一条。

### 本机测试更新流程

```bash
# 1) 把 Mac 当服务器（在 bank 目录下）
cd ~/AscentApp/bank && python3 -m http.server 8099

# 2) 手机与 Mac 连同一 Wi-Fi，然后在 App 里：
#    我的 → 底部连点版本号 7 次 → 开发者模式 → 在线题库地址
#    填入 http://<Mac 的局域网 IP>:8099   然后「保存并检查」
```

> debug 构建已允许明文 HTTP，release 仍强制 HTTPS。测完记得用「清空地址」恢复。

### JSON 结构

```jsonc
{
  "schema": 1,              // 数据格式版本，高于 App 支持则拒绝
  "version": 1,             // 题库版本号，与本地比较的那个值
  "updatedAt": "2026-09-28T15:11:01+08:00",
  "minAppVersion": 4,       // 低于这个 versionCode 的 App 不会被推送
  "units": [
    {
      "id": "u1", "index": 1, "code": "BASECAMP",
      "title": "大本营 · 高山入门", "altitude": 3200,
      "lessons": [
        {
          "id": "l1", "title": "高山环境与风险认知",
          "code": "ALPINE RISK", "icon": "🏔️", "needsCoach": false,
          "brief": ["高山的危险分两类…"],
          "questions": [
            {
              "prompt": "下列哪一项属于「客观危险」？",
              "context": "可选的情境说明",
              "options": ["落石与冰崩", "判断失误"],
              "correct": 0,
              "explanation": "客观危险指…",
              "safetyCritical": false,
              "review": false          // true = 复习题，答题界面会标记
            }
          ]
        }
      ]
    }
  ]
}
```

`manifest.json`：

```json
{
  "version": 1,
  "updatedAt": "2026-09-28T15:11:01+08:00",
  "file": "bank.json",
  "sha256": "86649492ff56df67…",
  "minAppVersion": 4
}
```

不想写 manifest 也可以：直接只放 `bank.json`，App 会退化为整体下载后比较 `version`（代价是每次检查都要传完整文件）。

### 当前题库规模

| 项 | 值 |
|---|---|
| 题量 | **711 题**（含 82 道复习题） |
| 课程 | **47 课 / 5 海拔单元** |
| 题库版本 | v14（schema 2，minAppVersion 6） |
| bank.json | 549 KB |

按现在的体量，题库再涨十几倍也只有几 MB，包体不会有压力。

---

## 九、实体书入库流水线

内容来源的策略：**实体书扫描资料为主，网络公开资料为辅（用于交叉核对）**。

### 录入原则（重要）

只取书中的**知识与技术事实**，用我们自己的表述重写题干与解析，并填写 `来源：` 标到具体页码。

- 事实与技术方法本身不受版权保护，可以直接使用
- 逐字摘抄原文则存在版权风险，且对题库可读性也无益
- 标注出处既是尊重，也让教练/医生审核时能直接翻书核对

### 流程

```
实体书扫描件
   ↓ 人工 / OCR 整理（本目录 import/*.md）
Markdown 源文件
   ↓ node tools/qbank/build.mjs
BookContent.kt（自动生成）
   ↓ gradle assembleDebug
内置题库（离线兼底）
   ↓ bash tools/contentcheck/run.sh
bank/bank.json + manifest.json
   ↓ 上传服务器
用户不发版即可收到新题库
```

### 录入

完整格式说明见 **`import/README.md`**。最简形式：

````markdown
# 课程 l23 · 冰锥放置进阶

- unit: u5
- code: ICE SCREW
- icon: 🧊
- needsCoach: true

## 要点

- 冰锥应打入冰质密实处，避开空腔与蜂窝冰。

## 题目

1. 打冰锥之前，最应该确认的是？
   - 冰锥的颜色是否醒目
   - 该处冰体是否密实，有无空腔或蜂窝化
   答案：B
   来源：《登山圣经》第 5 版 p.214
   解析：冰锥的强度不取决于放置技巧，而取决于冰本身。
````

也支持 `# 追加 l13` 给已有课程补题。

### 编译时会把关

红线（有错就不写文件）：题干为空、选项少于 2 个、答案越界、缺少 unit/title、某块无题目。

提示（不阻断）：缺少解析、技能要点为空、未知字段、**难度递增检查**。

> 最后一项很实用：新录入的课程如果题量明显少于所在营地的水平，脚本会直接报
> `难度递增=失败！`——提醒你补足题量，否则多邻国式的节奏会出现断层。

### 草稿文件

`import/` 下以 `_` 开头的 `.md` 不会参与编译，适合放写到一半的稿子。

---

## 十、已知限制与下一步

**当前限制**

- 未做模拟器运行时验证（本机未安装 Android 模拟器）；真机验证由开发者本人进行
- 登录为本地模拟，没有真实后端与 token 机制
- 进度仅存本机，登录后尚未真正同步
- 内容未经登山教练 / 医生实审

**建议的下一步**

1. **内容审核**：请持证教练与医生审核全部 711 题与 31 篇技能书，重点核对药物剂量与急救流程
   （技能书 h20 的药物清单来自 SEVEN SUMMIT TREKS 高海拔攀登队随队医生，
   已标注为参考值并要求医生现场确认）
2. **内容工程**：把课程数据外置为 JSON / CMS（`Expansion.kt` 已是过渡方案）
3. **AI 教练**：接入 LLM，实现课后答疑、错题归因、个性化训练计划生成
4. **登录后端**：实现真实 `AuthApi`，加上 token 刷新与错误码映射
5. **实操闭环**：教练端评估打卡，把「待教练评估」真正闭环
6. **云同步 + 排行榜**：账号打通后的下一步
7. **上线准备**：release 签名、R8 混淆、隐私政策与合规

---

## 十一、许可

本项目采用 **Apache License 2.0** —— 完整条款见 [`LICENSE`](LICENSE)。

选择 Apache 2.0 的原因：它是 Android / Kotlin 生态的标准许可（AOSP 同款），
条款完整且带明确的专利授权，同时要求衍生作品保留版权声明与变更说明。

第三方开源组件、内容来源与免责声明见 [`NOTICE`](NOTICE)，其中明确：

- 本仓库**不包含**任何纸质书的扫描原文，只含整理后的主题笔记
- 35 枚成就徽章为 AI 渲染生成
- 内容不构成医疗建议，药物清单仅为参考值
