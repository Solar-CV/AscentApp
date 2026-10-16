#!/usr/bin/env node
/**
 * 纸质资料知识库工具
 *
 *   node tools/kb.mjs build              重建 docs/ + catalog.json + topics/
 *   node tools/kb.mjs stats              覆盖度报告
 *   node tools/kb.mjs search <关键词>     全文检索（可加 --book 05 --limit 30）
 *   node tools/kb.mjs page <书号|书名> <页> 打印某一页原文
 *
 * 数据来源：
 *   ~/技能书/校对版/*.md   7 本扫描校对稿
 *   ~/技能书/*.docx        3 份我方内部资料
 */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const KB = path.resolve(HERE, '..');
const HOME = os.homedir();
const BOOK_DIR = path.join(HOME, '技能书', '校对版');
const OWN_DIR = path.join(HOME, '技能书');
const DOCS = path.join(KB, 'docs');
const TOPICS = path.join(KB, 'topics');

// ---------------------------------------------------------------- 书目

const BOOKS = [
  { id: '01', title: '了解高原病', kind: 'own', docx: '了解高原病.docx',
    note: 'SEVEN SUMMIT TREKS 内部向导资料：AMS / HACE / HAPE 的识别、预防与处置' },
  { id: '02', title: '装备指南', kind: 'own', docx: '装备指南.docx',
    note: 'SEVEN SUMMIT TREKS 6000/7000/8000 米装备清单（头饰、穿着、手套、鞋类、技术器材、睡眠、通讯）' },
  { id: '03', title: '探险活动通用指南', kind: 'own', docx: '探险活动通用指南.docx',
    note: 'SEVEN SUMMIT TREKS 行前与行程条款（保险、航班、行李、营地纪律、登顶证书）' },
  { id: '04', title: '高山探险', kind: 'book', file: '高山探险-校对版.md', pages: 313,
    quality: '双引擎一致率 82.8%；38 张表格已全部录入',
    note: '社会体育指导员国家职业资格培训教材，体系最完整' },
  { id: '05', title: '登山手册', kind: 'book', file: '登山手册-校对版.md', pages: 321,
    quality: '一致率 71.9%', note: '篇幅最大的技术手册' },
  { id: '06', title: '登山户外安全手册', kind: 'book', file: '登山户外安全手册-校对版.md', pages: 191,
    quality: '一致率 68.2%；图多，表格与图注为残留风险区', note: '中国登山协会，急救与安全为主' },
  { id: '07', title: '登山户外安全手册2', kind: 'book', file: '登山户外安全手册2-校对版.md', pages: 131,
    quality: '一致率 75.6%', note: '同上系列' },
  { id: '08', title: '行走在高海拔', kind: 'book', file: '行走在高海拔-结构化版.md', pages: 129,
    quality: '一致率 68.2%；已结构化，50 张表格', note: '高原病与高海拔健康专书' },
  { id: '09', title: '户外领导力', kind: 'book', file: '户外领导力-校对版.md', pages: 251,
    quality: '一致率 87.6%', note: '团队、决策、风险管理' },
  { id: '10', title: '手册合集', kind: 'book', file: '手册合集-校对版.md', pages: 20,
    quality: '人工精校版', note: '中国登山协会「全国户外安全教育计划」折页汇编' },
];

// ---------------------------------------------------------------- 文本工具

