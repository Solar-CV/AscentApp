# AscentRoam 云端服务方案（账号 · 进度同步 · 题库托管）

> 内部文档。建立于 2026-11-03，对应代码基线 v0.26.8 / versionCode 153（云端已上线：设备管理、头像与学习进度按账号隔离）。
> 注：本文件随仓库提交（不含密钥与凭据）；`HANDOFF.md` 仍只留本地。
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
  ├── data/cloud/CloudHttp.kt      HttpURLConnection + JSON（沿用 BankRemote 的零依赖风格）
  ├── data/cloud/CloudApi.kt       带 access token，401 自动刷新一次再重试
  ├── data/cloud/CloudAuthApi.kt   implements AuthApi
  ├── data/cloud/TokenStore.kt     Android Keystore AES-GCM 加密存 access + refresh
  ├── data/cloud/SyncPayload.kt    ProgressStore ⇄ JSON 快照 + 合并规则
  └── data/cloud/SyncEngine.kt     拉取 → 合并 → 回写，409 重试
        │  HTTPS + Bearer
        ▼
CloudBase HTTP 云函数  ascent-api（Node 18，单进程标准 http 服务，:9000，**零依赖**）
  ├── 自建账号体系（scrypt 密码哈希 + access/refresh）  ← 不绑定平台 auth，保证可搬迁
  ├── PostgreSQL：users / identities / credentials / sessions / states / avatars
  ├── 数据访问走 PG 的 REST 网关（PostgREST 语义），不带 node_modules
  └── 头像直接存库（base64，512×512 JPEG ≈ 40–60 KB），不引对象存储与签名 URL
