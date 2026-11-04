#!/usr/bin/env node
"use strict";

/**
 * ASCENT 备份恢复工具
 *
 *   node server/tools/restore.mjs <备份文件.asc> --dry-run
 *   node server/tools/restore.mjs <备份文件.asc> --suffix _drill   # 灌进影子表做演练
 *   node server/tools/restore.mjs <备份文件.asc> --yes              # 真的覆盖生产数据
 *
 * 密钥来源：环境变量 BACKUP_KEY，或 ~/.ascent/backup.key（一行 hex）。
 *
 * 三条安全设计：
 *  1. 默认是 dry-run —— 不带 --yes 绝不写库。
 *  2. 写库前先删后插，全部按外键顺序，并且**在同一个事务式流程里**尽量缩短空窗；
 *     中途失败会停在那张表上并打印已完成的进度，不会静默半成功。
 *  3. --suffix 只写 *_drill 影子表，用来定期演练。「没演练过的备份等于没有备份」。
 */

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import zlib from "node:zlib";

const MAGIC = Buffer.from("ASCB1", "ascii");

/** 与备份函数保持一致：父表在前，删除时反向。 */
const TABLES = [
  { name: "ascent_users", order: "id" },
  { name: "ascent_identities", order: "id" },
  { name: "ascent_credentials", order: "user_id" },
  { name: "ascent_states", order: "user_id" },
  { name: "ascent_avatars", order: "user_id" },
  { name: "ascent_sessions", order: "id" },
];

const args = process.argv.slice(2);
const file = args.find((a) => !a.startsWith("--"));
const dryRun = args.includes("--dry-run") || !args.includes("--yes");
const suffixArg = args.find((a) => a.startsWith("--suffix"));
const suffix = suffixArg
  ? suffixArg.includes("=")
    ? suffixArg.split("=")[1]
    : args[args.indexOf(suffixArg) + 1] || ""
  : "";

function die(msg) {
  console.error("✗ " + msg);
  process.exit(1);
}

if (!file) die("用法: restore.mjs <备份文件.asc> [--dry-run|--yes] [--suffix=_drill]");

// ---------------------------------------------------------------- 密钥

function loadKey() {
  if (process.env.BACKUP_KEY) return process.env.BACKUP_KEY.trim();
  const p = path.join(os.homedir(), ".ascent", "backup.key");
  if (fs.existsSync(p)) return fs.readFileSync(p, "utf8").trim();
  die("找不到密钥：设置 BACKUP_KEY，或写入 ~/.ascent/backup.key");
}

// ---------------------------------------------------------------- 解密

function open(filePath) {
  const buf = fs.readFileSync(filePath);
  if (buf.length < MAGIC.length + 28) die("文件太短，不像备份文件");
  if (!buf.subarray(0, MAGIC.length).equals(MAGIC)) die("magic 不匹配，不是 ASCB1 格式");

  const iv = buf.subarray(5, 17);
  const tag = buf.subarray(17, 33);
  const body = buf.subarray(33);
  const key = Buffer.from(loadKey(), "hex");
  if (key.length !== 32) die("密钥长度不对，应为 32 字节（64 位 hex）");

  let gz;
  try {
    const d = crypto.createDecipheriv("aes-256-gcm", key, iv);
    d.setAuthTag(tag);
    gz = Buffer.concat([d.update(body), d.final()]);
  } catch (e) {
    die("解密失败：密钥不对，或文件已损坏（GCM 校验未通过）");
  }

  try {
    return JSON.parse(zlib.gunzipSync(gz).toString("utf8"));
  } catch (e) {
    die("解压/解析失败：" + e.message);
  }
}

// ---------------------------------------------------------------- PG

const ENV_ID = process.env.TCB_ENV;
const API_KEY = process.env.CLOUDBASE_APIKEY;
if (!ENV_ID || !API_KEY) die("缺少 TCB_ENV / CLOUDBASE_APIKEY（见 ~/.ascent/cloudbase.env）");

const PG = `https://${ENV_ID}.api.tcloudbasegateway.com/v1/rdb/rest`;

