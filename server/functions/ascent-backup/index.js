"use strict";

/**
 * ASCENT 云端 · 每日备份
 *
 * 定时触发的**事件函数**（不是 HTTP，不挂网关，外网无法调用）。
 *
 * 为什么不用「升级套餐开数据回档」：那只能防误删和实例故障，
 * 防不了「环境被欠费回收」「我手滑删了环境」——而后者恰恰更致命。
 * 一份躺在对象存储里的导出，能防全部三类。
 *
 * 数据流：
 *   六张表 → PG REST 网关（keyset 分页）→ 单个 JSON
 *          → gzip → AES-256-GCM → PG 存储桶 backup/
 *          → 写 ascent_backup_log（每表行数）→ 删 30 天前的文件
 *
 * **零依赖**：PG 存储的 HTTP API（`/v1/storages/object/...`）和 PG REST 一样
 * 只要 Bearer 就够，不需要 @cloudbase/node-sdk。
 * 早期版本用 `app.uploadFile()` 会报 STORAGE_NOT_SUPPORTED —— 那是旧 COS 接口，
 * PG 环境必须走 pgstore 这套 HTTP 端点（见 storage-pg.md）。
 *
 * 环境变量：
 *   TCB_ENV            环境 ID
 *   CLOUDBASE_APIKEY   PG 网关的服务端 Key（service_role，旁路 RLS）
 *   BACKUP_KEY         32 字节 hex，AES-256-GCM 密钥。丢了备份就废了。
 */

const crypto = require("crypto");
const zlib = require("zlib");

const ENV_ID = process.env.TCB_ENV || process.env.CLOUDBASE_ENV_ID || "";
const API_KEY = process.env.CLOUDBASE_APIKEY || "";
const KEY_HEX = process.env.BACKUP_KEY || "";

const HTTP_TIMEOUT_MS = 30000;
const KEEP_DAYS = 30;
const BUCKET = "backup";
const MAGIC = Buffer.from("ASCB1", "ascii"); // 恢复脚本据此识别文件格式

/** PG REST 与 PG 存储共用同一个网关，只是路径前缀不同。 */
const GATEWAY = () => `https://${ENV_ID}.api.tcloudbasegateway.com`;
const PG_BASE = () => `${GATEWAY()}/v1/rdb/rest`;
const STORAGE_BASE = () => `${GATEWAY()}/v1/storages`;

/**
 * 每张表的读取计划。
 *
 * order 必须选**唯一且非空**的列：keyset 分页靠它推进游标，
 * 选可空列或非唯一列会漏行或重复。credentials / states / avatars 的主键是 user_id。
 *
 * page 按行宽分别设置：头像表一页 50 行就可能好几 MB，
 * 和 identities 那种几十字节一行不能用同一个值。
 */
const TABLES = [
  { name: "ascent_users", order: "id", page: 500 },
  { name: "ascent_identities", order: "id", page: 1000 },
  { name: "ascent_credentials", order: "user_id", page: 1000 },
  { name: "ascent_sessions", order: "id", page: 1000 },
  { name: "ascent_states", order: "user_id", page: 200 },
  { name: "ascent_avatars", order: "user_id", page: 50 },
];

// ---------------------------------------------------------------- HTTP

async function request(method, url, { json, body, contentType, timeout } = {}) {
  // return=representation 不能省：PostgREST 默认插入只回 201 空体，
  // 没有它就拿不到新行的 id，日志会「写成功了但不知道写的是哪一条」。
  const headers = { authorization: `Bearer ${API_KEY}`, prefer: "return=representation" };
  let payload = body;
  if (json !== undefined) {
    headers["content-type"] = "application/json";
    payload = JSON.stringify(json);
  } else if (contentType) {
    headers["content-type"] = contentType;
  }

  const res = await fetch(url, {
    method,
    headers,
    body: payload,
    signal: AbortSignal.timeout(timeout || HTTP_TIMEOUT_MS),
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`${method} ${url.replace(GATEWAY(), "")} → ${res.status} ${text.slice(0, 200)}`);
  }
  return text;
}

