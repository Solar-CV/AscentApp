"use strict";

/**
 * ASCENT 云端 API（CloudBase HTTP 云函数）
 *
 * 契约见仓库根目录 CLOUD-PLAN.md，改动接口时两边一起改。
 *
 * 设计约束：
 *  1. 只用 Node 内置模块 + @cloudbase/node-sdk。HTTP 云函数不会自动装依赖，
 *     node_modules 必须随包上传（见 server/README.md）。
 *  2. 账号自建（scrypt + 不透明 token），不绑平台身份认证 —— 将来搬 VPS 时数据可迁。
 *  3. CORS 由网关负责，这里**不设** Access-Control-Allow-Origin：
 *     网关会按环境安全域名填，函数再填一遍会变成 `<origin>,*`，浏览器直接拒绝。
 *  4. 任何响应都不得回显 req.headers / process.env，也不得把邮箱、token 写进日志。
 */

const http = require("http");
const crypto = require("crypto");
const tcb = require("@cloudbase/node-sdk");

// ---------------------------------------------------------------- 配置

const PORT = 9000;
const ENV_ID = process.env.TCB_ENV || process.env.CLOUDBASE_ENV_ID || "";
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || "";

const MAX_JSON_BYTES = 1024 * 1024; // 1 MB
const MAX_AVATAR_BYTES = 512 * 1024; // 512 KB
const MAX_PAYLOAD_BYTES = 512 * 1024; // 512 KB
const TOKEN_TTL_MS = 180 * 24 * 60 * 60 * 1000;
const LOGIN_WINDOW_MS = 15 * 60 * 1000;
const LOGIN_MAX_FAILS = 10;
const PASSWORD_MIN = 8;
const NICKNAME_MAX = 16;

const C_USERS = "ascent_users";
const C_TOKENS = "ascent_tokens";
const C_STATES = "ascent_states";
const C_AVATARS = "ascent_avatars";

const app = tcb.init({ env: ENV_ID, accessKey: process.env.CLOUDBASE_APIKEY });
const db = app.database();

// ---------------------------------------------------------------- HTTP 基础

function send(res, status, body, headers) {
  const payload = body === undefined ? "" : JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    ...(headers || {}),
  });
  res.end(payload);
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

/** scrypt 哈希，格式 scrypt$N$r$p$salt$hash（参数随包写死，方便日后升级）。 */
function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const N = 16384;
  const r = 8;
  const p = 1;
  const hash = crypto.scryptSync(password, salt, 64, { N, r, p, maxmem: 64 * 1024 * 1024 });
  return `scrypt$${N}$${r}$${p}$${salt.toString("hex")}$${hash.toString("hex")}`;
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
    actual = crypto.scryptSync(password, salt, expected.length, { N, r, p, maxmem: 64 * 1024 * 1024 });
  } catch (e) {
    return false;
  }
  return actual.length === expected.length && crypto.timingSafeEqual(actual, expected);
}

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

function normalizeEmail(v) {
  return typeof v === "string" ? v.trim().toLowerCase() : "";
}

/** 把文档里的下划线字段去掉，只暴露契约里的字段。 */
function publicUser(doc) {
  return {
    id: doc._id,
    email: doc.email,
    nickname: doc.nickname || "",
    provider: doc.provider || "password",
    avatarVersion: doc.avatarVersion || 0,
    createdAt: doc.createdAt || 0,
  };
}

/** node-sdk 的 add / get 返回形状在版本间有差异，这里统一。 */
function firstDoc(res) {
  if (!res) return null;
  const list = Array.isArray(res.data) ? res.data : res.data ? [res.data] : [];
  return list.length ? list[0] : null;
}

function insertedId(res) {
  if (!res) return null;
  return res.id || res._id || (Array.isArray(res.ids) ? res.ids[0] : null);
}

async function findOne(collection, where) {
  const res = await db.collection(collection).where(where).limit(1).get();
  return firstDoc(res);
}

async function issueToken(userId, deviceLabel) {
  const token = crypto.randomBytes(32).toString("hex");
  const t = now();
  await db.collection(C_TOKENS).add({
    userId,
    tokenHash: sha256(token),
    deviceLabel: typeof deviceLabel === "string" ? deviceLabel.slice(0, 60) : "",
    createdAt: t,
    lastUsedAt: t,
    expiresAt: t + TOKEN_TTL_MS,
  });
  return token;
}