```

**为什么是 PostgreSQL 而不是文档库**：这个环境 `RuntimeBackends.nosql=false`，
新建环境只给 PG（共享实例，只能走 SDK/网关，不能 TCP 直连）。这反而更合算——
表结构可读、能直接 `pg_dump` 搬走，正好把 §10.3 里排二期的 B3 提前做掉。

**为什么不用 CloudBase 内置身份认证**：账号数据会锁死在平台里，将来搬 VPS 无法迁移。
自建账号多写约 150 行，换来账号可导出、可搬迁。

---

## 3. API 契约 v2

> v2（2026-11-03）：账号从「单 token」改为 access + refresh，并拆出身份表、补设备管理。
> 变更是**破坏性**的，因为此时还没有任何真实账号，不保留 v1 兼容。

统一前缀 `{BASE}`（部署后填进 `CloudConfig`）。所有请求 `Content-Type: application/json; charset=utf-8`，
除 `/health`、`/auth/register`、`/auth/login`、`/auth/google`、`/auth/refresh` 外都要
`Authorization: Bearer <access_token>`。

错误响应统一：`{ "error": "code", "message": "人话" }`。客户端只认 `error` 码。

### 3.1 账号

| 方法 | 路径 | 请求 | 成功 | 主要错误 |
|---|---|---|---|---|
| POST | `/auth/register` | `{email, password, nickname, code?, device?}` | 201 `{user, access_token, refresh_token, expires_in, is_new_user}` | 409 `email_taken`、400 `invalid_email` / `weak_password` / `invalid_nickname` / **`invalid_code`**、429 `too_many_attempts` |
| POST | `/auth/email/code` | `{email, purpose: "register"\|"reset"}` | 200 `{ok}`（**无论邮箱是否已注册都 200**） | 400 `invalid_email`、429 `too_many_requests`、502 `mail_failed`、503 `mail_not_configured` |
| POST | `/auth/password/forgot` | `{email}` | 200 `{ok}`（同上，防枚举） | 同上 |
| POST | `/auth/password/reset` | `{email, code, newPassword}` | 200 `{ok}`；**作废该账号全部会话** | 400 `invalid_code` / `weak_password`、423 `account_locked`、429 `too_many_attempts` |
| POST | `/auth/login` | `{email, password, device?}` | 200 同上 | 401 `invalid_credentials`、423 `account_locked`、403 `email_unverified`、429 `too_many_attempts` |
| POST | `/auth/google` | `{idToken, nickname?, device?}` | 200 同上 + `google_picture?` | 401 `invalid_google_token`、**403 `email_unverified`**、409 `identity_conflict`、423 `account_locked`、501 `google_not_configured` |
| POST | `/auth/refresh` | `{refresh_token}` | 200 `{access_token, expires_in, user}` | 401 `invalid_token`、423 `account_locked` |
| GET | `/auth/me` | — | 200 `{user}` | 401 `unauthorized` / `token_expired` |
| PATCH | `/auth/profile` | `{nickname}` | 200 `{user}` | 400 `invalid_nickname` |
| POST | `/auth/logout` | — | 204（只撤销**当前会话**） | — |
| DELETE | `/auth/account` | — | 204（级联删 sessions / state / avatar / identities / credentials） | 401 |
| POST | `/auth/identities/google` | `{idToken}` | 200 `{user}`（已登录时的主动绑定） | 409 `identity_taken`、501 |
| DELETE | `/auth/identities/google` | — | 200 `{user}` | 404 `identity_not_found`、409 `last_identity` |

`user` 对象：`{id, email, nickname, emailVerified, avatarVersion, createdAt}`。永不返回密码哈希。

`/auth/google` 成功时额外带 `google_picture`（Google 头像 URL）。**服务端不下载这张图**：
客户端拿到 URL 自己下载、压缩、走 `/avatar` 上传，复用现有图片链路，
也避免服务端因「用户可控 URL」而暴露在 SSRF 下。

`device` 可选：`{device_id, platform, app_version}`，只用于「已登录设备」列表，
不含任何硬件标识。客户端用 `ProgressStore.deviceId`（随机生成，非 ANDROID_ID）。

**`401` 的两种码要分开**：`token_expired` 表示 access 过期、可以拿 refresh 换；
`unauthorized` 表示这个会话彻底不认了，必须重新登录。客户端只对前者自动重试一次。

### 3.1.1 设备管理
| 方法 | 路径 | 成功 |
|---|---|---|
| GET | `/me/devices` | 200 `{devices:[{id, device_id, platform, app_version, created_at, last_used_at, current}]}` |
| DELETE | `/me/devices/{id}` | 200 `{revoked:true, self:boolean}`；撤销自己那台时 `self=true`，客户端应清本地登录态 |

界面已落地（`ui/DevicesScreen.kt`，2026-11-04）：设置 → 账号 → 已登录设备。
列出平台/版本/最近使用，当前设备带「当前设备」标记；**当前设备也允许撤销**，
确认框会说明后果（需要重新登录、进度不丢），撤销成功后本机立即退出登录。

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

`GET /health` → `{ok:true, version:"2", time}`（探活用，不鉴权）。

---

## 4. 数据模型（CloudBase 文档数据库）

一个用户 = 一个 `user_id`，可以有多个登录身份。**不要把 provider 字段挂在 users 上**：
加第二个第三方登录就要改表，而拆表后加 Apple / 微信只是插一行。

```
ascent_users
  _id, email(小写，唯一), emailVerified, status("active"|"locked"),
  nickname, avatarVersion, createdAt, updatedAt, loginFails, loginFailWindow

ascent_identities          ← 登录身份，多对一
  _id, userId(索引), provider("password"|"google"), subject, createdAt
  unique(provider, subject)   ← password 的 subject 是邮箱，google 的是 sub

ascent_credentials         ← 只有 password 身份才有
  _id, userId(唯一索引), passwordHash, updatedAt

ascent_sessions            ← 一个会话 = 一台设备
  _id, userId(索引), accessHash(唯一), accessExpiresAt,
  refreshHash(唯一), refreshExpiresAt,
  deviceId, platform, appVersion, createdAt, lastUsedAt, revoked

ascent_states
  _id, userId(唯一索引), rev, payload(对象), updatedAt, payloadBytes

ascent_avatars
  _id, userId(唯一索引), data(base64), version, updatedAt
