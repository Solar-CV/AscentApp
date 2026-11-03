# ASCENT 云端服务方案（账号 · 进度同步 · 题库托管）

> 内部文档，仅本地保留（同 HANDOFF）。建立于 2026-11-03，对应代码基线 v0.25.2 / versionCode 143。
> 本文是云端的**契约层**：API 形状、数据模型、合并规则。实现可以换宿主，契约不换。
> 工程现状见 `HANDOFF.md` 头部；本文只记录云端相关。

---

## 0. 三条不可动摇的前提

1. **离线优先不能破。** README 把「全程离线可用、首次启动不登录」当卖点，云端是可选增强：
   未登录、断网、服务端 5xx，都必须退化成现在的行为，不拦任何主流程。
2. **账号是可选入口，不是前置门槛**。首启引导不问账号，路径页直接可学。
3. **契约与宿主解耦。** 客户端只认本文的 HTTP 契约（`/auth/*`、`/state`、`/avatar`）。
   现在由 CloudBase HTTP 云函数实现，将来搬到 VPS 时客户端一行不改。

---

## 1. 现状（读代码得出，不是推测）

| 能力 | 现状 | 位置 |
|---|---|---|
| 登录 | `MockAuthApi`：只校验邮箱格式 + 密码 ≥6，`delay(650)` 假装联网，id = 邮箱哈希，无 token | `data/Auth.kt` |
| 进度 | 20+ 字段全在 SharedPreferences `ascent_progress`，无导出无备份 | `data/ProgressStore.kt` |
| 头像 / 用户名 | `filesDir/avatar.jpg` + `displayName`（prefs），纯本地 | `ui/Avatar.kt` |
| 题库更新 | 三层加载 + 6 小时节流已实现，但 `DEFAULT_BASE_URL = example.com` | `data/bank/BankConfig.kt` |
| 反馈 | Formspree 直投（唯一在用的外部服务） | `data/FeedbackSender.kt` |
| 服务端 | 无 | — |

**有利条件**：`AuthApi` 已是 interface，注释里连 REST 形状都写好了；`deleteAccount()` 的
「服务端要一并删数据」约定也已写在注释里。换实现时 UI 不用动。

---

## 2. 架构

```
Android App
  ├── data/cloud/HttpClient.kt     HttpURLConnection + JSON（沿用 BankRemote 的零依赖风格）
  ├── data/cloud/CloudAuthApi.kt   implements AuthApi
  ├── data/cloud/TokenStore.kt     Android Keystore AES-GCM 加密存 token
  ├── data/cloud/SyncPayload.kt    ProgressStore ⇄ JSON 快照
  └── data/cloud/SyncEngine.kt     拉取 → 合并 → 回写，409 重试
        │  HTTPS + Bearer
        ▼
CloudBase HTTP 云函数  ascent-api（Node 20，单进程标准 http 服务，:9000）
  ├── 自建账号体系（scrypt 密码哈希 + 不透明 token）  ← 不绑定平台 auth，保证可搬迁
  ├── 文档数据库 ascent_users / ascent_tokens / ascent_states / ascent_avatars
  └── 头像直接存库（base64，512×512 JPEG ≈ 40–60 KB），不引对象存储与签名 URL
```

为什么不用 CloudBase 内置身份认证：账号数据会锁死在平台里，将来搬 VPS 无法迁移。
自建账号多写约 150 行，换来账号可导出、可搬迁。

---

## 3. API 契约 v1

统一前缀 `{BASE}`（部署后填进 `CloudConfig`）。所有请求 `Content-Type: application/json; charset=utf-8`，
除 `/health`、`/auth/register`、`/auth/login`、`/auth/google` 外都要 `Authorization: Bearer <token>`。

错误响应统一：`{ "error": "code", "message": "人话" }`。客户端只认 `error` 码。

### 3.1 账号

