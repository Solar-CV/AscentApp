#!/usr/bin/env node
/**
 * 题库编写流水线： import/*.md  →  app/.../data/BookContent.kt
 *
 * 为什么是 Markdown：实体书扫描出来的内容需要人工/OCR 整理与评审，
 * Markdown 在任何编辑器里都能读、能 diff、能直接对照书页核对。
 *
 * 用法：
 *   node tools/qbank/build.mjs            # 编译
 *   node tools/qbank/build.mjs --check    # 只校验不写文件
 *
 * 格式说明见 import/README.md
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..');
const IMPORT_DIR = path.join(ROOT, 'import');
const OUT_FILE = path.join(
  ROOT, 'app', 'src', 'main', 'java', 'com', 'ascent', 'climb', 'data', 'BookContent.kt'
);

const CHECK_ONLY = process.argv.includes('--check');

const errors = [];
const warnings = [];

// ---------------------------------------------------------------- 工具

function err(file, line, msg) {
  errors.push(`${file}:${line}  ${msg}`);
}
function warn(file, line, msg) {
  warnings.push(`${file}:${line}  ${msg}`);
}

/** Kotlin 字符串字面量转义 */
function kt(s) {
  if (s == null) return 'null';
  return (
    '"' +
    String(s)
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"')
      .replace(/\$/g, '\\$')
      .replace(/\r?\n/g, '\\n') +
    '"'
  );
}

function ktList(arr) {
  if (!arr || arr.length === 0) return 'emptyList()';
  return 'listOf(\n' + arr.map((s) => '                    ' + kt(s)).join(',\n') + '\n                )';
}

/** 全角/半角冒号都能识别 */
const COLON = '[:：]';

function stripLabel(line) {
  const m = line.match(new RegExp(`^\\s*([^:：]+)${COLON}\\s*(.*)$`));
  return m ? [m[1].trim(), m[2].trim()] : null;
}

function parseAnswer(raw, optionCount, file, line) {
  const v = raw.trim().toUpperCase();
  if (/^[A-Z]$/.test(v)) {
    const idx = v.charCodeAt(0) - 65;
    if (idx < 0 || idx >= optionCount) {
      err(file, line, `答案 ${v} 超出选项范围（共 ${optionCount} 项）`);
      return -1;
    }
    return idx;
  }
  if (/^\d+$/.test(v)) {
    const n = parseInt(v, 10);
    // 1 基或 0 基都接受：若落在范围内优先按 1 基解释
    if (n >= 1 && n <= optionCount) return n - 1;
    if (n >= 0 && n < optionCount) return n;
    err(file, line, `答案 ${v} 超出选项范围（共 ${optionCount} 项）`);
    return -1;
  }
  err(file, line, `无法识别的答案标记：${raw}`);
  return -1;
}

function truthy(raw) {
  return /^(是|真|true|yes|1|y)$/i.test(String(raw).trim());
}

// ---------------------------------------------------------------- 单文件解析

