# AscentRoam 云端服务端

对应契约见仓库根目录 `CLOUD-PLAN.md`。这里只写「怎么跑、怎么部署、有哪些坑」。

```
server/
├── functions/ascent-api/     HTTP 云函数（Node.js 18，监听 :9000）
│   ├── index.js              全部路由与业务逻辑
│   ├── scf_bootstrap         启动脚本，路径必须与运行时版本一致
│   └── package.json          **零依赖**
├── functions/ascent-backup/  Event 云函数（定时触发，不挂网关）
│   └── index.js              每日加密导出，**同样零依赖**
├── tools/restore.mjs         备份恢复 / 演练（本地跑）
└── test/smoke.js             本地冒烟（内存版 PostgREST，不连云）
../cloudbase/migrations/      数据库迁移（建表 DDL，由平台 migration 管理）
```

## 数据层为什么是零依赖

环境的 `RuntimeBackends.nosql=false` —— 只有 PostgreSQL，没有文档库；PG 又是**共享实例**
（`TenantType=SHARED`），只能走 SDK 或网关，不能 TCP 直连。

`@cloudbase/node-sdk@2.11` 也没有 `app.rdb()`（实测 `typeof app.rdb === "undefined"`），
所以数据访问直接打 CloudBase PG 的 REST 网关：

```
https://<envId>.api.tcloudbasegateway.com/v1/rdb/rest/<table>
Authorization: Bearer <服务端 API Key>   ← 映射成 service_role，绕过 RLS
```

PostgREST 语义：`?col=eq.value&limit=1`、`POST` 带 `Prefer: return=representation`
拿回整行、`PATCH`/`DELETE` 也用它。JSONB 原样往返，`on delete cascade` 由数据库保证。

**收益**：部署包里没有 node_modules（函数包 24 KB），冷启动更快；要搬 VPS 时
`pg_dump` 导出即可，客户端不用改。

## 本地冒烟

```bash
node server/test/smoke.js
```

用内存版 PostgREST 接管 `fetch`，覆盖注册、登录、鉴权、`401` 两种码、`409` 冲突、
`413` 超限、头像、`jsonb` 往返、级联删号等 67 条断言。测的是真实代码路径
（URL 拼装、错误码映射），只把数据库换成内存表。改完先跑这个，再上云。

## 部署清单（已落地，2026-11-03）

| 项 | 值 |
|---|---|
| 环境 | `ascent-app-d1g4gt73867789064`（上海 / 个人版 / PG 17.11） |
| 函数 | `ascent-api`，HTTP 型，`Nodejs18.15`，**内存 512 MB**、超时 30 s |
| 网关 | `https://ascent-app-d1g4gt73867789064-1503487069.ap-shanghai.app.tcloudbase.com/`，路由 `/`，`auth=false`，**开启路径透传** |
| 迁移 | `cloudbase/migrations/20261103144500_init_accounts.sql` |

1. **内存必须是 512 MB**：scrypt 用 N=2^17，单次哈希约 128 MB，256 MB 会 OOM。
   `manageFunctions` 建函数时不带内存参数，要用
   `callCloudApi(service="scf", action="UpdateFunctionConfiguration", params={MemorySize:512})` 补。
2. **环境变量**
   | 名称 | 说明 |
   |---|---|
   | `TCB_ENV` | 环境 ID（HTTP 函数不会自动注入，必须显式配） |
   | `CLOUDBASE_APIKEY` | 服务端 API Key（`manageAppAuth(action="createApiKey", keyType="api_key")`），兼作 PG 网关凭据 |
   | `GOOGLE_CLIENT_ID` | 可选。不填时 `/auth/google` 返回 501，其余功能不受影响 |
   | `CODE_PEPPER` | 验证码 HMAC 密钥，见「邮箱验证」一节 |
   | `MAIL_FROM` | 发件人，默认 `AscentRoam <no-reply@ascentroam.com>` |
   | `RESEND_API_KEY` | 不填则发码接口 503 |
   | `REQUIRE_EMAIL_CODE` | `false`（默认）/ `true` |
3. **网关路由**：`manageGateway(action="createRoute", upstreamResourceType="WEB_SCF", targetName="ascent-api", path="/", auth=false, enablePathTransmission=true)`。
   函数自己负责路由，所以**必须开路径透传**，否则 `/auth/login` 到后端会被截成别的形状。
4. **CORS**：函数里刻意不设 `Access-Control-Allow-Origin` —— 网关会按环境安全域名填，
   函数再填一遍会变成 `<origin>,*`，浏览器直接拒绝。原生 App 不受影响。
5. **表结构**只能通过 `managePgDatabase(action="applyMigration")` 改，不要用 `execute` 跑 DDL。

## 邮箱验证与找回密码