/** 从 Authorization 头解析出用户文档；失败返回 null。 */
async function authenticate(req) {
  const raw = req.headers["authorization"] || "";
  const m = /^Bearer\s+([0-9a-f]{64})$/i.exec(String(raw).trim());
  if (!m) return null;
  const tokenHash = sha256(m[1]);
  const tokenDoc = await findOne(C_TOKENS, { tokenHash });
  if (!tokenDoc) return null;
  if (Number(tokenDoc.expiresAt) < now()) {
    await db.collection(C_TOKENS).doc(tokenDoc._id).remove();
    return null;
  }
  const user = await findOne(C_USERS, { _id: tokenDoc.userId });
  if (!user) return null;
  // 使用即续期：低频写，不必每次请求都刷新
  if (now() - Number(tokenDoc.lastUsedAt || 0) > 24 * 60 * 60 * 1000) {
    db.collection(C_TOKENS)
      .doc(tokenDoc._id)
      .update({ lastUsedAt: now(), expiresAt: now() + TOKEN_TTL_MS })
      .catch(() => {});
  }
  return user;
}

async function loginGuard(user) {
  const fails = Number(user.loginFails || 0);
  const since = Number(user.loginFailWindow || 0);
  if (fails >= LOGIN_MAX_FAILS && now() - since < LOGIN_WINDOW_MS) {
    return Math.ceil((LOGIN_WINDOW_MS - (now() - since)) / 1000);
  }
  return 0;
}

async function noteLoginFailure(user) {
  const since = Number(user.loginFailWindow || 0);
  const expired = now() - since > LOGIN_WINDOW_MS;
  await db
    .collection(C_USERS)
    .doc(user._id)
    .update({
      loginFails: expired ? 1 : Number(user.loginFails || 0) + 1,
      loginFailWindow: expired ? now() : since,
    });
}

// ---------------------------------------------------------------- 路由：账号

async function handleRegister(req, res) {
  const body = await readJson(req);
  const email = normalizeEmail(body.email);
  const password = typeof body.password === "string" ? body.password : "";
  const nickname = typeof body.nickname === "string" ? body.nickname.trim() : "";

  if (!EMAIL_RE.test(email)) return fail(res, 400, "invalid_email", "邮箱格式不正确");
  if (password.length < PASSWORD_MIN) return fail(res, 400, "weak_password", "密码至少 8 位");
  if (!nickname) return fail(res, 400, "invalid_nickname", "请填写昵称");
  if (nickname.length > NICKNAME_MAX) return fail(res, 400, "invalid_nickname", "昵称不能超过 16 个字符");

  const existing = await findOne(C_USERS, { email });
  if (existing) return fail(res, 409, "email_taken", "该邮箱已注册");

  const t = now();
  const doc = {
    email,
    passwordHash: hashPassword(password),
    provider: "password",
    nickname,
    avatarVersion: 0,
    createdAt: t,
    updatedAt: t,
    loginFails: 0,
    loginFailWindow: 0,
  };
  let id;
  try {
    id = insertedId(await db.collection(C_USERS).add(doc));
  } catch (e) {
    // 唯一索引兜住并发注册
    if (String(e && e.message).indexOf("duplicate") >= 0) return fail(res, 409, "email_taken", "该邮箱已注册");
    throw e;
  }
  if (!id) return fail(res, 500, "server_error", "注册失败，请重试");

  const token = await issueToken(id, body.deviceLabel);
  send(res, 201, { token, user: publicUser({ ...doc, _id: id }) });
}

async function handleLogin(req, res) {
  const body = await readJson(req);
  const email = normalizeEmail(body.email);
  const password = typeof body.password === "string" ? body.password : "";
  if (!EMAIL_RE.test(email) || !password) return fail(res, 401, "invalid_credentials", "邮箱或密码不正确");

  const user = await findOne(C_USERS, { email });
  // 不区分「账号不存在」和「密码错误」，避免被枚举
  if (!user || !verifyPassword(password, user.passwordHash)) {
    if (user) await noteLoginFailure(user);
    return fail(res, 401, "invalid_credentials", "邮箱或密码不正确");
  }

  const wait = await loginGuard(user);
  if (wait > 0) return fail(res, 429, "too_many_attempts", `尝试过于频繁，${Math.ceil(wait / 60)} 分钟后再试`);

  await db.collection(C_USERS).doc(user._id).update({ loginFails: 0, loginFailWindow: 0, updatedAt: now() });
  const token = await issueToken(user._id, body.deviceLabel);
  send(res, 200, { token, user: publicUser(user) });
}

