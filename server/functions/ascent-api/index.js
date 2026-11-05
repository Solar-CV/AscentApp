"use strict";

/**
 * ASCENT 云端 API（CloudBase HTTP 云函数）
 *
 * 契约见仓库根目录 CLOUD-PLAN.md，改动接口时两边一起改。
 *
 * 设计约束：
 *  1. **零依赖**：只用 Node 内置模块。数据访问直接走 CloudBase PG 的 REST 网关
 *     （`/v1/rdb/rest/<table>`，PostgREST 语义），拿服务端 API Key 当 service_role，
 *     因此不需要 @cloudbase/node-sdk，部署包里没有 node_modules。
 *  2. 账号自建（scrypt + 不透明 token + refresh 顺延），不绑平台身份认证 ——
 *     将来搬 VPS 时数据可迁。
 *  3. 一个用户 = 一个 user_id，可有多个登录身份（ascent_identities）。
 *     **第三方登录只信服务端验过的 sub，不按邮箱并号**（CLOUD-PLAN.md §10.1 A2）。
 *  4. CORS 由网关负责，这里**不设** Access-Control-Allow-Origin：
 *     网关会按环境安全域名填，函数再填一遍会变成 `<origin>,*`，浏览器直接拒绝。
 *  5. 任何响应都不得回显 req.headers / process.env，也不得把邮箱、token 写进日志。
 */

const http = require("http");
const crypto = require("crypto");

// ---------------------------------------------------------------- 配置

const PORT = 9000;
const ENV_ID = process.env.TCB_ENV || process.env.CLOUDBASE_ENV_ID || "";
const API_KEY = process.env.CLOUDBASE_APIKEY || "";
/**
 * 在**调用时**读环境变量，而不是模块加载时定死。
 * 定死会让测试无法切换「已配置 / 未配置」两种状态，也让「改完环境变量要重新部署才生效」
 * 这种坑变得不可见。
 */
const googleClientId = () => process.env.GOOGLE_CLIENT_ID || "";

// ---------------------------------------------------------------- 发信与验证码

/**
 * 邮件通道：Resend。换供应商只改 sendMail 一处。
 * 没有 RESEND_API_KEY 时整个验证码链路返回 503，注册退回旧流程（见 REQUIRE_EMAIL_CODE）。
 */
const resendKey = () => (process.env.RESEND_API_KEY || "").trim();
const mailFrom = () => process.env.MAIL_FROM || "AscentRoam <no-reply@ascentroam.com>";
const mailConfigured = () => !!resendKey();

/**
 * 验证码是否强制。**开关而不是直接改流程**，因为发信还没接通时
 * 强制验证码会让「现在还能注册」这件事直接坏掉。接通后翻成 true 即可。
 */
const codeRequired = () => process.env.REQUIRE_EMAIL_CODE === "true";

/**
 * 验证码哈希用的 pepper。6 位数字只有 100 万种，不带 pepper 的 sha256
 * 在本地跑一遍就能全表反查 —— 这个 env 是防「库泄露」的关键，不能省。
 */
const codePepper = () => process.env.CODE_PEPPER || "";

const CODE_TTL_MS = 10 * 60 * 1000; // 10 分钟
const CODE_MAX_ATTEMPTS = 5;
const CODE_GAP_MS = 60 * 1000; // 同邮箱同用途 1 分钟 1 次
const CODE_HOURLY_MAX = 5; // 同邮箱同用途 1 小时 5 次
const T_CODES = "ascent_email_codes";

/**
 * 邮箱验证开关。发信服务（B2）落地前保持 false：
 * 此时未验证的账号仍可登录，但 `email_unverified` 分支已经在位，
 * 打开开关就能生效，不用再改代码。
 */
const REQUIRE_EMAIL_VERIFICATION = false;

const MAX_JSON_BYTES = 1024 * 1024; // 1 MB
const MAX_AVATAR_BYTES = 512 * 1024; // 512 KB
const MAX_PAYLOAD_BYTES = 512 * 1024; // 512 KB

const ACCESS_TTL_MS = 60 * 60 * 1000; // access 1 小时
const REFRESH_TTL_MS = 90 * 24 * 60 * 60 * 1000; // refresh 90 天，使用即顺延
const LOGIN_WINDOW_MS = 15 * 60 * 1000;
const LOGIN_MAX_FAILS = 10;
const PASSWORD_MIN = 8;
const NICKNAME_MAX = 16;
const PG_TIMEOUT_MS = 8000;

const T_USERS = "ascent_users";
const T_IDENTITIES = "ascent_identities";
const T_CREDENTIALS = "ascent_credentials";
const T_SESSIONS = "ascent_sessions";
const T_STATES = "ascent_states";
const T_AVATARS = "ascent_avatars";

/**
 * scrypt 参数。N=2^17 是 OWASP 现行建议的基线，内存占用约 128 MB，
 * 因此**云函数内存必须配 512 MB**（见 server/README.md）；留在 256 MB 会 OOM。
 * 参数写进哈希串里，以后调整不影响老密码的校验。
 */
const SCRYPT = { N: 1 << 17, r: 8, p: 1, maxmem: 256 * 1024 * 1024 };

// ---------------------------------------------------------------- 数据访问

const PG_BASE = () => `https://${ENV_ID}.api.tcloudbasegateway.com/v1/rdb/rest`;

class PgError extends Error {
  constructor(status, code, message) {
    super(message || code || "pg_error");
    this.status = status;
    this.code = code;
  }

  /** 唯一约束冲突。注册撞邮箱、重复绑同一身份都会走到这里。 */
  get duplicate() {
    return this.code === "DATABASE_23505" || this.status === 409;
  }
}

