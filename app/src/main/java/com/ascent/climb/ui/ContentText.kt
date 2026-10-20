package com.ascent.climb.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ascent.climb.data.i18n.ContentPack

/**
 * 非 Composable 场景（搜索过滤、列表预处理）使用的取词函数。
 * 与 [ctext] 同一条查找链：当前语言 → 英文 → 原文。
 */
fun localized(context: Context, key: String, original: String): String {
    val locales = context.resources.configuration.locales
    if (locales.isEmpty) return original
    return ContentPack.get(locales[0].toLanguageTag(), key) ?: original
}

/**
 * 内容层文案（课程名、成就名、技能书标题等）。
 *
 * 与 `stringResource` 的区别：这些文本来自数据层，不在资源文件里，
 * 所以走 [ContentPack] 覆盖表。当前语言没有译文时回退英文。
 *
 * 读的是 `LocalContext` 的语言，而 MainActivity 已经把 LocalContext
 * 换成了按用户偏好创建的 Context，因此切换语言会立即生效。
 */
@Composable
fun ctext(key: String, original: String): String {
    val locales = LocalContext.current.resources.configuration.locales
    if (locales.isEmpty) return original
    return ContentPack.get(locales[0].toLanguageTag(), key) ?: original
}

/** 课程标题。 */
@Composable
fun lessonTitle(id: String, original: String): String = ctext("lesson.$id.title", original)

/** 单元标题。 */
@Composable
fun unitTitle(id: String, original: String): String = ctext("unit.$id.title", original)

/** 成就名称。 */
@Composable
fun achievementTitle(id: String, original: String): String = ctext("ach.$id.title", original)

/** 成就描述。 */
@Composable
fun achievementDesc(id: String, original: String): String = ctext("ach.$id.desc", original)

/** 技能书标题。 */
@Composable
fun articleTitle(id: String, original: String): String = ctext("hb.$id.title", original)

/** 技能书摘要。 */
@Composable
fun articleSummary(id: String, original: String): String = ctext("hb.$id.summary", original)

/** 技能书分类（数据层用中文作分类键）。 */
@Composable
fun categoryName(original: String): String = ctext("cat.$original", original)

/** 稀有度（普通 / 稀有 / 史诗 / 传说）。 */
@Composable
fun rarityName(r: com.ascent.climb.data.Rarity): String = ctext("rarity.${r.name}", r.label)

/** 成就分类（课程进度 / 学习习惯 / 学习深度 / 探索里程）。 */
@Composable
fun achCategoryName(c: com.ascent.climb.data.AchCategory): String = ctext("acat.${c.name}", c.label)

/** 目标山峰名（专有名词按语言转写）。 */
@Composable
fun peakName(original: String): String = ctext("peak.$original", original)

/** 课程要点（`brief` 列表的第 [index] 条，从 1 开始）。 */
@Composable
fun briefText(lessonId: String, index: Int, original: String): String =
    ctext("lesson.$lessonId.brief.$index", original)

/** 技能书章节标题（`sections` 的第 [index] 个，从 1 开始）。 */
@Composable
fun articleSectionHeading(articleId: String, index: Int, original: String): String =
    ctext("hb.$articleId.s$index.h", original)

/** 技能书章节正文。 */
@Composable
fun articleSectionBody(articleId: String, index: Int, original: String): String =
    ctext("hb.$articleId.s$index.b", original)

// ---------------- 题库 ----------------
//
// key 结构：q.<questionId>.p / .c / .o<n> / .e （n 从 0 开始）
// 配对题左右列：.l<n> / .r<n>；填空答案：.a<n>
// 情景题子问题：q.<questionId>.s<stepIndex>.p / .o<n> / .e（stepIndex 从 0 开始）

/** 题干。 */
@Composable
fun qPrompt(q: com.ascent.climb.data.Question): String = ctext("q.${q.id}.p", q.prompt)

/** 情境背景（情景题/材料题）。 */
@Composable
fun qContext(q: com.ascent.climb.data.Question): String {
    val original = q.context ?: return ""
    return ctext("q.${q.id}.c", original)
}

/** 选项（单选题与多选题）。 */
@Composable
fun qOption(q: com.ascent.climb.data.Question, index: Int): String =
    ctext("q.${q.id}.o$index", q.options[index])

/** 解析。 */
@Composable
fun qExplanation(q: com.ascent.climb.data.Question): String =
    ctext("q.${q.id}.e", q.explanation)

/** 配对题左列。 */
@Composable
fun qPairLeft(q: com.ascent.climb.data.Question, index: Int): String =
    ctext("q.${q.id}.l$index", q.pairsLeft[index])

/** 填空题的可接受答案（第 [index] 个）。 */
@Composable
fun qAnswer(q: com.ascent.climb.data.Question, index: Int): String =
    ctext("q.${q.id}.a$index", q.answers[index])

/** 配对题右列。 */
@Composable
fun qPairRight(q: com.ascent.climb.data.Question, index: Int): String =
    ctext("q.${q.id}.r$index", q.pairsRight[index])

/** 情景题的某个子问题：题干。 */
@Composable
fun qStepPrompt(q: com.ascent.climb.data.Question, stepIndex: Int): String {
    val step = q.steps[stepIndex]
    return ctext("q.${q.id}.s$stepIndex.p", step.prompt)
}

/** 情景题的某个子问题：选项。 */
@Composable
fun qStepOption(q: com.ascent.climb.data.Question, stepIndex: Int, index: Int): String {
    val step = q.steps[stepIndex]
    return ctext("q.${q.id}.s$stepIndex.o$index", step.options[index])
}

/** 情景题的某个子问题：解析。 */
@Composable
fun qStepExplanation(q: com.ascent.climb.data.Question, stepIndex: Int): String {
    val step = q.steps[stepIndex]
    return ctext("q.${q.id}.s$stepIndex.e", step.explanation)
}