```

密码哈希：`scrypt$N$r$p$saltHex$hashHex`（**N=2^17, r=8, p=1**, 64 字节），
比对用 `timingSafeEqual`。N=2^17 单次约需 128 MB 内存，**云函数内存必须 512 MB**。

Token：access 与 refresh 都是 32 字节随机 hex，**库里只存 sha256**，拿到数据库不等于拿到会话。
access 1 小时；refresh 90 天、使用即顺延、可在设备列表里单独撤销。

Google 登录只信服务端验证过的 `sub`：验 `aud` / `iss` / `exp`，不信任客户端传来的用户 ID，
也**不按邮箱并号**（否则谁拿到同邮箱的 Google 账号就能接管进度）。

---

## 5. SyncPayload v1

只放**学习数据**。用户名与头像属于账号（走 `/auth/profile`、`/avatar`），主题 / 语言 / 轨道 /
开发者开关属于设备，一律不同步——这样就没有「两处都能改、谁赢」的冲突。

### 5.0 账号资料（昵称 / 头像）的归属

- **昵称以账号为准**：登录后本地 `displayName` 会被账号昵称覆盖；在 App 里改名会 `PATCH` 到账号，
  成功后写回 `syncedNickname`。同步时若本地没有未上传的改动（`displayName == syncedNickname`），
  就接受账号里的新名字 —— 于是别的设备改名这边也能看到。
- **头像双向对账**：本地改过（`avatarDirty`）就上传；否则云端的 `avatarVersion` 更大就下载。
  不用版本号比大小判断「本地是否改过」：下载也要 +1 触发重新解码，两个计数器必然错位，
  会变成刚下载完又推一次的来回打架。

### 5.0.1 换账号时的处理（2026-11-05）

设备上只有一份头像文件、一份学习进度，两者语义上都属于账号，但迁移策略不同：

- **头像**：退出即清（`AvatarSync.forgetLocal`），换号绝不复用。
  服务端 `avatar_rev` 只增不减，`avatar_version` 仅表示「当前头像的版本，0 = 没有」——
  否则「删除后再上传」会从 1 重来，别的设备记着更大的版本号，永远收不到新图。
- **进度**：退出**保留**（离线继续学是这个 App 的前提）；换到不同账号时，登录后的第一次同步
  以云端**覆盖**本地（`pendingCloudReplace`，只生效一次，成功后才清除）。
  首次登录（`progressOwner` 为空）仍走合并，避免老用户升级后被清空。

判断依据分别是 `avatarOwner` 与 `progressOwner`（记录本机这份数据属于哪个 userId，退出时不清）。
两者都是「设备上一份、账号里一份」，没有归属字段就只能靠版本号猜，必然出错。


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
- **密码永不出客户端明文存储**；access / refresh 用 Keystore 加密，且库里只存哈希。
- **access 1 小时、refresh 90 天**：前者限制泄露半径，后者让用户不必反复登录；
  「已登录设备」列表可以单独撤销某一台（丢手机的场景）。
- **Google 身份只认服务端验过的 `sub`**，不按邮箱并号（见 §10.1 A2）。
- **登录限流**：同邮箱 15 分钟内失败 10 次 → 429，计数存 `ascent_users` 旁挂文档。
- **删除账号级联**：user + tokens + state + avatar 一起删（Play 政策要求，且 `/auth/account`
  必须真的删服务端数据，不能只清登录态）。
- **隐私政策与 Play 数据安全表单**：条款版本 **2026-11-05**（v0.26.7）已在第 2 节补「App 内反馈」、
  第 7 节补 Formspree，App 与官网 `ascentroam.com/privacy` 同源。数据安全表单据此声明：
  邮箱 / 名称 / 用户 ID / 照片 / 应用互动 / 其他用户生成内容 / 设备或其他 ID / 崩溃日志，
  全部为**收集不共享、可选、HTTPS 传输**；第三方（Resend / ImprovMX / Formspree / CloudBase）
  属受托处理者，不计入「共享」。
- **GDPR**：`/auth/account` 删除 + SyncPayload 导出即满足访问权与删除权。
- 日志里不写邮箱、token、payload 全文；只记 `userId` 与状态码。

---

## 8. 里程碑与验收

| 阶段 | 内容 | 验收 | 状态 |
|---|---|---|---|
| M0 | 题库托管：`bank.json` + `manifest.json` 上静态托管，`DEFAULT_BASE_URL` 指向它 | 真机设置页「检查更新」能拿到新版题库；断网时不报错、保留内置 | 未开始 |
| M1 | 服务端 + 客户端账号：注册 / 登录 / 登出 / 改昵称 / 删号 / 头像上传 | 两台设备同一账号能登录；删号后服务端记录为零 | **服务端已上线并端到端验过**；客户端已接，待真机 |
| M2 | 进度同步：自动 + 手动 + 冲突重试 | 设备 A 学 3 课 → 设备 B 登录后看到同样海拔；B 再学 2 课 → A 下次启动合并到 5 课 | **已完成**：启动时 + 每小时一次自动同步，账号卡有「立即同步」与上次同步时间 |
| M3 | 导出 / 导入学习记录（本地 JSON 文件） | 不登录也能换机迁移 | 未开始 |

M3 与 M2 共用 `SyncPayload`，实现成本很低，且让「不注册也能备份」成立。

### 部署记录（2026-11-03）

| 项 | 值 |
|---|---|
| 环境 | `ascent-app-d1g4gt73867789064`（上海 / 个人版 / PG 17.11 / 2026-12-03 到期） |
| 函数 | `ascent-api`，HTTP，`Nodejs18.15`，512 MB / 30 s，包体 24 KB（零依赖） |
| 网关 | `https://ascent-app-d1g4gt73867789064-1503487069.ap-shanghai.app.tcloudbase.com`，`/` → 函数，`auth=false`，路径透传 |
| 迁移 | `20261103144500_init_accounts`（六张表 + 索引 + RLS） |