/** 从 docx 提取纯文本（zip + word/document.xml） */
function readDocx(file) {
  const xml = execFileSync('/bin/bash', ['-c', `unzip -p ${JSON.stringify(file)} word/document.xml`], {
    maxBuffer: 128 * 1024 * 1024,
  }).toString('utf8');
  let s = xml;
  s = s.replace(/<w:tab\b[^>]*\/>/g, '\t');
  s = s.replace(/<w:br\b[^>]*\/>/g, '\n');
  s = s.replace(/<\/w:p>/g, '\n');
  s = s.replace(/<\/w:tc>/g, ' | ');
  s = s.replace(/<\/w:tr>/g, '\n');
  s = s.replace(/<[^>]+>/g, '');
  s = s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'").replace(/&amp;/g, '&');
  return s.replace(/[ \t]+\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
}

/** 拆成「页」；支持 <!-- p.N --> 与 ## 第 N 页 两种约定 */
function splitPages(text) {
  const pages = [];
  const reComment = /<!--\s*p\.(\d+)\s*-->/g;
  const reHeading = /^##\s*第\s*(\d+)\s*页\s*$/gm;
  const useComment = reComment.test(text);
  const re = useComment ? reComment : reHeading;
  re.lastIndex = 0;
  const marks = [];
  let m;
  while ((m = re.exec(text)) !== null) marks.push({ n: parseInt(m[1], 10), at: m.index, end: m.index + m[0].length });
  if (marks.length === 0) return [{ n: 0, text: text.trim() }];
  for (let i = 0; i < marks.length; i++) {
    const to = i + 1 < marks.length ? marks[i + 1].at : text.length;
    pages.push({ n: marks[i].n, text: text.slice(marks[i].end, to).trim() });
  }
  return pages;
}

/** OCR 噪声评分：封面、目录、表格串行等页面的短行比例很高 */
function noiseScore(text) {
  const lines = text.split('\n').map((l) => l.trim()).filter(Boolean);
  if (lines.length === 0) return 1;
  const short = lines.filter((l) => l.length < 8).length / lines.length;
  const cjk = (text.match(/[\u4e00-\u9fff]/g) || []).length;
  const body = text.replace(/\s/g, '').length || 1;
  return Math.min(1, short * 0.75 + (1 - cjk / body) * 0.5);
}

const NOISY_THRESHOLD = 0.55;

/** 页 → 段落
 *  md 校对稿：段落之间有空行，直接按空行切。
 *  docx：逐行输出、几乎没有空行，需要按行切并用句末/行长发点合并成段。
 */
function toParagraphs(pageText) {
  const out = [];
  const push = (t) => { if (t.length >= 12) out.push(t); };
  for (const block of pageText.split(/\n\s*\n/)) {
    const lines = block.split('\n').map((l) => l.trim()).filter(Boolean);
    if (lines.length === 0) continue;
    if (lines.length <= 2) {
      push(lines.join(' ').replace(/\s{2,}/g, ' ').trim());
      continue;
    }
    let buf = [];
    for (const l of lines) {
      buf.push(l);
      if (l.length >= 40 || /[。！？；：]$/.test(l)) {
        push(buf.join('').replace(/\s{2,}/g, ' ').trim());
        buf = [];
      }
    }
    if (buf.length) push(buf.join(' ').replace(/\s{2,}/g, ' ').trim());
  }
  return out;
}

// ---------------------------------------------------------------- 载入

function loadAll() {
  const docs = [];
  for (const b of BOOKS) {
    let raw, pages;
    if (b.kind === 'own') {
      const f = path.join(OWN_DIR, b.docx);
      if (!fs.existsSync(f)) { console.warn(`跳过（找不到）：${f}`); continue; }
      raw = readDocx(f);
      pages = [{ n: 0, text: raw }];
    } else {
      const f = path.join(BOOK_DIR, b.file);
      if (!fs.existsSync(f)) { console.warn(`跳过（找不到）：${f}`); continue; }
      raw = fs.readFileSync(f, 'utf8');
      pages = splitPages(raw);
    }
    const scored = pages.map((p) => ({ ...p, noise: noiseScore(p.text) }));
    const paragraphs = [];
    for (const p of scored) {
      if (p.noise >= NOISY_THRESHOLD) continue;
      for (const t of toParagraphs(p.text)) paragraphs.push({ book: b.id, title: b.title, page: p.n, text: t });
    }
    docs.push({ book: b, pages: scored, paragraphs });
  }
  return docs;
}

// ---------------------------------------------------------------- build

function cmdBuild() {
  const docs = loadAll();
  fs.mkdirSync(DOCS, { recursive: true });
  fs.mkdirSync(TOPICS, { recursive: true });

  const catalog = [];

  for (const d of docs) {
    const b = d.book;
    const totalChars = d.pages.reduce((s, p) => s + p.text.length, 0);
    const noisy = d.pages.filter((p) => p.noise >= NOISY_THRESHOLD).length;

    // ---- docs/NN-书名.md ----
    const out = [];
    out.push(`# ${b.title}\n`);
    const src = b.kind === 'own' ? `~/技能书/${b.docx}` : `~/技能书/校对版/${b.file}`;
    out.push(`> 来源：${src}`);
    out.push(`> 类型：${b.kind === 'own' ? '我方内部资料' : '纸质书扫描校对稿'}`);
    if (b.pages) out.push(`> 页数：${b.pages}`);
    if (b.quality) out.push(`> 质量注记：${b.quality}`);
    if (b.note) out.push(`> 内容：${b.note}`);
    out.push(`> 有效段落：${d.paragraphs.length}　｜　字符数：${totalChars}`);
    out.push(`\n---\n`);
    for (const p of d.pages) {
      if (b.kind === 'own') {
        out.push(p.text);
      } else {
        const flag = p.noise >= NOISY_THRESHOLD ? ' <!-- noisy -->' : '';
        out.push(`<!-- page:${p.n} -->${flag}\n`);
        out.push(p.text);
        out.push('');
      }
    }
    fs.writeFileSync(path.join(DOCS, `${b.id}-${b.title.replace(/[（）()·、]/g, '')}.md`), out.join('\n'));

    catalog.push({
      id: b.id, title: b.title, kind: b.kind,
      pages: b.kind === 'own' ? null : (b.pages ?? d.pages.length),
      paragraphs: d.paragraphs.length,
      chars: totalChars,
      noisyPages: b.kind === 'own' ? 0 : noisy,
      quality: b.quality ?? null,
      note: b.note ?? null,
      file: `docs/${b.id}-${b.title.replace(/[（）()·、]/g, '')}.md`,
    });
  }

  fs.writeFileSync(path.join(KB, 'catalog.json'), JSON.stringify({
    generatedAt: new Date().toISOString(),
    bookCount: catalog.length,
    totalPages: catalog.reduce((s, c) => s + (c.pages || 0), 0),
    totalParagraphs: catalog.reduce((s, c) => s + c.paragraphs, 0),
    books: catalog,
  }, null, 2));

  console.log(`docs/ 已写出 ${catalog.length} 份`);
  for (const c of catalog) {
    console.log(`  ${c.id} ${c.title.padEnd(22)} ${String(c.pages ?? '-').padStart(4)} 页  ${String(c.paragraphs).padStart(6)} 段  ${String(Math.round(c.chars / 1024)).padStart(5)} KB  噪声页 ${c.noisyPages}`);
  }

  buildTopics(docs);
}

// ---------------------------------------------------------------- topics

function cmdBuildTopicsOnly() {
  const docs = loadAll();
  buildTopics(docs);
}

function buildTopics(docs) {
  const tax = JSON.parse(fs.readFileSync(path.join(KB, 'taxonomy.json'), 'utf8'));
  const groups = [
    ...tax.units.map((u) => ({ unit: u.title, id: u.id, topics: u.topics })),
    { unit: '跨主题', id: 'x', topics: tax.crossTopics },
  ];

  const all = docs.flatMap((d) => d.paragraphs);
  const coverage = [];

  for (const g of groups) {
    for (const t of g.topics) {
      const hits = [];
      for (const p of all) {
        let score = 0;
        for (const k of t.keywords) {
          if (p.text.includes(k)) score += k.length >= 4 ? 2 : 1;
        }
        if (score > 0) hits.push({ ...p, score });
      }
      hits.sort((a, b) => b.score - a.score || a.book.localeCompare(b.book) || a.page - b.page);

      // 每本书最多 10 段，总量最多 70 段
      const perBook = new Map();
      const picked = [];
      for (const h of hits) {
        const n = perBook.get(h.book) || 0;
        if (n >= 10) continue;
        perBook.set(h.book, n + 1);
        picked.push(h);
        if (picked.length >= 70) break;
      }
      picked.sort((a, b) => a.book.localeCompare(b.book) || a.page - b.page);

      const out = [];
      out.push(`# ${t.title}\n`);
      out.push(`> 主题编号：${t.id}　｜　所属：${g.unit}`);
      out.push(`> 命中段落：${hits.length}　｜　本汇编收录：${picked.length}（每本书最多 10 段）`);
      out.push(`> 关键词：${t.keywords.join('、')}`);
      out.push(`\n---\n`);
      let curBook = null;
      for (const p of picked) {
        if (p.book !== curBook) {
          curBook = p.book;
          const cnt = picked.filter((x) => x.book === curBook).length;
          out.push(`\n## 《${p.title}》　（${cnt} 段）\n`);
        }
        const cite = p.page > 0 ? `《${p.title}》p.${p.page}` : `《${p.title}》`;
        out.push(`**[${cite}]**　${p.text.length > 600 ? p.text.slice(0, 600) + '…' : p.text}\n`);
      }
      if (picked.length === 0) out.push(`_（暂无命中段落 —— 这个主题可能需要补充资料来源）_\n`);

      const fname = `${t.id}-${t.title.replace(/[：:/／\s]/g, '-')}.md`;
      fs.writeFileSync(path.join(TOPICS, fname), out.join('\n'));

      const byBook = {};
      for (const p of picked) byBook[p.book] = (byBook[p.book] || 0) + 1;
      coverage.push({ id: t.id, title: t.title, unit: g.unit, hits: hits.length, picked: picked.length, byBook });
    }
  }

  fs.writeFileSync(path.join(KB, 'topics', '_coverage.json'), JSON.stringify(coverage, null, 2));
  console.log(`\ntopics/ 已写出 ${coverage.length} 份主题汇编`);
}

// ---------------------------------------------------------------- stats

function cmdStats() {
  const cov = JSON.parse(fs.readFileSync(path.join(TOPICS, '_coverage.json'), 'utf8'));
  const cat = JSON.parse(fs.readFileSync(path.join(KB, 'catalog.json'), 'utf8'));
  const title = Object.fromEntries(cat.books.map((b) => [b.id, b.title]));

  console.log('=== 各主题资料厚度（收录/命中 段数）===\n');
  let curUnit = null;
  for (const c of cov) {
    if (c.unit !== curUnit) { curUnit = c.unit; console.log(`\n【${curUnit}】`); }
    const bar = '█'.repeat(Math.min(28, Math.round(c.picked / 2)));
    const books = Object.entries(c.byBook).sort().map(([k, v]) => `${k}:${v}`).join(' ');
    const bookCount = Object.keys(c.byBook).length;
    const warn = c.picked < 15 ? '  ⚠ 收录偏少' : (bookCount <= 2 ? '  ⚠ 仅 2 本书覆盖' : '');
    console.log(`  ${c.id.padEnd(4)} ${c.title.padEnd(24)} ${String(c.picked).padStart(3)}/${String(c.hits).padStart(4)} 段 ${bar}${warn}`);
    console.log(`       ${books}`);
  }

  console.log('\n=== 书目 ===\n');
  for (const b of cat.books) {
    console.log(`  ${b.id} ${b.title.padEnd(22)} ${String(b.pages ?? '-').padStart(4)} 页  ${String(b.paragraphs).padStart(6)} 段`);
  }
  console.log(`\n  合计 ${cat.totalPages} 页 · ${cat.totalParagraphs} 段`);
}

// ---------------------------------------------------------------- search

function loadParagraphsFromDocs() {
  const out = [];
  for (const f of fs.readdirSync(DOCS).filter((f) => f.endsWith('.md')).sort()) {
    const bookId = f.slice(0, 2);
    const title = f.slice(3, -3);
    let page = 0;
    const text = fs.readFileSync(path.join(DOCS, f), 'utf8');
    for (const line of text.split('\n')) {
      const pm = line.match(/<!--\s*page:(\d+)\s*-->/);
      if (pm) { page = parseInt(pm[1], 10); continue; }
      if (line.startsWith('#') || line.startsWith('>') || line.trim() === '---' || !line.trim()) continue;
      if (line.length < 12) continue;
      out.push({ bookId, title, page, text: line.trim() });
    }
  }
  return out;
}

function cmdSearch(query, opts) {
  const all = loadParagraphsFromDocs();
  const limit = opts.limit || 40;
  const terms = query.split(/\s+/).filter(Boolean);
  const hits = [];
  for (const p of all) {
    if (opts.book && p.bookId !== opts.book) continue;
    let score = 0;
    for (const t of terms) if (p.text.includes(t)) score += 1;
    if (score === terms.length) hits.push({ ...p, score });
  }
  hits.sort((a, b) => b.score - a.score || a.bookId.localeCompare(b.bookId) || a.page - b.page);
  console.log(`命中 ${hits.length} 段${opts.book ? `（限书 ${opts.book}）` : ''}，显示前 ${Math.min(limit, hits.length)} 段：\n`);
  for (const h of hits.slice(0, limit)) {
    const cite = h.page > 0 ? `《${h.title}》p.${h.page}` : `《${h.title}》`;
    console.log(`[${h.bookId}] ${cite}`);
    console.log(`    ${h.text.length > 320 ? h.text.slice(0, 320) + '…' : h.text}\n`);
  }
}

// ---------------------------------------------------------------- page

function cmdPage(which, pageNo) {
  const cat = JSON.parse(fs.readFileSync(path.join(KB, 'catalog.json'), 'utf8'));
  const b = cat.books.find((x) => x.id === which || x.title.includes(which));
  if (!b) { console.error(`找不到书：${which}（可用：${cat.books.map((x) => x.id + ' ' + x.title).join(' / ')}）`); process.exit(1); }
  const text = fs.readFileSync(path.join(KB, b.file), 'utf8');
  const re = new RegExp(`<!-- page:${pageNo} -->([\\s\\S]*?)(?=<!-- page:|$)`);
  const m = text.match(re);
  if (!m) { console.error(`《${b.title}》找不到第 ${pageNo} 页`); process.exit(1); }
  console.log(`=== 《${b.title}》 第 ${pageNo} 页 ===\n`);
  console.log(m[1].replace(/<!-- noisy -->/, '（OCR 噪声页，原文可能不可靠）').trim());
}

// ---------------------------------------------------------------- 入口

const [cmd, ...rest] = process.argv.slice(2);
const flags = {};
const args = [];
for (let i = 0; i < rest.length; i++) {
  if (rest[i] === '--book') flags.book = rest[++i];
  else if (rest[i] === '--limit') flags.limit = parseInt(rest[++i], 10);
  else args.push(rest[i]);
}

switch (cmd) {
  case 'build': cmdBuild(); break;
  case 'topics': cmdBuildTopicsOnly(); break;
  case 'stats': cmdStats(); break;
  case 'search': if (!args[0]) { console.error('用法：node tools/kb.mjs search <关键词> [--book 05] [--limit 30]'); process.exit(1); } cmdSearch(args.join(' '), flags); break;
  case 'page': if (!args[0] || !args[1]) { console.error('用法：node tools/kb.mjs page <书号|书名> <页>'); process.exit(1); } cmdPage(args[0], parseInt(args[1], 10)); break;
  default:
    console.log(`纸质资料知识库工具

  node tools/kb.mjs build              重建 docs/ + catalog.json + topics/
  node tools/kb.mjs topics             只重建 topics/
  node tools/kb.mjs stats              覆盖度报告
  node tools/kb.mjs search <词> [--book 05] [--limit 30]
  node tools/kb.mjs page <书号|书名> <页>`);
}