function parseFile(filePath) {
  const fileName = path.basename(filePath);
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/);

  const lessons = []; // { unit, id, title, code, icon, needsCoach, book, brief[], questions[] }
  const appends = []; // { lessonId, questions[] }

  let cur = null; // 当前块
  let mode = null; // 'meta' | 'brief' | 'questions'
  let q = null; // 当前题目
  let lastField = null;
  let blockTitle = null;

  const flushQuestion = () => {
    if (!q) return;
    if (!q.prompt) {
      err(fileName, q.line, '题干为空');
    } else if (q.options.length < 2) {
      err(fileName, q.line, `选项少于 2 个（${q.options.length}）`);
    } else if (q.correct < 0) {
      // 已在 parseAnswer 里报错
    } else {
      if (!q.explanation) warn(fileName, q.line, '缺少解析（explanation）');
      q.options.forEach((o, i) => {
        if (!o) err(fileName, q.line, `第 ${i + 1} 个选项为空`);
      });
      cur.questions.push(q);
    }
    q = null;
    lastField = null;
  };

  const flushBlock = () => {
    flushQuestion();
    if (!cur) return;
    if (cur.kind === 'lesson') {
      if (!cur.unit) err(fileName, cur.line, '缺少 unit（要挂到哪个营地）');
      if (!cur.id) err(fileName, cur.line, '缺少 id');
      if (!cur.title) err(fileName, cur.line, '缺少课程标题');
      if (cur.brief.length === 0) warn(fileName, cur.line, '技能要点为空');
      if (cur.questions.length === 0) err(fileName, cur.line, '没有任何题目');
      lessons.push(cur);
    } else {
      if (!cur.lessonId) err(fileName, cur.line, '追加块缺少课程 id');
      if (cur.questions.length === 0) err(fileName, cur.line, '追加块没有任何题目');
      appends.push(cur);
    }
    cur = null;
    mode = null;
  };

  lines.forEach((rawLine, i) => {
    const lineNo = i + 1;
    const line = rawLine.replace(/\s+$/, '');

    // ---- 一级标题：开新块 ----
    const h1 = line.match(/^#\s+(.+)$/);
    if (h1) {
      flushBlock();
      blockTitle = h1[1].trim();
      const isAppend = /^追加/.test(blockTitle);
      if (isAppend) {
        // 追加 <lessonId> [· 标题]
        const rest = blockTitle.replace(/^追加\s*/, '');
        const id = rest.split(/[·•\s]/)[0].trim();
        cur = { kind: 'append', lessonId: id, questions: [], line: lineNo };
      } else {
        // 课程 <id> · <title>
        const rest = blockTitle.replace(/^课程\s*/, '');
        const parts = rest.split('·');
        const id = (parts[0] || '').trim();
        const title = parts.slice(1).join('·').trim();
        cur = {
          kind: 'lesson', id, title, unit: null, code: '', icon: '',
          needsCoach: false, book: '', brief: [], questions: [], line: lineNo,
        };
      }
      mode = 'meta';
      return;
    }

    // ---- 二级标题：切分区 ----
    const h2 = line.match(/^##\s+(.+)$/);
    if (h2) {
      if (!cur) {
        err(fileName, lineNo, '在没有一级标题的情况下出现了二级标题');
        return;
      }
      flushQuestion();
      const name = h2[1].trim();
      if (/要点|brief/i.test(name)) mode = 'brief';
      else if (/题目|questions?/i.test(name)) mode = 'questions';
      else warn(fileName, lineNo, `未知的二级标题「${name}」，已忽略`);
      return;
    }

    if (!cur) {
      if (line.trim()) {
        // 允许文件开头的注释/空行
        if (!/^<!--/.test(line.trim())) {
          warn(fileName, lineNo, '忽略标题之前的内容');
        }
      }
      return;
    }

    // ---- 元信息 ----
    if (mode === 'meta') {
      if (!line.trim()) return;
      const kv = stripLabel(line.replace(/^[-*]\s*/, ''));
      if (!kv) return;
      const [k, v] = kv;
      switch (k.toLowerCase()) {
        case 'unit': cur.unit = v; break;
        case 'id': cur.id = v; break;
        case 'code': cur.code = v; break;
        case 'icon': cur.icon = v; break;
        case 'needscoach': cur.needsCoach = truthy(v); break;
        case 'book': case '书': cur.book = v; break;
        default: warn(fileName, lineNo, `未知的元信息字段「${k}」`);
      }
      return;
    }

    // ---- 技能要点 ----
    if (mode === 'brief') {
      const b = line.match(/^\s*[-*]\s+(.+)$/);
      if (b) cur.brief.push(b[1].trim());
      else if (line.trim()) lastField = null;
      return;
    }

    // ---- 题目 ----
    if (mode === 'questions') {
      // 新题目
      const newQ = line.match(/^\s*(\d+)[.、)]\s*(.+)$/);
      if (newQ) {
        flushQuestion();
        q = { prompt: newQ[2].trim(), options: [], correct: -1, explanation: '', source: '', context: '', safety: false, line: lineNo };
        lastField = 'prompt';
        return;
      }
      if (!q) {
        if (line.trim()) warn(fileName, lineNo, '题目编号之外的内容被忽略');
        return;
      }
      // 选项
      const opt = line.match(/^\s+[-*]\s+(.+)$/);
      if (opt) {
        q.options.push(opt[1].trim());
        lastField = null;
        return;
      }
      // 带标签的字段
      const kv = stripLabel(line);
      if (kv) {
        const [k, v] = kv;
        switch (k.toLowerCase()) {
          case '答案': case 'answer': q.correct = parseAnswer(v, q.options.length, fileName, lineNo); lastField = null; return;
          case '解析': case 'explanation': q.explanation = v; lastField = 'explanation'; return;
          case '来源': case 'source': q.source = v; lastField = 'source'; return;
          case '情境': case 'context': q.context = v; lastField = 'context'; return;
          case '安全': case 'safety': q.safety = truthy(v); lastField = null; return;
          default: break;
        }
      }
      // 续行
      const cont = line.match(/^\s{2,}(\S.*)$/);
      if (cont && lastField) {
        const add = cont[1].trim();
        if (lastField === 'explanation') q.explanation += (q.explanation ? '\n' : '') + add;
        else if (lastField === 'source') q.source += ' ' + add;
        else if (lastField === 'context') q.context += ' ' + add;
        else if (lastField === 'prompt') q.prompt += add;
        return;
      }
      if (line.trim()) warn(fileName, lineNo, `无法识别的内容：${line.trim().slice(0, 30)}…`);
    }
  });

  flushBlock();
  return { lessons, appends };
}