端到端实测（上海 → 网关）：注册 2.5 s（冷启动 + scrypt 128 MB），后续请求 0.4 s；
`409` 冲突、`jsonb` 往返、`refresh`、设备列表、删号级联全部符合预期。

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
>
> **状态（2026-11-03）**：A1–A6 已落地 —— 服务端 `server/functions/ascent-api/index.js`
> （冒烟 66 条全过），客户端 `data/cloud/`（`CloudApi` 自动刷新、`TokenStore` 双 token）。
> A7 备份演练要等环境开出来才能做。设备管理的接口、数据与界面均已完成（2026-11-04）。

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
CloudBase HTTP 云函数 ascent-api（零依赖）
   ├── 账号：users + identities + credentials + sessions(access/refresh)
   ├── 同步：ascent_states（快照 + rev 乐观锁）
   └── 头像：内联 base64（<100 KB 规模够用）；超过再迁对象存储
        │
CloudBase PostgreSQL（共享实例，走 REST 网关，不能 TCP 直连）
```

四条硬边界（从备选方案里吸收的口径）：

1. **一个用户 = 一个 user_id，可绑多个 provider。**
2. **第三方身份只信服务端验过的 `sub`**，不信客户端传来的用户 ID，也不按邮箱并号。
3. **训练进度同步接口不承担内容分发**：课程、题库、3D 模型继续走内置资源或独立的
   `bank.json` 通道，云端只存用户数据（§3.1 已按此划分）。
4. **本地永远可离线**：云端不可达时，任何主流程都不受影响（与 §0 一致）。


---

## 11. 备份 · 邮箱验证 · 找回密码（方案对比，2026-11-03）

三件事看起来独立，实际共用两个底座：

```
        ┌──────────── 发信通道（域名 + 邮件服务） ────────────┐
        │                    │                    │          │
   注册验证码            找回密码验证码          （以后）改邮箱
        └──────────── 验证码表 + 限流 + 一次性令牌 ──────────┘

   定时任务（CloudBase timer）── 每日备份 ── 对象存储
