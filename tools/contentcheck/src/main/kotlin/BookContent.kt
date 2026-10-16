package com.ascent.climb.data

/**
 * 由实体书扫描资料整理而来的题库内容。
 *
 * ⚠️ 本文件由 `tools/qbank/build.mjs` 自动生成，请勿手动编辑。
 *     源文件在 `import/` 目录（Markdown），编辑那里的内容后重新运行：
 *
 *         node tools/qbank/build.mjs
 *
 * 本次来源：（暂无）
 */

/** 新增课程，按营地 id 分组。 */
val BOOK_LESSONS: Map<String, List<Lesson>> = emptyMap()

/** 追加到已有课程的题目，按课程 id 分组。 */
val BOOK_QUESTIONS: Map<String, List<Question>> = emptyMap()