async function pgGet(path) {
  const text = await request("GET", PG_BASE() + path);
  return text ? JSON.parse(text) : [];
}

// ---------------------------------------------------------------- 数据读取

/**
 * keyset 分页拉全量。
 *
 * 不用 offset：offset 在大表上越翻越慢，而且备份期间有写入时，
 * 前面的行被删会导致整页前移、**悄悄漏掉一行**。keyset 只看上一页最后一行的值，
 * 漏行不会发生（新增行可能落在已读区间，但备份本来就是一个时间点附近的一致视图）。
 */
async function dumpTable(name, order, page) {
  const rows = [];
  let cursor = null;
  for (;;) {
    const params = [`select=*`, `order=${order}.asc`, `limit=${page}`];
    if (cursor) params.push(`${order}=gt.${encodeURIComponent(cursor)}`);
    const batch = await pgGet(`/${name}?${params.join("&")}`);
    if (!Array.isArray(batch) || batch.length === 0) break;
    rows.push(...batch);
    cursor = batch[batch.length - 1][order];
    if (cursor === null || cursor === undefined) {
      // 排序列出现空值说明选错了列，继续翻会原地打转，直接失败比静默漏数据好
      throw new Error(`${name}: 排序列 ${order} 出现空值`);
    }
    if (batch.length < page) break;
  }
  return rows;
}

// ---------------------------------------------------------------- 加密

function keyBuffer() {
  if (!/^[0-9a-fA-F]{64}$/.test(KEY_HEX)) {
    throw new Error("BACKUP_KEY 必须是 64 位 hex（32 字节）");
  }
  return Buffer.from(KEY_HEX, "hex");
}

/**
 * gzip → AES-256-GCM。
 * 文件布局：magic(5) | iv(12) | tag(16) | ciphertext
 * 先压缩再加密：加密后的数据压不动，顺序反了文件会大一圈。
 */
function seal(plain) {
  const gz = zlib.gzipSync(Buffer.from(plain, "utf8"), { level: 9 });
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", keyBuffer(), iv);
  const body = Buffer.concat([cipher.update(gz), cipher.final()]);
  return {
    data: Buffer.concat([MAGIC, iv, cipher.getAuthTag(), body]),
    rawBytes: gz.length,
  };
}

// ---------------------------------------------------------------- 日志

async function logStart() {
  const text = await request("POST", `${PG_BASE()}/ascent_backup_log`, {
    json: { started_at: Date.now(), status: "running" },
  });
  const rows = JSON.parse(text || "[]");
  if (!Array.isArray(rows) || !rows.length) throw new Error("写备份日志失败：未返回 id");
  return rows[0].id;
}

async function logFinish(id, patch) {
  await request("PATCH", `${PG_BASE()}/ascent_backup_log?id=eq.${id}`, {
    json: { finished_at: Date.now(), ...patch },
  });
}

/**
 * 把卡在 running 的旧行标成 failed。
 * 函数如果在 logStart 与 logFinish 之间被超时杀掉（或进程崩了），
 * 那行会永远停在 running —— 之后所有「最近一次备份成功了吗」的判断都会失真。
 */
async function closeStaleRuns() {
  const cutoff = Date.now() - 60 * 60 * 1000;
  await request(
    "PATCH",
    `${PG_BASE()}/ascent_backup_log?status=eq.running&started_at=lt.${cutoff}`,
    { json: { status: "failed", finished_at: Date.now(), error: "stale: 函数未正常收尾" } }
  ).catch(() => {});
}

// ---------------------------------------------------------------- 存储

function dateKey(offsetDays) {
  // 统一按东八区算日期，避免 UTC 跨日导致文件名和「今天」对不上
  const t = new Date(Date.now() + 8 * 3600e3 + (offsetDays || 0) * 86400e3);
  return t.toISOString().slice(0, 10);
}

