# ASCENT 攀登 · 工程交接文件

> **这是本工程的单一事实来源（SSOT）。每次更新 App 后同步更新本文件。**
>
> 最后更新：2026-10-14　｜　App **v0.15.0**（versionCode 32）　｜　代码仓库 **github.com/Solar-CV/AscentApp**　｜　题库 **v14**　｜　课程大纲 **47/47 完成**　｜　**成就系统已落地**（35 成就 + 徽章 + 解锁弹窗 + 开发者测试区，详见 §13 与 `ACHIEVEMENTS.md`）；**2026-10-14 徽章视觉方向重建「Summit Relics」完成艺术指导 + 9 枚样稿；同日 V2 材质纠偏：几何图标 → 游戏神器材质（AI 渲染三方向对比 + 矢量验证，见 `ACHIEVEMENTS-ART.md` §0 与 HANDOFF §14/§15）**

---

## 0. 一分钟速览

**这是什么**：高海拔攀登 / 攀冰培训 Android App，游戏化学习路径（多邻国式），核心机制是**「学习进度 = 海拔」**。课程按海拔分五个单元，从 5000 米科普入门一路到 8000 米终极考核。

**现在到哪了**：课程大纲 **47 门课全部写完**，共 **629 道收录题**（8 种题型；另有 82 道自动复习副本，合计 711 条展示项），五个单元全部达标。App 可正常安装运行，题库支持在线更新（用户不发版即可改题）。已实现间隔重复复习。全库通读校对与 P0/P2 修复已完成；v0.9.7 起看图题支持「查看大图 / 双指缩放」，打乱算法已通过 Java 交叉校验与全库验证（记录见 `REVIEW-711-通读校对报告.md` 第 7 节）。

**下一步做什么**：UI 改版（pen.dev 4 套方案待选定，交接文件：`UI-DESIGN-HANDOFF.md`）；iOS 适配（CMP 迁移，按 `PORTING-01/02/03` 执行）；待核项：发病率「25%–43%」来源、EARNEST 定义（详见报告 §3.3）。

---

## 1. 环境与命令

### 工具链（全部装在用户目录，无需 Homebrew / Android Studio）

| 组件 | 路径 | 来源 |
|---|---|---|
| JDK 17 | `~/android-toolchain/jdk` | 清华 Adoptium 镜像 |
| Android SDK | `~/android-toolchain/sdk` | 腾讯镜像（android-34 / build-tools 34.0.0） |
| Gradle 8.9 | `~/android-toolchain/gradle-8.9` | 腾讯镜像 |

> ⚠️ `api.adoptium.net` 与 `services.gradle.org` 在本机**超时**，必须走镜像。
> Maven 依赖已在 `settings.gradle.kts` 配好阿里云镜像优先。

### 常用命令

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

# 编译 App（首次约 1'40"，增量 3-30"）
cd ~/AscentApp && "$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug

# 内容校验 + 导出在线题库（会跑真实装配逻辑）
bash ~/AscentApp/tools/contentcheck/run.sh

# 从 Markdown 编译题目进 App
node ~/AscentApp/tools/qbank/build.mjs