async function pg(method, p, body) {
  const res = await fetch(PG + p, {
    method,
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${API_KEY}`,
      prefer: "return=minimal",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(30000),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`${method} ${p} → ${res.status} ${t.slice(0, 300)}`);
  }
  return res.status === 204 ? null : res.text();
}

async function countRows(table) {
  // 影子表名带后缀，要还原成原表名才查得到排序列；
  // credentials / states / avatars 没有 id 列，写死 select=id 会稳拿 400。
  const base = suffix && table.endsWith(suffix) ? table.slice(0, -suffix.length) : table;
  const col = TABLES.find((t) => t.name === base)?.order || "id";
  const res = await fetch(`${PG}/${table}?select=${col}&limit=1`, {
    headers: {
      authorization: `Bearer ${API_KEY}`,
      prefer: "count=exact",
      range: "0-0",
    },
    signal: AbortSignal.timeout(20000),
  });
  if (!res.ok) throw new Error(`统计 ${table} 失败：HTTP ${res.status}`);
  const cr = res.headers.get("content-range") || "";
  const n = cr.split("/")[1];
  return n && n !== "*" ? Number(n) : null;
}

function target(name) {
  return suffix ? `${name}${suffix}` : name;
}

// ---------------------------------------------------------------- 主流程

const dump = open(file);
const taken = new Date(dump.takenAt).toISOString().replace("T", " ").slice(0, 19);

console.log(`备份文件  ${file}`);
console.log(`导出时间  ${taken} UTC   schema v${dump.schema}   env=${dump.env}`);
console.log(`写入目标  ${suffix ? `影子表（*${suffix}）` : "生产表"}`);
console.log(`模式      ${dryRun ? "只读检查（不会写库）" : "★ 真实写入 ★"}`);
console.log("");
console.log("表名                        备份行数    当前行数");
console.log("─".repeat(58));

let total = 0;
let mismatch = false;
for (const t of TABLES) {
  const rows = dump.tables[t.name] || [];
  total += rows.length;
  let live = "?";
  try {
    live = await countRows(target(t.name));
  } catch (e) {
    live = "查不到";
  }
  const flag = typeof live === "number" && live !== rows.length ? "  ← 不一致" : "";
  if (flag && !suffix) mismatch = true;
  console.log(
    `${t.name.padEnd(26)} ${String(rows.length).padStart(8)} ${String(live).padStart(10)}${flag}`
  );
}
console.log("─".repeat(58));
console.log(`${"合计".padEnd(26)} ${String(total).padStart(8)}`);
console.log("");

if (dump.counts) {
  for (const [k, v] of Object.entries(dump.counts)) {
    const actual = (dump.tables[k] || []).length;
    if (actual !== v) die(`自校验失败：${k} 头部记 ${v} 行，实际 ${actual} 行`);
  }
  console.log("✓ 文件内自校验通过（头部行数与实际行数一致）\n");
}

if (dryRun) {
  console.log("只读检查结束。确认无误后加 --yes 才会真正写库。");
  process.exit(0);
}

// ---- 真实写入 ----

// 先删后插，删除按外键反序，插入按正序。中途失败会打印停在哪一步。
console.log("① 清空目标表（反外键序）");
for (const t of [...TABLES].reverse()) {
  const tbl = target(t.name);
  try {
    await pg("DELETE", `/${tbl}?${t.order}=not.is.null`);
    console.log(`   清空 ${tbl}`);
  } catch (e) {
    die(`清空 ${tbl} 失败：${e.message}`);
  }
}

console.log("② 插入数据（正外键序，每批 200 行）");
for (const t of TABLES) {
  const rows = dump.tables[t.name] || [];
  if (!rows.length) {
    console.log(`   ${target(t.name)} 无可插入行`);
    continue;
  }
  const tbl = target(t.name);
  for (let i = 0; i < rows.length; i += 200) {
    const batch = rows.slice(i, i + 200);
    try {
      await pg("POST", `/${tbl}`, batch);
    } catch (e) {
      die(`插入 ${tbl} 第 ${i + 1}–${i + batch.length} 行失败：${e.message}`);
    }
  }
  console.log(`   ${tbl} 写入 ${rows.length} 行`);
}

console.log("\n③ 复核行数");
let ok = true;
for (const t of TABLES) {
  const want = (dump.tables[t.name] || []).length;
  const got = await countRows(target(t.name));
  const same = got === want;
  if (!same) ok = false;
  console.log(`   ${same ? "✓" : "✗"} ${target(t.name)} 期望 ${want} 实际 ${got}`);
}

console.log(ok ? "\n✓ 恢复完成，行数全部对得上。" : "\n✗ 有表行数不一致，请勿对外服务。");
process.exit(ok ? 0 : 1);
