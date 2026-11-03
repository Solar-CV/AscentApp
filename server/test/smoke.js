"use strict";

/**
 * 本地冒烟测试：不起云端，用内存替身替换 @cloudbase/node-sdk，
 * 把 CLOUD-PLAN.md 里的契约逐条打一遍。
 *
 *   node server/test/smoke.js
 *
 * 目的不是替代真机联调，而是在部署前把路由、鉴权、冲突、级联删除这些
 * 「一错就要等一轮部署」的地方先验证掉。
 */

const path = require("path");
const http = require("http");

const FN_DIR = path.join(__dirname, "..", "functions", "ascent-api");

// ---------------------------------------------------------------- 内存版文档库

let seq = 0;
const store = new Map();

function coll(name) {
  if (!store.has(name)) store.set(name, []);
  return store.get(name);
}

function matches(doc, where) {
  return Object.keys(where || {}).every((k) => doc[k] === where[k]);
}

function makeQuery(name) {
  const state = { where: null, limit: Infinity };
  const q = {
    where(w) {
      state.where = w;
      return q;
    },
    limit(n) {
      state.limit = n;
      return q;
    },
    async get() {
      const all = coll(name).filter((d) => (state.where ? matches(d, state.where) : true));
      return { data: all.slice(0, state.limit === Infinity ? all.length : state.limit).map((d) => ({ ...d })) };
    },
    async update(patch) {
      let n = 0;
      for (const d of coll(name)) {
        if (state.where ? matches(d, state.where) : true) {
          Object.assign(d, patch);
          n++;
        }
      }
      return { updated: n };
    },
    async remove() {
      const keep = [];
      let n = 0;
      for (const d of coll(name)) {
        if (state.where ? matches(d, state.where) : true) n++;
        else keep.push(d);
      }
      store.set(name, keep);
      return { deleted: n };
    },
  };
  return q;
}

const fakeDb = {
  collection(name) {
    return {
      where: (w) => makeQuery(name).where(w),
      async add(doc) {
        const _id = "id_" + ++seq;
        coll(name).push({ ...doc, _id });
        return { id: _id };
      },
      doc(id) {
        return {
          async update(patch) {
            const d = coll(name).find((x) => x._id === id);
            if (!d) return { updated: 0 };
            Object.assign(d, patch);
            return { updated: 1 };
          },
          async remove() {
            const i = coll(name).findIndex((x) => x._id === id);
            if (i >= 0) coll(name).splice(i, 1);
            return { deleted: i >= 0 ? 1 : 0 };
          },
        };
      },
    };
  },
};

const fakeSdk = { init: () => ({ database: () => fakeDb }) };

// 让 index.js 里的 require("@cloudbase/node-sdk") 拿到替身
const sdkPath = require.resolve("@cloudbase/node-sdk", { paths: [FN_DIR] });
require.cache[sdkPath] = { id: sdkPath, filename: sdkPath, loaded: true, exports: fakeSdk };

process.env.TCB_ENV = "local-test";
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
  const res = await fetch(BASE + p, { method, headers, body: payload });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch (e) {
    json = null;
  }
  return { status: res.status, json, text, buf: Buffer.from(text, "binary") };
}

