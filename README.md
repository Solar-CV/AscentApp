# ASCENT · 攀登之路

一个给高海拔攀登和攀冰新手做的 Android 应用。课程按海拔组织：每过一课，海拔往上走一段。五个单元从 5000 米入门排到 8000 米，47 课学完，海拔从 3,200 m 走到 8,848 m。

当前源码版本 v0.15.1（versionCode 33）。这一版主要动文案：清掉应用里残留的 markdown 星号、多余的破折号和套话，登录页也不再承诺一个还没做的同步功能。README 顺手重写了一遍。

## 装到手机上

最省事是从 [Releases](https://github.com/Solar-CV/AscentApp/releases) 下预构建的 debug APK。推 `v*` 标签时，GitHub Actions 会自动构建并附到 release 上。如果 Release 里最新还停在 v0.15.0，说明这次文案整理的包还没发，自己构建一下就行。

自己构建也可以，命令在「自己构建」一节。

- 包名：`com.ascent.climb`
- 最低系统：Android 8.0（API 26）
- 体积：约 16 MB
- 权限：联网与网络状态，只用于检查题库更新。断网时所有功能照常，内置题库一直在。

安装步骤没什么特别：把 APK 传到手机，在系统设置里允许安装未知来源应用，点安装。第一次打开不需要登录。

## 里面有什么

### 路径

主页面是一条从大本营往上走的路线，分五个海拔单元。课程线性解锁，过一课开下一课。节点上会显示星级，当前课有「开始」提示，单元最后一课用金色标出来。

进度不是另外存一个数字，而是由「已完成课程」实时算出来的。这样以后调整海拔曲线，旧存档也会跟着重算，不会卡在旧值上。

### 测验与复习

题库现在有 711 条展示项：629 道正式题，加上 82 道自动插入的复习副本。题量随海拔递增，5000 米入门平均每课 11 题，到 8000 米单元平均 19 题。

题型有八种：

- 单选、多选
- 排序、配对
- 填空、计算
- 热点题（在图上点位置）
- 情景题（一道题干里分几步作答）

答完当课会立刻进入错题重做。答对的错题加分，仍然错的进复习中心。复习用间隔重复，间隔是 0、1、3、7、16、35 天。安全关键题会加权，答错后要求确认理解才能继续。

### 技能书

31 篇长文，每篇 4 到 8 节。内容分两块：SEVEN SUMMIT TREKS 的三份向导资料，以及 7 本中文登山书的整理稿。中文书的部分按产品约定不显示出处，向导资料的部分会标来源。

### 训练、复习、我的

训练页给一个准备度百分比和目标峰海拔。复习页把今天到期的题和安全关键错题排在前面。「我的」页有档案头、当前海拔、能力概览、徽章入口和账号区。点右上角齿轮进设置，点头像进个人主页。

个人主页只展示已经解锁的徽章。全部 35 枚在「全部徽章」页，未解锁的显示为灰度预览，隐藏成就给一个「?」。

### 设置

外观可以跟随系统，也可以手动锁浅色或深色，选择会持久化，状态栏配色一起变。题库区能看当前版本、检查更新、恢复内置题库。开发者模式藏在关于页的版本号后面，连点 7 次打开，能解锁全部关卡、显示答案、进成就测试区。

## 最近改了什么

v0.15.1 是一次文案清理，不碰题库结构和判分逻辑。具体改了这些：

- 应用内所有字面 `**` 标记都去掉了，`**` 在 Compose 的 `Text` 里不会被渲染成粗体，只会原样显示。
- 课程要点、技能书摘要、气象与地形题的解析重写了一批。删掉「核心思路只有一句」「这就是……的全部意义」这类腔调，破折号也减了一些。
- 登录页原来写「同步你的海拔、徽章与学习记录」，但当前实现是本地模拟，根本不会同步。现在改成如实说明。
- 题库版本从 14 升到 15。发布到服务器后，旧版用户下次检查更新能拿到新题面。
- README 重写。本地的交接文件里也记了这次整理。

再往前：

- v0.15.0：技能书补 9 篇中文书籍篇（h23–h31），新增「户外领导力」分类，总数 22 → 31。
- v0.14.0：补 6 篇 SEVEN SUMMIT TREKS 向导内容（睡眠系统、背包收纳、个人护理、探险医疗、高原病风险人群、探险规定与登顶认证）。
- v0.13.x：新增设置页、个人主页、全部徽章墙；次级页面接入返回手势，全面屏侧滑不再直接退出 App。
- v0.12.x：35 枚成就徽章改为 AI 渲染位图，作为 256px WebP 资源打包。
- v0.11.0：成就系统落地，35 枚成就、解锁判定、弹窗和开发者测试区。

## 课程怎么排

| 单元 | 海拔 | 课程 | 题量 | 平均每课 |
|---|---|---|---|---|
| 5000M · 入门 | 5,000 m | 12 | 135 | 11 题 |
| 5000M · 进阶 | 5,000 m | 9 | 112 | 12 题 |
| 6000M | 6,000 m | 10 | 143 | 14 题 |
| 7000M | 7,000 m | 8 | 163 | 20 题 |
| 8000M | 8,000 m | 8 | 158 | 19 题 |

大纲规划 47 课，已经写完 47 课。目标山峰列了 30 座，从 5,211 m 的奥太娜一直到 14 座 8000 米级山峰。

## 内容从哪来

题目和技能书主要来自两类材料。

公共资料包括 Wilderness Medical Society 的《急性高原病临床实践指南》2024 更新版、CDC Yellow Book、American Alpine Institute 和 American Alpine Club 的技术资料、REI Expert Advice、UIAA 与 CE 的装备标准，以及 avalanche.org 的雪崩数据。

SEVEN SUMMIT TREKS 的三份向导资料是另一条主线：高原病、装备指南、探险活动通用指南。这三份资料整理成了技能书中的若干篇，也贡献了一批题目。

后来又把 7 本中文登山书的校对稿整理进技能书。书里的知识和案例用自己的话重写，出处标到页码。仓库里不放纸质书的扫描原文。

| 材料 | 用在哪 |
|---|---|
| WMS / CDC / StatPearls | 高海拔医学题与技能书 |
| AAI / AAC / REI / BMC | 冰雪技术与行进 |
| UIAA / CE 标准 | 装备题 |
| avalanche.org | 雪崩判断 |
| SEVEN SUMMIT TREKS 三份资料 | 技能书 h17–h22 与相关题目 |
| 7 本中文登山书 | 技能书 h23–h31 |

## 安全边界

这个 App 只覆盖理论和认知准备。冰雪、绳索、攀冰、救援这些实践技能，必须由持证教练现场指导和评估。

内容不构成医疗建议。涉及药物和急救的决定，要问医生。

不承诺登顶，也不保证高海拔适应结果。

标了「需教练评估」的课程，学完理论后会明确提示还需要实操验证。

## 自己构建

工具链可以全部装在用户目录，不需要 Homebrew 或 Android Studio。本机现在用的是：

- `~/android-toolchain/jdk`：Temurin OpenJDK 17
- `~/android-toolchain/sdk`：Android SDK，platforms;android-34、build-tools;34.0.0、platform-tools
- `~/android-toolchain/gradle-8.9`：Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

仓库里也带了 Gradle Wrapper 8.9，本机没装 Gradle 也能直接用：

```bash
cd AscentApp
./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

## 项目结构

```
AscentApp/
├── import/                      题库编写（Markdown，实体书整理）
│   └── README.md                格式说明与录入原则
├── bank/                        在线题库产物（bank.json + manifest.json）
├── knowledge/                   纸质资料知识库
│   ├── docs/                    规范化正文，保留页码锚点
│   ├── topics/                  主题汇编，带页码出处
│   └── tools/kb.mjs             build / stats / search / page
├── tools/
│   ├── contentcheck/            内容校验 + 题库导出（纯 JVM）
│   ├── qbank/build.mjs          Markdown → 题库编译器
│   └── shufflecheck/            打乱算法校验
└── app/src/main/
    ├── AndroidManifest.xml
    ├── res/                     主题、颜色、字符串、图标、字体、徽章位图
    └── java/com/ascent/climb/
        ├── MainActivity.kt      底部导航、全屏课程、题库检查
        ├── data/
        │   ├── Model.kt         数据模型
        │   ├── Unit1~5.kt       主课程内容
        │   ├── Expansion*.kt    后续补充题目
        │   ├── NewLessons.kt    新大纲课程
        │   ├── Curriculum.kt    课程大纲与目标山峰
        │   ├── BundledBank.kt   课程池装配与复习题插入
        │   ├── BookContent.kt   由 import/*.md 生成，勿手改
        │   ├── Content.kt       界面统一入口与解锁规则
        │   ├── Auth.kt          登录接口与本地模拟实现
        │   ├── ProgressStore.kt 进度持久化
        │   ├── bank/            题库配置、JSON 解码、网络层、仓库
        │   ├── HandbookA~E.kt   技能书
        │   └── Achievements.kt  35 枚成就定义
        └── ui/                  各页面与组件
```

## 改内容

给已有课程加题，写在 `data/Expansion*.kt`：

```kotlin
val EXTRA_QUESTIONS = mapOf(
    "l5" to listOf(
        Question(
            prompt = "题干",
            options = listOf("A", "B", "C"),
            correct = 0,
            explanation = "解析",
            source = "出处"
        )
    )
)
```

加新课程，在 `data/NewLessons.kt` 里写 `Lesson(...)`，再把 id 加进 `Curriculum.kt` 对应单元的 `lessonIds`。顺序和单元归属只改 `Curriculum.kt`，不用动课程本体。

从实体书整理题目走另一条流水线：

```
扫描校对稿
   ↓ 人工 / OCR 整理
import/*.md
   ↓ node tools/qbank/build.mjs
data/BookContent.kt
   ↓ gradle assembleDebug
内置题库
```

`import/` 下以 `_` 开头的 `.md` 是草稿，不参与编译。格式说明在 `import/README.md`。

改完内容跑一次校验：

```bash
bash tools/contentcheck/run.sh
```

这个脚本会把 App 的内容源同步进一个纯 JVM 工程，跑真实装配逻辑，检查题量、难度递增、答案越界、空题、解锁逻辑和成就定义，然后导出 `bank/bank.json` 与 `bank/manifest.json`。不用装模拟器。

改动题库后记得把 `data/bank/BankConfig.kt` 里的 `BUNDLED_VERSION` 加一。否则客户端会认为没有新版本。

## 题库在线更新

题库有三层来源：

```
① 本地缓存    filesDir/bank_cache.json    之前下载并校验通过的 JSON
② 内置题库    随 APK 编译进来              永远可用
③ 在线题库    有网时检查，比本地新才覆盖 ①
```

实际取用顺序是 ① → ②。在线题库只做增量更新，所以飞机上、山里没信号、服务端挂了、JSON 格式错、sha256 不匹配、版本更旧，这些情况都只会保留现状，不会影响学习。

启动后静默检查更新，距上次检查超过 6 小时、且已经配置地址才发请求。首次启动不会弹窗阻塞。

发布一份新题库：

1. 改内容
2. 把 `BUNDLED_VERSION` 加一
3. 跑 `bash tools/contentcheck/run.sh`
4. 把 `bank/bank.json` 和 `bank/manifest.json` 传到服务器、CDN 或 GitHub raw
5. 下次打开 App 自动更新，不用发版

旧版 App 不会被新题库搞崩。`manifest.json` 里的 `minAppVersion` 会拦住版本不够的客户端，未知字段直接忽略，单条题目格式错误只会跳过那一条。

本机测试可以拿 Mac 当服务器：

```bash
cd bank && python3 -m http.server 8099
# 手机和 Mac 连同一 Wi-Fi
# App：我的 → 设置 → 关于 → 连点版本号 7 次 → 开发者模式 → 在线题库地址
# 填 http://<Mac 的局域网 IP>:8099，保存并检查
```

debug 构建允许明文 HTTP，release 仍强制 HTTPS。测完用「清空地址」恢复。

### 题库 JSON 长什么样

```jsonc
{
  "schema": 2,
  "version": 15,
  "updatedAt": "2026-10-19T11:24:02+08:00",
  "minAppVersion": 6,
  "tracks": [
    {
      "id": "climb",
      "title": "高山攀登",
      "units": [
        {
          "id": "m5i",
          "index": 1,
          "code": "5000M · 入门",
          "title": "5000 米 · 入门",
          "altitude": 5000,
          "subtitle": "以科普为主。这个高度的山峰多是徒步型，先把环境、装备、体能打好底。",
          "peaks": [
            { "name": "奥太娜", "altitude": 5211, "location": "四川·黑水", "style": "徒步型" }
          ],
          "lessons": [
            {
              "id": "l1",
              "title": "高山环境与风险认知",
              "code": "ALPINE RISK",
              "icon": "🏔️",
              "needsCoach": false,
              "brief": ["高山的危险分两类……"],
              "questions": [
                {
                  "type": "Single",
                  "prompt": "题干",
                  "options": ["A", "B"],
                  "correct": 0,
                  "explanation": "解析"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

`manifest.json` 只放版本、文件名、sha256 和最低 App 版本。不想写 manifest 也可以，只放 `bank.json`，App 会退化成整体下载后比较 `version`，代价是每次检查都要传完整文件。

## 登录接口

界面只依赖 `AuthApi` 这一层：

```kotlin
interface AuthApi {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String, nickname: String): AuthResult
    suspend fun logout()
}
```

现在的实现是 `MockAuthApi`。不联网，只做格式校验，账号存在本机 SharedPreferences。所以登录页写的是「本地模拟登录」，没有写同步。

后端就绪后换掉这个实现就行，UI 不用动。接口大致是：

```
POST /auth/login    { email, password }            -> { token, user }
POST /auth/register { email, password, nickname }  -> { token, user }
POST /auth/logout
```

## 已知限制

- 没有做模拟器运行时验证。本机没装 Android 模拟器，验证都是编译、打包、资源和逻辑校验，真机走查由开发者自己做。
- 登录是本地模拟，没有真实后端和 token。
- 进度只存在本机，登录之后也没有真正同步。
- 内容没有经过登山教练和医生的实审。药物剂量和急救流程建议正式上线前请人过一遍。
- 题图只随 APK 发布，还不能像题目那样从在线题库下载。图片加多了 APK 会变大。
- 路径页的 UI 改版还在进行，部分设计稿没有落到 Compose。

## 许可

这个项目用 [PolyForm Noncommercial License 1.0.0](LICENSE)。非商业用途可以自由使用、复制、修改和分发，商业用途需要另外的书面授权。教育、研究、公益、公共研究机构、政府机构这些属于许可范围内的非商业用途。

第三方组件、内容来源和免责声明在 [NOTICE](NOTICE)。简单说几句：

- 仓库里不放纸质书的扫描原文，只放整理后的主题笔记。
- 35 枚成就徽章是 AI 渲染生成的。
- 内容不构成医疗建议，药物清单只是参考值。