async function putObject(key, buffer) {
  const url = `${STORAGE_BASE()}/object/${BUCKET}/${key}`;
  // PUT 是 upsert：同一天重跑会覆盖，不会堆出一堆副本
  await request("PUT", url, { body: buffer, contentType: "application/octet-stream", timeout: 60000 });
}

async function listObjects() {
  const out = [];
  let offset = 0;
  for (let i = 0; i < 20; i++) {
    const text = await request("POST", `${STORAGE_BASE()}/object/list/${BUCKET}`, {
      json: { limit: 100, offset },
    });
    const page = JSON.parse(text || "{}");
    const items = Array.isArray(page.objects) ? page.objects : [];
    out.push(...items);
    if (!page.hasNext) break;
    offset += items.length || 100;
  }
  return out;
}

/**
 * 按**文件名里的日期**清理，不依赖 updated_at。
 * 30 天前的备份留着只是占空间；真出事时用的一定是最近几天的那份。
 *
 * 单个删除失败不影响整体：文件已经上传成功了，不该因为一次清理 404
 * 就把这次备份记成失败 —— 那会让「备份是否可用」这个信号失真。
 */
async function pruneOld() {
  const cutoff = dateKey(-KEEP_DAYS);
  const removed = [];
  const failed = [];
  for (const obj of await listObjects()) {
    const m = /^(\d{4}-\d{2}-\d{2})\.asc$/.exec(obj.name || "");
    if (!m || m[1] >= cutoff) continue;
    try {
      await request("DELETE", `${STORAGE_BASE()}/object/${BUCKET}/${obj.name}`);
      removed.push(obj.name);
    } catch (e) {
      failed.push(`${obj.name}: ${String(e.message).slice(0, 80)}`);
    }
  }
  if (failed.length) console.warn(`[backup] prune partial: ${failed.join(" | ")}`);
  return removed;
}

// ---------------------------------------------------------------- 主流程

exports.main = async () => {
  const startedAt = Date.now();
  let logId = null;

  try {
    if (!ENV_ID || !API_KEY) throw new Error("缺少 TCB_ENV / CLOUDBASE_APIKEY");
    keyBuffer(); // 先校验密钥，别等读完六张表才发现加密配置不对

    logId = await logStart();
    await closeStaleRuns();

    const tables = {};
    const counts = {};
    for (const t of TABLES) {
      const rows = await dumpTable(t.name, t.order, t.page);
      tables[t.name] = rows;
      counts[t.name] = rows.length;
    }

    const payload = { schema: 1, takenAt: startedAt, env: ENV_ID, counts, tables };
    const { data, rawBytes } = seal(JSON.stringify(payload));

    const objectKey = `${dateKey(0)}.asc`;
    await putObject(objectKey, data);

    // 清理是「顺带做的事」，失败只记警告：备份文件已经安全落地了
    const removed = await pruneOld().catch((e) => {
      console.warn(`[backup] prune skipped: ${String(e.message).slice(0, 120)}`);
      return [];
    });

    await logFinish(logId, {
      status: "ok",
      object_key: objectKey,
      bytes: data.length,
      tables: counts,
    });

    console.log(
      `[backup] ok ${BUCKET}/${objectKey} sealed=${data.length} gzip=${rawBytes} ` +
        `rows=${JSON.stringify(counts)} pruned=${removed.length ? removed.join(",") : "-"} ` +
        `took=${Date.now() - startedAt}ms`
    );

    return {
      ok: true,
      objectKey,
      bytes: data.length,
      counts,
      pruned: removed,
      tookMs: Date.now() - startedAt,
    };
  } catch (e) {
    const message = String((e && e.message) || e).slice(0, 500);
    console.error(`[backup] failed: ${message}`);
    if (logId) {
      await logFinish(logId, { status: "failed", error: message }).catch(() => {});
    }
    // 抛出去，让定时任务把这次执行标记为失败，控制台能看见
    throw e;
  }
};
