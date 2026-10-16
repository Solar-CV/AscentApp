#!/usr/bin/env node
/**
 * 生成热点题示例图（坡度与雪崩风险）。
 *
 * 为什么自己生成：热点题需要题图才能跑通「资源 → 渲染 → 点击 → 判定」整条链路，
 * 而真实图片（云图、地形图照片）需要另行采集。这张图用于验证管线本身。
 *
 * 输出：app/src/main/assets/bank/img/demo-slope.png  （800×600，4:3）
 * 用法：node tools/qbank/gen-demo-image.mjs
 */
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, '..', '..', 'app', 'src', 'main', 'assets', 'bank', 'img', 'demo-slope.png');

const W = 800, H = 600;

// ---------------- 最小画布 ----------------

const px = Buffer.alloc(W * H * 3);

function setPx(x, y, c) {
  if (x < 0 || y < 0 || x >= W || y >= H) return;
  const i = (y * W + x) * 3;
  px[i] = c[0]; px[i + 1] = c[1]; px[i + 2] = c[2];
}

function hex(s) {
  return [parseInt(s.slice(1, 3), 16), parseInt(s.slice(3, 5), 16), parseInt(s.slice(5, 7), 16)];
}

function fillAll(c) {
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) setPx(x, y, c);
}

function hLine(x0, x1, y, c) {
  for (let x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) setPx(x, y, c);
}

function line(x0, y0, x1, y1, c, w = 1) {
  const dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx - dy, x = x0, y = y0;
  for (;;) {
    for (let oy = 0; oy < w; oy++) for (let ox = 0; ox < w; ox++) setPx(x + ox, y + oy, c);
    if (x === x1 && y === y1) break;
    const e2 = 2 * err;
    if (e2 > -dy) { err -= dy; x += sx; }
    if (e2 < dx) { err += dx; y += sy; }
  }
}

/** 扫描线填充多边形 */
function fillPoly(pts, c) {
  const ys = pts.map((p) => p[1]);
  const yMin = Math.max(0, Math.min(...ys));
  const yMax = Math.min(H - 1, Math.max(...ys));
  for (let y = yMin; y <= yMax; y++) {
    const xs = [];
    for (let i = 0; i < pts.length; i++) {
      const [x1, y1] = pts[i];
      const [x2, y2] = pts[(i + 1) % pts.length];
      if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
        xs.push(x1 + ((y - y1) / (y2 - y1)) * (x2 - x1));
      }
    }
    xs.sort((a, b) => a - b);
    for (let k = 0; k + 1 < xs.length; k += 2) hLine(Math.round(xs[k]), Math.round(xs[k + 1]), y, c);
  }
}

// ---------------- PNG 编码 ----------------

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(td), 0);
  return Buffer.concat([len, td, crc]);
}

function encodePNG() {
  const raw = Buffer.alloc(H * (W * 3 + 1));
  for (let y = 0; y < H; y++) {
    raw[y * (W * 3 + 1)] = 0;
    px.copy(raw, y * (W * 3 + 1) + 1, y * W * 3, (y + 1) * W * 3);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(W, 0);
  ihdr.writeUInt32BE(H, 4);
  ihdr[8] = 8; ihdr[9] = 2; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

// ---------------- 画面内容 ----------------

const paper = hex('#F2EFE8');
const gridC = hex('#DED8CA');
const rockC = hex('#C9C2B2');
const rockD = hex('#9C947F');
const inkC = hex('#3A352C');

fillAll(paper);

// 等高参考线
for (let y = 80; y < H; y += 60) hLine(0, W - 1, y, gridC);

// 山体轮廓：三段坡度差异明显
const terrain = [
  [0, 600],
  [30, 520],
  [270, 450],   // A 段：缓（约 16°）
  [500, 260],   // B 段：中等偏陡（约 40°）—— 正确答案
  [620, 40],    // C 段：极陡（约 61°）
  [780, 40],
  [800, 600],
];
fillPoly(terrain, rockC);

// 山脊线
for (let i = 1; i < terrain.length - 2; i++) {
  line(terrain[i][0], terrain[i][1], terrain[i + 1][0], terrain[i + 1][1], rockD, 3);
}

// 三段斜面上加纹理短斜线，强化坡度差异
function hatch(x0, y0, x1, y1, count, len) {
  for (let i = 1; i <= count; i++) {
    const t = i / (count + 1);
    const x = Math.round(x0 + (x1 - x0) * t);
    const y = Math.round(y0 + (y1 - y0) * t);
    line(x, y, x + Math.round(len * 0.4), y + len, gridC);
  }
}
hatch(30, 520, 270, 450, 6, 26);
hatch(270, 450, 500, 260, 7, 30);
hatch(500, 260, 620, 40, 5, 34);

// 顶部天空标记（避免完全空白）
fillPoly([[0, 0], [800, 0], [800, 40], [620, 40], [0, 80]], hex('#E8EEF2'));

// 外框
line(0, 0, W - 1, 0, inkC, 2);
line(0, H - 2, W - 1, H - 2, inkC, 2);
line(0, 0, 0, H - 1, inkC, 2);
line(W - 2, 0, W - 2, H - 1, inkC, 2);

// ---------------- 写出 ----------------

fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, encodePNG());
const kb = (fs.statSync(OUT).size / 1024).toFixed(1);
console.log(`已生成 ${path.relative(path.resolve(HERE, '..', '..'), OUT)}  ${W}×${H}  ${kb} KB`);
console.log('');
console.log('配套的 hotspots 坐标（归一化 0..1）：');
const regions = [
  ['B 段（正确）', 385, 355, 0.13, true],
  ['A 段', 150, 485, 0.11, false],
  ['C 段', 560, 150, 0.11, false],
];
for (const [name, x, y, r, ok] of regions) {
  console.log(`  ${name.padEnd(12)} x=${(x / W).toFixed(3)} y=${(y / H).toFixed(3)} r=${r} correct=${ok}`);
}