| 方法 | 路径 | 请求 | 成功 | 主要错误 |
|---|---|---|---|---|
| POST | `/auth/register` | `{email, password, nickname}` | 201 `{token, user}` | 409 `email_taken`、400 `invalid_email` / `weak_password` / `invalid_nickname` |
| POST | `/auth/login` | `{email, password}` | 200 `{token, user}` | 401 `invalid_credentials`、429 `too_many_attempts` |
| POST | `/auth/google` | `{idToken, nickname?}` | 200 `{token, user}` | 401 `invalid_google_token`、400 `google_not_configured` |
| GET | `/auth/me` | — | 200 `{user}` | 401 `unauthorized` |
| POST | `/auth/logout` | — | 204 | — |
| DELETE | `/auth/account` | — | 204（级联删 tokens / state / avatar） | 401 |
| PATCH | `/auth/profile` | `{nickname}` | 200 `{user}` | 400 `invalid_nickname` |

`user` 对象：`{id, email, nickname, provider, avatarVersion, createdAt}`。永不返回密码哈希。

### 3.2 进度

| 方法 | 路径 | 请求 | 成功 | 冲突 |
|---|---|---|---|---|
| GET | `/state` | — | 200 `{rev, updatedAt, payload}`；无记录时 `{rev:0, payload:null}` | — |
| PUT | `/state` | `{rev, payload}` | 200 `{rev, updatedAt}` | **409** `{rev, updatedAt, payload}`（服务端更新，客户端需重合并） |

`payload` ≤ 512 KB（超出 413 `payload_too_large`）。`rev` 单调递增，服务端每次写入 +1。

### 3.3 头像

| 方法 | 路径 | 请求 | 成功 |
|---|---|---|---|
| PUT | `/avatar` | `image/jpeg` 原始字节（≤ 512 KB） | 200 `{version, updatedAt}` |
| GET | `/avatar` | — | 200 `image/jpeg`；无头像 404 |
| DELETE | `/avatar` | — | 204 |

头像只对本人可读（无社交功能，不需要公开 URL）。客户端换头像后 `avatarVersion` 递增，
用于触发本地重新解码，同时作为「是否需要上传」的判断依据。

### 3.4 其它

`GET /health` → `{ok:true, version:"1", time}`（探活用，不鉴权）。

---

## 4. 数据模型（CloudBase 文档数据库）

```
ascent_users
  _id, email(小写唯一索引), passwordHash, provider("password"|"google"), googleSub,
  nickname, avatarVersion, createdAt, updatedAt

ascent_tokens
  _id, userId(索引), tokenHash(sha256), createdAt, lastUsedAt, expiresAt

ascent_states
  _id, userId(唯一索引), rev, payload(对象), updatedAt, payloadBytes

ascent_avatars
  _id, userId(唯一索引), data(base64), version, updatedAt
```

密码哈希：`scrypt$N$r$p$saltHex$hashHex`（N=16384, r=8, p=1, 64 字节），比对用 `timingSafeEqual`。
Token：32 字节随机 hex，**库里只存 sha256**，泄露数据库不等于拿到会话。有效期 180 天，使用即续期。

---

## 5. SyncPayload v1

只放**学习数据**。用户名与头像属于账号（走 `/auth/profile`、`/avatar`），主题 / 语言 / 轨道 /
开发者开关属于设备，一律不同步——这样就没有「两处都能改、谁赢」的冲突。

```json
{
  "schema": 1,
  "appVersion": "0.26.0",
  "exportedAt": 1762000000,
  "completed": ["m5i-l01", "m5i-l02"],
  "stars": { "m5i-l01": 3 },
  "reviews": [ { "k": "m5i-l01#3", "lvl": 2, "due": 20400 } ],
  "achievements": ["first_step"],
  "counters": { "lessonRuns": 42, "reviewDone": 17 },
  "days": ["2026-10-01", "2026-10-02"],
  "streak": { "current": 5, "max": 12, "broken": true, "lastDay": "2026-11-02" },
  "drills": [ { "id": "breath44", "count": 3, "seconds": 300, "best": 0, "last": 20400 } ]
}
```