async function handleGoogle(req, res) {
  if (!GOOGLE_CLIENT_ID) return fail(res, 501, "google_not_configured", "Google 登录尚未配置");
  const body = await readJson(req);
  const idToken = typeof body.idToken === "string" ? body.idToken : "";
  if (!idToken) return fail(res, 401, "invalid_google_token", "缺少 idToken");

  let info;
  try {
    const r = await fetch("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodeURIComponent(idToken));
    if (!r.ok) throw new Error("bad token");
    info = await r.json();
  } catch (e) {
    return fail(res, 401, "invalid_google_token", "Google 凭证无效");
  }
  if (info.aud !== GOOGLE_CLIENT_ID || !info.sub) return fail(res, 401, "invalid_google_token", "Google 凭证无效");

  const email = normalizeEmail(info.email);
  let user = await findOne(C_USERS, { provider: "google", googleSub: info.sub });
  if (!user) {
    const t = now();
    const doc = {
      email: email || "",
      provider: "google",
      googleSub: info.sub,
      nickname: (body.nickname && String(body.nickname).trim()) || (email.split("@")[0] || "Climber").slice(0, NICKNAME_MAX),
      avatarVersion: 0,
      createdAt: t,
      updatedAt: t,
    };
    const id = insertedId(await db.collection(C_USERS).add(doc));
    user = { ...doc, _id: id };
  }
  const token = await issueToken(user._id, body.deviceLabel);
  send(res, 200, { token, user: publicUser(user) });
}

async function handleMe(req, res, user) {
  send(res, 200, { user: publicUser(user) });
}

async function handleLogout(req, res) {
  const raw = req.headers["authorization"] || "";
  const m = /^Bearer\s+([0-9a-f]{64})$/i.exec(String(raw).trim());
  if (m) {
    const tokenDoc = await findOne(C_TOKENS, { tokenHash: sha256(m[1]) });
    if (tokenDoc) await db.collection(C_TOKENS).doc(tokenDoc._id).remove();
  }
  res.writeHead(204);
  res.end();
}

async function handlePatchProfile(req, res, user) {
  const body = await readJson(req);
  const nickname = typeof body.nickname === "string" ? body.nickname.trim() : null;
  if (nickname === null) return fail(res, 400, "invalid_nickname", "缺少昵称");
  if (!nickname || nickname.length > NICKNAME_MAX) return fail(res, 400, "invalid_nickname", "昵称需为 1–16 个字符");
  await db.collection(C_USERS).doc(user._id).update({ nickname, updatedAt: now() });
  send(res, 200, { user: publicUser({ ...user, nickname }) });
}

async function handleDeleteAccount(req, res, user) {
  const uid = user._id;
  await Promise.all([
    db.collection(C_TOKENS).where({ userId: uid }).remove(),
    db.collection(C_STATES).where({ userId: uid }).remove(),
    db.collection(C_AVATARS).where({ userId: uid }).remove(),
  ]);
  await db.collection(C_USERS).doc(uid).remove();
  res.writeHead(204);
  res.end();
}

// ---------------------------------------------------------------- 路由：进度

async function handleGetState(req, res, user) {
  const doc = await findOne(C_STATES, { userId: user._id });
  if (!doc) return send(res, 200, { rev: 0, updatedAt: 0, payload: null });
  send(res, 200, { rev: doc.rev || 0, updatedAt: doc.updatedAt || 0, payload: doc.payload || null });
}

async function handlePutState(req, res, user) {
  const body = await readJson(req, MAX_JSON_BYTES);
  const payload = body.payload;
  const rev = Number(body.rev || 0);
  if (!payload || typeof payload !== "object") return fail(res, 400, "invalid_payload", "payload 缺失");

  const raw = JSON.stringify(payload);
  const bytes = Buffer.byteLength(raw, "utf8");
  if (bytes > MAX_PAYLOAD_BYTES) return fail(res, 413, "payload_too_large", "进度数据超过 512 KB");

  const existing = await findOne(C_STATES, { userId: user._id });
  const serverRev = existing ? Number(existing.rev || 0) : 0;

  if (rev !== serverRev) {
    // 客户端拿着旧版本：把最新的还给它，让它合并后重试
    return send(res, 409, {
      rev: serverRev,
      updatedAt: existing ? existing.updatedAt : 0,
      payload: existing ? existing.payload : null,
    });
  }

  const nextRev = serverRev + 1;
  const t = now();
  if (existing) {
    await db.collection(C_STATES).doc(existing._id).update({ rev: nextRev, payload, updatedAt: t, payloadBytes: bytes });
  } else {
    await db.collection(C_STATES).add({ userId: user._id, rev: nextRev, payload, updatedAt: t, payloadBytes: bytes });
  }
  send(res, 200, { rev: nextRev, updatedAt: t });
}

// ---------------------------------------------------------------- 路由：头像

async function handlePutAvatar(req, res, user) {
  const buf = await readBody(req, MAX_AVATAR_BYTES);
  if (!buf.length) return fail(res, 400, "empty_body", "缺少图片数据");
  // 只收 JPEG：客户端固定存 JPEG 90，别的类型一律拒绝，避免把这里当图床
  if (!(buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff)) {
    return fail(res, 415, "unsupported_media", "只接受 JPEG");
  }
  const version = Number(user.avatarVersion || 0) + 1;
  const existing = await findOne(C_AVATARS, { userId: user._id });
  const t = now();
  const data = buf.toString("base64");
  if (existing) {
    await db.collection(C_AVATARS).doc(existing._id).update({ data, version, updatedAt: t });
  } else {
    await db.collection(C_AVATARS).add({ userId: user._id, data, version, updatedAt: t });
  }
  await db.collection(C_USERS).doc(user._id).update({ avatarVersion: version, updatedAt: t });
  send(res, 200, { version, updatedAt: t });
}

async function handleGetAvatar(req, res, user) {
  const doc = await findOne(C_AVATARS, { userId: user._id });
  if (!doc || !doc.data) return fail(res, 404, "no_avatar", "没有头像");
  sendBytes(res, 200, Buffer.from(doc.data, "base64"), "image/jpeg");
}

async function handleDeleteAvatar(req, res, user) {
  await db.collection(C_AVATARS).where({ userId: user._id }).remove();
  await db.collection(C_USERS).doc(user._id).update({ avatarVersion: 0, updatedAt: now() });
  res.writeHead(204);
  res.end();
}

// ---------------------------------------------------------------- 分发

/**
 * 网关可能把路由挂在形如 /ascent/... 的前缀下，也可能原样透传。
 * 这里两种都吃：先按原样匹配，匹配不到再砍掉第一段试一次。
 */
function candidates(pathname) {
  const out = [pathname];
  const m = /^\/[^/]+(\/.*)$/.exec(pathname);
  if (m) out.push(m[1]);
  return out;
}

const ROUTES = [
  { method: "GET", path: "/health", public: true, handler: (req, res) => send(res, 200, { ok: true, version: "1", time: now() }) },
  { method: "POST", path: "/auth/register", public: true, handler: handleRegister },
  { method: "POST", path: "/auth/login", public: true, handler: handleLogin },
  { method: "POST", path: "/auth/google", public: true, handler: handleGoogle },
  { method: "GET", path: "/auth/me", handler: handleMe },
  { method: "POST", path: "/auth/logout", public: true, handler: handleLogout },
  { method: "PATCH", path: "/auth/profile", handler: handlePatchProfile },
  { method: "DELETE", path: "/auth/account", handler: handleDeleteAccount },
  { method: "GET", path: "/state", handler: handleGetState },
  { method: "PUT", path: "/state", handler: handlePutState },
  { method: "PUT", path: "/avatar", handler: handlePutAvatar },
  { method: "GET", path: "/avatar", handler: handleGetAvatar },
  { method: "DELETE", path: "/avatar", handler: handleDeleteAvatar },
];

function match(method, pathname) {
  for (const p of candidates(pathname)) {
    const route = ROUTES.find((r) => r.method === method && r.path === p);
    if (route) return route;
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
    const route = match(req.method, url.pathname);
    if (!route) {
      status = 404;
      return fail(res, 404, "not_found", "Not Found");
    }

    if (route.public) {
      await route.handler(req, res);
      status = res.statusCode;
      return;
    }

    const user = await authenticate(req);
    if (!user) {
      status = 401;
      return fail(res, 401, "unauthorized", "登录已失效，请重新登录");
    }
    uid = user._id;
    await route.handler(req, res, user);
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
    console.error("[ascent-api] error", req.method, req.url, e && e.message);
    status = 500;
    if (!res.headersSent) fail(res, 500, "server_error", "服务异常，请稍后重试");
  } finally {
    // 只记路由、状态、耗时、用户 id —— 不记邮箱、token、payload 全文
    console.log(`[ascent-api] ${req.method} ${req.url} ${status} ${now() - started}ms uid=${uid}`);
  }
});

server.listen(PORT, () => {
  console.log(`[ascent-api] listening on ${PORT}, env=${ENV_ID || "(missing)"}`);
});