// ---------------------------------------------------------------- 主流程

if (!fs.existsSync(IMPORT_DIR)) {
  console.error(`找不到目录：${IMPORT_DIR}`);
  process.exit(1);
}

const mdFiles = fs
  .readdirSync(IMPORT_DIR)
  .filter((f) => f.endsWith('.md'))
  .filter((f) => f.toLowerCase() !== 'readme.md')
  .filter((f) => !f.startsWith('_')) // _ 开头为草稿 / 测试文件，不参与编译
  .sort();

if (mdFiles.length === 0) {
  console.log('import/ 目录下没有待编译的 .md 文件。');
  console.log('（README.md 与 _ 开头的草稿文件会被跳过）');
  console.log('目前将生成空的 BookContent.kt。');
}

let nextId = 1;
const byUnit = new Map(); // unitId -> lessons[]
const byLesson = new Map(); // lessonId -> questions[]

for (const f of mdFiles) {
  const { lessons, appends } = parseFile(path.join(IMPORT_DIR, f));

  for (const l of lessons) {
    if (!l.id || /^auto$/i.test(l.id)) l.id = `b${nextId++}`;
    if (!byUnit.has(l.unit)) byUnit.set(l.unit, []);
    byUnit.get(l.unit).push(l);
  }
  for (const a of appends) {
    if (!byLesson.has(a.lessonId)) byLesson.set(a.lessonId, []);
    byLesson.get(a.lessonId).push(...a.questions);
  }
}

// ---- 校验 ----

function questionToKt(q, indent) {
  const pad = ' '.repeat(indent);
  const lines = [];
  lines.push(`${pad}Question(`);
  lines.push(`${pad}    prompt = ${kt(q.prompt)},`);
  if (q.context) lines.push(`${pad}    context = ${kt(q.context)},`);
  lines.push(`${pad}    options = listOf(`);
  q.options.forEach((o) => lines.push(`${pad}        ${kt(o)},`));
  lines.push(`${pad}    ),`);
  lines.push(`${pad}    correct = ${q.correct},`);
  lines.push(`${pad}    explanation = ${kt(q.explanation)},`);
  if (q.safety) lines.push(`${pad}    safetyCritical = true,`);
  if (q.source) lines.push(`${pad}    source = ${kt(q.source)},`);
  lines.push(`${pad})`);
  return lines.join('\n');
}