同一份 JSON 就是「导出 / 导入」文件——换机迁移与云同步共用一个格式，不做两套。

### 5.1 合并规则（客户端执行，确定性）

| 字段 | 规则 |
|---|---|
| `completed` | 并集 |
| `stars` | 每课取 max |
| `reviews` | 键并集；`lvl` 取 max；`lvl` 相同则 `due` 取 max（避免重复催同一题） |
| `achievements` | 并集（只增不减，从不回收） |
| `counters` | 各自取 max |
| `days` | 并集 |
| `streak.current` / `max` | 取 max |
| `streak.broken` | 或 |
| `streak.lastDay` | 取较晚（ISO 日期可直接字符串比较） |
| `drills` | 同 id 的 count / seconds / best / last 各自取 max |

合并是幂等且单调的，所以 409 重试一定收敛（最多重试 3 次，仍冲突则放弃并保留本地，下次再同步）。

### 5.2 已知取舍

- `lessonRuns` / `reviewDone` 取 max 而不是求和：两台设备各练 15 次，合并后是 15 不是 30。
  影响仅限「20 课次」「20 次复习」两条成就，最多稍晚解锁；换成按设备分别计数会把
  成就判定的核心改复杂，不值得。
- 合并后新增的成就只写入 `unlockedAchievements`，并追加到本机待播队列；
  `pendingAchievements` 本身不同步（否则同一枚徽章会在每台设备各弹一次）。

---

## 6. 客户端改动清单

| 文件 | 动作 |
|---|---|
| `data/Auth.kt` | `MockAuthApi` 保留（离线开发用）；新增 `CloudAuthApi`；`AuthApi` 接口不变 |
| `data/cloud/CloudConfig.kt` | 新增：`BASE_URL`、`ENABLED`（空串即关闭，UI 隐藏账号入口） |
| `data/cloud/HttpClient.kt` | 新增：GET/POST/PUT/PATCH/DELETE + JSON 编解码 + 超时 + 错误码解析 |
| `data/cloud/TokenStore.kt` | 新增：Keystore AES-GCM 加密存 token |
| `data/cloud/SyncPayload.kt` | 新增：快照编解码 + 合并 |
| `data/cloud/SyncEngine.kt` | 新增：同步流程、409 重试、脏标记 |
| `data/ProgressStore.kt` | 新增：`snapshot()` / `applySnapshot()`、`deviceId`、`lastSyncRev` / `lastSyncAt` |
| `ui/ProfileScreen.kt` | 登录页文案从「本地模拟登录」改为真实语义；账号卡显示上次同步时间与「立即同步」 |
| `ui/SettingsScreen.kt` | 账号段补「导出学习记录」（分享 JSON）；删除账号走服务端 |
| `res/values*` ×10 | 新增约 10 条文案 ×10 语言 |

新增文案尽量复用已有键（`prof_login_*`、`set_section_account`、`acc_delete*` 都在），
只有「同步中 / 上次同步 / 同步失败 / 仅本机」这组是全新的。

---

## 7. 安全与合规

- **全链 HTTPS**，不在客户端内置任何密钥（`BASE_URL` 是公开信息）。
- **密码永不出客户端明文存储**；token 用 Keystore 加密。
- **登录限流**：同邮箱 15 分钟内失败 10 次 → 429，计数存 `ascent_users` 旁挂文档。
- **删除账号级联**：user + tokens + state + avatar 一起删（Play 政策要求，且 `/auth/account`
  必须真的删服务端数据，不能只清登录态）。
- **隐私政策与 Play 数据安全表单**需同步更新：新增「邮箱、昵称、头像、学习进度」四项，
  声明可删除、可导出、不用于广告。