```
POST /auth/email/code      {email, purpose}        发码（register | reset）
POST /auth/password/forgot {email}                 同上，强制 purpose=reset
POST /auth/password/reset  {email, code, newPassword}
POST /auth/register        {email, password, nickname, code}   ← 开关打开后必须带 code
```

- 码 6 位，**10 分钟**有效，最多试 5 次，同邮箱同用途 1 分钟 1 次 / 1 小时 5 次
- 库里只存 `HMAC-SHA256(CODE_PEPPER, email|purpose|code)`。裸 sha256 对 6 位数字
  可以秒级反查，pepper 不能省
- 发码接口**永远回 200**（邮箱不存在也一样），否则它就成了注册状态查询器
- 重置成功会**作废该账号全部会话**；Google-only 账号首次设密码走 **insert** 而不是 update
  （没有 credentials 行，update 会静默影响 0 行）

**环境变量**

| 名称 | 说明 |
|---|---|
| `RESEND_API_KEY` | 已配（仅发信权限）。不填时 `/auth/email/code` 回 503 |
| `MAIL_FROM` | `AscentRoam <no-reply@ascentroam.com>`（域名已于 2026-11-04 验证） |
| `CODE_PEPPER` | 64 位 hex，本机副本 `~/.ascent/code.pepper` |
| `REQUIRE_EMAIL_CODE` | 已置 `true`：注册必须带验证码 |

开关和所有发信配置都是**调用时读环境变量**，改完只需在控制台改值，不用重新部署。

## 备份（已上线，2026-11-03）

`ascent-backup` 每天 03:20 跑一次：读六张表 → gzip → AES-256-GCM →
写 PG 存储桶 `backup/YYYY-MM-DD.asc` → 写 `ascent_backup_log` → 删 30 天前的文件。
同一账号规模的实测：1,640 ms，30 KB。

**存储必须是 pgstore，不是旧 COS。** `app.uploadFile()` 在这个环境直接报
`STORAGE_NOT_SUPPORTED`，正确端点是：

```
PUT    /v1/storages/object/{bucket}/{key}         上传（upsert）
GET    /v1/storages/object/{bucket}/{key}         下载
DELETE /v1/storages/object/{bucket}/{key}         删除
POST   /v1/storages/object/list/{bucket}          列表（hasNext 分页）
Authorization: Bearer <服务端 API Key>
```

桶要手工建，SDK 不会自动建：

```sql
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('backup', 'backup', false, 52428800, NULL);
```

**环境变量（`ascent-backup`）**：`TCB_ENV`、`CLOUDBASE_APIKEY`、`BACKUP_KEY`（64 位 hex）。
`BACKUP_KEY` 丢失 = 备份作废，本机副本在 `~/.ascent/backup.key`，另需存密码管理器。

**恢复**

```bash
set -a; . ~/.ascent/cloudbase.env; set +a
# 只检查，不写库（默认行为）
node server/tools/restore.mjs ~/.ascent/backups/2026-11-03.asc --dry-run
# 演练：灌进影子表，核对完再删
node server/tools/restore.mjs ~/.ascent/backups/2026-11-03.asc --suffix=_drill --yes
# 真恢复：覆盖生产数据
node server/tools/restore.mjs ~/.ascent/backups/2026-11-03.asc --yes
```

**演练结果（2026-11-03）**：影子表六表行数全对，逐字段比对
（邮箱/昵称、密码哈希、state payload+rev、头像 data+version、身份 provider+subject）
全部一致。**改过表结构后必须重跑一次演练**，否则备份可能悄悄不再覆盖新列。

## 运维事项

- **会话模型**：access 1 小时 + refresh 90 天（使用即顺延）。`401` 分两种码：
  `token_expired` 可刷新，`unauthorized` 必须重新登录，客户端只对前者重试一次。
- **邮箱验证开关** `REQUIRE_EMAIL_VERIFICATION` 现为 `false`：发信服务未接，
  未验证账号仍可登录，但 `email_unverified` 分支已在位，打开即生效。
- **日志**只打「方法 / 路径 / 状态 / 耗时 / userId」，不打邮箱、token、payload 全文。
- **限流**按账号算（15 分钟 10 次失败），存在用户表上，不依赖网关。
- **删除账号**只删 `ascent_users` 一行：其余五张表都是 `on delete cascade`，
  由数据库保证一次删干净，不会出现「删了一半」的中间状态。
- 改 `SyncPayload` 的 schema 时，服务端不需要动（它只存不解释 payload），
  但 `SyncPayload.SCHEMA` 要加一，并保证旧客户端能读懂新数据。

## 待补

- **Google 登录客户端**：服务端 `/auth/google` 已验票并带昵称/头像，缺 Credential Manager
  与 OAuth Web client ID。注意 `applicationId` 已改为 **`com.ascentroam.app`**，
  Google Cloud 里登记的包名与签名 SHA-1 要用这个。
