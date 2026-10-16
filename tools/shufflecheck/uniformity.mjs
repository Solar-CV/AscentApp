#!/usr/bin/env node
// 1) 情景题明细（新旋转算法）
// 2) 大样本均匀性：对 200000 个随机 prompt，统计「下标 1 / 下标 0」在打乱后落在各位置的频率
import fs from 'fs';
const MASK48 = (1n << 48n) - 1n, MULT = 0x5DEECE66Dn, ADD = 0xBn;
function javaHash(s) { let h = 0; for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0; return h; }
class JRandom {
  constructor(seedLong) { this.seed = ((BigInt.asUintN(64, BigInt(seedLong)) ^ MULT) & MASK48); }
  next(bits) { this.seed = (this.seed * MULT + ADD) & MASK48; return Number(this.seed >> BigInt(48 - bits)); }
  nextInt(bound) { let r = this.next(31); const m = bound - 1; if ((bound & m) === 0) return Math.floor((bound * r) / 0x80000000); let u = r; while (true) { const rr = u % bound; if (u - rr + m >= 0) return rr; u = this.next(31); } }
}
function perm(n, seedLong) { const rnd = new JRandom(seedLong); const idx = [...Array(n).keys()]; for (let i = n - 1; i >= 1; i--) { const j = rnd.nextInt(i + 1); const t = idx[i]; idx[i] = idx[j]; idx[j] = t; } return idx; }
const seedSingle = (hash) => BigInt.asIntN(64, BigInt(hash) * 1000003n ^ 0x5DEECE66Dn);
function rotIfIdentity(hash, idx) { if (idx.every((v, i) => v === i)) { const m = idx.length - 1; const k = 1 + (((hash % m) + m) % m); for (let r = 0; r < k; r++) { const f = idx.shift(); idx.push(f); } } return idx; }
function singleDisplay(prompt, n) { return perm(n, seedSingle(javaHash(prompt))); }

// ---- 明细 ----
const bank = JSON.parse(fs.readFileSync('/Users/lolo/AscentApp/bank/bank.json', 'utf8'));
const steps = [];
for (const t of bank.tracks) for (const u of t.units) for (const l of u.lessons) for (let i = 0; i < l.questions.length; i++) {
  const q = l.questions[i];
  if (q.type === 'scenario') (q.steps || []).forEach((s, si) => { if (s.type === 'single') steps.push({ ref: `${l.id}#${i}.s${si}`, s }); });
}
console.log('== 情景题明细（新算法） ==');
const seen = new Set();
for (const { ref, s } of steps) {
  const disp = singleDisplay(s.prompt, s.options.length);
  const pos = disp.indexOf(s.correct);
  const dup = seen.has(s.prompt) ? '(副本)' : '';
  seen.add(s.prompt);
  console.log(`${ref} call=${s.correct} pos=${pos} [${disp}] ${dup} ${s.prompt.slice(0, 12)}`);
}

// ---- 均匀性大样本 ----
console.log('\n== 均匀性测试：200000 个随机 prompt ==');
const N = 200000;
const pos1 = [0, 0, 0, 0], pos0 = [0, 0, 0, 0];
let rotCount = 0;
for (let i = 0; i < N; i++) {
  const prompt = '随机题面 ' + i + ' ' + Math.floor(Math.random() * 1e9);
  const h = javaHash(prompt);
  const disp = singleDisplay(prompt, 4);
  if (disp.every((v, k) => v === k)) rotCount++; // 单选允许恒等（约 1/24）
  pos1[disp.indexOf(1)]++;
  pos0[disp.indexOf(0)]++;
}
console.log('值 1 的位置分布：', pos1.map(x => (x / N * 100).toFixed(2) + '%').join(' / '));
console.log('值 0 的位置分布：', pos0.map(x => (x / N * 100).toFixed(2) + '%').join(' / '));
console.log('恒等出现率：', (rotCount / N * 100).toFixed(2) + '%（单选允许，期望≈4.17%）');