let out = '';
out += 'package com.ascent.climb.data\n\n';
out += '/**\n';
out += ' * 由实体书扫描资料整理而来的题库内容。\n';
out += ' *\n';
out += ' * ⚠️ 本文件由 `tools/qbank/build.mjs` 自动生成，请勿手动编辑。\n';
out += ' *     源文件在 `import/` 目录（Markdown），编辑那里的内容后重新运行：\n';
out += ' *\n';
out += ' *         node tools/qbank/build.mjs\n';
out += ' *\n';
out += ` * 本次来源：${mdFiles.length === 0 ? '（暂无）' : mdFiles.join(', ')}\n`;
out += ' */\n\n';

// BOOK_LESSONS
out += '/** 新增课程，按营地 id 分组。 */\n';
if (byUnit.size === 0) {
  out += 'val BOOK_LESSONS: Map<String, List<Lesson>> = emptyMap()\n\n';
} else {
  out += 'val BOOK_LESSONS: Map<String, List<Lesson>> = mapOf(\n';
  const units = [...byUnit.keys()].sort();
  units.forEach((u, ui) => {
    out += `    ${kt(u)} to listOf(\n`;
    byUnit.get(u).forEach((l, li) => {
      out += '        Lesson(\n';
      out += `            id = ${kt(l.id)},\n`;
      out += `            title = ${kt(l.title)},\n`;
      out += `            code = ${kt(l.code || l.id.toUpperCase())},\n`;
      out += `            icon = ${kt(l.icon || '\u{1F4D8}')},\n`;
      out += `            brief = ${ktList(l.brief)},\n`;
      if (l.needsCoach) out += '            needsCoach = true,\n';
      out += '            questions = listOf(\n';
      out += l.questions.map((q) => questionToKt(q, 16) + ',').join('\n');
      out += '\n            )\n';
      out += '        )';
      out += li === byUnit.get(u).length - 1 ? '\n' : ',\n';
    });
    out += '    )';
    out += ui === units.length - 1 ? '\n' : ',\n';
  });
  out += ')\n\n';
}

// BOOK_QUESTIONS
out += '/** 追加到已有课程的题目，按课程 id 分组。 */\n';
if (byLesson.size === 0) {
  out += 'val BOOK_QUESTIONS: Map<String, List<Question>> = emptyMap()\n';
} else {
  out += 'val BOOK_QUESTIONS: Map<String, List<Question>> = mapOf(\n';
  const ids = [...byLesson.keys()].sort();
  ids.forEach((id, i) => {
    out += `    ${kt(id)} to listOf(\n`;
    out += byLesson.get(id).map((q) => questionToKt(q, 8) + ',').join('\n');
    out += '\n    )';
    out += i === ids.length - 1 ? '\n' : ',\n';
  });
  out += ')\n';
}

// ---- 报告 ----
const lessonCount = [...byUnit.values()].reduce((s, a) => s + a.length, 0);
const questionCount =
  [...byUnit.values()].reduce((s, a) => s + a.reduce((t, l) => t + l.questions.length, 0), 0) +
  [...byLesson.values()].reduce((s, a) => s + a.length, 0);

console.log(`源文件 ${mdFiles.length} 个`);
console.log(`新增课程 ${lessonCount} 门，追加题目 ${questionCount} 道`);

if (warnings.length) {
  console.log(`\n警告 ${warnings.length} 条：`);
  warnings.forEach((w) => console.log('  ⚠ ' + w));
}

if (errors.length) {
  console.log(`\n错误 ${errors.length} 条：`);
  errors.forEach((e) => console.log('  ✗ ' + e));
  console.log('\n未写出文件。修好以上问题后重试。');
  process.exit(1);
}

if (CHECK_ONLY) {
  console.log('\n--check 模式，未写出文件。');
  process.exit(0);
}

fs.writeFileSync(OUT_FILE, out);
console.log(`\n已写出 ${path.relative(ROOT, OUT_FILE)}`);
