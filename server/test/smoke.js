"use strict";

/**
 * 本地冒烟测试：不起云端，用内存版 PostgREST 接管 fetch，
 * 把 CLOUD-PLAN.md 里的契约逐条打一遍。
 *
 *   node server/test/smoke.js
 *
 * 测的是真实代码路径（URL 拼装、错误码映射、鉴权、事务边界），
 * 只把「数据库」换成了内存表。云端一轮部署 + 翻日志要几分钟，这里不到 1 秒。
 */

const path = require("path");

const FN_DIR = path.join(__dirname, "..", "functions", "ascent-api");
const ENV = "local-test";

// ---------------------------------------------------------------- 内存版 PostgREST

let seq = 0;
const tables = new Map();

function table(name) {
  if (!tables.has(name)) tables.set(name, []);
  return tables.get(name);
}

/** 需要唯一约束的列组合。撞了就按 PostgREST 的行为回 409 + DATABASE_23505。 */
const UNIQUE = {
  ascent_users: [["email"]],
  ascent_identities: [["provider", "subject"]],
  ascent_credentials: [["user_id"]],
  ascent_sessions: [["access_hash"], ["refresh_hash"]],
  ascent_states: [["user_id"]],
  ascent_avatars: [["user_id"]],
};

/** users 删除时级联清掉的子表（模拟 on delete cascade）。 */
const CASCADE = [
  ["ascent_identities", "user_id"],
  ["ascent_credentials", "user_id"],
  ["ascent_sessions", "user_id"],
  ["ascent_states", "user_id"],
  ["ascent_avatars", "user_id"],
];

/**
 * PostgREST 的过滤条件。除了 eq 还要支持 gt —— 验证码的限流窗口
 * （`created_at=gt.<一小时前>`）在真实库里就是这么查的，替身不支持就测不到那条路径。
 */
function parseEq(searchParams) {
  const where = {};
  const gt = {};
  for (const [k, v] of searchParams.entries()) {
    if (k === "select" || k === "limit" || k === "order") continue;
    if (v.startsWith("eq.")) {
      const raw = v.slice(3);
      where[k] = raw === "true" ? true : raw === "false" ? false : raw;
    } else if (v.startsWith("gt.")) {
      gt[k] = v.slice(3);
    }
  }
  return { where, gt };
}

function num(v) {
  const n = Number(v);
  return Number.isNaN(n) ? null : n;
}

function passesGt(row, gt) {
  return Object.entries(gt).every(([k, v]) => {
    const a = num(row[k]);
    const b = num(v);
    if (a === null || b === null) return String(row[k]) > String(v);
    return a > b;
  });
}

/** order=col.asc / col.desc。只支持单列，够用。 */
function applyOrder(rows, order) {
  if (!order) return rows;
  const [col, dir] = order.split(".");
  const out = [...rows].sort((x, y) => {
    const a = num(x[col]);
    const b = num(y[col]);
    const cmp = a !== null && b !== null ? a - b : String(x[col]).localeCompare(String(y[col]));
    return dir === "desc" ? -cmp : cmp;
  });
  return out;
}

function matches(row, where) {
  return Object.entries(where).every(([k, v]) => String(row[k]) === String(v));
}

function uuid() {
  return require("crypto").randomUUID();
}

function handle(method, name, params, body) {
  const rows = table(name);
  const { where, gt } = parseEq(params);
  const limit = params.get("limit") ? Number(params.get("limit")) : Infinity;

  if (method === "GET") {
    // 顺序和 PostgREST 一致：过滤 → 排序 → 截断。弄反了 limit 会截到错的那些行。
    const hit = applyOrder(rows.filter((r) => matches(r, where) && passesGt(r, gt)), params.get("order"));
    return { status: 200, data: hit.slice(0, limit === Infinity ? undefined : limit) };
  }

  if (method === "POST") {
    const doc = { ...body };
    if (!doc.id) doc.id = uuid();
    for (const cols of UNIQUE[name] || []) {
      const clash = rows.some((r) => cols.every((c) => String(r[c]) === String(doc[c])));
      // users.email 是部分唯一索引：空邮箱不算冲突
      if (clash && !(name === "ascent_users" && cols[0] === "email" && !doc.email)) {
        return { status: 409, data: { code: "DATABASE_23505", message: "duplicate key" } };
      }
    }
    rows.push(doc);
    return { status: 201, data: [doc] };
  }

  if (method === "PATCH") {
    const hit = rows.filter((r) => matches(r, where));
    hit.forEach((r) => Object.assign(r, body));
    return { status: 200, data: hit };
  }

  if (method === "DELETE") {
    const removed = rows.filter((r) => matches(r, where));
    tables.set(name, rows.filter((r) => !matches(r, where)));
    if (name === "ascent_users") {
      for (const u of removed) {
        for (const [child, col] of CASCADE) {
          tables.set(child, table(child).filter((r) => String(r[col]) !== String(u.id)));
        }
      }
    }
    return { status: 200, data: removed };
  }

  return { status: 405, data: { message: "method not allowed" } };
}