- **GDPR**：`/auth/account` 删除 + SyncPayload 导出即满足访问权与删除权。
- 日志里不写邮箱、token、payload 全文；只记 `userId` 与状态码。

---

## 8. 里程碑与验收

| 阶段 | 内容 | 验收 |
|---|---|---|
| M0 | 题库托管：`bank.json` + `manifest.json` 上静态托管，`DEFAULT_BASE_URL` 指向它 | 真机设置页「检查更新」能拿到新版题库；断网时不报错、保留内置 |
| M1 | 服务端 + 客户端账号：注册 / 登录 / 登出 / 改昵称 / 删号 / 头像上传 | 两台设备同一账号能登录；删号后服务端记录为零 |
| M2 | 进度同步：自动 + 手动 + 冲突重试 | 设备 A 学 3 课 → 设备 B 登录后看到同样海拔；B 再学 2 课 → A 下次启动合并到 5 课 |
| M3 | 导出 / 导入学习记录（本地 JSON 文件） | 不登录也能换机迁移 |

M3 与 M2 共用 `SyncPayload`，实现成本很低，且让「不注册也能备份」成立。

---

## 9. 待定

- Google 登录需要 Google Cloud OAuth 客户端（Web client ID）与 Play 签名 SHA-1 指纹，
  服务端 `/auth/google` 的校验骨架已留（验 `aud` / `sub` / `exp`），配置到位即启用。
- 题库托管域名（GitHub Pages / CloudBase 静态托管 / 自建 CDN）待定，先做 M1/M2。
- 发信服务选型（SMTP / Resend / 腾讯云邮件推送）—— 邮箱验证与找回密码依赖它，见 §10.2。

---

## 10. 与备选方案（Ktor + PostgreSQL 自建）的对照与修订

2026-11-03 收到一份独立设计方案（Ktor Server + PostgreSQL + Docker Compose + Caddy + Restic，
含账号体系、同步机制、备份三层）。逐项对过，结论：**它的数据模型与账号体系比本文档扎实，
部署形态不适合本项目**。以下记录采纳了什么、为什么，避免以后重走一遍讨论。

### 10.1 采纳（部署前就改，趁没有真实账号）

| # | 改动 | 原因 | 成本 |
|---|---|---|---|
| A1 | `users` 拆出 `auth_identities(user_id, provider, subject)`，`unique(provider, subject)` | 原设计把 `provider` / `googleSub` 挂在 `users` 上，加第二个第三方登录就要改表；拆表后加 Apple / 微信只是加一行 | 半天 |
| A2 | Google 登录禁止按邮箱自动并号，改为三条分支：已登录可主动绑定 / 同邮箱先验证再绑 / 已被占用不允许转移 | **原设计有真实接管风险**：按邮箱匹配直接并号，等于任何持有同邮箱 Google 账号的人可接管进度 | 半天 |
| A3 | 密码哈希 scrypt N 由 `2^14` 升到 `2^17`（r=8, p=1），`maxmem` 同步放开 | 当前 16 MB 内存成本低于 OWASP 现行建议（2^17 ≈ 128 MB）。**注意云函数内存要调到 512 MB**，否则 OOM；不想调内存就用 N=2^16 + p=2（≈64 MB） | 1 行 + 配置 |
| A4 | token 拆 access + refresh：access 1 小时，refresh 90 天滑动 + 轮换 | 原设计单枚 180 天不透明 token，泄露半径 180 天。access 取 1 小时而不是方案里的 10–15 分钟：这个 App 冷启动就要同步，15 分钟会让几乎每次启动都触发刷新，收益不抵复杂度 | 1 天（含 401 自动刷新重试） |
| A5 | 登录响应补 `is_new_user`；错误码补 `email_unverified` / `account_locked` | 「成功/失败」两态不够，未验证与冻结必须能区分，否则用户只会看到「密码错误」 | 半天 |
| A6 | 增加 `/me/devices` 与撤销单设备会话 | 有了 refresh token 才有意义，否则用户丢了手机只能改密码 | 半天 |
| A7 | 备份写进文档并做一次恢复演练 | 原文档没有备份章节。没演练过的备份等于没有备份 | 半天 |