```

先把底座定下来，三件事都是薄薄一层业务。下面逐个对比。

### 11.1 备份

**先明确防什么**，否则方案一定选偏：

| 威胁 | 例子 | 需要的形态 |
|---|---|---|
| 误删 / 改错 | 我跑错一条 `delete`、用户手滑删号 | 时间点恢复（能回到昨天） |
| 环境级事故 | 欠费被回收、误删环境 | 异地副本，且不依赖平台 |
| 实例损坏 | PG 磁盘故障 | 导出文件（平台修不了的部分） |

| 方案 | 成本 | 能防 | 评价 |
|---|---|---|---|
| **A. 升级标准版（14 天数据回档）** | 套餐差价，月付量级 | 误删、实例故障 | 最省事但最贵，且防不了「环境被删」；零用户阶段不值 |
| **B. 定时导出 → 对象存储** | 几乎为零（函数 + 几 MB COS） | 全部三类（只要桶还在） | **推荐主方案** |
| **C. 本机/异地第三份** | 零 | 平台级事故 | 作为 B 的补充，每月手工拉一份 |
| **D. `users.deleted_at` 软删除** | 小 | 误删账号 | 只能防一类，且和「账号删除」政策冲突，不做 |

**B 已落地（2026-11-03，实测通过）**

共享实例不能 TCP 连，也没有 `pg_dump` 二进制，所以导出走**已有的 PG REST 网关**：

```
ascent-backup（Event 函数，每天 03:20，不挂网关）
  ├── keyset 分页读六张表   GET /v1/rdb/rest/<table>?order=<pk>.asc&<pk>=gt.<cursor>
  ├── 打包 {schema,takenAt,env,counts,tables}
  ├── gzip(9) → AES-256-GCM
  └── PUT /v1/storages/object/backup/YYYY-MM-DD.asc
        写 ascent_backup_log  → 按文件名日期删 30 天前的文件