async function waitForServer() {
  for (let i = 0; i < 50; i++) {
    try {
      const r = await fetch(BASE + "/health");
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
  r = await call("GET", "/auth/login");
  check("方法不对 404（未注册该组合）", r.status === 404);

  console.log("\n[注册]");
  r = await call("POST", "/auth/register", { body: { email: "Alex@Example.com ", password: "climb8848", nickname: "Alex" } });
  check("注册 201", r.status === 201, JSON.stringify(r.json));
  const token = r.json && r.json.token;
  check("返回 token（64 位 hex）", typeof token === "string" && /^[0-9a-f]{64}$/.test(token));
  check("邮箱已归一化为小写", r.json.user && r.json.user.email === "alex@example.com", r.json.user && r.json.user.email);
  check("未回显密码字段", r.json.user && r.json.user.passwordHash === undefined);

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
  check("不存在账号同样 401（不暴露账号是否存在）", r.status === 401 && r.json.error === "invalid_credentials");

  r = await call("POST", "/auth/login", { body: { email: "alex@example.com", password: "climb8848" } });
  check("正确密码 200", r.status === 200 && typeof r.json.token === "string");
  const token2 = r.json.token;
  check("每次登录发新 token", token2 !== token);

  console.log("\n[鉴权]");
  r = await call("GET", "/auth/me");
  check("无 token 401", r.status === 401 && r.json.error === "unauthorized");
  r = await call("GET", "/auth/me", { token: "f".repeat(64) });
  check("伪造 token 401", r.status === 401);
  r = await call("GET", "/auth/me", { token });
  check("带 token 拿到自己", r.status === 200 && r.json.user.email === "alex@example.com");

  console.log("\n[进度]");
  r = await call("GET", "/state", { token });
  check("新账号无记录 rev=0 payload=null", r.status === 200 && r.json.rev === 0 && r.json.payload === null);

  const p1 = { schema: 1, completed: ["m5i-l01"], stars: { "m5i-l01": 3 }, reviews: [], achievements: ["first_step"], counters: { lessonRuns: 1, reviewDone: 0 }, days: ["2026-11-03"], streak: { current: 1, max: 1, broken: false, lastDay: "2026-11-03" }, drills: [] };
  r = await call("PUT", "/state", { token, body: { rev: 0, payload: p1 } });
  check("首次写入 200 rev=1", r.status === 200 && r.json.rev === 1, JSON.stringify(r.json));

  r = await call("GET", "/state", { token });
  check("读回内容一致", r.status === 200 && r.json.rev === 1 && r.json.payload.completed[0] === "m5i-l01");

  r = await call("PUT", "/state", { token, body: { rev: 0, payload: p1 } });
  check("拿旧 rev 写入 409", r.status === 409 && r.json.rev === 1 && r.json.payload);
  check("409 带回服务端最新 payload", r.json.payload && r.json.payload.completed[0] === "m5i-l01");

  r = await call("PUT", "/state", { token, body: { rev: 1, payload: { ...p1, completed: ["m5i-l01", "m5i-l02"] } } });
  check("rev 对齐后写入成功 rev=2", r.status === 200 && r.json.rev === 2);

  const huge = { schema: 1, blob: "x".repeat(600 * 1024) };
  r = await call("PUT", "/state", { token, body: { rev: 2, payload: huge } });
  check("超 512 KB 413", r.status === 413 && r.json.error === "payload_too_large");

  console.log("\n[资料]");
  r = await call("PATCH", "/auth/profile", { token, body: { nickname: "Alex on ice" } });
  check("改昵称 200", r.status === 200 && r.json.user.nickname === "Alex on ice");
  r = await call("GET", "/auth/me", { token });
  check("昵称已持久化", r.json.user.nickname === "Alex on ice");
  r = await call("PATCH", "/auth/profile", { token, body: { nickname: "" } });
  check("空昵称 400", r.status === 400 && r.json.error === "invalid_nickname");

  console.log("\n[头像]");
  const jpeg = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(1024, 7), Buffer.from([0xff, 0xd9])]);
  r = await call("PUT", "/avatar", { token, raw: jpeg });
  check("上传 JPEG 200 version=1", r.status === 200 && r.json.version === 1, JSON.stringify(r.json));
  r = await call("GET", "/avatar", { token });
  check("取回头像 200", r.status === 200 && r.text.length > 0);
  r = await call("PUT", "/avatar", { token, raw: Buffer.from("not a jpeg at all") });
  check("非 JPEG 415", r.status === 415 && r.json.error === "unsupported_media");
  r = await call("DELETE", "/avatar", { token });
  check("删除头像 204", r.status === 204);
  r = await call("GET", "/avatar", { token });
  check("删后 404", r.status === 404);

  console.log("\n[路径前缀兼容]");
  r = await call("GET", "/ascent/auth/me", { token });
  check("/ascent 前缀也能路由", r.status === 200 && r.json.user.email === "alex@example.com");

  console.log("\n[删除账号级联]");
  r = await call("DELETE", "/auth/account", { token });
  check("删除 204", r.status === 204);
  r = await call("GET", "/auth/me", { token });
  check("旧 token 失效 401", r.status === 401);
  r = await call("POST", "/auth/login", { body: { email: "alex@example.com", password: "climb8848" } });
  check("账号已不存在 401", r.status === 401);

  const leftUsers = coll("ascent_users").length;
  const leftStates = coll("ascent_states").length;
  const leftTokens = coll("ascent_tokens").length;
  const leftAvatars = coll("ascent_avatars").length;
  check("users 归零", leftUsers === 0, "left=" + leftUsers);
  check("states 归零", leftStates === 0, "left=" + leftStates);
  check("tokens 归零", leftTokens === 0, "left=" + leftTokens);
  check("avatars 归零", leftAvatars === 0, "left=" + leftAvatars);

  console.log("\n[Google 未配置]");
  r = await call("POST", "/auth/google", { body: { idToken: "x" } });
  check("未配置 501", r.status === 501 && r.json.error === "google_not_configured");

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
