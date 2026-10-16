# PORTING-01｜架构与数据算法（iOS / 鸿蒙移植用）

> 对应代码：`app/src/main/java/com/ascent/climb/`（Kotlin 2.0.21 + Jetpack Compose，零第三方依赖）
> 本文描述**与平台无关**的部分：数据模型、规则算法、题库加载。UI 规格见 02，内容与发布见 03。

---

## 1. 设计原则（移植时要保持的三条）

1. **内容 = 单一 JSON**：所有课程/题目/山峰都在 `bank/bank.json`（schema=2），跨端只消费它；改题不需要动客户端。
2. **纯逻辑可移植**：海拔曲线、解锁规则、复习间隔、装配（含复习题回插）都是纯函数，无 Android 依赖（已有 JVM 校验工程 `tools/contentcheck` 在用）。
3. **平台能力最小化**：只有 5 个平台接口——键值存储、文件、网络、资源（题图）、时钟。iOS 用 expect/actual，鸿蒙用 ArkTS 原生实现即可。

## 2. 模块边界

| 模块 | 内容 | 移植动作 |
|---|---|---|
| `data/Model.kt` | Unit / Lesson / Question / QType / HotspotRegion / Peak / Track / Handbook* | 原样翻译（数据结构） |
| `data/Curriculum.kt` | 大纲（按 id 引用课程）+ 30 座目标山峰 | 原样 |
| `data/BundledBank.kt` | 课程池装配 + 复习题回插（见 §4.4） | 原样（纯函数） |
| `data/Altitude.kt` | 海拔曲线（见 §4.2） | 原样 |
| `data/Unit1~5.kt / Expansion1~13.kt / NewLessons.kt` | 课程与题目内容（编译期内置） | 无需移植代码——对应内容都在 bank.json |
| `data/Content.kt` | 界面统一入口（ALL_LESSONS 等） + 解锁规则 | 保留接口 |
| `data/ProgressStore.kt` | 进度持久化（键值存储） | 换成平台存储（见 §5） |
| `data/bank/BankRepository.kt` | 三层题库加载与在线更新 | 换网络/文件实现 |
| `ui/` | 全部 Compose 界面 | iOS：Compose Multiplatform 直接复用；鸿蒙：ArkTS 重写（对照 02） |

## 3. 数据模型要点

- `Question.type`：`Single / Multi / Order / Match / Fill / Calc / Hotspot / Scenario`（8 种）。
- 各题型使用的字段（其余留空）：
  - Single: `options` + `correct`（-1 表示未用）
  - Multi: `options` + `correctSet`
  - Order: `options`（**按正确顺序编写**）+ `correctOrder`（当前全部为 `[0,1,2,3]`）+ 客户端负责打乱初始展示顺序（见 §4.5）
  - Match: `pairsLeft` / `pairsRight`（同下标为一对）
  - Fill: `answers`（可接受答案数组，归一化比对）
  - Calc: `answers`（数值字符串）+ `tolerance` + `unit`
  - Hotspot: `image` + `hotspots[{x,y,r,correct,label}]`（归一化 0..1，r 相对宽度）
  - Scenario: `steps`（子题数组，仅 Single/Multi；≥2 步；`prompt` 标题、`context` 情境）
- `safetyCritical`：安全关键标记（加权显示）；`review`：自动回插的复习副本标记；`source`：出处标注（如 `《高山探险》p.196`）。
- 复习题键格式：**`lessonId#题目下标`**（0 起，含运行时回插后的实际位置——两端重做时保持同一算法即可一致）。

## 4. 核心算法（可直接照抄的规则）

### 4.1 解锁（线性，多邻国式）
```
isUnlocked(id): index<=0 → true；否则之前所有课都完成
currentLessonId = 第一门未完成的课
```

### 4.2 海拔曲线（按课授予，实时累加、不存储）
- 常量：`BASE=3200`、`FIRST=4800`、`SUMMIT=8848`
- `awardFor(i)`：i==0 → 1600；其余 `per = (8848-4800)/(n-1)` 整除，余数 `carry` 并入最后一课
- `altitudeFor(completed) = 3200 + Σ awardFor(已完成课)`（47 课，单课 88～1600 m）
- 好处：调曲线只改 3 个常量，旧存档自动重算。

### 4.3 星级与安全绳
- `stars(correct,total)`：全对 3 星；≥60% 2 星；否则 1 星。
- 安全绳（rope）：初始/每日重置为 5；答错消耗 1（视觉机制，与复习队列解耦）。

### 4.4 复习题回插（构建题库时做）
- `REVIEW_PER_LESSON = 2`，`REVIEW_START_INDEX = 6`（**全局课序号**，0 起）。
- 每课从「之前所有课的题池」确定性抽取 2 题（`cursor=(i*5)%pool` 递推、跳过与本课重复/已抽中的题），复制并标 `review=true` 追加在本课末尾。
- 抽取是确定性的：同一课每次构建结果一致。
- ⚠️ 血泪教训：`REVIEW_START_INDEX` 用**全局**序号判断，曾误用单元内序号导致一题都插不进。