> A1–A6 必须在**第一条真实账号写入之前**完成。上线后再改就要写数据迁移，成本和风险都翻倍。

### 10.2 采纳但排到二期

| # | 内容 | 为什么不在第一批 |
|---|---|---|
| B1 | `training_attempts` 训练事件表（事件与汇总分开） | 这是「以后做统计页 / 周报」的唯一前提，但成本在**客户端**不在服务端：现在进度全在 SharedPreferences 的集合里，没有可写的事件表。要上就得先做一次本地存储迁移（→ Room / DataStore），属于独立批次 |
| B2 | 邮箱验证 + 找回密码（`email_action_tokens`） | 没有找回密码，用户忘密码就是永久丢号，而这里的进度是几十小时的积累，**必须有**；但它依赖发信服务选型，单独排期 |
| B3 | 数据库换成 PostgreSQL | 见 §10.3，这属于「换宿主」范畴，等 M2 跑通再评估 |

### 10.3 不采纳

| 内容 | 理由 |
|---|---|
| 后端改为自建 Ktor + Docker Compose（新加坡单机） | 单人维护一台 VPS 意味着 Caddy 证书、systemd、PG 大版本升级、Restic 定时任务、磁盘与内存监控都是长期负担，不是写一次就完。**更关键：它想要的「SQL + 可迁移」用 CloudBase PostgreSQL 就能拿到**，不必换宿主 —— 需要时把 `pg_dump` 导出走人即可，不必现在就承担运维 |
| 客户端「Room 沿用现有实现即可」 | **这句与代码不符**：工程里没有 Room，进度是 `SharedPreferences` 的一堆 StringSet。所以「沿用」不成立，实体级同步前必须先做本地存储迁移（这也是 B1 成本被低估的地方） |
| 每次改动走 mutation log + 游标（`sync_changes` / `sync_mutations` / `pull?after=`） | 对一份 ≈60 KB、以「并集 / 取更大」为主的进度快照，这是用消息队列的思路解决一个 GET 能解决的问题。它还自带两个新麻烦：变更日志要 GC（否则无限增长），客户端游标与本地写必须原子（漏一条就永久错位）。**bootstrap 用快照、真有增量需求再上**，与方案自己写的「对你预计的规模足够可靠」并不矛盾——够用不等于值得现在做 |
| Access token 10–15 分钟 | 见 A4 |
| 新加坡单地域 | 取决于主战场：国内用户走上海/广州明显更快；Play 海外为主才该选新加坡 |
| 模块化单体、不拆微服务 | 无分歧，两边一致 |

### 10.4 融合后的目标形态

```
Android（现状不变：SharedPreferences + 快照）
   │ HTTPS / JSON
CloudBase HTTP 云函数 ascent-api
   ├── 账号：users + auth_identities + password_credentials + sessions(refresh) + email_action_tokens
   ├── 同步：ascent_states（快照 + rev 乐观锁）；M2 之后再评估是否改实体表
   └── 头像：内联 base64（<100 KB 规模够用）；超过再迁对象存储
        │
CloudBase 文档库（或 PG，B3 决定）
```

四条硬边界（从备选方案里吸收的口径）：

1. **一个用户 = 一个 user_id，可绑多个 provider。**
2. **第三方身份只信服务端验过的 `sub`**，不信客户端传来的用户 ID，也不按邮箱并号。
3. **训练进度同步接口不承担内容分发**：课程、题库、3D 模型继续走内置资源或独立的
   `bank.json` 通道，云端只存用户数据（§3.1 已按此划分）。
4. **本地永远可离线**：云端不可达时，任何主流程都不受影响（与 §0 一致）。