function pgPath(table, params) {
  const q = Object.entries(params || {})
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join("&");
  return `/${table}${q ? "?" + q : ""}`;
}

/** 把 { id: "x", revoked: false } 变成 PostgREST 的 eq 过滤。false 不能被当成空值跳过。 */
function eq(where) {
  const out = {};
  for (const [k, v] of Object.entries(where || {})) {
    if (v === null || v === undefined) continue;
    out[k] = `eq.${v}`;
  }
  return out;
}

async function pg(method, path, body) {
  if (!ENV_ID || !API_KEY) throw new PgError(500, "pg_not_configured", "缺少 TCB_ENV / CLOUDBASE_APIKEY");
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), PG_TIMEOUT_MS);
  let res;
  try {
    res = await fetch(PG_BASE() + path, {
      method,
      headers: {
        "content-type": "application/json",
        authorization: `Bearer ${API_KEY}`,
        prefer: "return=representation",
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
  } catch (e) {
    throw new PgError(503, "pg_unreachable", "数据库连接失败");
  } finally {
    clearTimeout(timer);
  }

  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch (e) {
    data = null;
  }
  if (!res.ok) {
    throw new PgError(res.status, data && data.code, data && data.message);
  }
  return data;
}

async function findOne(table, where) {
  const rows = await pg("GET", pgPath(table, { select: "*", ...eq(where), limit: 1 }));
  return Array.isArray(rows) && rows.length ? rows[0] : null;
}

async function selectAll(table, where) {
  const rows = await pg("GET", pgPath(table, { select: "*", ...eq(where) }));
  return Array.isArray(rows) ? rows : [];
}

async function insert(table, row) {
  const rows = await pg("POST", pgPath(table, {}), row);
  return Array.isArray(rows) && rows.length ? rows[0] : null;
}

async function update(table, where, patch) {
  return pg("PATCH", pgPath(table, eq(where)), patch);
}

async function remove(table, where) {
  return pg("DELETE", pgPath(table, eq(where)));
}

// ---------------------------------------------------------------- HTTP 基础

function send(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
  });
  res.end(body === undefined ? "" : JSON.stringify(body));
}

function fail(res, status, code, message) {
  send(res, status, { error: code, message: message || code });
}

function sendBytes(res, status, buffer, contentType) {
  res.writeHead(status, {
    "Content-Type": contentType,
    "Cache-Control": "no-store",
    "Content-Length": buffer.length,
  });
  res.end(buffer);
}

