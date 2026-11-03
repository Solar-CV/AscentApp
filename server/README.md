# ASCENT 云端服务端

对应契约见仓库根目录 `CLOUD-PLAN.md`。这里只写「怎么跑、怎么部署、有哪些坑」。

```
server/
├── functions/ascent-api/     HTTP 云函数（Node.js 18，监听 :9000）
│   ├── index.js              全部路由与业务逻辑
│   ├── scf_bootstrap         启动脚本，路径必须与运行时版本一致
│   └── package.json          唯一依赖 @cloudbase/node-sdk
└── test/smoke.js             本地冒烟（内存替身，不连云）
```

## 本地冒烟

```bash
node server/test/smoke.js
```

用 `require.cache` 把 `@cloudbase/node-sdk` 换成内存实现，覆盖注册、登录、鉴权、
`409` 冲突、`413` 超限、头像、级联删号、路径前缀兼容等 42 条断言。
改了 `index.js` 先跑这个，再上云 —— 云端一轮部署 + 调日志要几分钟，本机不到 1 秒。

## 部署清单

1. **函数配置**
   - 运行时 `Nodejs18.15`（`scf_bootstrap` 里写死 `/var/lang/node18/bin/node`，改运行时必须同步改）。
   - **内存 512 MB**：scrypt 用 N=2^17，单次哈希约需 128 MB，留 256 MB 会 OOM。
   - 超时 ≥ 20 秒；类型选 **HTTP 函数**，不是事件函数。
2. **环境变量**
   | 名称 | 说明 |
   |---|---|
   | `TCB_ENV` | 环境 ID（HTTP 函数不会自动注入，必须显式配） |
   | `CLOUDBASE_APIKEY` | 服务端 API Key，`@cloudbase/node-sdk` 用它访问数据库 |
   | `GOOGLE_CLIENT_ID` | 可选。不填时 `/auth/google` 返回 501，其余功能不受影响 |
3. **数据库集合**（必须手动建，代码不会自动建集合）
   | 集合 | 索引 |
   |---|---|
   | `ascent_users` | `email` 唯一 |
   | `ascent_tokens` | `tokenHash` 唯一、`userId` 普通 |
   | `ascent_states` | `userId` 唯一 |
   | `ascent_avatars` | `userId` 唯一 |
4. **网关**：`manageGateway(action="createRoute", upstreamResourceType="WEB_SCF", auth=false)`。
   公网访问必须走网关（`auth:false`），不要去放宽函数的安全规则。
5. **CORS**：函数里刻意不设 `Access-Control-Allow-Origin` —— 网关会按环境安全域名填，
   函数再填一遍会变成 `<origin>,*`，浏览器直接拒绝。原生 App 不受影响。

## 运维事项

- **日志**只打「方法 / 路径 / 状态 / 耗时 / userId」，不打邮箱、token、payload 全文。
- **限流**按账号算（15 分钟 10 次失败），存在用户文档上，不依赖网关。
- **删除账号**会级联删 tokens / states / avatars，再删用户；顺序不能反，否则中途失败会留下孤立数据。
- 改 `SyncPayload` 的 schema 时，服务端不需要动（它只存不解释 payload），
  但 `SyncPayload.SCHEMA` 要加一，并保证旧客户端能读懂新数据。