const realFetch = global.fetch;

/**
 * Google tokeninfo 的替身。测 Google 分支时必须拦住这个域名：
 * 否则测试会真的打到 oauth2.googleapis.com —— 慢、不确定、离线就跑不了。
 */
let googleStub = null;

/** 发出去的邮件。没有真实 Resend，但正文里带着验证码，测试靠它取码。 */
const sentMails = [];

global.fetch = async (url, init) => {
  const raw = String(url);
  if (raw.includes("oauth2.googleapis.com/tokeninfo")) {
    if (!googleStub) {
      return new Response(JSON.stringify({ error: "invalid_token" }), { status: 400 });
    }
    return new Response(JSON.stringify(googleStub), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  }

  if (raw.includes("api.resend.com/emails")) {
    if (!process.env.RESEND_API_KEY) {
      return new Response(JSON.stringify({ message: "missing key" }), { status: 401 });
    }
    sentMails.push(JSON.parse(init.body));
    return new Response(JSON.stringify({ id: "stub-" + sentMails.length }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  }

  const u = new URL(raw);
  const prefix = `/v1/rdb/rest/`;
  const idx = u.pathname.indexOf(prefix);
  if (idx < 0) return realFetch(url, init);

  const name = u.pathname.slice(idx + prefix.length);
  const method = (init && init.method) || "GET";
  let body;
  if (init && init.body) body = JSON.parse(init.body);

  const out = handle(method, name, u.searchParams, body);
  return new Response(JSON.stringify(out.data), {
    status: out.status,
    headers: { "content-type": "application/json" },
  });
};

process.env.TCB_ENV = ENV;
process.env.CLOUDBASE_APIKEY = "test-key";
delete process.env.GOOGLE_CLIENT_ID;
require(path.join(FN_DIR, "index.js"));

// ---------------------------------------------------------------- 断言

const BASE = "http://127.0.0.1:9000";
let pass = 0;
const failures = [];

function check(name, cond, detail) {
  if (cond) {
    pass++;
    console.log("  ok   " + name);
  } else {
    failures.push(name + (detail ? " :: " + detail : ""));
    console.log("  FAIL " + name + (detail ? " :: " + detail : ""));
  }
}

function count(name) {
  return table(name).length;
}

async function call(method, p, { token, body, raw } = {}) {
  const headers = {};
  if (token) headers["authorization"] = "Bearer " + token;
  let payload;
  if (raw) {
    payload = raw;
    headers["content-type"] = "application/octet-stream";
  } else if (body !== undefined) {
    payload = JSON.stringify(body);
    headers["content-type"] = "application/json";
  }
  const res = await realFetch(BASE + p, { method, headers, body: payload });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch (e) {
    json = null;
  }
  return { status: res.status, json, text };
}

const TOKEN_RE = /^[0-9a-f]{64}$/;

async function waitForServer() {
  for (let i = 0; i < 50; i++) {
    try {
      const r = await realFetch(BASE + "/health");
      if (r.ok) return true;
    } catch (e) {
      /* not yet */
    }
    await new Promise((r) => setTimeout(r, 100));
  }
  return false;
}

async function main() {
  if (!(await waitForServer())) {
    console.error("服务未启动");
    process.exit(1);
  }

  console.log("\n[health & 路由]");
  let r = await call("GET", "/health");
  check("GET /health 200", r.status === 200 && r.json && r.json.ok === true);
  r = await call("GET", "/nope");
  check("未知路径 404", r.status === 404 && r.json.error === "not_found");

  console.log("\n[注册]");
  r = await call("POST", "/auth/register", {
    body: { email: "Alex@Example.com ", password: "climb8848", nickname: "Alex" },
  });
  check("注册 201", r.status === 201, JSON.stringify(r.json));
  const access1 = r.json && r.json.access_token;
  const refresh1 = r.json && r.json.refresh_token;
  check("返回 access（64 位 hex）", TOKEN_RE.test(access1 || ""));
  check("返回 refresh（64 位 hex）", TOKEN_RE.test(refresh1 || ""));
  check("两个 token 不相同", access1 !== refresh1);
  check("expires_in = 3600", r.json.expires_in === 3600);
  check("is_new_user = true", r.json.is_new_user === true);
  check("邮箱归一化为小写", r.json.user.email === "alex@example.com");
  check("新账号 emailVerified = false", r.json.user.emailVerified === false);
  check("未回显密码字段", r.json.user.passwordHash === undefined && r.json.user.password_hash === undefined);
  check("建了 password 身份", count("ascent_identities") === 1);
  check("建了凭证", count("ascent_credentials") === 1);
  check("建了会话", count("ascent_sessions") === 1);
  check("库里只存 token 哈希", !JSON.stringify(table("ascent_sessions")).includes(access1));

  r = await call("POST", "/auth/register", { body: { email: "alex@example.com", password: "climb8848", nickname: "Alex2" } });
  check("重复邮箱 409", r.status === 409 && r.json.error === "email_taken");
  r = await call("POST", "/auth/register", { body: { email: "bad", password: "climb8848", nickname: "x" } });
  check("邮箱格式 400", r.status === 400 && r.json.error === "invalid_email");
  r = await call("POST", "/auth/register", { body: { email: "b@example.com", password: "short", nickname: "x" } });
  check("弱密码 400", r.status === 400 && r.json.error === "weak_password");
  r = await call("POST", "/auth/register", { body: { email: "c@example.com", password: "climb8848", nickname: "12345678901234567" } });
  check("昵称超长 400", r.status === 400 && r.json.error === "invalid_nickname");

  console.log("\n[登录]");
  r = await call("POST", "/auth/login", { body: { email: "alex@example.com", password: "wrongpass" } });
  check("错密码 401", r.status === 401 && r.json.error === "invalid_credentials");
  r = await call("POST", "/auth/login", { body: { email: "nobody@example.com", password: "climb8848" } });
  check("不存在账号同样 401", r.status === 401 && r.json.error === "invalid_credentials");
  r = await call("POST", "/auth/login", { body: { email: "alex@example.com", password: "climb8848" } });
  check("正确密码 200", r.status === 200 && TOKEN_RE.test(r.json.access_token || ""));
  check("is_new_user = false", r.json.is_new_user === false);
  const access2 = r.json.access_token;
  check("第二次登录是新会话", access2 !== access1);

  console.log("\n[鉴权]");
  r = await call("GET", "/auth/me");
  check("无 token 401", r.status === 401 && r.json.error === "unauthorized");
  r = await call("GET", "/auth/me", { token: "f".repeat(64) });
  check("伪造 token 401", r.status === 401);
  r = await call("GET", "/auth/me", { token: access1 });
  check("带 access 拿到自己", r.status === 200 && r.json.user.email === "alex@example.com");

  console.log("\n[刷新会话]");
  r = await call("POST", "/auth/refresh", { body: { refresh_token: refresh1 } });
  check("refresh 换到新 access", r.status === 200 && TOKEN_RE.test(r.json.access_token || ""), JSON.stringify(r.json));
  const access3 = r.json.access_token;
  check("换出来的是新 token", access3 !== access1);
  r = await call("GET", "/auth/me", { token: access3 });
  check("新 access 可用", r.status === 200);
  r = await call("POST", "/auth/refresh", { body: { refresh_token: "e".repeat(64) } });
  check("伪造 refresh 401", r.status === 401 && r.json.error === "invalid_token");
  r = await call("POST", "/auth/refresh", { body: { refresh_token: access1 } });
  check("拿 access 当 refresh 被拒", r.status === 401);

  console.log("\n[设备管理]");
  r = await call("GET", "/me/devices", { token: access3 });
  check("能列出设备", r.status === 200 && Array.isArray(r.json.devices));
  check("两台设备", r.json.devices.length === 2, "n=" + (r.json.devices && r.json.devices.length));
  check("其中一台标记为 current", r.json.devices.filter((d) => d.current).length === 1);
  const other = r.json.devices.find((d) => !d.current);
  r = await call("DELETE", "/me/devices/" + other.id, { token: access3 });
  check("撤销另一台设备", r.status === 200 && r.json.revoked === true && r.json.self === false);
  r = await call("GET", "/auth/me", { token: access2 });
  check("被撤销的设备立刻失效", r.status === 401);
  r = await call("GET", "/me/devices", { token: access3 });
  check("设备列表只剩一台", r.json.devices.length === 1);

  console.log("\n[进度]");
  r = await call("GET", "/state", { token: access3 });
  check("新账号无记录 rev=0 payload=null", r.status === 200 && r.json.rev === 0 && r.json.payload === null);
  const p1 = { schema: 1, completed: ["m5i-l01"], stars: { "m5i-l01": 3 }, reviews: [], achievements: ["first_step"], counters: { lessonRuns: 1, reviewDone: 0 }, days: ["2026-11-03"], streak: { current: 1, max: 1, broken: false, lastDay: "2026-11-03" }, drills: [] };
  r = await call("PUT", "/state", { token: access3, body: { rev: 0, payload: p1 } });
  check("首次写入 200 rev=1", r.status === 200 && r.json.rev === 1);
  r = await call("GET", "/state", { token: access3 });
  check("读回内容一致", r.json.rev === 1 && r.json.payload.completed[0] === "m5i-l01");
  check("jsonb 原样往返", r.json.payload.stars["m5i-l01"] === 3);
  r = await call("PUT", "/state", { token: access3, body: { rev: 0, payload: p1 } });
  check("拿旧 rev 写入 409", r.status === 409 && r.json.rev === 1 && r.json.payload);
  r = await call("PUT", "/state", { token: access3, body: { rev: 1, payload: { ...p1, completed: ["m5i-l01", "m5i-l02"] } } });
  check("rev 对齐后写入 rev=2", r.status === 200 && r.json.rev === 2);
  r = await call("PUT", "/state", { token: access3, body: { rev: 2, payload: { schema: 1, blob: "x".repeat(600 * 1024) } } });
  check("超 512 KB 413", r.status === 413 && r.json.error === "payload_too_large");

  console.log("\n[资料]");
  r = await call("PATCH", "/auth/profile", { token: access3, body: { nickname: "Alex on ice" } });
  check("改昵称 200", r.status === 200 && r.json.user.nickname === "Alex on ice");
  r = await call("PATCH", "/auth/profile", { token: access3, body: { nickname: "" } });
  check("空昵称 400", r.status === 400 && r.json.error === "invalid_nickname");

  console.log("\n[头像]");
  const jpeg = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(1024, 7), Buffer.from([0xff, 0xd9])]);
  r = await call("PUT", "/avatar", { token: access3, raw: jpeg });
  check("上传 JPEG 200 version=1", r.status === 200 && r.json.version === 1);
  r = await call("GET", "/avatar", { token: access3 });
  check("取回头像 200", r.status === 200 && r.text.length > 0);
  r = await call("PUT", "/avatar", { token: access3, raw: Buffer.from("not a jpeg at all") });
  check("非 JPEG 415", r.status === 415 && r.json.error === "unsupported_media");
  r = await call("DELETE", "/avatar", { token: access3 });
  check("删除头像 204", r.status === 204);

  console.log("\n[路径前缀兼容]");
  r = await call("GET", "/ascent/auth/me", { token: access3 });
  check("/ascent 前缀也能路由", r.status === 200);

  console.log("\n[退出登录]");
  r = await call("POST", "/auth/logout", { token: access3 });
  check("登出 204", r.status === 204);
  r = await call("GET", "/auth/me", { token: access3 });
  check("登出后 access 失效", r.status === 401);
  r = await call("POST", "/auth/refresh", { body: { refresh_token: refresh1 } });
  check("登出后 refresh 也失效", r.status === 401);

  console.log("\n[删除账号级联]");
  r = await call("POST", "/auth/login", { body: { email: "alex@example.com", password: "climb8848" } });
  const access4 = r.json && r.json.access_token;
  check("重新登录成功", r.status === 200);
  r = await call("DELETE", "/auth/account", { token: access4 });
  check("删除 204", r.status === 204);
  r = await call("GET", "/auth/me", { token: access4 });
  check("旧 token 失效 401", r.status === 401);
  r = await call("POST", "/auth/login", { body: { email: "alex@example.com", password: "climb8848" } });
  check("账号已不存在 401", r.status === 401);

  for (const t of ["ascent_users", "ascent_identities", "ascent_credentials", "ascent_sessions", "ascent_states", "ascent_avatars"]) {
    check(t + " 归零（由 users 级联）", count(t) === 0, "left=" + count(t));
  }

  console.log("\n[Google 未配置]");
  r = await call("POST", "/auth/google", { body: { idToken: "x" } });
  check("未配置 501", r.status === 501 && r.json.error === "google_not_configured");
  r = await call("POST", "/auth/identities/google", { body: { idToken: "x" } });
  check("绑定接口先鉴权（未带 token 401）", r.status === 401);

  // ---- 配置好之后的分支。GOOGLE_CLIENT_ID 是运行时读取的，可以就地切换。
  process.env.GOOGLE_CLIENT_ID = "test-client-id";

  console.log("\n[Google 注册/登录]");
  googleStub = {
    aud: "test-client-id",
    sub: "google-sub-1001",
    email: "alex@gmail.com",
    email_verified: "true",
    name: "Alex Ridge",
    given_name: "Alex",
    picture: "https://lh3.googleusercontent.com/a/alex=s96-c",
  };

  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("首次即建号", r.status === 200 && r.json.is_new_user === true, "status=" + r.status);
  check("昵称取自 Google name", r.json.user.nickname === "Alex Ridge", r.json.user.nickname);
  check("带回 google_picture", r.json.google_picture === googleStub.picture);
  check("邮箱已验证（无需验证码）", r.json.user.email === "alex@gmail.com");
  const gToken = r.json.access_token;

  r = await call("GET", "/auth/me", { token: gToken });
  check("Google 账号可直接用 access", r.status === 200 && r.json.user.nickname === "Alex Ridge");

  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("再次登录不是新用户", r.status === 200 && r.json.is_new_user === false);

  // 关键回归：tokeninfo 的字段都是字符串，"false" 是真值。
  // 写成 !!info.email_verified 会把未验证的邮箱判成已验证。
  googleStub = { ...googleStub, sub: "google-sub-1002", email: "unverified@gmail.com", email_verified: "false" };
  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("email_verified=false 必须拒绝", r.status === 403 && r.json.error === "email_unverified", "status=" + r.status);

  // aud 不匹配 = 别人家的 client id 签的票，不能认
  googleStub = { ...googleStub, sub: "google-sub-1003", email: "other@gmail.com", aud: "someone-else", email_verified: "true" };
  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("aud 不匹配拒绝", r.status === 401 && r.json.error === "invalid_google_token");

  // 同邮箱已有密码账号 → 不许并号
  await call("POST", "/auth/register", { body: { email: "taken@example.com", password: "climb8848", nickname: "Taken" } });
  googleStub = { ...googleStub, sub: "google-sub-1004", email: "taken@example.com", aud: "test-client-id", email_verified: "true" };
  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("同邮箱不并号（409）", r.status === 409 && r.json.error === "identity_conflict", "status=" + r.status);

  // name 缺失时退回 given_name
  googleStub = { ...googleStub, sub: "google-sub-1005", email: "noname@gmail.com", name: "", given_name: "Mira" };
  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("无 name 时退回 given_name", r.status === 200 && r.json.user.nickname === "Mira", r.json.user && r.json.user.nickname);

  googleStub = null;
  delete process.env.GOOGLE_CLIENT_ID;

  // ---------------------------------------------------------------- 邮件验证码
  console.log("\n[邮件未配置]");
  r = await call("POST", "/auth/email/code", { body: { email: "a@b.com" } });
  check("未配 Key 时 503", r.status === 503 && r.json.error === "mail_not_configured", "status=" + r.status);

  process.env.RESEND_API_KEY = "re_test";
  process.env.CODE_PEPPER = "pepper-for-tests";
  process.env.REQUIRE_EMAIL_CODE = "true";

  const codeOf = (mail) => {
    const m = /\n {4}(\d{6})\n/.exec(mail.text);
    return m ? m[1] : null;
  };

  console.log("\n[注册验证码]");
  r = await call("POST", "/auth/email/code", { body: { email: "mailtest@example.com", purpose: "register" } });
  check("发码成功", r.status === 200 && r.json.ok === true, "status=" + r.status);
  check("确实发出了一封", sentMails.length === 1, "count=" + sentMails.length);
  check("发件人是域名邮箱", /@ascentroam\.com>?$/.test(sentMails[0].from), sentMails[0].from);

  const regCode = codeOf(sentMails[0]);
  check("正文含 6 位码", /^\d{6}$/.test(String(regCode)), String(regCode));

  r = await call("POST", "/auth/email/code", { body: { email: "mailtest@example.com", purpose: "register" } });
  check("1 分钟内重发被拦（429）", r.status === 429 && r.json.error === "too_many_requests", "status=" + r.status);

  const wrongCode = regCode === "000000" ? "111111" : "000000";
  r = await call("POST", "/auth/register", {
    body: { email: "mailtest@example.com", password: "climb8848", nickname: "Mailer", code: wrongCode },
  });
  check("错码注册被拒（400）", r.status === 400 && r.json.error === "invalid_code", "status=" + r.status);

  r = await call("POST", "/auth/register", {
    body: { email: "mailtest@example.com", password: "climb8848", nickname: "Mailer", code: regCode },
  });
  check("正确码注册成功（201）", r.status === 201 && r.json.is_new_user === true, "status=" + r.status);
  check("注册后邮箱标记已验证", r.json.user.emailVerified === true, String(r.json.user.emailVerified));
  const mailToken = r.json.access_token;

  r = await call("POST", "/auth/register", {
    body: { email: "mailtest@example.com", password: "climb8848", nickname: "Mailer", code: regCode },
  });
  check("同一封码不能再用", r.status === 409 || r.status === 400, "status=" + r.status);

  console.log("\n[找回密码]");
  r = await call("POST", "/auth/password/forgot", { body: { email: "mailtest@example.com" } });
  check("发重置码成功", r.status === 200, "status=" + r.status);
  const resetCode = codeOf(sentMails[sentMails.length - 1]);
  check("重置邮件正文含码", /^\d{6}$/.test(String(resetCode)));

  r = await call("POST", "/auth/password/forgot", { body: { email: "nobody@example.com" } });
  check("不存在的邮箱也回 200（防枚举）", r.status === 200, "status=" + r.status);

  r = await call("POST", "/auth/password/reset", {
    body: { email: "mailtest@example.com", code: resetCode, newPassword: "newpass2026" },
  });
  check("重置成功", r.status === 200, "status=" + r.status);

  r = await call("GET", "/auth/me", { token: mailToken });
  check("改密后旧会话立即失效", r.status === 401, "status=" + r.status);

  r = await call("POST", "/auth/login", { body: { email: "mailtest@example.com", password: "climb8848" } });
  check("旧密码登不上", r.status === 401, "status=" + r.status);

  r = await call("POST", "/auth/login", { body: { email: "mailtest@example.com", password: "newpass2026" } });
  check("新密码能登录", r.status === 200, "status=" + r.status);

  console.log("\n[Google 账号首次设密码]");
  process.env.GOOGLE_CLIENT_ID = "test-client-id";
  googleStub = {
    aud: "test-client-id",
    sub: "google-sub-2001",
    email: "gonly@gmail.com",
    email_verified: "true",
    name: "Gina Only",
  };
  r = await call("POST", "/auth/google", { body: { idToken: "stub" } });
  check("建 Google-only 账号", r.status === 200);

  r = await call("POST", "/auth/password/forgot", { body: { email: "gonly@gmail.com" } });
  check("Google 账号也能收重置码", r.status === 200);
  const gCode = codeOf(sentMails[sentMails.length - 1]);

  r = await call("POST", "/auth/password/reset", {
    body: { email: "gonly@gmail.com", code: gCode, newPassword: "climb8848" },
  });
  check("首次设密码成功", r.status === 200, "status=" + r.status);

  // 这条是关键回归：Google-only 账号没有 credentials 行，
  // 写成 update 会静默影响 0 行 —— 用户以为设好了，其实登不上。
  r = await call("POST", "/auth/login", { body: { email: "gonly@gmail.com", password: "climb8848" } });
  check("设完密码能登录（insert 而非 update）", r.status === 200, "status=" + r.status);

  r = await call("POST", "/auth/login", { body: { email: "gonly@gmail.com", password: "wrongpass" } });
  check("错误密码仍被拒", r.status === 401);

  googleStub = null;
  delete process.env.GOOGLE_CLIENT_ID;

  console.log("\n结果：" + pass + " 通过，" + failures.length + " 失败");
  if (failures.length) {
    failures.forEach((f) => console.log("  - " + f));
    process.exit(1);
  }
  process.exit(0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