```

**踩过的坑（很重要，别再走一遍）**

- `app.uploadFile()` 在这个环境直接报 `STORAGE_NOT_SUPPORTED`。PG 环境不是旧 COS，
  存储走 **pgstore**：`/v1/storages/object/{bucket}/{key}`，只需 `Authorization: Bearer <API Key>`。
  桶必须先手工建 —— 浏览器/SDK 都不会自动建。已建私有桶 `backup`（`storage.buckets`）。
- 因此这个函数**也是零依赖**，和 `ascent-api` 一样只有 index.js。
- 插入日志时 `Prefer: return=representation` 不能省，否则 PostgREST 只回 201 空体，拿不到 id。
- 分页**必须 keyset**（`> 上一页最后一行`），不能用 offset：备份期间有写入时 offset 会整页前移、悄悄漏行。

**密钥**：`BACKUP_KEY`（32 字节 hex，AES-256-GCM）存在函数环境变量，
本机另存一份在 `~/.ascent/backup.key`（权限 600），**务必再抄一份到密码管理器**。
定位要说清楚：加密防的是「备份文件泄露」，不防「控制台被盗」——后者能看到环境变量。
密钥丢了备份就废了，这不是可选项。

**文件格式**：`magic("ASCB1") | iv(12) | tag(16) | ciphertext`，恢复脚本据此识别与校验。

**恢复**：`server/tools/restore.mjs`
- 默认 dry-run（只解密 + 比对行数，绝不写库），`--yes` 才真写。
- `--suffix=_drill` 写影子表，用于定期演练。
- 写入顺序按外键：users → identities/credentials → states/avatars → sessions，删除反序。

**已做的演练（2026-11-03）**：建 `ascent_*_drill` 影子表 → 灌入当天备份 →
六表行数全对，且逐字段比对（邮箱/昵称、密码哈希、state payload+rev、头像 data+version、
身份 provider+subject）**全部一致** → 删掉影子表。


### 11.2 发信通道（三件事的共同前提）

**没有域名就没有像样的邮件**。这是硬前提，不是可选项：

- 用个人邮箱（QQ/163）走 SMTP：能发，但发件人是个人邮箱、日限额低、几乎必进垃圾箱，只能用来跑通流程。
- 任何正经邮件服务都要求**验证域名**（SPF / DKIM / DMARC），否则到达率惨不忍睹。

| 方案 | 成本 | 国内到达 | 接入 | 备注 |
|---|---|---|---|---|
| 腾讯云邮件推送 SES | 按量，便宜 | 好 | 中（域名验证 + 模板审核） | 同厂，控制台统一；模板要审核 |
| 阿里云邮件推送 | 按量 | 好 | 中 | 同上 |
| **Resend** | 免费额度约 100 封/天 | 一般（跨境延迟） | **极简**（一个 POST） | API 干净，适合零依赖函数；海外用户最顺 |
| SendGrid / Postmark | $15–20/月起 | 中 | 中 | 功能全，对当前规模偏重 |

推荐：**看主要用户群**。国内为主 → 腾讯云 SES；海外为主或想省事 → Resend。
两边都要做的话，抽象成 `sendMail(to, template, vars)` 一个函数，换供应商只改这一处。

**先说一个容易忽略的硬约束：备案。**

腾讯云 SES 的国内节点要求发信域名**已完成 ICP 备案**；而备案要求域名在境内有资质的注册商注册，
并且通常要绑一台境内服务器（备案服务号）。Cloudflare 注册的域名无法在国内备案。

所以这里有个二选一，必须在买域名之前定：

| 路线 | 域名注册 | 发国内邮箱 | 启动成本 | 等待 |
|---|---|---|---|---|
| **A. 先跑通**（建议） | 腾讯云注册（保留备案资格） | 用 Resend 试，实测 qq/163 到达率 | 域名 ~¥80/年 + 发信 ¥0 | 域名实名 1–3 天 |
| B. 一步到位 | 腾讯云注册 | 腾讯云 SES（备案后） | 域名 + 轻量服务器 ≥3 个月 ≈ ¥300 | **1–3 周备案** |
| C. 纯海外 | Cloudflare（成本价） | 无 | ~$11/年 | 几小时 |

**已定并已采购：`ascentroam.com`**（DNSPod 注册，2026-11-03，2027-11-03 到期）。

**DNS 留在 DNSPod，不迁 Cloudflare。** 原因：

- 域名本来就注册在 DNSPod，默认 NS 已经在用，迁走是纯粹的多余动作
- 国内解析 DNSPod 更快，而这个 App 的主要用户在国内
- Cloudflare 的真正价值是 CDN/WAF 反代，但 API 已在 CloudBase（腾讯侧本来就有 CDN），
  法务页面用 GitHub Pages 就够 —— 没有需要 Cloudflare 挡在前面的东西
- 保留备案资格：将来真要备案，NS 在境内是最省事的形态

发信接 **Resend 免费版**（3,000 封/月、100 封/天）。域名验证要加 3–4 条记录
（DKIM 的 `resend._domainkey`、`send` 子域的 SPF 与 MX、可选 `_dmarc`），
具体值以 Resend 控制台为准（DKIM 密钥是每域名生成的，不能预先写死）。

**发件地址必须落在已验证的域名下**，当前默认 `AscentRoam <no-reply@ascentroam.com>`，
配在环境变量 `MAIL_FROM` 里，改地址不用改代码。

判断依据是「先花 1–3 周备案」不合理：现在连一个真实用户都没有，用 Resend 实测
qq.com / 163.com 的到达率只要半天。如果实测进垃圾箱，那时再花时间备案并切腾讯云 SES，
代码只需换 `sendMail()` 的实现。域名注册在境内，这条路始终留着 —— 这就是不选 Cloudflare 的原因。

**模板语言**：10 种界面语言不可能各写一封。建议**只做中 / 英两版**，其余语言回退英文，
邮件里明确写出「验证码 10 分钟内有效」，数字用半角。

### 11.2.1 域名与邮件（已上线，2026-11-04）

域 `ascentroam.com`，注册与解析都在 **DNSPod**（NS `cap/plover.dnspod.net`）。
下面这张表是**完整的记录清单**，加任何新服务前先看它，别造出第二条 SPF。

| 类型 | 主机记录 | 记录值 | 归属 | 作用 |
|---|---|---|---|---|
| TXT | `resend._domainkey` | `p=MIGf…IDAQAB` | Resend | DKIM 签名 |
| CNAME | `rsend` | `rsend-apne1.forge.rmta.net` | Resend | 退信/SPF 对齐 |
| CNAME | `send` | `send.forge.rmta.net` | Resend | 发信 SPF（Return-Path） |
| TXT | `_dmarc` | `v=DMARC1; p=none;` | 公共 | 暂只观察，不拦截 |
| MX | `@` | `mx1.improvmx.com`（10） | ImprovMX | 收信 |
| MX | `@` | `mx2.improvmx.com`（20） | ImprovMX | 收信备用 |
| TXT | `@` | `v=spf1 include:spf.improvmx.com ~all` | ImprovMX | 根域发信声明 |

**两条系统各管一端，互不影响**：`no-reply@` 走 Resend（SPF/DKIM 都在 `send` 子域与
`resend._domainkey`），`support@` 走 ImprovMX 转发到 `sakikodesu@126.com`。
根域的 MX 只决定「别人发给 @ascentroam.com 的信投到哪」，不参与发信。

**红线**：根域**只能有一条 SPF**。将来若让根域也用别的服务发信，必须把 include 合并进同一条
TXT，加第二条会让两条同时失效。同理，MX 换服务商时要先删旧的，否则邮件会随机走错服务器。

**踩过的坑**：`send.forge.rmta.net` / `rsend-apne1.forge.rmta.net` **没有 A 记录是正常的**，
它们只挂 TXT。查 A 为空不等于配错，要查 TXT。另外本机 DoH（`cloudflare-dns.com`）不通，
别拿它当判据，直接用 `dig @8.8.8.8`。

**ImprovMX 免费版**：1 域名 / 25 别名 / 500 封每天 / 7 天日志。**不含发信**（SMTP 额度 0），
所以回信要从 126 邮箱发，别在页面上写「回复此邮件」。

**邮箱地址的用途划分**：`support@` 是对外唯一联系入口（隐私政策、删号页、Play 控制台）；
`no-reply@` 只发不收。



### 11.3 验证码设施（注册与找回共用）

一张表 + 一个接口，两个用途共用：

```
ascent_email_codes                       -- 已建，见 20261104103000_email_codes.sql
  id(uuid), email, purpose('register'|'reset'), code_hash,
  expires_at, attempts, consumed_at, created_at