### 4.5 展示顺序打乱（2026-09-30 新增，两端必须一致）
- 目的：修掉「单选 84% 正确答案在 B」「排序题原地不动即正确」两个问题。
- **单选**（含情景单选）：按 `prompt` 哈希做确定性 Fisher–Yates 打乱展示顺序（**不处理恒等**——单选不存在「不动即对」）。判定仍用原始下标。
- **排序**：初始顺序 = 确定性打乱（另一组种子）且保证 ≠ `correctOrder`；若打乱结果恰为正确序，按 `hash mod (n-1)` 再做 1..n-1 位循环移位。判定 = 当前序列恢复成 `correctOrder` 才算对。
- 种子公式（保真移植用）：
  - 单选：`seed = prompt.hashCode()*1000003 xor 0x5DEECE66D`
  - 排序：`seed = prompt.hashCode()*31 xor 0x2545F4914F6CDD1D`（注意溢出按 64 位截断）
  - 用与 Java 相同的 hash（UTF-16）即可得到同一排列；若语言不同无法复现也可改为「随机打乱」——只是三端每种题序会不同，不影响判定正确性。

### 4.6 判分细节
- Fill：归一化后比对——转小写、去空白与 `，,。.、；;：:℃°度%`。
- Calc：解析为 Double，`|v-answer| <= tolerance + 1e-6`。
- Hotspot：点击坐标归一化后在 0..1 空间内做圆判定（`dx²+dy² ≤ r²`）。
- Scenario：逐步判定、逐步反馈；**全部步骤答对**才算通过；否则整段进错题重做。

## 5. 平台能力接口（建议的移植边界）

| 接口 | Android 现状 | iOS 建议 | 鸿蒙建议 |
|---|---|---|---|
| KeyValueStore（StringSet/Int/Bool/String） | SharedPreferences（`ascent_progress`、`ascent_bank`） | DataStore-Preferences KMP 或 NSUserDefaults | `@ohos.data.preferences` |
| FileStore（读写缓存） | `filesDir/bank_cache.json` | NSFileManager（Application Support） | `context.filesDir` |
| Http（GET 文本，超时/大小上限） | HttpURLConnection（12s / 6MB） | Ktor Darwin 或 NSURLSession | `@ohos.net.http` / rcp |
| Assets（题图 Bitmap） | `assets/bank/img/`（BitmapFactory） | Compose Resources / NSBundle 图片 | `resources/rawfile` + image.createImageSource |
| Clock | java.time.LocalDate | Foundation Date/Calendar | Date/`i18n` |

Kotlin 侧建议以 `expect/actual` 暴露（原工程可直接演进为 KMP）：
```kotlin
expect object Platform {
    fun kvGet(key: String): String? ...
    fun readCache(name: String): String?
    fun fetchText(url: String, timeoutMs: Long): String?   // 失败返回 null
    fun loadLessonImage(name: String): ImageBitmap?        // 题图
    fun todayEpochDay(): Long
}
```

## 6. 题库加载与在线更新（BankRepository 现状，逐条对齐）

- 键值：`prefs=ascent_bank`，键 `cache_version / cache_updated / custom_url / last_check / last_result`；缓存文件 `bank_cache.json`。
- 启动顺序：**本地缓存副本 → 内置题库**（缓存版本不高于内置则用内置）。
- 静默更新：距上次检查 >6 小时且（默认或自定义）地址可用时拉取 `manifest.json` → 比较 `version` → 需要时下载 `bank.json` → 校验（schema、`minAppVersion`、大小 ≤6MB）→ 通过才落盘缓存。
- 失败/更旧/校验不过：**一律保留现状**，离线永远可用。
- 更新结果类型：`Downloaded / UpToDate / NeedAppUpgrade / Failed`（供「我的」页展示）。
- `DEFAULT_BASE_URL` 目前是占位地址（example.com），实际由用户/开发者模式配置。

## 7. 移植 Checklist

- [ ] 翻译 Model/Question 结构（注意 Order 的 correctOrder 语义 + 打乱规则）
- [ ] 对齐常量：海拔三常量、复习间隔 `[0,1,3,7,16,35]`、`REVIEW_PER_LESSON=2 / START=6`、绳 5、星星阈值 60%
- [ ] 实现 5 个平台接口 + 题库三层加载（含 6 小时节流、失败回退）
- [ ] 复刻打乱规则（或明确改为随机 + 记录差异）
- [ ] 复刻判分细节（Fill 归一化字符集、Calc 容差、Scenario 全对才过、错题重做整段重来）
- [ ] 用 bank.json 做冒烟：47 课 / 711 条 / 每课 ≥10 题 / 海拔 3200→8848