# 知识库检索 / 统计
cd ~/AscentApp/knowledge && node tools/kb.mjs stats
cd ~/AscentApp/knowledge && node tools/kb.mjs search 冰爪 --limit 10
```

**APK 产物**：`~/AscentApp/app/build/outputs/apk/debug/app-debug.apk`

### 沙箱注意（重要）

- `user_exec` 沙箱与用户真实环境**隔离**：user_exec 看不到 `/var/folders/.../chatbox-sandbox/...`
- 但 `code_execution` **能读**用户宿主路径（如 `/Users/lolo/...`）
- 要把宿主文件（如 APK）交给用户下载：在 `code_execution` 里用 `fs.copyFileSync` 从宿主路径复制进沙箱，再调 `create_download`

---

## 2. 工程结构

```
~/AscentApp/
├── HANDOFF.md                    ← 本文件
├── README.md                     面向用户的说明文档
├── REVIEW-711-通读校对报告.md     全库通读校对结果 + 待修清单（2026-09-30）
├── PLATFORM-适配规划.md           iOS / 鸿蒙适配方案（2026-09-30）
├── PORTING-01-架构与数据算法.md    移植文档 1/3：数据模型、规则算法、平台接口
├── PORTING-02-界面与交互规格.md    移植文档 2/3：屏幕/题型/组件/主题
├── PORTING-03-内容素材与发布流程.md 移植文档 3/3：bank.json、题图、发布流程
├── UI-DESIGN-HANDOFF.md           UI 设计交接（4 套方案档案 + 改版任务书）
├── import/                       题库编写（Markdown，实体书整理）
│   └── README.md                 格式说明
├── bank/                         在线题库产物（bank.json + manifest.json）
├── knowledge/                     纸质资料知识库
│   ├── taxonomy.json             主题分类（对应 22 门课 + 6 跨主题）
│   ├── docs/                     10 份规范化正文（保留 <!-- page:N --> 锚点）
│   ├── topics/                   28 份主题汇编（带页码出处）
│   └── tools/kb.mjs              build / stats / search / page
├── tools/
│   ├── contentcheck/             内容校验 + 题库导出（纯 JVM）
│   │   ├── run.sh                一条命令：同步 → 校验 → 导出
│   │   ├── Check.kt
│   │   └── BankExport.kt
│   ├── qbank/                   Markdown → 题库编译器
│   │   ├── build.mjs
│   │   └── gen-demo-image.mjs   零依赖 PNG 生成器（热点题示例图 + 输出热点坐标）
│   └── shufflecheck/            打乱算法校验（Java↔Node 等价实现 + 均匀性测试）
└── app/src/main/java/com/ascent/climb/
    ├── MainActivity.kt           底导 + 全屏课程 + 题库检查
    ├── data/
    │   ├── Model.kt              QType / Question / Lesson / Peak / Unit / Track / Handbook*
    │   ├── Unit1~5.kt            原始课程内容
    │   ├── Expansion1~13.kt      十三批补充题目（按 lessonId 追加）
    │   ├── NewLessons.kt         新大纲课程（m01–m25）
    │   ├── Curriculum.kt         课程大纲（UnitSpec + 30 座目标山峰）
    │   ├── BundledBank.kt        课程池 → 按大纲装配 → 插复习题
    │   ├── BookContent.kt        由 import/*.md 自动生成，勿手改
    │   ├── Content.kt            界面统一数据入口 + 解锁规则
    │   ├── Auth.kt               AuthApi 登录接口 + MockAuthApi
    │   ├── ProgressStore.kt      进度持久化（SharedPreferences）
    │   └── bank/                 题库区块
    │       ├── BankConfig.kt     版本号 / 超时 / 体积上限
    │       ├── BankJson.kt       JSON 解码（org.json，零依赖）
    │       ├── BankRemote.kt     网络层（HttpURLConnection，零依赖）
    │       └── BankRepository.kt 统一入口：缓存 → 内置 + 在线更新
    └── ui/
        ├── Theme.kt              双主题（日夜）+ LocalAscentColors
        ├── Components.kt         等高线背景 / 卡片 / 进度条 / 按钮
        ├── PathScreen.kt         攀登路径
        ├── LessonScreen.kt       技能要点 → 测验 → 错题重做 → 结算
        ├── HandbookScreen.kt     技能书
        ├── TrainingScreen.kt     训练计划
        ├── ReviewScreen.kt       复习中心
        └── ProfileScreen.kt      账号 + 题库 + 雷达 + 徽章 + 开发者模式
```

---

## 3. 数据架构

### 课程大纲（海拔单元）

大纲在 `data/Curriculum.kt`，**只声明「这个单元用哪些课」**，课程本体从 `BundledBank.kt` 的课程池按 id 取。**调整顺序或移动课程只改这一个文件。**

| 单元 id | 名称 | 定位 | 课程数 |
|---|---|---|---|
| `m5i` | 5000 米 · 入门 | 科普为主，徒步型山峰；末尾引入技术攀登 | 12 |
| `m5a` | 5000 米 · 进阶 | 技术入门，学完整套技术动作 | 9 |
| `m6` | 6000 米 | 强化认知 + 技术比重上升 + 高原病专章 | 10 |
| `m7` | 7000 米 | 大量技术 + 危险判断 + 天气云图 + 自救 | 8 |
| `m8` | 8000 米 · 终极考核 | 生理极限、氧气、14 座、综合考核 | 8 |

### 题型（`QType`）——共 8 种

```kotlin
Single  单选    options + correct
Multi   多选    options + correctSet
Order   排序    options（打乱）+ correctOrder（正确顺序索引）
Match   配对    pairsLeft / pairsRight（同下标为一对）
Fill    填空    answers（可接受答案，按归一化比对）
Calc    计算题  answers（数值）+ tolerance（容差）+ unit（单位）
Hotspot 热点题  image（题图）+ hotspots（归一化坐标与半径）
Scenario 情景题  steps（子题列表，仅支持 Single/Multi，至少 2 步）
```

- 配对题 UI：先点左侧条目，再点右侧备选
- 排序题：上下箭头调整
- 填空：归一化比对（忽略空格与 `℃°度%` 等单位符）
- 计算：转成 Double 比较，容差可配（默认 0，额外容忍 1e-6 浮点误差）
- **热点题**：题图必须按 **4:3** 制作（代码里用 `aspectRatio(4f/3f)` + `ContentScale.FillBounds` 充满容器，这样点击坐标的归一化映射才与 hotspots 一致）；点区判定在 UI 层完成（那里知道尺寸），只把命中索引传回逻辑层
- **情景题**：`prompt` 做标题、`context` 做情境背景，子题在 `steps` 里。界面显示步骤进度点，逐步判定并逐步给反馈；**全部步骤答对才算通过**，否则进错题重做（整段重来）。状态是 `scenarioStep / scenarioAllOk / stepOk`，**这三个必须作为参数传进 QuizView**（QuizView 是独立组件，拿不到 LessonScreen 的局部状态）

> Kotlin 提醒：局部函数不能在其声明之前被调用（曾因 `scenarioStepAnswered` 声明在 `answered` 之后而报 Unresolved reference）。

### 题图管线

**位置**：`app/src/main/assets/bank/img/`

**加载**：`LessonScreen.rememberAssetBitmap()`（`BitmapFactory` 从 assets 解码，失败时显示“题图缺失”占位）

**宽高比自适应（重要）**：热点题的容器用图片**真实宽高比**（从 bitmap 取 `width/height`），不写死 4:3。因为题图类型差异很大——气象 App 截图是竖屏（≈0.53），等高线图是横屏（≈1.17）。点击的归一化坐标与画面一致才能准确映射。

**图片展示位置**：
- 热点题：自带可点击大图
- 其它题型（含情景子题）：在题干下方以 `fillMaxWidth(0.88f)` 展示参考图

**如何新增题图**：
1. 用 `sips` 压缩：照片类 `sips -s format jpeg -s formatOptions 82 -Z 1500 输入 --out 目标.jpg`（等高线图 PNG 有 1.5 MB，转 JPEG 后降到 500 KB）
2. 热点坐标**必须用裁剪验证，不能目测**：
   ```bash
   # 假设估计点在归一化 (0.50, 0.496)，图 695×1300 → 像素 (348, 645)
   sips -c 340 340 --cropOffset 475 178 图.png --out /tmp/check.png
   ```
   然后用 `view_image` 看这个裁剪，根据目标在裁剪里的实际位置反推真实坐标。
   **已经踩过这个坑**：座标目测值偏了 6%，热点完全点不中。

**现有素材**（均来自 `~/技能书/图片资源/`）：

| 文件 | 尺寸 | 用途 |
|---|---|---|
| `wx-manaslu-before.png` | 695×1300 | 马纳斯鲁气象预报·气旋前（正常窗口） |
| `wx-manaslu-after.png` | 691×1300 | 同山·气旋导致攀登季提前结束（单日 280 cm 降雪） |
| `wx-gongga-1.png` | 701×1300 | 贡嘎山 7,556 m 逐小时预报（含零度层/风寒/日照） |
| `wx-gongga-2.png` | 699×1300 | 贡嘎 16 天预报（**已出题**：雨雪分界线 + 午后雷暴） |
| `wx-mtnforecast.png` | 1400×1101 | mountain-forecast.com 专业桌面界面（已出题） |
| `topo-manaslu-srtm.jpg` | 1500×1283 | 马纳斯鲁 SRTM 等高线（**50 m 等高距**） |
| `topo-manaslu-google.jpg` | 1500×1050 | 同山谷歌地图（**200 m 等高距 + 地形阴影 + 峰顶标记**） |
| `topo-everest-srtm.jpg` | 1500×1225 | 珠峰–洛子峰 SRTM 等高线（**已出题**：峰顶 + 南坳鞍部） |
| `topo-muztagh-srtm.jpg` | 1500×1290 | 慕士塔格 SRTM 等高线（**已出题**：穹状峰顶） |
| `topo-muztagh-google.jpg` | 1500×999 | 慕士塔格谷歌地图（**已出题**：地标的价值与边界） |
| `demo-slope.png` | 800×600 | 程序生成的坡面示意图 |

**地形图的核心教学点（已入题）**：
- 谷歌地图等高距 **200 m**，带地形阴影与峰顶标记，**直观但精度低**；SRTM 等高距 **50 m**，纯高程色带，**精度高但不直观**。两者应配合使用：先用直观图建立空间印象，再用高精度等高线做量化判断。
- **地形阴影只反映光照与坡向关系，不是坡度指标**——朝北的缓坡会比朝南的陡坡更暗。这是很常见的地形误读。
- 200 m 等高距会把一段 150 m 的陡灰「抹平」成缓坡；50 m 等高距能如实画出。

**已标定坐标**（可直接复用）：
- `wx-manaslu-before` 零度层曲线：(0.50, 0.496)，r=0.12
- `topo-manaslu-srtm` 峰顶区域：(0.50, 0.55)，r=0.11

**素材来源与生成方式**：
- NASA SRTM 高程数据 → QGIS 转等高线，工程文件在 `~/技能书/QGIS SAVE/`（Manaslu.qgz / everest.qgz / muz ata.qgz）
- 气象截图为登山界常用天气 App 的真实界面
- 尚有未入库素材：`专业气象界面1.png`、`真实气象界面3/4.png`、两张谷歌地图等高线（可做 SRTM vs 谷歌的对比题）

**尚未实现**：从在线题库下载图片并缓存（现在题图只随 APK 发布）

### 复习：间隔重复

`ProgressStore` 里维护复习队列（`reviews` 存为 `key|level|due` 的字符串集）：

- 答错 → 等级回到 0，**当天**就进复习中心
- 复习答对 → 等级 +1，到期日推后 `intervals = [0, 1, 3, 7, 16, 35]` 天
- `dueReviewKeys()` 只返回**今天到期**的题，按紧迫度排序
- 界面上的“今日待复习 / 队列总数”两个数字分别来自 `dueReviewKeys().size` 与 `reviewTotal()`

> 旧字段 `ProgressStore.wrong` 已不再被界面使用（保留以兼容旧数据）。

### 海拔曲线（`data/Altitude.kt`）

**起步 3,200 m　→　第一课通关后 4,800 m　→　最后一课通关后 8,848 m**

- 海拔改为**按课授予**（不再按题累加）。重做同一课不再重复获得海拔。
- 第一课直接拉 1,600 m（大本营→前进营地的跃升），之后每课均分（≈89 m），零头并入最后一课（冲顶段自然给得多）。
- **关键设计**：海拔不是独立存储的计数器，而是由「已完成课程」实时累加得出（`altitudeFor(completed)`）。好处是**调曲线后所有旧存档会自动重算**，不会残留旧值、也不需要迁移。
- 调曲线只需改 `Altitude.kt` 里三个常量；`tools/contentcheck` 会校验两个锚点。

验证输出（contentcheck）：
```
起步 3200 m → 第一课通关后 4800 m → 最后一课通关后 8848 m
海拔曲线=OK
节点：第1课=4800m 第2课=4889m 第6课=5245m 第12课=5779m
      第21课=6580m 第33课=7648m 第46课=8848m
单课奖励 89～1600 m
```

### 已标定的热点坐标（经 sips 裁剪 + 目视复核）

| 题图 | 目标 | 归一化坐标 | 半径 |
|---|---|---|---|
| `wx-manaslu-before.png` | 零度层曲线 | (0.500, 0.496) | 0.12 |
| `topo-manaslu-srtm.jpg` | 马纳斯鲁峰顶 | (0.500, 0.550) | 0.11 |
| `topo-everest-srtm.jpg` | 珠峰峰顶 | (0.450, 0.185) | 0.09 |
| `topo-muztagh-srtm.jpg` | 慕士塔格峰顶 | (0.535, 0.475) | 0.085 |
| `demo-slope.png` | 最陡坡面 | (0.481, 0.592) | 0.13 |

> ⚠️ **加新的热点题时坐标必须验证**：用 `sips -c 高 宽 --cropOffset top left 图 --out /tmp/x.png` 裁出以估测点为中心的区域，再用 `view_image` 看目标是否居中。曾经目测自由层曲线 y=0.558、实际 0.496，偏 6% 会完全点不中。

### 题库三层来源

```
① 本地缓存副本   filesDir/bank_cache.json   ← 之前下载并校验通过的 JSON
② 内置题库       随 APK 编译进来             ← 永远可用，兜底
③ 在线题库       有网时检查，比本地新才覆盖 ①
```

实际取用顺序 **① → ②**。启动后静默检查更新（距上次 >6 小时且已配地址才发请求）。拉取失败 / 校验失败 / 版本更旧 → 一律保留现状。

**题库 JSON schema = 2**，顶层是 `tracks`（兼容旧的 `units` 格式）。

### 多课程轨道

`Track` 模型已就绪，目前只有 `climb` 轨道有内容。`Content.kt` 里的 `PLANNED_TRACKS`（潜水 / 徒步 / 滑雪）作为「即将开放」占位。接入新轨道时只需新增轨道定义，不用改模型与 JSON 格式。

---

## 4. 内容状态

### 规模

```
课程 47 门　｜　收录题 629 道（+82 复习副本 = 711 条展示项）　｜　目标山峰 30 座
```

| 单元 | 课程 | 题量 | 每课 |
|---|---|---|---|
| 5000 入门 | 12 | 135 | 10–14 |
| 5000 进阶 | 9 | 112 | 12–14 |
| 6000 | 10 | 143 | 14–16 |
| 7000 | 8 | 163 | 16–31 |
| 8000 | 8 | 158 | 18–22 |

### 题型分布

```
单选 563　多选 61　排序 35　配对 20　填空 14　计算 5　热点 5　情景 8   ← 含 82 道复习副本
非复习口径：单选 495　多选 55　排序 30　配对 18　填空 14　计算 5　热点 5　情景 7
8 道情景题共包含 27 个子问题
```

### 溯源与质量

- **540 / 711 条带出处标注**（收录题口径 481 / 629 = 76.5%），例如 `《高山探险》p.196`
- **16 处题目引用题图**（热点题 5 + 参考图 11；共 11 张图，4.4 MB）
- 格式校验：错误 0 · 空解析 0 · 空题干 0 · 重复课程 id 0
- 全部 47 课均 ≥10 题
- **全库通读校对（2026-09-30）**：结论与修复记录见 `REVIEW-711-通读校对报告.md`（P0/P2 已在 v0.9.6 修复）
- **热点题校验**（仅 contentcheck 工具）：题图文件是否存在、是否至少有一个正确区域
- **情景题校验**：子题不少于 2 个，且只能是 Single / Multi

> ⚠️ **APK 体积已到 13.6 MB**（其中题图 4.4 MB）。若继续大量加图，应考虑把题图改为从在线题库下载并缓存，而不是全打进 APK。

### 目标山峰（30 座）

| 单元 | 山峰 |
|---|---|
| 5000 入门 | 奥太娜 5,211、四姑娘山大峰 5,025、二峰 5,276、乞力马扎罗 5,895 |
| 5000 进阶 | 岗什卡三峰 5,005、主峰 5,254、哈巴雪山 5,396、厄尔布鲁士 5,642 |
| 6000 | 玉珠峰 6,178、启孜峰 6,206、罗布切东峰 6,119、岛峰 6,189 |
| 7000 | 慕士塔格 7,509、列宁峰 7,134、希姆隆峰 7,126、阿玛达布朗 6,812 |
| 8000 | 全部 14 座 |

> **注意数据准确性**：奥太娜官方海拔 5,211 m 但实测约 4,800 m（两个数都标注了）；阿玛达布朗 6,812 m 按用户要求放在 7000 单元，但标注了「技术难度对标 7000 米以上」。

---

## 5. 工作流

### 加题 / 改题（从实体书）

```
纸质书 → 已完成的 OCR 校对稿（~/技能书/校对版/）
       ↓ 查知识库主题汇编，核对页码
knowledge/topics/*.md（每主题已抽好相关段落 + 出处）
       ↓ 写成 Markdown
import/*.md
       ↓ node tools/qbank/build.mjs
data/BookContent.kt（自动生成）
       ↓
BundledBank 合并 → 构建 → 导出 bank.json
```

**录入原则**：只取书中的**知识与技术事实**，用自己的话重写题干与解析，并填写 `来源：` 标注到页码。避免逐字摘抄（版权 + 可读性）。

### 直接加题到已有课程

在 `data/Expansion*.kt` 里加：

```kotlin
"l13" to listOf(Question(prompt = "...", options = listOf(...), correct = 0, explanation = "...", source = "..."))
```

然后在 `BundledBank.withExtraQuestions()` 里接线（目前已接 1~13）。

### 加新课程

1. 在 `data/NewLessons.kt` 里加 `Lesson(...)`
2. 在 `Curriculum.kt` 对应单元的 `lessonIds` 里加上 id

### 发布新题库（用户不发版即可更新）

1. 改内容
2. `data/bank/BankConfig.kt` 的 `BUNDLED_VERSION` +1
3. `bash tools/contentcheck/run.sh` → 生成 `bank/bank.json` + `manifest.json`
4. 两个文件上传服务器（或 CDN / GitHub raw）
5. 完成。用户下次打开 App 自动更新

**本机测试更新流程**：

```bash
cd ~/AscentApp/bank && python3 -m http.server 8099
# 手机与 Mac 连同一 Wi-Fi，App 里：我的 → 连点版本号 7 次 → 开发者模式 → 在线题库地址
# 填入 http://<Mac 局域网 IP>:8099 → 保存并检查
```

> debug 构建已允许明文 HTTP，release 仍强制 HTTPS。

### 隐藏开发者模式

「我的」页底部**连点版本号 7 次**解锁，可：解锁全部关卡、显示正确答案、修改在线题库地址。状态存 `ProgressStore`，重置学习进度不会清除。

---

## 6. 约定与血泪经验

### 版本号规则（用户明确要求）

**内容批次走 patch 位**（0.8.1 → 0.8.2 → 0.8.3），**架构变更才动 minor 位**。`versionCode` 每次必须 +1（Android 要求单调递增）。题库版本 `BUNDLED_VERSION` 与 App 版本是两套独立编号。

### 难度与内容分布原则（用户明确要求）

- **题量随海拔递增**：5000 入门 10–13 → 5000 进阶 12–14 → 6000 米 14–15 → 7000 米 16–20 → 8000 米 18–22
- **题型随难度增加**：选择题在全库都是主力，但排序 / 配对 / 计算 / 情景题集中在 6000 米以上
- **初级单元不放医学理论题**（v0.9.1 调整）：5000 米单元只讲环境科学、装备、体能与安全习惯；**流行病学统计（发病率、年龄与风险）、病理机制、用药剂量等统一放在 6000 米单元的 l13–l16 与 m09**。判断标准：涉及“为什么会这样”的生理机制、统计数据、药物 → 医学理论，往后放；“该怎么做”的行为建议 → 科普，可以放前面
- **同一知识点可以在不同课程里重复出现**，用于强化记忆；但**同一道题不能重复**（写完后检索确认）
- **答案要经得起追问**：例如冲顶包容量题，因为“不同路线答案不同”，所以要**把情境写进题干**（需自带技术装备 + 备用保暖 + 应急物资），并在解析里说明容量并非固定值

### 复习题必须按「全局课序号」判断

`BundledBank.withReviewQuestions()` 里的 `REVIEW_START_INDEX` 是**全局**课序号。曾经按单元内局部序号写，结果一个单元都不到 6 课，**一道复习题都插不进去**。

### 写题后必须回查避免重复

多次出现新写的题与已有题重复（例如「结组间距 8–12 m」写了两遍）。写完一批后应检索关键词确认。

### 展示打乱约定（v0.9.6 起，两处都要记）

- **单选**（含情景单选）：展示顺序按题面哈希稳定打乱（`shuffledSingleDisplay`），判定仍用原始下标——**新增单选题不要依赖某个选项固定排在第几位**。
- **排序**：初始顺序打乱且保证 ≠ `correctOrder`（`shuffledOrderStart`）；若恰为正确序则按哈希循环移位；数据仍按正确顺序写选项 + 恒等 `correctOrder`。
- **多选不打乱**（其解析里存在「第 N 项」类表述）；给多选写解析时可以用序数指代。
- **验证（2026-09-30）**：Java `Random` 逐位交叉校验通过；25 万样本下答案位置严格均匀；全库非复习单选分布 A 25.7% / B 26.5% / C 24.6% / D 23.2%（修复前 B 84.2%）；35 道排序题开局即正确 0 道。校验脚本：`tools/shufflecheck/`。

### 非中文字符自查

大量中文输出时容易混入误字（曾出现西里尔文 `снег`）。可用：
```bash
grep -n "снег" app/src/main/java/com/ascent/climb/data/*.kt
```

### Kotlin 字符串插值陷阱

`"$units.size"` 会被解析成 `$units` + 字面量 `.size`，必须写 `${units.size}`。曾因此打印出整个对象。

### 主题色是语义色

`Theme.kt` 用 `LocalAscentColors` + `Ascent` 对象的 `@Composable getter`，所以调用点写 `Ascent.Ice` 即自动跟随主题。**但 Canvas 绘制 lambda 内不能用**（非 composable 作用域），需要提前取出到局部变量。

### pen.dev（Pencil MCP）要点

- 变量**不跨 execute 调用保留**，跨调用要用节点 id 字面量
- Frame 默认 `layout: horizontal`
- 文字必须设 `fill` 否则不可见
- 只有 `layout:"none"` 的容器子节点才需要 x/y
- 构建过程中 `Get` 的 `c.problems` 报 clipped **常是瞬时误报**，用 `c.bounds` 逐层累加验证才准确
- 当前画布有 4 套路径主页方案：
  - `zk5vR` A·冰川仪器（深色冷调/等宽/海拔剖面图）
  - `Wrhhr` B·等高线地图（浅色纸感/地形图/衬线）
  - `T9gME` C·探险日志（深暖色/日志条目/衬线）
  - `Z1qI78` D·登山标识（亮色高对比/粗字重/编号路标）

### 内容校验工具的用法

`tools/contentcheck` 是**纯 JVM 工程**（app 的 data 目录除 `ProgressStore.kt`/`Auth.kt` 外都是纯 Kotlin），`run.sh` 会把源文件同步过去跑真实装配逻辑。改动内容后跑一次就能验证题量、难度递增、答案越界、解锁逻辑，**不用装 App**。

---

## 7. 待办

### 用户已明确要做的

- [x] ~~新题型：计算题、热点题~~ ✅ v0.9.0
- [x] ~~错题/复习优化：间隔重复~~ ✅ v0.9.0
- [x] ~~多步情景题~~ ✅ v0.9.1
- [x] ~~真实题图入库~~ ✅ v0.9.2
- [x] ~~谷歌地图 vs SRTM 对比题~~ ✅ v0.9.3
- [ ] **UI 改版**（pen.dev 4 套方案待选定）
- [x] ~~看图题「查看大图 / 双指缩放」~~ ✅ v0.9.7（非热点题点击图片或标注打开；热点题用独立按钮；「选出峰顶位置」类除外）

### 已知缺口

- [ ] **题图只随 APK 发布**：还不能像题目那样从在线题库下载图片。（用户已明确：200 MB 以内都可以接受，暂不优先）
- [x] ~~4 张剩余题图出题~~ ✅ v0.9.5（热点坐标已逐张验证，标定表见上）
- [x] ~~初级阶段心理情景题~~ ✅ v0.9.5（新增 m25「高原心理与决策习惯」）
- [ ] **UI 改版**：pen.dev 4 套方案待用户选定；选定后改全部页面
- [ ] **界面未用上新数据**：`PathScreen` 尚未展示目标山峰；轨道切换入口未做
- [ ] **技能书未外置**：`HandbookA/B/C.kt` 仍是内置，未纳入在线题库
- [ ] **无真机运行验证**：本机未装 Android 模拟器，所有验证都是静态的（编译 / 打包 / 签名 / 资源 / 逻辑校验）
- [ ] **内容未经专家实审**：建议请持证教练与医生逐条审核，重点是药物剂量与急救流程

### 2026-09-30 全库通读校对（已修复 ✅，见 `REVIEW-711-通读校对报告.md` 第 7 节）

- [x] **P0-1 排序题恒等序** → UI 打乱初始顺序（`LessonScreen.kt` 新增 `shuffledOrderStart`）
- [x] **P0-2 单选 84% B 位** → 单选/情景单选展示打乱（`shuffledSingleDisplay`，判定仍用原始下标）
- [x] **P0-3 多选全选即满分** → 48 道各补 1 个干扰项（选项变 5 个，correctSet 不变）
- [x] **P0-4 L45.1 答案键** → `correct = 1`
- [x] **P2 文本**：繁体字×2、whitepout×2、穿看、高山病×3、登山靴（含技能书）、含氧量→氧分压、铁锁→主锁×13、流明措辞、L44.16 补「25」
- [x] **口径**：珠峰统一 8,848（注明 8,848.86）、马纳斯鲁统一 8,163、技能书升幅口径统一（3,000 米/≤500 + 说明）
- [ ] 待核项：发病率「25%–43%」来源、EARNEST 定义、药物剂量请医生过审

### 平台适配（2026-09-30 新增）

- [ ] **iOS**：Kotlin Multiplatform + Compose Multiplatform 迁移（CMP iOS 自 1.8.0 起稳定；实施按 `PORTING-01/02/03`）
- [ ] **鸿蒙**：ArkTS / ArkUI 重写（官方唯一路径）或评估腾讯 Kuikly；方案见 `PLATFORM-适配规划.md`，实施按 `PORTING-01/02/03`

---

## 8. 关联资料

| 资料 | 位置 | 说明 |
|---|---|---|
| 纸质书扫描校对稿 | `~/技能书/校对版/` | 7 本书 · 1,356 页 · 双引擎 OCR 一致率 68–88% |
| 我方内部资料 | `~/技能书/*.docx` | 了解高原病 / 装备指南 / 探险活动通用指南 |
| 知识库 | `~/AscentApp/knowledge/` | 上两者的规范化 + 主题汇编（带页码） |
| 校对说明 | `~/技能书/校对版/校对说明.md` | OCR 流程与残留风险说明 |
| 表格图 | `~/技能书/校对版/表格图/` | 约 30 张表格高清裁图 |

### 校对质量注记（引用数据时留意）

- **正文可信度高**，可直接作为出题依据
- **错误集中在表格、图注、装饰性标题、竖排文字、跨栏混排**
- 65 条含剂量/温度/尺寸的关键数据行已逐条与扫描件核对，**全部一致**
- 知识库的 `docs/` 里噪声页标了 `<!-- noisy -->`，主题汇编会自动跳过

---

## 9. 快速恢复清单

对话被压缩后，按这个顺序恢复认知：

1. **读本文件**（你正在读）
2. 看当前版本：`grep versionName ~/AscentApp/app/build.gradle.kts`
3. 看内容实况：`bash ~/AscentApp/tools/contentcheck/run.sh`
4. 看大纲：`cat ~/AscentApp/app/src/main/java/com/ascent/climb/data/Curriculum.kt`
5. 有实体书素材要录入时：`cd ~/AscentApp/knowledge && node tools/kb.mjs stats`
6. 改完 → 构建 → 导出题库 → **回来更新本文件的「最后更新」行与内容状态**
7. 校对/适配/移植/设计材料：`REVIEW-711-通读校对报告.md`（第 7 节=修复记录）、`PLATFORM-适配规划.md`（路线）、`PORTING-01/02/03`（实施规格）、`UI-DESIGN-HANDOFF.md`（UI 改版交接）

---

## 10. UI 改版「方向 E · HeroUI × 攀登」（进行中 · 2026-10-08）

设计稿与决策见 `UI-DESIGN-HANDOFF.md` §7；画布 `~/.pencil/documents/de8d993a-…/pencil-new.pen`（x=3000 起，11 屏 × 深浅 + 边角态）。

### 已落地（`assembleDebug` 通过，APK ≈ 14.6 MB）
| 文件 | 内容 |
|---|---|
| `ui/Icons.kt` | **新增**：lucide 图标集 **79 个** → 纯 Compose `ImageVector`（`Ic.Xxx`），**零第三方依赖** |
| `ui/Theme.kt` | 调色重写为 HeroUI：zinc 中性 + 冰川蓝 `#0485F7`。**新增** `Accent/Surface/Border/Fg/Fg2/Fg3/Success/Warning/Danger(+Soft)/Scrim`；**旧 token 名保留为别名**（Ice/Panel/Line/Txt…），既有屏不破坏 |
| `ui/Components.kt` | **新增** `PillButton` / `OutlineCard` / `SoftTag` / `MonoLabel` / `IconBadge` / `StatTile` / `StarRow` / `ElevationProfile`（海拔剖面，纯 Compose）/ `CapsuleTabBar`；旧组件保留 |
| `ui/PathScreen.kt` | **多邻国式单元路径（v5 · 圆角长方块）**：顶部海拔 + 路线剖面卡；**单元标题 = 整条单元色带**（白字）+ **每单元一个亮色户外色**（草甸绿 `#4E9B3A` / 冰川湖蓝 `#3E86C4` / 高原紫 `#7C6FC0` / 岩橙 `#C97A45` / 雪青 `#3F9A93`）；**课程入口 = 圆角长方块 94×62dp / 圆角 22**（步长 **178dp**、摆动 **±64dp**、**无连线**；字号 → 星级/标题的纵向偏移必须 ≥ `NODE_LIP`（12dp），否则会被加厚的底块压住）。**卡通立体** = 厚底块（下移 **12dp**，非锁定加深 0.22）+ 块面 + 底部柔和收暗 + **左上→右下斜向柔光**（白32%→14%→0.70 处透明），均为平滑渐变、无硬边。**字形全部手绘**（Canvas）：粗圆头对勾 / 胖三角 / 胖五角星 / **卡通挂锁（圆角锁体 + 上半圆锁梁）**，做法 = 先填充再叠同形状粗圆角描边。**每个单元最后一课统一用登顶金 `#E0A22C` + 深色字形 `#4A3208`**；锁定面 = `Track` 灰；两侧小装饰（单元色 38%）；星级用实心小星 |
| `ui/ReviewScreen.kt` | 重写：待复习队列卡 + 分段条 + 安全优先分组 + 空态 |
| `ui/TrainingScreen.kt` | 重写：渐变「今日推荐」+ 推荐列表 + 安全边界卡 |
| `ui/HandbookScreen.kt` | 重写：搜索 + 分类筛选 + 卡片列表 + 长文详情（渐变封面 / 来源） |
| `ui/ProfileScreen.kt` | 重写：档案头 + 海拔卡 + **能力条（替换原先失效的雷达图）** + 徽章墙 + 账号 / 题库 / 外观 / 声明 / 开发者 |
| `MainActivity.kt` | 底栏换为 `CapsuleTabBar`（lucide 图标），移除旧 `TopoBackground` 全局等高线 |
| `ui/LessonScreen.kt` | 重写视觉层（**逻辑零改动**：判分 / 打乱 / 错题重做 / 情景分步 / 查看大图手势全部保留）：顶部进度、题型徽标、8 题型作答控件（单选/多选/排序/配对/填空/计算/热点/情景）、反馈与解析卡、三态结算、查看大图查看器 |

### 待办
- ✅ 字体内嵌已完成（Noto Sans SC 400/600 + JetBrains Mono 400/600，已子集化）→ 见 §11。
- ✅ 路径页改为多邻国式单元-节点连线（2026-10-08 下午）。
- 🎨 **课程入口节点多邻国化（2026-10-09，仅设计稿，未动 Compose）**：按用户提供的多邻国真实按钮截图，节点从「94×62 圆角长方块 + 厚底」重建为**俯视椭圆盘**（椭圆面 88×64 + 侧壁 88×12 + 底座下移 12，总高 76）；高光已整体移除（多轮手绘方案表现不佳，2026-10-10 决策），盘面纯色平涂 + 唇边，字形 38（盘宽 43%）直接坐在面上；单元色提亮（绿 `#58CC02`/唇 `#46A302`，登顶金 `#FFC800`/唇 `#D19E00`）；当前课光环改 108×88 椭圆描边 + 「开始」气泡加三角尾巴；锁定态改中性灰（替换 `#4A3208` 系）；星级行/标题偏移改为 节点顶 +92 / +110。实现规格与 Compose 落地清单见 `UI-DESIGN-HANDOFF.md` §7.5。
- ✅ 版本已并入 **v0.10.1**（versionCode 21，2026-10-10）：**节点多邻国化 Compose 已落地**——`PathScreen.kt` 节点从「94×62 圆角长方块 + 厚底 + 渐变打光」改为**椭圆盘三件套**（椭圆面 88×64 + 侧壁 88×12 + 底座下移 12，纯色平涂无高光）；单元色提亮为多邻国糖果色（`#58CC02 / #1CB0F6 / #CE82FF / #FF9600 / #00CD9C`，登顶金 `#FFC800`）；节点步长 178 → **185**；当前课光环改 116×92 椭圆（填充 14% + 描边 55%）；「开始」气泡加三角尾巴；星级/标题偏移 = 节点中心 +46 / +64。`assembleDebug` 通过，APK 15.7 MB。
- 🧭 **方向 V2 · 多邻国真实风路径主页**：✅ **已落地 v0.10.2**（2026-10-10），详见 §12。
- ⬜ 仅剩：**真机走查**（题序打乱 / 查看器 / 双主题 / 视觉细节）。
- 注：环境无设备 / 模拟器（无 system image），只能 `compileDebugKotlin` + `assembleDebug` 验证。
- 图标集交付包：`ASCENT图标集-lucide-SVG+VectorDrawable.zip`（79 个）。

---

## 11. 字体内嵌（2026-10-08）

已内嵌字体取代系统字体。构建脚本：**`bash tools/fonts/build-fonts.sh`**（幂等，可重跑）。

| 资源 | 字体 | 字重 | 用途 |
|---|---|---|---|
| `res/font/ascent_sans_regular.ttf` | Noto Sans SC（≈思源黑体） | 400 | 正文 / 标题 |
| `res/font/ascent_sans_semibold.ttf` | Noto Sans SC | 600 | 强调（Medium / SemiBold / Bold 均指向此文件） |
| `res/font/ascent_mono_regular.ttf` | JetBrains Mono | 400 | 海拔 / 坐标 / 编号 / meta 微标签 |
| `res/font/ascent_mono_semibold.ttf` | JetBrains Mono | 600 | 同上强调 |

- **接入点**：`ui/Theme.kt` 的 `SansBody` / `MonoNumbers`（`ascentTypography` 与全部组件都走这两个 family）。
- **子集化**：字符集从 `app/src/main/java` + `bank/` + `import/` + `knowledge/` 全量提取（CJK ≈ **3300** 字，另含 ASCII 与中文标点），用 `pyftsubset` 裁剪。
- **体积**：4 个文件共 **≈2.0 MB**（sans 1.0MB×2 + mono 46KB×2）；APK 14.6 MB → **15.9 MB**。
- **依赖**：`pip3 install --user fonttools`。
- ⚠️ 日后若新内容引入了语料中没出现过的新汉字，需重跑脚本；否则该字会回退到系统字体（不会出现豆腐块，但风格不一致）。

---

## 12. 方向 V2 · 路径主页落地（2026-10-10 · v0.10.2）

设计稿见 `UI-DESIGN-HANDOFF.md` §7.6；画布 `~/.pencil/documents/de8d993a-…/untitled.pen`（帧 `H106xO` 浅 / `rOaZE` 深）。

### 12.1 新增字体：Nunito ExtraBold（数字/拉丁展示）
- `res/font/ascent_display_bold.ttf`（**21 KB**，拉丁子集）；`Theme.kt` 新增 `DisplayFont`。
- 取字体：Google Fonts CSS2（`family=Nunito:wght@800`，非现代 UA 会返回 `fonts.gstatic.com` 的 ttf），再 `pyftsubset`。
- 用途：顶部数值条的 chips 数字（多邻国同款字形）。

### 12.2 新增主题 token（浅 / 深）
- 中性：`duoBg` `#FFFFFF/#060607`、`duoFg` `#3C3C3C/#FAFAFA`、`duoFg2` `#777777/#A1A1AA`、`duoFg3` `#AFAFAF/#71717A`、`duoBorder` `#E5E5E5/#2A2A2E`。
- 锁定：`lockFace2` `#E5E5E5/#2C2C31`、`lockLip3` `#C9C9C9/#232327`、`lockGlyph` `#AFAFAF/#71717A`。
- 品牌（单值）：`uGreen #58CC02` / `uGreenLip #46A302`、`uBlue #1CB0F6` / `uBlueLip #1899D6`、`uGold #FFC800` / `uGoldLip #D19E00`、`goldGlyph #4A3208`、`streak #FF9600`。

### 12.3 PathScreen 结构（重写）
1. **顶部数值条**：`mountain-snow` 22dp（绿）+ 海拔 mono 22 ExtraBold + `M`；右侧 chips（火 12 / 绳 5，Nunito ExtraBold 15）。
2. **单元横幅**：整条**单元色**（每单元一色：绿 `#58CC02` / 蓝 `#1CB0F6` / 紫 `#CE82FF` / 橙 `#FF9600` / 青绿 `#00CD9C`；唇色 = 面 ×0.80）、圆角 16、内边距 16×15；三行 = `第 N 单元 · <营地>`（11 / `#D8F5B8`）+ 标题（20 Bold 白）+ `N 课 · 起 – 止 M`（mono 11 / `#D8F5B8`）；右侧 48×48 圆角 12 唇色块 + `book-open` 白。
3. **营地区分线**（第 2 个单元起）：左右各一条 `duoBorder` 2dp + 中间 12sp `duoFg3` 文本「C1 营地 · <单元名> · <起始海拔> M」。
4. **S 形路径**：横向偏移周期 6 = `[0, +64, +44, 0, -44, -64]`、步长 **185**；起/终海拔侧标（mono 10 `duoFg3`）；帐篷装饰（绿 38%，66% 高度处）。
5. **球面盘（三件套）**：**底座椭圆 + 侧壁矩形 + 盘面椭圆**；尺寸 **79×58**（2026-10-10 应用户要求整体缩小 10%）、底座下移 **11**、侧壁 **79×11**（夹在两个椭圆的**圆心之间**）。⚠️ **侧壁不能省** —— 它提供圆柱侧面的**竖直直边**，缺了就会被看成「两个重叠的椭圆」（v0.10.4 即此问题）；⚠️ 形状必须用自定义 `OvalShape`（`GenericShape { addOval(Rect(...)) }`），**不能用 `CircleShape`**（百分比圆角按短边算，非正方形盒子会画成胶囊/圆角矩形；v0.10.1–0.10.3 均中招）。字形 **34dp**（锁定用 icon 23dp）。**节点颜色 = 所在单元色**（不是「可挑战 = 蓝色」—— 蓝色只是单元 2 自己的色）；完成 = 单元色 + 白对勾 + 3 实心星；当前 = 单元色 + 播放 + **104×83 椭圆环**（填充 14% + 2dp 描边 55%）+ 带尾巴的「开始」气泡；未开始 = 单元色 + 白星；锁定 = 中性灰 + 灰星；单元通关 = 金 + 深色星。
6. **顶峰插画块**：`mountain` 96dp（`duoFg3`）+ 金色 `flag` 22dp；`距顶峰 X M`（mono 13 Bold）+ `已完成 n / N 课 · P%` + 160×8 进度条（`uGreen`）。
7. **底栏**：`CapsuleTabBar` 改 V2 风格 —— 胶囊 r36、`surface` 填充 + `duoBorder` 描边；当前项为**绿色实心胶囊（r26）** + 白色图标/文字，非当前 `duoFg3`；图标改为 `mountain / book-open / dumbbell / rotate-ccw / user`。

### 12.4 版本
**v0.10.5（versionCode 25）**；`compileDebugKotlin` + `assembleDebug` 通过，APK **15.7 MB**。

版本链（同一 V2 页面的四次修正，刻意错开版本号便于真机辨认）：
- **v0.10.2**（vc 22）：V2 页面首版，但**误将所有单元做成同一绿色**；
- **v0.10.3**（vc 23）：补回每单元配色（绿/蓝/紫/橙/青绿 + 末课金）；
- **v0.10.4**（vc 24）：修复节点形状 —— `CircleShape` 在非正方形盒子上画不出椭圆（得到胶囊/圆角矩形），改用自定义 `OvalShape`；
- **v0.10.5**（vc 25）：**补齐侧壁**（盘面/底座之间的竖直直边，修复「两个重叠椭圆」感）+ **整体缩小 10%**（88×64 → 79×58）。

> 画布同步（2026-10-10）：`untitled.pen` 已删除旧的 **E · 路径主页** 两帧（`h8Bq0m` / `o4IQ2`），其余 E 页面保留；V2 两帧的节点同步缩小 10%。

---

## 13. 成就系统落地（2026-10-13 · v0.11.0）

设计文档：`ACHIEVEMENTS.md`（35 个成就的完整清单与口径）；徽章设计板：`untitled.pen` 帧「成就徽章规范板」。

### 13.1 体系

- **35 个成就**：A 课程进度 12 / B 学习习惯 8 / C 学习深度 9 / D 探索里程 6；稀有度 = 普通 14 / 稀有 13 / 史诗 6 / 传说 2；隐藏 2（`comeback`、`curious_dev`）。
- 判定全部为纯函数 `evaluate(stats)`（`data/Achievements.kt`），输入 `AchievementStats` 由 `ProgressStore.achievementStats(units)` 组装；判定与 UI 完全分离。
- **新增统计口径**（ProgressStore 增量字段，旧数据兼容、reset 会清）：`activeDays`（累计活跃天数）、`lessonRuns`（累计课次）、`reviewDone`（复习答对总数）、`maxStreak`、`streakBroken`（连续 ≥3 天中断标记，comeback 用）。**连续天数 ≠ 累计天数 ≠ 课次**三个口径严格区分。
- 持久化：`unlockedAchievements`（幂等不重复发）+ `pendingAchievements`（待播弹窗队列，**进程死亡不丢弹窗**）。

### 13.2 事件接入（MainActivity）

`refreshAchievements()` 统一在 ① 启动 touchDay 后 ② 课程通关后 ③ 复习判定后 ④ 解锁开发者模式后调用；弹窗 `AchievementUnlockOverlay` 每次播一条，关闭才播下一条。

### 13.3 徽章与弹窗

- 徽章 `ui/Badges.kt`：椭圆盘（面 + 唇，Canvas 绘制）+ lucide 字形（**全部复用现有 Icons.kt，零新增资源**）；稀有度 = 绿/蓝/紫/金；未解锁灰盘、隐藏未解锁「?」。
- 弹窗 `ui/AchievementUnlock.kt`：徽章弹性放大 + 卡片淡入，传说级金色光环脉冲；尊重系统「移除动画」（`ANIMATOR_DURATION_SCALE`）；scrim 点击与「收下」按钮均可快速关闭。

### 13.4 开发者模式 · 成就测试区

「我的 → 开发者模式 → 成就测试区」→ `ui/AchievementsDevScreen.kt`：

- 判定数据快照（maxStreak / activeDays / 课次 / 复习 / 队列 / 海拔 / streakBroken）；
- **定义有效性校验**（重复 ID / 缺字形 / 条件引用失效）；
- 分组总览（状态 / 稀有度 / 条件 / 进度条）；
- 单项操作：**预览弹窗**（本地播放不入库）、**强制解锁**（入库）、**重置此项**；
- 批量：批量播放动画、**重置全部成就**（二次确认，只清成就字段不动课程进度）。

### 13.5 校验与版本

- `tools/contentcheck` 新增「成就系统」段：定义有效性、空进度 = 0、满进度 35/35 全可达、幂等 = 0、第 1 课 = first_steps —— **全绿**。
- **v0.11.0（versionCode 26）**；`compileDebugKotlin` + `assembleDebug` 通过，APK 15.8 MB。
- ⚠️ 教训：`validateAchievements` 的课程引用正则须写成 `[lm]\d+[a-z]*`，否则 `m5i` 会被截断成 `m5` 误报。


---

## 14. 成就徽章视觉重绘（2026-10-13 · 仅设计稿，Compose 待落地）

参照多邻国真实成就页重绘全部 38 款徽章示例，画布帧 `z6A9I`（`untitled.pen`）。规格见 `UI-DESIGN-HANDOFF.md` §7.7。

**核心变化**：`ui/Badges.kt` 现有的「椭圆盘 + lucide 线性图标」本质还是按钮形状，缺角色感。新方案：

- **盾形轮廓**（尖底盾牌）替代椭圆盘，粗描边 `#2F2F33`；
- **雪鸮吉祥物**：头身一体剪影 + 大眼 + 橙喙 + 耳羽 + 身后双翅（翅膀层级必须在身体之下，否则出现白缝）；
- **22 种登山语义道具**（冰镐/旗/火焰×3/书×2/绳圈/奖杯/心/雪花/星×2/帐篷/靴/罗盘/地图/时钟/学士帽/马灯/药丸/靶心/雪山/心电/脚掌/补给箱/路线），按成就语义分配，避免「只换色」的重复感；
- **底部描边数字**（Nunito ExtraBold + 7px 深色描边）；
- **三态**：解锁（彩色角色）/ 未解锁（灰盾灰数字，无角色）/ 隐藏（灰盾 `?`）。

**配色**：稀有度只作用于盾面 + 身体浅色（普通 `#58CC02`/`#8FE45C`、稀有 `#1CB0F6`/`#77DAFB`、史诗 `#CE82FF`/`#EBC4FF`、传说 `#FFC800`/`#FFE173`），角色本体保持白/浅色以突出剪影。未解锁 `#EAEAEE` + 描边 `#E0E0E4`。

⚠️ 画布坑：盾形描边外扩 6px，格子高须 ≥176、标题 y ≥150；数字 `fontSize 40 / y 76` 是安全值（44/84 会被裁）。


---

## 15. 徽章视觉方向重建 · Summit Relics（2026-10-14 · 仅艺术指导与样稿）

> 完整文档：**`ACHIEVEMENTS-ART.md`**（诊断 / 三方向 / 9 样稿 / SVG 规范 / 35 枚设计矩阵 / 工程交接）。样稿画布：pen.dev `untitled.pen` → 帧 `Zj9en`。

- **诊断结论**：§14 的「盾形+雪鸮」方案本身是模板化设计（同一盾形 ×35、吉祥物抢主体、道具 48dp 不可读、稀有度仅换色），已废弃为对照样本（旧帧 `z6A9I` 保留）。
- **三方向**：A「切面与寒霜」（推荐，主体系）/ B「远征藏品」（铭牌概念，可吸收特例）/ C「遗物核心」（晶体概念，hidden 成就可借鉴）。三套概念稿均在画布上可对比。
- **方向 A 核心**：外轮廓=主体轮廓；全扁平切面无渐变；稀有度=材质与细节密度（Common ≤6 path 无金 → Rare +银结构 → Epic +唯一金件 → Legendary 金为主体构件）。
- **9 枚样稿已画**：first_steps 登山靴 / streak_7 七段环 / seven_k_grad 双峰金旗 / knot_tied 八字结 / five_zones 五层台地 / runs_20 冰镐刻痕 / review_20 书+回旋弧 / streak_30 双环金核 / full_route 单峰金星。评审均 ≥34/40。
- **工程交接要点**：SVG 256 viewBox → Compose ImageVector；只出全彩一版，未解锁用 saturation(0)+tint；新增 `badgeRes` 映射而非改 Achievement data class；徽章槽位 ≥96dp。**本阶段无任何 Kotlin 改动。**


---

## 16. 徽章 V2 材质纠偏 · 从几何图标到游戏神器（2026-10-14 下午 · 仅设计稿）

> 用户裁定 §15 的 9 枚样稿是「几何图标」而非「游戏神器」。纠偏公式：**几何结构是骨架，精密金属是材质，游戏神器是最终观感**。详见 `ACHIEVEMENTS-ART.md` §0（当前有效方向）。

- **三套材质提案**（同一成就 seven_k_grad · Epic · 同构图同尺寸，AI 渲染 1024²，存 `badge-samples/`）：A 精密钛合金+深蓝珐琅 / B 冰晶核心+银钛框架 / C 高阶锻造+珐琅浮雕。
- **结论**：**C 为主体系**（圆章剪影 48dp 最稳、层结构最可系统化）；A 的机加框+角嵌件吸收为 Epic 框升级；B 的晶体主体只给 Legendary。**稀有度 = 材质体系阶梯**（Common 单层锻造 → Rare 浮雕纪念章 → Epic 镶嵌徽章 → Legendary 神器框镶晶体），不再是同一体系的细节增减。
- **矢量可行性已验证**：pen.dev 帧 `WIH42`（C/Epic 全材质）——线性/径向渐变金属、inner-shadow 倒角、珐琅盘、浮雕山体、黄铜铆钉、晶体嵌件全部成立。SVG 翻译要点：pen 的 inner shadow 在 SVG/VectorDrawable 中须换成**两条显式弧线描边**（vector drawable 不支持 inner shadow）。
- 样稿评审 7 项验收表见 `ACHIEVEMENTS-ART.md` §0.3。
- 本阶段：AI 样稿 3 张 + 矢量验证 1 帧。**未做**：35 枚新材质重绘、Kotlin 改动。


---

## 17. 徽章 V3 定稿 · 冰晶神器体系（2026-10-14 下午 · 仅设计稿）

> 用户确认 3 张材质提案中 **方向 B 最佳（不规整外观 + 高光渲染）**，9 枚样稿已按此语言在 pen.dev 重做并覆盖旧版。帧 `OH8K3`；旧扁平版 `Zj9en` 已删除。

- **体系公式**：晶体主体（成就语义具象切割体）+ 不规整金属框（六边/八边）+ 高光渲染（白色棱线 + 半透明 streak）。
- **语义呼应**：靴印=首次出发、七段环=连续7天、双峰山=七千米毕业、中空绳环=绳结之约、五层台地=五区足迹、冰镐刻痕=二十课次、书+环绕碎片=复习二十、满环+发光核心=连续30天、单峰+顶星=理论大满贯。
- **稀有度阶梯**：Common 六边框少切面 → Rare 八边框+铆钉 → Epic +晶体嵌件 → Legendary +金属爪臂+晶体光晕。
- 详细映射表与材质令牌：`ACHIEVEMENTS-ART.md` §0.7。**本阶段无 Kotlin 改动**；下一步为 35 枚矢量化 + Compose ImageVector 集成（§14 交接要点继续有效）。


---

## 18. 徽章 V4 定稿 · AI 渲染 + 材质差异化（2026-10-14 · 最终方向）

> 用户裁定：① 样稿全部改用 AI 渲染（同 3 张提案方式），大体轴对称，扩展边框类型（星/盾/圆/方等）；② **不可全冰晶材质，要有差异**。V3 手绘矢量版废弃。

- **成品形态 = AI 渲染 PNG 位图**（1024²，存 `badge-samples/`，画布帧 `AztNS` 排版）。工程集成改为多密度位图（192/144/96/72dp）+ Compose `Image`；未解锁态 `ColorFilter.saturation(0f)`。§14 的 ImageVector 路线对本方案不适用。
- **材质按语义分配**：钢+皮革珐琅（首次出发）/ 琥珀宝石×7（连续7天）/ 冰晶双峰（七千米毕业）/ 青铜编织绳+主锁（绳结之约）/ 翡翠珐琅台地（五区足迹）/ 工具钢冰镐+铜刻痕（二十课次）/ 羊皮纸书+黄铜环（复习二十）/ 传奇金齿轮环+光核（连续30天）/ 棱彩晶体峰+金星（理论大满贯）。
- **边框类型**：圆 / 六边 / 盾 / 圆角方 / 五边 / 八边 / 菱 / 十二齿齿轮环 / 八角星。
- 完整映射表与提示词锚点：`ACHIEVEMENTS-ART.md` §0.8。本阶段零 Kotlin 改动。


---

## 19. 徽章 V5 全量定稿 + 集成决策（2026-10-14）

> 35/35 枚 AI 渲染完成（26 枚新增 + 9 枚已定稿），全部去水印，集成路线已决策。详见 `ACHIEVEMENTS-ART.md` §0.8/§0.9。

- **资产**：`badge-samples/named/`（1024² 原图）、`badge-samples/clean/`（**去水印交付基准**，按成就 ID 命名）。水印（右下角 AI 生成角标）用 OpenCV Telea inpaint 批量清除。
- **pen.dev**：图片可同步（拷入 `.pencil/documents/<docId>/` + image fill）；全量总览板帧 `Vhp5J`（5×7）。
- **集成路线（已决策）**：256px WebP q88 → `res/drawable-nodpi/badge_<id>.webp`（APK 增量约 1.5MB）；`ui/BadgeArt.kt` 做 id→res 映射，**不改 Achievement data class / glyph 字段**；未解锁 `setToSaturation(0f)` + alpha 0.55，隐藏用通用占位；Profile 墙 64dp / 弹窗 112dp / 开发者区 56dp。
- **待办（工程）**：跑 §0.9 转换脚本 → 资源入 res → BadgeArt.kt → 真机验收（清单见 §0.9）。本阶段零 Kotlin 改动。


---

## 20. 徽章 V5 资源落地 + v0.12.0 打包（2026-10-14）

> 35 枚 AI 渲染徽章已作为**位图资源**正式集成并打包，可真机验证。规格见 `ACHIEVEMENTS-ART.md` §0.9，验证方法见 `ACHIEVEMENTS.md` 末尾「成就获取方法 · 真机验证速查」。

**改动**
- 新增 `app/src/main/res/drawable-nodpi/badge_<id>.webp` × 35（256px WebP q88，合计 434 KB）。
- 新增 `ui/BadgeArt.kt`：`badgeRes(id)` 做 id→资源映射，缺失返回 null。
- `ui/Badges.kt`：`BadgeIcon`（椭圆盘 + lucide 字形）→ **`AchievementBadge(id, unlocked, concealed, size)`**，渲染位图；未解锁 = 去饱和 + 62% 不透明度；隐藏未解锁/缺资源 = 灰圆 + 「?」占位。**删除** `badgeGlyphIcon`；保留 `rarityColors`（弹窗/标签仍用）与 `BADGE_GLYPH_KEYS`（contentcheck 用）。
- 调用点：ProfileScreen 徽章墙 56dp、AchievementUnlock 弹窗 112dp、AchievementsDevScreen 48dp。
- 版本 **v0.12.0 / versionCode 27**；`assembleDebug` 通过；APK 16.3 MB（徽章资源净增 0.43 MB）。

**未改动**：`Achievement` data class、`glyph` 字段、成就判定与持久化——contentcheck 不回归。

**踩坑**：① `Modifier.graphicsLayer{}` 导入不对，改用 `Modifier.alpha()`；② BadgeArt.kt 用 KDoc 时编译器报 "Unclosed comment"，改成 `//` 注释后正常（原因未深究，后续生成 Kotlin 文件建议用行注释）。


### 20.1 修复：开发者模式「批量播放动画 ×3」弹窗卡死（v0.12.1）

**现象**：点「批量播放动画 ×3」→ 第 2 个弹窗（冰爪出师）卡住，「收下」无反应；点其他区域可暂时解除，再次触发又卡在同一项；重启后再次点击仍复现。

**根因**（`ui/AchievementsDevScreen.kt`）：预览推进逻辑写错——

```kotlin
onDismiss = {
    val next = ACHIEVEMENTS.filter { it.id !in unlocked }.drop(1).firstOrNull()
    preview = next
}
```

预览不入库，`unlocked` 是 `remember(revision)` 的快照、恒定不变，因此 `filter(...).drop(1).firstOrNull()` **每次都算出同一个元素**（未解锁列表的第 2 项 = `crampon_ready`），队列永远无法推进 → 弹窗停在同一项，「收下」看似失效。

**修复**：改为真正的队列推进——新增 `previewQueue: List<Achievement>`，队首即当前项，关闭 = `drop(1)`；×3 取未解锁前 3 项入队。

**顺带加固**：`ProgressStore.clearPendingAchievements()` + 开发者模式新增「清空待播队列（n）」按钮，用于清除持久化弹窗队列残留（解卡逃生口，不影响已解锁记录）。

版本 **v0.12.1 / versionCode 28**，`assembleDebug` 通过。


---

## 21. 设置页 + 个人主页 + 徽章墙重构（v0.13.0）

用户三点需求：① 齿轮点了没用，要有真设置项；② 徽章展示太多，改点头像进个人主页、只展示已解锁、未解锁进「全部」预览；③ 个人主页要有精简进度、徽章、学习概览。

**新增文件**
- `ui/SettingsScreen.kt`：外观（跟随系统 / 浅色 / 深色，持久化到 `ProgressStore.themeMode`）· 题库（版本信息、检查更新、恢复内置、题库地址仅开发者可见）· 开发者模式（开关、解锁全部关卡、显示答案、成就测试区入口、退出）· 数据（重置学习进度，二次确认）· 关于（版本号，连点 7 次解锁开发者模式的隐藏入口迁到此处）。危险操作均二次确认。
- `ui/ProfileHomeScreen.kt`：个人主页 —— 档案头（海拔/完成课程）· 学习概览（连续学习天数 / 累计学习天数 / 完成课程 / 成就徽章数 + 总进度条 + 海拔进度条）· 已解锁徽章网格（最多 8 枚，超出走「查看全部」）。
- `ui/BadgeWallScreen.kt`：全部 35 枚徽章，按分类分组，未解锁灰度预览，隐藏成就未解锁显示「隐藏成就」+「?」占位。

**改动文件**
- `ProfileScreen.kt`：齿轮 → 设置页；头像 → 个人主页；**原 35 枚徽章墙替换为紧凑入口卡**（成就徽章 n/35 →）；题库 / 外观 / 开发者模式三段整段迁移到设置页，「我的」页只保留档案头、海拔、能力概览、徽章入口、账号、声明。
- `MainActivity.kt`：`ProgressStore` 提升到 `onCreate` 创建；`AscentRoot(store, themeMode, onThemeModeChange)`；主题由 `themeMode`（auto/light/dark）+ `isSystemInDarkTheme()` 共同决定，状态栏配色跟随。
- `ProgressStore.kt`：新增 `themeMode` 字段（默认 auto）。

**未动**：成就数据与判定、题库逻辑、路径/课程流程、徽章资源。版本 **v0.13.0 / versionCode 29**，`assembleDebug` 通过。

**待真机确认**：设置项切换深色后整 App 是否即时生效；个人主页统计数字是否与「我的」页一致；徽章墙 35 枚灰度预览观感。


### 21.1 交互改善：次级页面的返回手势（v0.13.1）

**问题**：在次级菜单中做全面屏侧滑（系统返回手势）会直接退出到桌面，而不是回到 App 上一个界面——因为这些页面从未注册返回拦截。

**处理**：为所有次级页面加 `BackHandler { onBack() }`（`androidx.activity.compose`）：

| 页面 | 返回行为 |
|---|---|
| `SettingsScreen` | 回到「我的」 |
| `ProfileHomeScreen` | 回到「我的」 |
| `BadgeWallScreen` | 回到个人主页 |
| `AchievementsDevScreen` | 回到设置页 |
| `AuthScreen`（登录/注册） | 关闭登录页 |
| `ArticleDetail`（手册长文） | 回到手册列表 |
| `AchievementUnlockOverlay` | 关闭当前弹窗（队列继续下一条） |
| `LessonScreen` | 关闭课程（原有逻辑，MainActivity 中已注册） |

**行为说明**：顶级页（路径/技能书/复习/训练/我的）不注册 BackHandler，侧滑仍退出 App —— 符合 Android 惯例。弹窗在根布局最后组合，因此 BackHandler 优先级高于其下的次级页面，会先关弹窗。

版本 **v0.13.1 / versionCode 30**，`assembleDebug` 通过。


---

## 22. 技能书内容扩充 + 出处收敛（v0.14.0）

> 资料库位置：`/Users/lolo/技能书`（三份 SEVEN SUMMIT TREKS 向导资料 + 7 本中文登山书籍校对版）。

**① 新增 6 篇（h17–h22，全部来自本地资料库 SST 三本指南的未覆盖部分）**

| id | 标题 | 分类 | 来源 |
|---|---|---|---|
| h17 | 高海拔睡眠系统（睡袋与床垫） | 装备系统 | 装备 GUIDE BOOK |
| h18 | 背包、行李与收纳 | 装备系统 | 装备 GUIDE BOOK + 探险通用指南 |
| h19 | 个人护理与营地生活 | 装备系统 | 装备 GUIDE BOOK |
| h20 | 探险医疗与急救包 | 高海拔医学 | 装备 GUIDE BOOK + 了解高原病 + 探险通用指南 |
| h21 | 高原病风险人群与预防策略 | 高海拔医学 | 了解高原病 |
| h22 | 探险期间规定与登顶认证 | 行程管理 | 探险活动通用指南 |

技能书由 16 篇 → **22 篇**。新增文件 `data/HandbookD.kt`；`Content.kt` 的 `ALL_ARTICLES` 加入 `HANDBOOK_D`；`tools/contentcheck/run.sh` 同步拷贝 HandbookD.kt。

**② 出处展示只保留 SEVEN SUMMIT TREKS**

`ui/HandbookScreen.kt` 的 `ArticleDetail`：`article.sources.filter { it.contains("SEVEN SUMMIT TREKS") }`，过滤后为空则**整个「内容来源」区块不渲染**。数据层的 `sources` 列表保持不动（不删历史数据），只是展示层收敛。因此 WMS / CDC / REI / UIAA / AAC 等出处不再出现在界面上。

**注意事项（需人工复核）**
- h20 药物清单**来源已由用户确认：SEVEN SUMMIT TREKS 高海拔攀登队随队医生**编制。已按此改写 summary 与开头新增「关于本清单的重要说明」一节，明确：药物品项与剂量均为**参考值**、具体情况须由医生**现场确认**、处方类药物无医生指导不得自行服用。原「待复核」状态已解除，但建议正式上线前仍由持证医生过一遍。
- 资料库中的 7 本中文登山书籍已于 v0.15.0 全部取用，见 §23。

**未动**：成就、题库、路径/课程流程、徽章资源。版本 **v0.14.0 / versionCode 31**，`assembleDebug` 通过，contentcheck 全绿（35 成就定义有效、空进度 0、满进度 35/35、幂等 0）。


---

## 23. 技能书补充中文书籍篇 + 出处策略确定（v0.15.0）

用户两项裁定：① **7 本中文登山书也用，出处展示为空白/不显示**；② h20 药物清单来源 = SST 高海拔攀登队随队医生，**具体情况需医生现场确认，清单内容与剂量只作为参考值**。

### ① 新增 9 篇（h23–h31）→ 技能书 22 篇 → **31 篇**

新增 `data/HandbookE.kt`（中文书籍篇），`sources = listOf()` → 详情页「内容来源」区块整体不渲染。

| id | 标题 | 分类 | 主要来源书 |
|---|---|---|---|
| h23 | 户外领导力：定义与软技能 | **户外领导力**（新分类） | 户外领导力 |
| h24 | 制定最佳决策 | 户外领导力 | 户外领导力 |
| h25 | 领队责任、冲突与跟随者 | 户外领导力 | 户外领导力 |
| h26 | 山区导航与定向 | 行前准备 | 高山探险 |
| h27 | 营地的选择、建设与 LNT 环保 | 行前准备 | 高山探险 + 手册合集 |
| h28 | 迷路与野外求救 | 风险判断 | 手册合集 + 高山探险 |
| h29 | 脱水、体温过高与常见外伤 | 高海拔医学 | 手册合集 + 高山探险 |
| h30 | 户外安全原则与风险管理 | 风险判断 | 安全手册2 + 高山探险 |
| h31 | 高山攀登的营养与能量策略 | 行前准备 | 高山探险 |

**内容均直接取自资料库原文**，含大量可核验的具体案例与数据：麦金利河渡河事故、贝克山雪崩搜救演习失败、独木桥三选项加权推演、梅里雪山 1990 年 17 人遇难的营地选址教训、珠峰北坡 7500 m 大风口、LNT 七大法则、手表测向「时数折半对太阳」、北极星 5 倍延伸法、热射病 40.5 ℃ 阈值、低血钠症体温正常、GU 胶 4%~8% 碳水浓度、支链氨基酸与氨的关系、四三三 / 三新不出行 / STOP 原则、风险「后果 × 可能性」矩阵（1~10000 × 0.1~1.0）。

**分类现状**：高海拔医学 8 / 装备系统 5 / 绳索技术 3 / 户外领导力 3 / 行前准备 4 / 风险判断 3 / 冰雪技术 2 / 攀冰技术 1 / 装备知识 1 / 行程管理 1。

### ② h20 药物清单来源与免责

新增「关于本清单的重要说明」为首节，明确四点：清单由 SST 高海拔攀登队**随队医生**编制；**药物品项与剂量均为参考值**；具体情况必须由医生**当面评估后现场确认**；处方类药物（乙酰唑胺、地塞米松、硝苯地平、西地那非、安定、可待因等）无医生指导**不得自行服用**。summary 与「急救包构成」「用药原则」两节同步改口径，并补一句「下撤是唯一确切有效的治疗，任何药物都不能替代它」。

### 出处展示规则（最终）

`ArticleDetail` 中 `sources.filter { it.contains("SEVEN SUMMIT TREKS") }`，**过滤后为空则整块不渲染**。数据层 `sources` 保留原样不删，仅展示层收敛 → SST 篇显示来源，中文书籍篇完全不显示来源区块。

**未动**：成就、题库、路径/课程流程、徽章资源。版本 **v0.15.0 / versionCode 32**，`assembleDebug` 通过，contentcheck 全绿（35 成就定义有效、空进度 0、满进度 35/35、幂等 0、课程 47 解锁）。


---

## 24. 代码仓库建立（2026-10-16）

**仓库**：<https://github.com/Solar-CV/AscentApp>　·　公开　·　分支 `main`　·　SSH 推送

首次提交 2 个 commit、225 个文件（87 个 `.kt`、35 个徽章 webp、41 个 md 文档）。

### .gitignore 排除项（重要）

| 排除 | 原因 |
|---|---|
| `app/build/`、`build/`、`.gradle/`、`.kotlin/`、`*.log` | 构建产物（app/build 约 92 MB） |
| `local.properties` | 含本机 SDK 绝对路径 |
| `.idea/`、`*.iml` | IDE 配置 |
| **`knowledge/docs/`** | **7 本纸质书的扫描校对全文**——受版权保护，不进公开仓库 |
| **`/bank/`** | 题库导出物（由 `tools/contentcheck` 生成）。**前导斜杠**，避免误伤 `app/src/main/assets/bank/` 的 10 张题目配图 |

已验证远端：无 `local.properties`、无 build 产物、无书籍全文、无 `bank/`。
内置题库在源码 `data/BundledBank.kt` 中，排除 `bank/` 不影响编译。

### 顺带修正

`README.md` 此前停留在 **v0.5.0**，与实际严重不符，已同步为真实数据（数据取自 `tools/contentcheck` 实测输出）：
47 课 / 711 题 / 复习 82 道 / 5 海拔单元（5000M 入门·5000M 进阶·6000M·7000M·8000M）/
31 篇技能书 / 35 枚徽章 / 题库 v14 / 体积约 16.3 MB；补充 v0.11–v0.15 更新记录；修正项目结构树。

### Release 与自动构建（GitHub Actions）

- `.github/workflows/release.yml`：推送 `v*` 标签 → 自动 `assembleDebug` → 创建 Release → 附上 APK。
  使用 Actions 内置 `GITHUB_TOKEN`，**无需 PAT**。
- **踩坑**：`android-actions/setup-android@v3` 在 `ubuntu-latest` 上失败，导致后续步骤全部跳过。
  改用 runner 镜像自带的 Android SDK（`/usr/local/lib/android/sdk`，已含 platforms;android-34 与 build-tools;34.0.0），
  显式设置 `ANDROID_HOME` / `ANDROID_SDK_ROOT` + 接受 licenses 即可。
- 补了 **Gradle Wrapper 8.9**（此前缺失，公开仓库 clone 后无法直接 `./gradlew` 构建）。
- **首个 Release 已发布**：<https://github.com/Solar-CV/AscentApp/releases/tag/v0.15.0>　
  附件 `ascent-v0.15.0-debug.apk`（15.58 MB）。
- 用户提供的 PAT 调用 API 返回 **401 Bad credentials**（格式正确但凭证无效），
  故未走 API 路径；已提醒用户 revoke 该 token。

### 环境备注

- GitHub MCP 集成**无权创建仓库**（403 `Resource not accessible by integration`）；本机无 brew / gh CLI。
  因此**远端仓库需人工在网页创建**（New repository，不勾选 README / .gitignore / license）。
- `ssh -T git@github.com` 认证通过（账号 Solar-CV），推送通道正常。
- 后续推送：`git push`（已 `git push -u origin main` 建立上游跟踪）。