function readBody(req, limit) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on("data", (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(Object.assign(new Error("too_large"), { code: "too_large" }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

async function readJson(req, limit) {
  const buf = await readBody(req, limit || MAX_JSON_BYTES);
  if (!buf.length) return {};
  try {
    const v = JSON.parse(buf.toString("utf8"));
    return v && typeof v === "object" ? v : {};
  } catch (e) {
    throw Object.assign(new Error("bad_json"), { code: "bad_json" });
  }
}

// ---------------------------------------------------------------- 工具

const now = () => Date.now();

function sha256(s) {
  return crypto.createHash("sha256").update(s).digest("hex");
}

function randomToken() {
  return crypto.randomBytes(32).toString("hex");
}

function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const hash = crypto.scryptSync(password, salt, 64, SCRYPT);
  return `scrypt$${SCRYPT.N}$${SCRYPT.r}$${SCRYPT.p}$${salt.toString("hex")}$${hash.toString("hex")}`;
}

function verifyPassword(password, stored) {
  if (typeof stored !== "string") return false;
  const parts = stored.split("$");
  if (parts.length !== 6 || parts[0] !== "scrypt") return false;
  const N = Number(parts[1]);
  const r = Number(parts[2]);
  const p = Number(parts[3]);
  const salt = Buffer.from(parts[4], "hex");
  const expected = Buffer.from(parts[5], "hex");
  let actual;
  try {
    actual = crypto.scryptSync(password, salt, expected.length, { N, r, p, maxmem: SCRYPT.maxmem });
  } catch (e) {
    return false;
  }
  return actual.length === expected.length && crypto.timingSafeEqual(actual, expected);
}

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

function normalizeEmail(v) {
  return typeof v === "string" ? v.trim().toLowerCase() : "";
}

function str(v, max) {
  return typeof v === "string" ? v.trim().slice(0, max) : "";
}

/** 数据库是 snake_case，对外一律 camelCase。转换只在这一处发生。 */
function publicUser(row) {
  return {
    id: row.id,
    email: row.email || "",
    nickname: row.nickname || "",
    emailVerified: !!row.email_verified,
    avatarVersion: row.avatar_version || 0,
    createdAt: Number(row.created_at) || 0,
  };
}

// ---------------------------------------------------------------- 会话（access + refresh）

/**
 * 一个会话 = 一台设备。两种 token：
 *  - access  1 小时，随请求带上，频繁轮换；
 *  - refresh 90 天，只在换 access 时用，每次使用顺延，可在「设备管理」里单独撤销。
 * 库里只存两者的 sha256，拿到数据库不等于拿到会话。
 */
async function createSession(userId, meta) {
  const access = randomToken();
  const refresh = randomToken();
  const t = now();
  const row = await insert(T_SESSIONS, {
    user_id: userId,
    access_hash: sha256(access),
    access_expires_at: t + ACCESS_TTL_MS,
    refresh_hash: sha256(refresh),
    refresh_expires_at: t + REFRESH_TTL_MS,
    device_id: str(meta.deviceId, 64),
    platform: str(meta.platform, 20) || "android",
    app_version: str(meta.appVersion, 24),
    created_at: t,
    last_used_at: t,
    revoked: false,
  });
  return { sessionId: row && row.id, access, refresh };
}

function authMeta(body) {
  const d = (body && body.device) || {};
  return {
    deviceId: d.device_id || (body && body.deviceId) || "",
    platform: d.platform || "",
    appVersion: d.app_version || (body && body.appVersion) || "",
  };
}

function sessionPayload(user, tokens, isNewUser) {
  return {
    user: publicUser(user),
    access_token: tokens.access,
    refresh_token: tokens.refresh,
    expires_in: Math.floor(ACCESS_TTL_MS / 1000),
    is_new_user: !!isNewUser,
  };
}

function bearer(req) {
  const raw = req.headers["authorization"] || "";
  const m = /^Bearer\s+([0-9a-f]{64})$/i.exec(String(raw).trim());
  return m ? m[1] : null;
}

/** 解析 access。返回 { user, session } 或 { error }。区分「过期」便于客户端只刷新一次。 */
async function resolveAccess(req) {
  const token = bearer(req);
  if (!token) return { error: "unauthorized" };
  const session = await findOne(T_SESSIONS, { access_hash: sha256(token), revoked: false });
  if (!session) return { error: "unauthorized" };
  if (Number(session.access_expires_at) <= now()) return { error: "token_expired" };
  const user = await findOne(T_USERS, { id: session.user_id });
  if (!user) return { error: "unauthorized" };
  return { user, session };
}

async function touchSession(session) {
  if (now() - Number(session.last_used_at || 0) > 60 * 60 * 1000) {
    // 一小时才写一次，别把每个请求都变成一次 UPDATE
    update(T_SESSIONS, { id: session.id }, { last_used_at: now() }).catch(() => {});
  }
}

async function loginGuard(user) {
  const fails = Number(user.login_fails || 0);
  const since = Number(user.login_fail_window || 0);
  if (fails >= LOGIN_MAX_FAILS && now() - since < LOGIN_WINDOW_MS) {
    return Math.ceil((LOGIN_WINDOW_MS - (now() - since)) / 1000);
  }
  return 0;
}

async function noteLoginFailure(user) {
  const since = Number(user.login_fail_window || 0);
  const expired = now() - since > LOGIN_WINDOW_MS;
  await update(T_USERS, { id: user.id }, {
    login_fails: expired ? 1 : Number(user.login_fails || 0) + 1,
    login_fail_window: expired ? now() : since,
  });
}

// ---------------------------------------------------------------- 账号：注册 / 登录

// ---------------------------------------------------------------- 验证码

function hashCode(email, purpose, code) {
  const pepper = codePepper();
  if (!pepper) throw new Error("CODE_PEPPER 未配置");
  return crypto.createHmac("sha256", pepper).update(`${email}|${purpose}|${code}`).digest("hex");
}

function safeEqualHex(a, b) {
  // 长度不同直接 false，否则 timingSafeEqual 会抛
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  try {
    return crypto.timingSafeEqual(Buffer.from(a, "hex"), Buffer.from(b, "hex"));
  } catch (e) {
    return false;
  }
}

/** 取某邮箱某用途的最新一条码（不管是否已用）。 */
async function latestCode(email, purpose) {
  const rows = await pg(
    "GET",
    pgPath(T_CODES, {
      select: "*",
      email: `eq.${email}`,
      purpose: `eq.${purpose}`,
      order: "created_at.desc",
      limit: 1,
    })
  );
  return Array.isArray(rows) && rows.length ? rows[0] : null;
}

/** 邮件正文：中英双语，正文短，不必做十语言模板。 */
function codeMail(purpose, code) {
  if (purpose === "reset") {
    return {
      subject: `AscentRoam 密码重置验证码 ${code}`,
      text: [
        "你的 AscentRoam 密码重置验证码是：",
        "",
        `    ${code}`,
        "",
        "10 分钟内有效。如果不是你本人操作，请忽略这封邮件，你的密码不会被更改。",
        "",
        "---",
        "",
        "Your AscentRoam password reset code is:",
        "",
        `    ${code}`,
        "",
        "Valid for 10 minutes. If you didn't request this, ignore this email — your password stays unchanged.",
      ].join("\n"),
    };
  }
  return {
    subject: `AscentRoam 注册验证码 ${code}`,
    text: [
      "你的 AscentRoam 注册验证码是：",
      "",
      `    ${code}`,
      "",
      "10 分钟内有效。验证完成后这个邮箱就能用来登录了。",
      "",
      "---",
      "",
      "Your AscentRoam sign-up code is:",
      "",
      `    ${code}`,
      "",
      "Valid for 10 minutes.",
    ].join("\n"),
  };
}

async function sendMail({ to, subject, text }) {
  const key = resendKey();
  if (!key) throw new Error("RESEND_API_KEY 未配置");
  const r = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { authorization: `Bearer ${key}`, "content-type": "application/json" },
    body: JSON.stringify({ from: mailFrom(), to: [to], subject, text }),
    signal: AbortSignal.timeout(15000),
  });
  if (!r.ok) {
    const t = await r.text();
    throw new Error(`Resend ${r.status}: ${t.slice(0, 200)}`);
  }
}

/**
 * 校验并消费验证码。
 * 失败一律回 invalid_code —— 不区分「码错」「过期」「没发过」，
 * 少给一条情报，就少一次被拿来探测的机会。
 */
async function consumeCode(email, purpose, raw) {
  const code = String(raw || "").trim();
  if (!/^\d{6}$/.test(code)) return { ok: false, error: "invalid_code" };

  const row = await latestCode(email, purpose);
  if (!row || row.consumed_at) return { ok: false, error: "invalid_code" };
  if (Number(row.expires_at) <= now()) return { ok: false, error: "invalid_code" };
  if (Number(row.attempts) >= CODE_MAX_ATTEMPTS) return { ok: false, error: "too_many_attempts" };

  if (!safeEqualHex(hashCode(email, purpose, code), row.code_hash)) {
    // 次数由服务端扣，客户端说「我没试过」不算数
    await update(T_CODES, { id: row.id }, { attempts: Number(row.attempts) + 1 });
    return { ok: false, error: "invalid_code" };
  }

  await update(T_CODES, { id: row.id }, { consumed_at: now() });
  return { ok: true };
}

// ---------------------------------------------------------------- 注册

/**
 * 注册。`REQUIRE_EMAIL_CODE=true` 时要求带 `code`（先调 /auth/email/code 发码）。
 * 关掉开关时退回旧流程：不验证、email_verified 记 false —— 这样发信还没接通时
 * 注册不会整条坏掉，接通后翻开关即可。
 */
async function handleRegister(req, res) {
  const body = await readJson(req);
  const email = normalizeEmail(body.email);
  const password = typeof body.password === "string" ? body.password : "";
  const nickname = typeof body.nickname === "string" ? body.nickname.trim() : "";

  if (!EMAIL_RE.test(email)) return fail(res, 400, "invalid_email", "邮箱格式不正确");
  if (password.length < PASSWORD_MIN) return fail(res, 400, "weak_password", "密码至少 8 位");
  if (!nickname) return fail(res, 400, "invalid_nickname", "请填写昵称");
  if (nickname.length > NICKNAME_MAX) return fail(res, 400, "invalid_nickname", "昵称不能超过 16 个字符");

  // 身份表是唯一的事实来源：邮箱被占用 = 已存在 password 身份
  if (await findOne(T_IDENTITIES, { provider: "password", subject: email })) {
    return fail(res, 409, "email_taken", "该邮箱已注册");
  }
  if (await findOne(T_USERS, { email })) {
    return fail(res, 409, "email_taken", "该邮箱已注册");
  }

  // 先验证后建号：验码失败就什么都不留下，库里不会出现「占着邮箱但没人用」的账号
  if (codeRequired()) {
    if (!codePepper()) return fail(res, 500, "code_not_configured", "验证码服务未配置");
    const v = await consumeCode(email, "register", body.code);
    if (!v.ok) {
      if (v.error === "too_many_attempts") {
        return fail(res, 429, "too_many_attempts", "验证码尝试次数过多，请重新获取");
      }
      return fail(res, 400, "invalid_code", "验证码不正确或已失效");
    }
  }

  const t = now();
  let user = null;
  try {
    user = await insert(T_USERS, {
      email,
      email_verified: codeRequired(),
      status: "active",
      nickname,
      avatar_version: 0,
      created_at: t,
      updated_at: t,
      login_fails: 0,
      login_fail_window: 0,
    });
    if (!user) throw new PgError(500, "insert_failed", "用户未创建");

    await insert(T_CREDENTIALS, { user_id: user.id, password_hash: hashPassword(password), updated_at: t });
    await insert(T_IDENTITIES, { user_id: user.id, provider: "password", subject: email, created_at: t });

    const tokens = await createSession(user.id, authMeta(body));
    send(res, 201, sessionPayload(user, tokens, true));
  } catch (e) {
    // 半成品账号必须清掉，否则这个邮箱会永远「已注册但登不上」。
    // 删 users 一行即可：credentials / identities 都是 on delete cascade。
    if (user && user.id) await remove(T_USERS, { id: user.id }).catch(() => {});
    if (e instanceof PgError && e.duplicate) return fail(res, 409, "email_taken", "该邮箱已注册");
    throw e;
  }
}

/**
 * 发验证码。注册与找回共用，用 purpose 区分。
 *
 * **无论邮箱是否已注册都回 200** —— 否则这个接口就成了「这个邮箱有没有 AscentRoam 账号」
 * 的查询器。代价是用户可能收到一封「你正在注册」但其实已注册的邮件，可以接受。
 */
async function handleSendCode(req, res, forcedPurpose) {
  if (!mailConfigured()) return fail(res, 503, "mail_not_configured", "邮件服务尚未配置");
  if (!codePepper()) return fail(res, 500, "code_not_configured", "验证码服务未配置");

  const body = await readJson(req);
  const email = normalizeEmail(body.email);
  const purpose = forcedPurpose || (body.purpose === "reset" ? "reset" : "register");
  if (!EMAIL_RE.test(email)) return fail(res, 400, "invalid_email", "邮箱格式不正确");

  const t = now();
  const recent = await pg(
    "GET",
    pgPath(T_CODES, {
      select: "created_at",
      email: `eq.${email}`,
      purpose: `eq.${purpose}`,
      created_at: `gt.${t - 60 * 60 * 1000}`,
    })
  );
  const list = Array.isArray(recent) ? recent : [];
  if (list.length >= CODE_HOURLY_MAX) {
    return fail(res, 429, "too_many_requests", "请求过于频繁，请稍后再试");
  }
  const last = list.reduce((m, r) => Math.max(m, Number(r.created_at) || 0), 0);
  if (last && t - last < CODE_GAP_MS) {
    return fail(res, 429, "too_many_requests", "请求过于频繁，请稍后再试");
  }

  const code = String(crypto.randomInt(0, 1000000)).padStart(6, "0");
  const row = await insert(T_CODES, {
    email,
    purpose,
    code_hash: hashCode(email, purpose, code),
    expires_at: t + CODE_TTL_MS,
    attempts: 0,
    created_at: t,
  });

  const mail = codeMail(purpose, code);
  try {
    await sendMail({ to: email, subject: mail.subject, text: mail.text });
  } catch (e) {
    // 发不出去就把这条码删掉：留着也没人能收到，只会占住 1 分钟的发送间隔
    if (row && row.id) await remove(T_CODES, { id: row.id }).catch(() => {});
    console.error(`[mail] send failed: ${String(e.message).slice(0, 160)}`);
    return fail(res, 502, "mail_failed", "验证码发送失败，请稍后再试");
  }

  return send(res, 200, { ok: true });
}

/**
 * 重置密码。改密成功必须**作废该账号全部会话** ——
 * 否则「账号被盗后改密码」等于没改：攻击者手里的 refresh 还能用 90 天。
 */
async function handleResetPassword(req, res) {
  if (!codePepper()) return fail(res, 500, "code_not_configured", "验证码服务未配置");

  const body = await readJson(req);
  const email = normalizeEmail(body.email);
  const password = typeof body.newPassword === "string" ? body.newPassword : "";
  if (!EMAIL_RE.test(email)) return fail(res, 400, "invalid_email", "邮箱格式不正确");
  if (password.length < PASSWORD_MIN) return fail(res, 400, "weak_password", "密码至少 8 位");

  // 先验码再查用户：反过来的话，「账号不存在」和「码错了」的响应差异会泄露注册状态
  const v = await consumeCode(email, "reset", body.code);
  if (!v.ok) {
    if (v.error === "too_many_attempts") {
      return fail(res, 429, "too_many_attempts", "验证码尝试次数过多，请重新获取");
    }
    return fail(res, 400, "invalid_code", "验证码不正确或已失效");
  }

  const user = await findOne(T_USERS, { email });
  if (!user) return fail(res, 400, "invalid_code", "验证码不正确或已失效");
  if (user.status === "locked") return fail(res, 423, "account_locked", "账号已被停用");

  const t = now();
  const cred = await findOne(T_CREDENTIALS, { user_id: user.id });
  if (cred) {
    await update(T_CREDENTIALS, { user_id: user.id }, { password_hash: hashPassword(password), updated_at: t });
  } else {
    // Google-only 账号第一次设密码：必须 insert。写成 update 会静默影响 0 行，
    // 用户以为设成功了，其实下次登录还是没密码。
    await insert(T_CREDENTIALS, { user_id: user.id, password_hash: hashPassword(password), updated_at: t });
  }

  // 登录是按 identities 查的，没有这条身份就登不上
  if (!(await findOne(T_IDENTITIES, { provider: "password", subject: email }))) {
    await insert(T_IDENTITIES, {
      user_id: user.id,
      provider: "password",
      subject: email,
      created_at: t,
    }).catch(() => {});
  }

  await update(T_SESSIONS, { user_id: user.id }, { revoked: true });
  await update(T_USERS, { id: user.id }, {
    email_verified: true,
    login_fails: 0,
    login_fail_window: 0,
    updated_at: t,
  });

  return send(res, 200, { ok: true });
}

async function handleLogin(req, res) {
  const body = await readJson(req);
  const email = normalizeEmail(body.email);
  const password = typeof body.password === "string" ? body.password : "";
  if (!EMAIL_RE.test(email) || !password) return fail(res, 401, "invalid_credentials", "邮箱或密码不正确");

  const identity = await findOne(T_IDENTITIES, { provider: "password", subject: email });
  if (!identity) return fail(res, 401, "invalid_credentials", "邮箱或密码不正确");

  const user = await findOne(T_USERS, { id: identity.user_id });
  const credential = await findOne(T_CREDENTIALS, { user_id: identity.user_id });
  if (!user || !credential || !verifyPassword(password, credential.password_hash)) {
    if (user) await noteLoginFailure(user);
    return fail(res, 401, "invalid_credentials", "邮箱或密码不正确");
  }

  if (user.status === "locked") return fail(res, 423, "account_locked", "账号已被停用");
  if (REQUIRE_EMAIL_VERIFICATION && !user.email_verified) {
    return fail(res, 403, "email_unverified", "请先验证邮箱");
  }

  const wait = await loginGuard(user);
  if (wait > 0) return fail(res, 429, "too_many_attempts", `尝试过于频繁，${Math.ceil(wait / 60)} 分钟后再试`);

  await update(T_USERS, { id: user.id }, { login_fails: 0, login_fail_window: 0, updated_at: now() });
  const tokens = await createSession(user.id, authMeta(body));
  send(res, 200, sessionPayload(user, tokens, false));
}

// ---------------------------------------------------------------- 账号：Google

/**
 * 新账号的昵称取值顺序：客户端显式传的 > Google 的 name > given_name > 邮箱前缀。
 * 用户看到「自己的名字」比看到「邮箱前半截」自然得多，而 Google 恰好给了。
 */
function googleNickname(body, info, email) {
  const explicit = typeof body.nickname === "string" ? body.nickname.trim() : "";
  const candidates = [explicit, info.name, info.given_name];
  for (const c of candidates) {
    const s = typeof c === "string" ? c.trim() : "";
    if (s) return s.slice(0, NICKNAME_MAX);
  }
  return (email.split("@")[0] || "Climber").slice(0, NICKNAME_MAX);
}

/**
 * 把 Google 头像 URL 附在登录响应里。
 *
 * **服务端不下载这张图**：那要处理出网、大小、超时，还要防「用户可控 URL」的 SSRF。
 * 客户端本来就有图片解码与压缩链路，拿到 URL 自己下载、压成 512×512、
 * 走现有的 `/avatar` 上传即可 —— 复用而不是新造一条路。
 */
function withPicture(payload, info) {
  if (info && typeof info.picture === "string" && info.picture) {
    payload.google_picture = info.picture;
  }
  return payload;
}

/** 用 Google 的 tokeninfo 端点验证 idToken。返回 payload 或 null。 */async function verifyGoogleToken(idToken) {
  if (!googleClientId() || !idToken) return null;
  try {
    const r = await fetch("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodeURIComponent(idToken));
    if (!r.ok) return null;
    const info = await r.json();
    if (String(info.aud) !== googleClientId()) return null;
    if (!info.sub) return null;
    if (info.exp && Number(info.exp) * 1000 < now()) return null;
    if (info.iss && info.iss !== "https://accounts.google.com" && info.iss !== "accounts.google.com") return null;
    return info;
  } catch (e) {
    return null;
  }
}

/**
 * Google 登录。规则见 CLOUD-PLAN.md §10.1 A2 与 §11.4：
 *  - 有 google 身份 → 直接登录；
 *  - 没有身份但邮箱已被别的账号占用 → **不并号**，回 409 identity_conflict，
 *    让用户先用原方式登录，再到设置里主动绑定；
 *  - 都没有 → 建新账号。**不需要邮箱验证码**：Google 已验证过这个邮箱。
 *
 * 注意 tokeninfo 返回的字段全是**字符串**，"false" 是真值，
 * 所以 email_verified 必须按字符串比较，写成 `!!info.email_verified` 会把它判成已验证。
 */
async function handleGoogleLogin(req, res) {
  if (!googleClientId()) return fail(res, 501, "google_not_configured", "Google 登录尚未配置");
  const body = await readJson(req);
  const info = await verifyGoogleToken(typeof body.idToken === "string" ? body.idToken : "");
  if (!info) return fail(res, 401, "invalid_google_token", "Google 凭证无效");

  // 极少见但真实存在：Workspace 域被管理员设成未验证。不能因为「是 Google 就信」。
  if (String(info.email_verified) !== "true") {
    return fail(res, 403, "email_unverified", "该 Google 账号的邮箱未验证");
  }

  const subject = String(info.sub);
  const email = normalizeEmail(info.email);

  const identity = await findOne(T_IDENTITIES, { provider: "google", subject });
  if (identity) {
    const user = await findOne(T_USERS, { id: identity.user_id });
    if (!user) return fail(res, 401, "invalid_google_token", "账号不存在");
    if (user.status === "locked") return fail(res, 423, "account_locked", "账号已被停用");
    const tokens = await createSession(user.id, authMeta(body));
    return send(res, 200, withPicture(sessionPayload(user, tokens, false), info));
  }

  if (email && (await findOne(T_USERS, { email }))) {
    return fail(res, 409, "identity_conflict", "该邮箱已用其他方式注册，请先登录后在设置里绑定 Google");
  }

  const t = now();
  let user = null;
  try {
    user = await insert(T_USERS, {
      email,
      email_verified: true,
      status: "active",
      nickname: googleNickname(body, info, email),
      avatar_version: 0,
      created_at: t,
      updated_at: t,
      login_fails: 0,
      login_fail_window: 0,
    });
    if (!user) throw new PgError(500, "insert_failed", "用户未创建");
    await insert(T_IDENTITIES, { user_id: user.id, provider: "google", subject, created_at: t });
    const tokens = await createSession(user.id, authMeta(body));
    send(res, 200, withPicture(sessionPayload(user, tokens, true), info));
  } catch (e) {
    if (user && user.id) await remove(T_USERS, { id: user.id }).catch(() => {});
    if (e instanceof PgError && e.duplicate) {
      return fail(res, 409, "identity_conflict", "该邮箱已用其他方式注册，请先登录后在设置里绑定 Google");
    }
    throw e;
  }
}

/** 已登录时主动绑定 Google。 */
async function handleGoogleLink(req, res, user) {
  if (!googleClientId()) return fail(res, 501, "google_not_configured", "Google 登录尚未配置");
  const body = await readJson(req);
  const info = await verifyGoogleToken(typeof body.idToken === "string" ? body.idToken : "");
  if (!info) return fail(res, 401, "invalid_google_token", "Google 凭证无效");

  const subject = String(info.sub);
  const existing = await findOne(T_IDENTITIES, { provider: "google", subject });
  if (existing && existing.user_id !== user.id) {
    return fail(res, 409, "identity_taken", "这个 Google 账号已绑定其他账号");
  }
  if (existing) return send(res, 200, { user: publicUser(user) });

  try {
    await insert(T_IDENTITIES, { user_id: user.id, provider: "google", subject, created_at: now() });
  } catch (e) {
    if (e instanceof PgError && e.duplicate) {
      return fail(res, 409, "identity_taken", "这个 Google 账号已绑定其他账号");
    }
    throw e;
  }
  send(res, 200, { user: publicUser(user) });
}

/** 解绑 Google。至少要留一个能登录的身份，否则这个号就锁死了。 */
async function handleGoogleUnlink(req, res, user) {
  const list = await selectAll(T_IDENTITIES, { user_id: user.id });
  const google = list.filter((i) => i.provider === "google");
  if (!google.length) return fail(res, 404, "identity_not_found", "没有绑定 Google");
  if (list.length - google.length < 1) {
    return fail(res, 409, "last_identity", "这是唯一的登录方式，不能解绑");
  }
  await remove(T_IDENTITIES, { user_id: user.id, provider: "google" });
  send(res, 200, { user: publicUser(user) });
}

// ---------------------------------------------------------------- 账号：会话

/** 用 refresh 换新的 access。refresh 不轮换，但每次使用都会顺延有效期。 */
async function handleRefresh(req, res) {
  const body = await readJson(req);
  const refresh = typeof body.refresh_token === "string" ? body.refresh_token : "";
  if (!/^[0-9a-f]{64}$/.test(refresh)) return fail(res, 401, "invalid_token", "会话已失效");

  const session = await findOne(T_SESSIONS, { refresh_hash: sha256(refresh), revoked: false });
  if (!session) return fail(res, 401, "invalid_token", "会话已失效，请重新登录");
  if (Number(session.refresh_expires_at) <= now()) {
    await update(T_SESSIONS, { id: session.id }, { revoked: true });
    return fail(res, 401, "invalid_token", "登录已过期，请重新登录");
  }

  const user = await findOne(T_USERS, { id: session.user_id });
  if (!user) return fail(res, 401, "invalid_token", "账号不存在");
  if (user.status === "locked") return fail(res, 423, "account_locked", "账号已被停用");

  const access = randomToken();
  const t = now();
  await update(T_SESSIONS, { id: session.id }, {
    access_hash: sha256(access),
    access_expires_at: t + ACCESS_TTL_MS,
    refresh_expires_at: t + REFRESH_TTL_MS,
    last_used_at: t,
  });
  send(res, 200, {
    access_token: access,
    expires_in: Math.floor(ACCESS_TTL_MS / 1000),
    user: publicUser(user),
  });
}

async function handleMe(req, res, user) {
  send(res, 200, { user: publicUser(user) });
}

async function handlePatchProfile(req, res, user) {
  const body = await readJson(req);
  const nickname = typeof body.nickname === "string" ? body.nickname.trim() : null;
  if (nickname === null) return fail(res, 400, "invalid_nickname", "缺少昵称");
  if (!nickname || nickname.length > NICKNAME_MAX) return fail(res, 400, "invalid_nickname", "昵称需为 1–16 个字符");
  await update(T_USERS, { id: user.id }, { nickname, updated_at: now() });
  send(res, 200, { user: publicUser({ ...user, nickname }) });
}

async function handleLogout(req, res, session) {
  if (session) await update(T_SESSIONS, { id: session.id }, { revoked: true });
  res.writeHead(204);
  res.end();
}

async function handleListDevices(req, res, user, session) {
  const list = (await selectAll(T_SESSIONS, { user_id: user.id, revoked: false }))
    .filter((s) => Number(s.refresh_expires_at) > now())
    .sort((a, b) => Number(b.last_used_at || 0) - Number(a.last_used_at || 0))
    .map((s) => ({
      id: s.id,
      device_id: s.device_id || "",
      platform: s.platform || "",
      app_version: s.app_version || "",
      created_at: Number(s.created_at) || 0,
      last_used_at: Number(s.last_used_at) || 0,
      current: session ? s.id === session.id : false,
    }));
  send(res, 200, { devices: list });
}

async function handleRevokeDevice(req, res, user, session, deviceId) {
  const target = await findOne(T_SESSIONS, { id: deviceId, user_id: user.id });
  if (!target) return fail(res, 404, "device_not_found", "设备不存在");
  await update(T_SESSIONS, { id: target.id }, { revoked: true });
  send(res, 200, { revoked: true, self: session ? session.id === target.id : false });
}

/**
 * 删除账号。只删 users 一行：credentials / identities / sessions / states / avatars
 * 全是 `on delete cascade`，由数据库保证一次删干净，不会出现「删了一半」的中间状态。
 */
async function handleDeleteAccount(req, res, user) {
  await remove(T_USERS, { id: user.id });
  res.writeHead(204);
  res.end();
}

// ---------------------------------------------------------------- 进度

async function handleGetState(req, res, user) {
  const row = await findOne(T_STATES, { user_id: user.id });
  if (!row) return send(res, 200, { rev: 0, updatedAt: 0, payload: null });
  send(res, 200, { rev: row.rev || 0, updatedAt: Number(row.updated_at) || 0, payload: row.payload || null });
}

async function handlePutState(req, res, user) {
  const body = await readJson(req, MAX_JSON_BYTES);
  const payload = body.payload;
  const rev = Number(body.rev || 0);
  if (!payload || typeof payload !== "object") return fail(res, 400, "invalid_payload", "payload 缺失");

  const raw = JSON.stringify(payload);
  const bytes = Buffer.byteLength(raw, "utf8");
  if (bytes > MAX_PAYLOAD_BYTES) return fail(res, 413, "payload_too_large", "进度数据超过 512 KB");

  const existing = await findOne(T_STATES, { user_id: user.id });
  const serverRev = existing ? Number(existing.rev || 0) : 0;

  if (rev !== serverRev) {
    return send(res, 409, {
      rev: serverRev,
      updatedAt: existing ? Number(existing.updated_at) : 0,
      payload: existing ? existing.payload : null,
    });
  }

  const nextRev = serverRev + 1;
  const t = now();
  if (existing) {
    await update(T_STATES, { user_id: user.id }, { rev: nextRev, payload, payload_bytes: bytes, updated_at: t });
  } else {
    await insert(T_STATES, { user_id: user.id, rev: nextRev, payload, payload_bytes: bytes, updated_at: t });
  }
  send(res, 200, { rev: nextRev, updatedAt: t });
}

// ---------------------------------------------------------------- 头像

async function handlePutAvatar(req, res, user) {
  const buf = await readBody(req, MAX_AVATAR_BYTES);
  if (!buf.length) return fail(res, 400, "empty_body", "缺少图片数据");
  if (!(buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff)) {
    return fail(res, 415, "unsupported_media", "只接受 JPEG");
  }
  // 版本号必须只增不减：删除头像会把 avatar_version 归零，若下次上传从 0+1 开始，
  // 别的设备（记着更大的 cloudAvatarVersion）就永远不会下载这张新图。
  // avatar_rev 是永不复位的计数器，avatar_version 只是「当前头像的版本，0 = 没有」。
  const version = Number(user.avatar_rev || 0) + 1;
  const existing = await findOne(T_AVATARS, { user_id: user.id });
  const t = now();
  const data = buf.toString("base64");
  if (existing) {
    await update(T_AVATARS, { user_id: user.id }, { data, version, updated_at: t });
  } else {
    await insert(T_AVATARS, { user_id: user.id, data, version, updated_at: t });
  }
  await update(T_USERS, { id: user.id }, { avatar_version: version, avatar_rev: version, updated_at: t });
  send(res, 200, { version, updatedAt: t });
}

async function handleGetAvatar(req, res, user) {
  const row = await findOne(T_AVATARS, { user_id: user.id });
  if (!row || !row.data) return fail(res, 404, "no_avatar", "没有头像");
  sendBytes(res, 200, Buffer.from(row.data, "base64"), "image/jpeg");
}

async function handleDeleteAvatar(req, res, user) {
  await remove(T_AVATARS, { user_id: user.id });
  await update(T_USERS, { id: user.id }, { avatar_version: 0, updated_at: now() });
  res.writeHead(204);
  res.end();
}

// ---------------------------------------------------------------- 分发

/** 网关可能把路由挂在 /ascent 一类前缀下，两种都吃。 */
function candidates(pathname) {
  const out = [pathname];
  const m = /^\/[^/]+(\/.*)$/.exec(pathname);
  if (m) out.push(m[1]);
  return out;
}

const ROUTES = [
  { method: "GET", path: "/health", public: true, handler: (req, res) => send(res, 200, { ok: true, version: "2", time: now() }) },
  { method: "POST", path: "/auth/register", public: true, handler: handleRegister },
  { method: "POST", path: "/auth/email/code", public: true, handler: (req, res) => handleSendCode(req, res) },
  { method: "POST", path: "/auth/password/forgot", public: true, handler: (req, res) => handleSendCode(req, res, "reset") },
  { method: "POST", path: "/auth/password/reset", public: true, handler: handleResetPassword },
  { method: "POST", path: "/auth/login", public: true, handler: handleLogin },
  { method: "POST", path: "/auth/google", public: true, handler: handleGoogleLogin },
  { method: "POST", path: "/auth/refresh", public: true, handler: handleRefresh },
  { method: "GET", path: "/auth/me", handler: handleMe },
  { method: "PATCH", path: "/auth/profile", handler: handlePatchProfile },
  { method: "POST", path: "/auth/logout", handler: (req, res, user, session) => handleLogout(req, res, session) },
  { method: "DELETE", path: "/auth/account", handler: handleDeleteAccount },
  { method: "POST", path: "/auth/identities/google", handler: handleGoogleLink },
  { method: "DELETE", path: "/auth/identities/google", handler: handleGoogleUnlink },
  { method: "GET", path: "/me/devices", handler: handleListDevices },
  { method: "DELETE", path: "/me/devices", device: true, handler: handleRevokeDevice },
  { method: "GET", path: "/state", handler: handleGetState },
  { method: "PUT", path: "/state", handler: handlePutState },
  { method: "PUT", path: "/avatar", handler: handlePutAvatar },
  { method: "GET", path: "/avatar", handler: handleGetAvatar },
  { method: "DELETE", path: "/avatar", handler: handleDeleteAvatar },
];

/** 返回 { route, param }。`device` 路由形如 /me/devices/<id>。 */
function match(method, pathname) {
  for (const p of candidates(pathname)) {
    for (const r of ROUTES) {
      if (r.method !== method) continue;
      if (r.device) {
        const prefix = r.path + "/";
        const rest = p.startsWith(prefix) ? p.slice(prefix.length) : "";
        if (rest && !rest.includes("/")) return { route: r, param: decodeURIComponent(rest) };
      } else if (p === r.path) {
        return { route: r, param: null };
      }
    }
  }
  return null;
}

const server = http.createServer(async (req, res) => {
  const started = now();
  let uid = "-";
  let status = 500;

  try {
    if (req.method === "OPTIONS") {
      res.writeHead(204);
      res.end();
      return;
    }

    const url = new URL(req.url || "/", "http://127.0.0.1");
    const hit = match(req.method, url.pathname);
    if (!hit) {
      status = 404;
      return fail(res, 404, "not_found", "Not Found");
    }
    const { route, param } = hit;

    if (route.public) {
      await route.handler(req, res);
      status = res.statusCode;
      return;
    }

    const auth = await resolveAccess(req);
    if (!auth || auth.error) {
      status = 401;
      const code = (auth && auth.error) || "unauthorized";
      return fail(res, 401, code, code === "token_expired" ? "登录已过期" : "登录已失效，请重新登录");
    }
    uid = auth.user.id;
    await touchSession(auth.session);

    if (route.device) {
      await route.handler(req, res, auth.user, auth.session, param);
    } else {
      await route.handler(req, res, auth.user, auth.session);
    }
    status = res.statusCode;
  } catch (e) {
    if (e && e.code === "too_large") {
      status = 413;
      return fail(res, 413, "payload_too_large", "请求体过大");
    }
    if (e && e.code === "bad_json") {
      status = 400;
      return fail(res, 400, "bad_json", "请求体不是合法 JSON");
    }
    console.error("[ascent-api] error", req.method, req.url, e && (e.code || e.message));
    status = 500;
    if (!res.headersSent) fail(res, 500, "server_error", "服务异常，请稍后重试");
  } finally {
    console.log(`[ascent-api] ${req.method} ${req.url} ${status} ${now() - started}ms uid=${uid}`);
  }
});

server.listen(PORT, () => {
  console.log(`[ascent-api] listening on ${PORT}, env=${ENV_ID || "(missing)"}, pg=${API_KEY ? "ok" : "(missing key)"}`);
});
