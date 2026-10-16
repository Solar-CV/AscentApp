#!/usr/bin/env node
// 打乱算法验证（含按哈希旋转修正）：与 LessonScreen.kt 的
// shuffledSingleDisplay / shuffledOrderStart 等价实现。
// 用法：
//   node shuffle_sim.mjs --print   # 与 Java 版输出同格式（用于 diff 交叉校验）
//   node shuffle_sim.mjs --stats   # 对 bank.json 全库统计打乱后的答案位置分布
import fs from 'fs';

const MASK48 = (1n << 48n) - 1n;
const MULT = 0x5DEECE66Dn, ADD = 0xBn;

function javaHash(s) {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}

class JRandom {
  constructor(seedLong) { this.seed = ((BigInt.asUintN(64, BigInt(seedLong)) ^ MULT) & MASK48); }
  next(bits) { this.seed = (this.seed * MULT + ADD) & MASK48; return Number(this.seed >> BigInt(48 - bits)); }
  nextInt(bound) {
    let r = this.next(31);
    const m = bound - 1;
    if ((bound & m) === 0) return Math.floor((bound * r) / 0x80000000);
    let u = r;
    while (true) { const rr = u % bound; if (u - rr + m >= 0) return rr; u = this.next(31); }
  }
}

function perm(n, seedLong) {
  const rnd = new JRandom(seedLong);
  const idx = [...Array(n).keys()];
  for (let i = n - 1; i >= 1; i--) {
    const j = rnd.nextInt(i + 1);
    const t = idx[i]; idx[i] = idx[j]; idx[j] = t;
  }
  return idx;
}
const seedSingle = (hash) => BigInt.asIntN(64, BigInt(hash) * 1000003n ^ 0x5DEECE66Dn);
const seedOrder  = (hash) => BigInt.asIntN(64, BigInt(hash) * 31n ^ 0x2545F4914F6CDD1Dn);

function rotIfIdentity(hash, idx) {
  if (idx.every((v, i) => v === i)) {
    const m = idx.length - 1;
    const k = 1 + (((hash % m) + m) % m);
    for (let r = 0; r < k; r++) { const f = idx.shift(); idx.push(f); }
  }
  return idx;
}
function singleDisplay(prompt, n) {
  return perm(n, seedSingle(javaHash(prompt)));
}
function orderStart(prompt, n, correctOrder) {
  const h = javaHash(prompt);
  const idx = rotIfIdentity(h, perm(n, seedOrder(h)));
  return idx;
}

const fmt = (a) => '[' + a.join(', ') + ']';
const mode = process.argv[2];

if (mode === '--print') {
  const hashes = [0, 42, -1, 123456789, -987654321, 2147483647, -2147483648,
                  55555555, -42, 1000000007, -1000000007, 7];
  for (const h of hashes) {
    const s = perm(4, seedSingle(h));
    const o = rotIfIdentity(h, perm(4, seedOrder(h)));
    console.log(`H ${h} S ${fmt(s)} O ${fmt(o)}`);
  }
  for (const h of [7, -5, 3, 5, -3, 0]) {
    console.log(`R ${h} ${fmt(rotIfIdentity(h, [0, 1, 2, 3]))}`);
  }
} else if (mode === '--stats') {
  const bank = JSON.parse(fs.readFileSync('/Users/lolo/AscentApp/bank/bank.json', 'utf8'));
  const singles = [], steps = [], orders = [];
  for (const t of bank.tracks) for (const u of t.units) for (const l of u.lessons) {
    for (let i = 0; i < l.questions.length; i++) {
      const q = l.questions[i];
      const ref = `${l.id}#${i}`;
      if (q.type === 'single') singles.push({ ref, q });
      else if (q.type === 'order') orders.push({ ref, q });
      else if (q.type === 'scenario') {
        (q.steps || []).forEach((s, si) => { if (s.type === 'single') steps.push({ ref: `${ref}.s${si}`, q: s }); });
      }
    }
  }
  const dist = { 0: 0, 1: 0, 2: 0, 3: 0, other: 0 };
  const nonRev = singles.filter(x => !x.q.review);
  for (const { q } of nonRev) {
    const disp = singleDisplay(q.prompt, q.options.length);
    const pos = disp.indexOf(q.correct);
    if (pos in dist) dist[pos]++; else dist.other++;
  }
  const stepDist = { 0: 0, 1: 0, 2: 0, 3: 0, other: 0 };
  for (const { q } of steps) {
    const disp = singleDisplay(q.prompt, q.options.length);
    const pos = disp.indexOf(q.correct);
    if (pos in stepDist) stepDist[pos]++; else stepDist.other++;
  }
  let solvedAtStart = 0, orderRot = 0;
  for (const { q, ref } of orders) {
    const start = orderStart(q.prompt, q.options.length, q.correctOrder);
    if (start.every((v, i) => v === (q.correctOrder[i] ?? i))) solvedAtStart++;
    if (start.every((v, i) => v === i)) orderRot++;
  }
  const pct = (d, n) => `A ${(d[0] / n * 100).toFixed(1)}%  B ${(d[1] / n * 100).toFixed(1)}%  C ${(d[2] / n * 100).toFixed(1)}%  D ${(d[3] / n * 100).toFixed(1)}%`;
  console.log('== 非复习单选 ==');
  console.log('n =', nonRev.length, ' 打乱后正确答案位置分布：', pct(dist, nonRev.length), ' 其余:', dist.other);
  console.log('（修复前历史值：A 10.5% / B 84.2% / C 4.4% / D 0.8%）');
  console.log('== 情景题内单选步骤 ==');
  console.log('n =', steps.length, ' 分布：', pct(stepDist, steps.length), ' 其余:', stepDist.other);
  console.log('== 排序题 ==');
  console.log('n =', orders.length, ' 开局即正确：', solvedAtStart, '（应为 0）；因恒等被旋转修正：', orderRot);
} else {
  console.log('usage: --print | --stats');
}