```

- 码为 **6 位数字**，库里只存 **HMAC-SHA256(pepper, email|purpose|code)**
  —— 裸 sha256 对 100 万种可能可以秒级全表反查，pepper 是必需的，不是锦上添花
- 有效期 **10 分钟**；最多试 **5 次**，超过作废；`attempts` 由服务端扣，客户端说的不算
- 同一邮箱同用途：**1 分钟 1 次、1 小时 5 次**
- 校验成功即写 `consumed_at`，不可重放
- 发信失败会把刚插入的那条码删掉，否则用户拿着一个永远收不到的码，还占住 1 分钟间隔

**开关**：`REQUIRE_EMAIL_CODE`。发信没接通时保持 `false`，注册退回旧流程
（`email_verified` 记 false）；接通后翻 `true` 即生效，**不用改代码也不用重新部署**。
这说明为什么这些配置是「调用时读环境变量」而不是模块加载时定死。

**为什么用验证码而不是邮件里的链接**：链接需要一个落地页（Web 站）。我们没有 Web 前端，
为一封邮件去建站点 + 配 App Links（域名校验文件、签名指纹）不划算。验证码在 App 内输入，
零 Web 依赖，和现有登录页的形态也一致。等以后做 Web 版或官网，再补链接式。

### 11.4 注册验证：先验证还是后验证

| | **A. 先验证（发码 → 验码 → 建号）** | **B. 后验证（先建号，标记未验证）** |
|---|---|---|
| 库里状态 | 只有验证过的账号 | 会有未验证的垃圾账号 |
| 注册步骤 | 多一步输码 | 少一步 |
| 中途退出 | 什么都不留下（更好） | 留一个半成品账号 |
| 改造量 | `register` 拆两步：`send-code` + `register(code)` | 几乎不动接口，只加提醒 UI |
| 兼容存量 | 现在**零真实用户**，正是改的时机 | 需要考虑老账号怎么补验证 |

推荐 **A**。理由不是「更安全」这种空话，而是现在库里没有存量账号：改成先验证，
以后永远不会出现「这个邮箱被谁占了但其实没人用」的脏数据；晚改就要写迁移，
还要处理一堆未验证的老账号。

顺带：A 落地后 `emailVerified` 恒为 true，`REQUIRE_EMAIL_VERIFICATION` 开关就可以删掉了 ——
代码里少一个分支。

**Google 注册是例外，不发验证码**（已确认的设计）：

| 项 | 做法 |
|---|---|
| 邮箱 | `id_token.email` + `email_verified=true` 由 Google 担保，**跳过验证码直接建号** |
| 昵称 | 取 `id_token.name`（无则 `given_name`），落 `users.nickname`，客户端同时写进 `displayName` |
| 头像 | 取 `id_token.picture`，**客户端下载 → 压成 512×512 JPEG → 走现有 `/avatar` 上传** |
| 拒绝条件 | `email_verified=false` 必须拒绝（Workspace 域配置异常时会出现），不能只看 `email` |

头像为什么让客户端下载而不是服务端抓：服务端抓要处理出网、大小、超时、以及
「用户给了任意 URL」带来的 SSRF 风险；客户端本来就有图片解码与压缩链路，复用它最省。

Google 账号在建号时**不创建 `ascent_credentials` 行** —— 它没有密码。
所以「忘记密码」对 Google-only 账号的语义是「首次设置密码」：
校验码通过后**新插一条 credential**，而不是 update。这条要在实现时写清楚，
否则会在 `update ... where user_id=` 上静默影响 0 行。

`emailVerified` 因此仍然恒为 true（两条路都验证过），但**字段保留**：
以后若要支持「先试用后验证」，它是唯一的开关点。


### 11.5 找回密码

流程（全部在 App 内完成，无 Web）：

1. 登录页 →「忘记密码」→ 输入邮箱
2. `POST /auth/password/forgot {email}` → 无论邮箱是否存在都返回 200（防枚举）
3. 用户收码 → `POST /auth/password/reset {email, code, newPassword}`
4. 成功 → **作废该用户全部会话**（密码变了，旧设备必须重新登录）+ 清空验证码

安全要点：
- 验证码校验失败统一回 `invalid_code`，不区分「码错」和「已过期」（少一条情报）
- 改密后踢掉所有 session 是必须的 —— 否则「账号被盗后改密」等于没改
- 新密码沿用现有策略（≥8 位 + scrypt 2^17）
- 忘记密码界面本身不需要登录态，但要走 §7 的限流

### 11.6 工作量与顺序

| 顺序 | 内容 | 状态 |
|---|---|---|
| 1 | 备份：导出函数 + 桶 + 加密 + 恢复脚本 + 演练 | **已完成（2026-11-03）** |
| 2 | 域名 + 静态托管（隐私政策 / 账号删除页） | 域名已购（`ascentroam.com`，DNSPod）；静态页未做 |
| 3 | 发信通道 + 验证码表 + 发码接口 | **已完成（2026-11-04，域名已验证，已发真实邮件到 QQ 邮箱）** |
| 4 | 注册改造（两步）+ 客户端输码页 | **已完成（前后端，模拟器验证）** |
| 5 | 找回密码（接口 + 页面 + 踢会话） | **已完成（前后端）** |
| 6 | 中英文案 + 10 语言界面文案（约 14 键 ×10） | **已完成** |

剩最后一件外部事项：隐私政策 / 账号删除的**网页落地页**（Play 要求），可以放 GitHub Pages，
绑 `ascentroam.com` 或 `www`，不需要备案（托管在境外）。

### 11.7 四个决定（已确认）

1. **域名**：还没有，走腾讯云注册（保留备案资格），DNS 放 Cloudflare。
2. **用户群**：国内海外都有 → 发信先 Resend 实测，代码抽象成 `sendMail()`，需要时切腾讯云 SES。
3. **备份加密**：要。AES-256-GCM，密钥放函数环境变量 + 本机 `~/.ascent/backup.key`，
   用户需自行再抄一份到密码管理器（**密钥丢失 = 备份作废**）。
4. **注册改先验证**：接受。**Google 注册例外**——Google 已验证邮箱，直接建号，且顺带带入昵称与头像。

